#!/usr/bin/env python3
"""
load_balancer.py — A lightweight HTTP *redirect* load balancer for the
StarHarbour Payments Service.

Instead of proxying traffic, this LB computes which backend should handle a
request and answers the client with an HTTP 307 Temporary Redirect whose
`Location` header points at the chosen backend. The client then re-sends the
*exact same* request (method + body preserved) to that backend.

Why 307 and not 302?
    A 302 makes most clients rewrite POST -> GET and drop the body. The Payments
    API is POST-with-a-JSON-body, so 302 would silently corrupt requests. 307
    guarantees the method and body are preserved on the retry.

Features:
    * Static backend registry from the BACKENDS env var (comma-separated).
    * Thread-safe in-memory active/dead state.
    * Round-robin selection over the *currently active* backends.
    * Background active health-checker that probes every backend on an interval
      and flips them ALIVE <-> DEAD based on consecutive successes/failures.
    * `GET /lb/status` returns JSON describing the pool (does not get balanced).

Run:
    BACKENDS=http://localhost:8081,http://localhost:8082,http://localhost:8083 \
        python3 load_balancer.py

Config (all via environment variables):
    BACKENDS         comma-separated backend base URLs
                     (default: http://localhost:8081,:8082,:8083)
    LB_PORT          port the LB listens on            (default: 8080)
    HEALTH_PATH      path probed on each backend       (default: /api/v1/payments?storeId=health-check)
    HEALTH_INTERVAL  seconds between health sweeps      (default: 5)
    HEALTH_TIMEOUT   per-probe timeout in seconds       (default: 2)
    FAIL_THRESHOLD   consecutive fails -> DEAD          (default: 2)
    RISE_THRESHOLD   consecutive oks   -> ALIVE         (default: 1)
"""

import json
import logging
import os
import threading
import time
import urllib.error
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%H:%M:%S",
)
log = logging.getLogger("lb")


def _env_int(name: str, default: int) -> int:
    try:
        return int(os.environ.get(name, default))
    except ValueError:
        return default


DEFAULT_BACKENDS = "http://localhost:8081,http://localhost:8082,http://localhost:8083"
BACKENDS = [b.strip().rstrip("/") for b in os.environ.get("BACKENDS", DEFAULT_BACKENDS).split(",") if b.strip()]
LB_PORT = _env_int("LB_PORT", 8080)
HEALTH_PATH = os.environ.get("HEALTH_PATH", "/api/v1/payments?storeId=health-check")
HEALTH_INTERVAL = _env_int("HEALTH_INTERVAL", 5)
HEALTH_TIMEOUT = _env_int("HEALTH_TIMEOUT", 2)
FAIL_THRESHOLD = _env_int("FAIL_THRESHOLD", 2)
RISE_THRESHOLD = _env_int("RISE_THRESHOLD", 1)


class BackendRegistry:
    """Thread-safe registry of backends with round-robin selection."""

    def __init__(self, urls):
        self._lock = threading.Lock()
        # All backends start ALIVE; the first health sweep corrects this quickly.
        self._state = {
            url: {"alive": True, "fails": 0, "oks": 0} for url in urls
        }
        self._counter = 0

    def next_backend(self):
        """Return the next active backend (round robin), or None if none alive."""
        with self._lock:
            active = [u for u, s in self._state.items() if s["alive"]]
            if not active:
                return None
            target = active[self._counter % len(active)]
            self._counter += 1
            return target

    def record(self, url, ok):
        """Update health state from a probe result; logs on transitions."""
        with self._lock:
            s = self._state[url]
            if ok:
                s["fails"] = 0
                s["oks"] += 1
                if not s["alive"] and s["oks"] >= RISE_THRESHOLD:
                    s["alive"] = True
                    log.info("Backend %s is back ALIVE", url)
            else:
                s["oks"] = 0
                s["fails"] += 1
                if s["alive"] and s["fails"] >= FAIL_THRESHOLD:
                    s["alive"] = False
                    log.warning("Backend %s marked DEAD after %d consecutive failures", url, s["fails"])

    def snapshot(self):
        with self._lock:
            return {
                url: {"alive": s["alive"], "fails": s["fails"], "oks": s["oks"]}
                for url, s in self._state.items()
            }


registry = BackendRegistry(BACKENDS)


def probe(url: str) -> bool:
    """GET the health path on a backend; True iff it answers 2xx in time."""
    health_url = url + HEALTH_PATH
    try:
        req = urllib.request.Request(health_url, method="GET")
        with urllib.request.urlopen(req, timeout=HEALTH_TIMEOUT) as resp:
            return 200 <= resp.status < 300
    except Exception:
        return False


def health_loop():
    """Background thread: probe every backend on a fixed interval, forever."""
    log.info(
        "Health checker started: probing %d backend(s) every %ds (timeout %ds)",
        len(BACKENDS), HEALTH_INTERVAL, HEALTH_TIMEOUT,
    )
    while True:
        for url in BACKENDS:
            registry.record(url, probe(url))
        time.sleep(HEALTH_INTERVAL)


class LBHandler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    # Quieter default logging — we log routing decisions ourselves.
    def log_message(self, fmt, *args):
        return

    def _drain_body(self):
        """Consume any request body so the connection stays usable."""
        length = int(self.headers.get("Content-Length", 0) or 0)
        if length:
            self.rfile.read(length)

    def _status_page(self):
        body = json.dumps(
            {"port": LB_PORT, "backends": registry.snapshot()}, indent=2
        ).encode()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _redirect(self):
        self._drain_body()
        target = registry.next_backend()
        if target is None:
            log.error("503 — no healthy backends for %s %s", self.command, self.path)
            self.send_response(503)
            msg = b'{"error":"no healthy backends available"}'
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(msg)))
            self.end_headers()
            self.wfile.write(msg)
            return

        location = target + self.path
        log.info("307 %-6s %s -> %s", self.command, self.path, location)
        self.send_response(307)
        self.send_header("Location", location)
        self.send_header("Content-Length", "0")
        self.end_headers()

    def _handle(self):
        # Observability endpoint is served directly, never balanced.
        if self.path.split("?", 1)[0] == "/lb/status":
            self._drain_body()
            self._status_page()
            return
        self._redirect()

    do_GET = _handle
    do_POST = _handle
    do_PUT = _handle
    do_DELETE = _handle
    do_PATCH = _handle


def main():
    if not BACKENDS:
        raise SystemExit("No backends configured. Set the BACKENDS env var.")

    threading.Thread(target=health_loop, daemon=True).start()

    server = ThreadingHTTPServer(("0.0.0.0", LB_PORT), LBHandler)
    log.info("Redirect LB listening on http://localhost:%d", LB_PORT)
    log.info("Backends: %s", ", ".join(BACKENDS))
    log.info("Status:   http://localhost:%d/lb/status", LB_PORT)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        log.info("Shutting down.")
        server.shutdown()


if __name__ == "__main__":
    main()
