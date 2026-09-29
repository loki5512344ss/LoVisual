#!/bin/bash
# Boots accounts-service, configs-service and gateway on localhost for manual
# poking and scripts/e2e.sh. Restarts them if they are already running.
#
# Prerequisites (one-time):
#   cargo build -p accounts-service -p configs-service -p gateway
#   a Postgres with accounts_db + configs_db, and MinIO with the
#   lovisual-avatars-test bucket (see backend/PLAN.md, Task 2).
#   Dev-only secrets below are fine locally; production reads backend/.env.
set -euo pipefail
B=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
L=${TMPDIR:-/tmp}/lovisual-stack
mkdir -p "$L"

export JWT_SECRET="local-jwt-secret-local-jwt-secret-1234"
export INTERNAL_KEY="local-internal-key-local-internal-key-1"
export DATABASE_URL="postgres://lovisual:lovisual@localhost:5432/accounts_db"
export CONFIGS_DATABASE_URL="postgres://lovisual:lovisual@localhost:5432/configs_db"
export ACCOUNTS_GRPC_URL="http://127.0.0.1:50051"
export S3_ENDPOINT="http://localhost:9000"
export S3_BUCKET="lovisual-avatars-test"
export S3_ACCESS_KEY="minioadmin"
export S3_SECRET_KEY="minioadmin"
export COOKIE_SECURE="false"
# browser-facing avatar prefix served through the gateway (mirrors prod /api/media)
export AVATAR_PUBLIC_BASE_URL="http://127.0.0.1:8080/media"

pkill -f 'target/debug/(accounts-service|configs-service|gateway)' 2>/dev/null || true
sleep 1

PORT=8081 GRPC_PORT=50051 setsid nohup "$B/target/debug/accounts-service" >"$L/accounts.log" 2>&1 &
CONFIGS_PORT=8082 setsid nohup "$B/target/debug/configs-service" >"$L/configs.log" 2>&1 &
GATEWAY_PORT=8080 ACCOUNTS_HTTP_URL="http://127.0.0.1:8081" \
  CONFIGS_HTTP_URL="http://127.0.0.1:8082" SITE_ORIGIN="http://localhost:5173" \
  setsid nohup "$B/target/debug/gateway" >"$L/gateway.log" 2>&1 &

up() { [ "$(curl -sf "$1" 2>/dev/null || true)" = "ok" ]; }
for i in $(seq 1 60); do
  up http://127.0.0.1:8081/health && up http://127.0.0.1:8082/health && up http://127.0.0.1:8080/health && break
  sleep 0.5
done
for u in 8081 8082 8080; do
  up "http://127.0.0.1:$u/health" || { echo "port $u did not come up"; tail -5 "$L"/*.log; exit 1; }
done
echo "accounts+configs+gateway up"
