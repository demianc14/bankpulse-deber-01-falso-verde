#!/usr/bin/env bash
set -euo pipefail

base_url="${BANKPULSE_URL:-http://localhost:8080}"
payment_key="smoke-$(date +%s)"

echo "[1/4] Verificando APIs"
curl -fsS "$base_url/health/payments" >/dev/null
curl -fsS "$base_url/health/audit" >/dev/null

echo "[2/4] Creando pago idempotente"
curl -fsS -X POST "$base_url/api/payments" \
  -H 'Content-Type: application/json' \
  -H "X-Idempotency-Key: $payment_key" \
  -d '{"account":"EC-4242","amount":27.50,"currency":"USD"}' >/tmp/bankpulse-payment.json

echo "[3/4] Reintentando la misma solicitud"
curl -fsS -X POST "$base_url/api/payments" \
  -H 'Content-Type: application/json' \
  -H "X-Idempotency-Key: $payment_key" \
  -d '{"account":"EC-4242","amount":27.50,"currency":"USD"}' >/tmp/bankpulse-payment-retry.json
cmp /tmp/bankpulse-payment.json /tmp/bankpulse-payment-retry.json

echo "[4/4] Esperando publicación del outbox"
for _ in $(seq 1 20); do
  pending="$(curl -fsS "$base_url/api/outbox" | sed -n 's/.*"pending":\([0-9]*\).*/\1/p')"
  [ "$pending" = "0" ] && break
  sleep 1
done
[ "${pending:-1}" = "0" ]
curl -fsS "$base_url/api/audit" | grep -q 'PAYMENT_CREATED'
echo "Smoke test OK"
