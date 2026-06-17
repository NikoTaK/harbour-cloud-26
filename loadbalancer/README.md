# Redirect Load Balancer

A lightweight, zero-dependency HTTP **redirect** load balancer for the StarHarbour
Payments Service. It does not proxy traffic — it answers each request with an
**HTTP 307 Temporary Redirect** pointing at a chosen backend, and the client
re-sends the request there.

## Why 307 (not 302)

A `302` makes most HTTP clients rewrite `POST` → `GET` and **drop the request
body**. The Payments API is `POST` with a JSON body, so `302` would silently
corrupt every write. `307 Temporary Redirect` guarantees the client repeats the
**exact** method and body against the `Location` URL. This LB always uses 307.

## Design

| Concern            | Choice                                                                 |
|--------------------|------------------------------------------------------------------------|
| Service discovery  | Static list from `BACKENDS` env var (comma-separated base URLs)        |
| State              | In-memory, thread-safe `BackendRegistry` (active/dead per backend)     |
| Algorithm          | Round robin via an atomic counter over the *currently active* backends |
| Health checks      | Background thread probes `GET /api/v1/payments?storeId=health-check`    |
| Mark DEAD          | After `FAIL_THRESHOLD` (default 2) consecutive failures / 5xx / timeout |
| Mark ALIVE         | After `RISE_THRESHOLD` (default 1) consecutive successes                |
| No backends alive  | LB returns `503 Service Unavailable`                                    |

## Configuration (environment variables)

| Var               | Default                                              | Meaning                          |
|-------------------|------------------------------------------------------|----------------------------------|
| `BACKENDS`        | `http://localhost:8081,:8082,:8083`                  | comma-separated backend URLs     |
| `LB_PORT`         | `8080`                                               | port the LB listens on           |
| `HEALTH_PATH`     | `/api/v1/payments?storeId=health-check`              | path probed on each backend      |
| `HEALTH_INTERVAL` | `5`                                                  | seconds between health sweeps     |
| `HEALTH_TIMEOUT`  | `2`                                                  | per-probe timeout (seconds)       |
| `FAIL_THRESHOLD`  | `2`                                                  | consecutive fails → DEAD          |
| `RISE_THRESHOLD`  | `1`                                                  | consecutive oks → ALIVE           |

No `pip install` needed — pure Python 3 standard library.

---

## Verification & Testing

### 1. Start 3 backend instances

The Payments app reads its port from Spring Boot, so override `--server.port`
per instance. We also disable the Docker Compose / Toxiproxy sidecar (not needed
here) so each instance boots without Docker. Run each in its own terminal:

```bash
# Terminal 1
SPRING_DOCKER_COMPOSE_ENABLED=false ./gradlew bootRun --args='--server.port=8081'

# Terminal 2
SPRING_DOCKER_COMPOSE_ENABLED=false ./gradlew bootRun --args='--server.port=8082'

# Terminal 3
SPRING_DOCKER_COMPOSE_ENABLED=false ./gradlew bootRun --args='--server.port=8083'
```

Wait for each to log `Started CloudApplication`.

### 2. Start the load balancer

```bash
# Terminal 4 — defaults to backends 8081/8082/8083, listens on 8080
python3 loadbalancer/load_balancer.py
```

You should see the health checker mark all three `ALIVE`. Inspect the pool:

```bash
curl -s http://localhost:8080/lb/status | python3 -m json.tool
```

### 3. Send traffic and verify round-robin

```bash
chmod +x loadbalancer/test_lb.sh
./loadbalancer/test_lb.sh
```

**Expected:**
- **Phase A** — every response is `307`, and the `Location` tally is split evenly
  across the three backends (≈ `4  8081`, `3  8082`, `3  8083`).
- **Phase B** — `-L` follows each redirect to a backend; all 10 finish `201`/`200`,
  printing `10/10 requests completed successfully end-to-end.`

### 4. Kill a backend and verify failover

Stop the instance on `:8082` (Ctrl-C its terminal). Within ~10 s (2 failed
sweeps × 5 s) the LB logs:

```
Backend http://localhost:8082 marked DEAD after 2 consecutive failures
```

Re-run `./loadbalancer/test_lb.sh`. Now the `Location` tally only covers `8081`
and `8083` (≈ 5/5), and all requests still succeed. Restart `:8082` and the next
sweep logs `Backend http://localhost:8082 is back ALIVE`, returning it to the pool.

### 5. All backends down → 503

Stop all three instances. The LB logs `503 — no healthy backends` and:

```bash
curl -s -o /dev/null -w '%{http_code}\n' -X POST http://localhost:8080/api/v1/payments
# 503
```
