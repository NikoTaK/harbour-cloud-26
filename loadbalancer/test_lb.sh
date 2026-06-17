#!/usr/bin/env bash
#
# test_lb.sh — drive the redirect load balancer and prove round-robin behaviour.
#
# Phase A: send 10 POSTs to the LB WITHOUT following redirects, and tally the
#          `Location` headers. This proves the LB itself round-robins across the
#          active backends (works even before backends are reachable).
#
# Phase B: send 10 POSTs WITH `-L` (follow redirects). curl re-issues the exact
#          POST + body to the backend (307 semantics). This proves end-to-end
#          delivery through the redirect.
#
# Usage:   ./test_lb.sh [LB_URL]
#          LB_URL defaults to http://localhost:8080
set -euo pipefail

LB_URL="${1:-http://localhost:8080}"
ENDPOINT="$LB_URL/api/v1/payments"

body() {
  printf '{"coffeeType":"LATTE","price":3.50,"currency":"EUR","loyaltyCardId":"card-%s"}' "$1"
}

echo "== Phase A: 10 POSTs to LB, tallying 307 Location headers (no -L) =="
tmp="$(mktemp)"
for i in $(seq 1 10); do
  curl -sS -o /dev/null -D - -X POST "$ENDPOINT" \
    -H "Content-Type: application/json" \
    -H "Store-Id: store-test" \
    -H "Idempotency-Key: lb-test-$i" \
    --data "$(body "$i")" \
  | awk '/^HTTP\//{print "  status:", $2} /^[Ll]ocation:/{u=$2; sub(/\r$/,"",u); print "  ->", u}'
done | tee "$tmp"

echo
echo "Distribution of redirect targets:"
grep -i '^  ->' "$tmp" | awk '{print $2}' | sed 's/\/api.*//' | sort | uniq -c
rm -f "$tmp"

echo
echo "== Phase B: 10 POSTs to LB WITH -L (follow redirect to backend) =="
created=0
for i in $(seq 1 10); do
  code="$(curl -sS -L -o /dev/null -w '%{http_code}' -X POST "$ENDPOINT" \
    -H "Content-Type: application/json" \
    -H "Store-Id: store-test" \
    -H "Idempotency-Key: lb-follow-$i" \
    --data "$(body "$i")")"
  echo "  request $i -> final HTTP $code"
  [[ "$code" == "201" || "$code" == "200" ]] && created=$((created + 1))
done
echo
echo "$created/10 requests completed successfully end-to-end."
