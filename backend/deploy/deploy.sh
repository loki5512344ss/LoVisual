#!/bin/bash
# Idempotent redeploy for LoVisual. Run as: /opt/lovisual/deploy.sh
set -euo pipefail

REPO_DIR=/opt/lovisual
FRONTEND_ROOT=/var/www/visual.loki-code.dev/html

cd "$REPO_DIR"
git pull --ff-only

echo "==> Building & (re)starting backend services"
cd "$REPO_DIR/backend"
docker compose -f docker-compose.prod.yml up -d --build

echo "==> Building frontend"
cd "$REPO_DIR/frontend"
npm install
npm run build

echo "==> Syncing frontend build to $FRONTEND_ROOT"
mkdir -p "$FRONTEND_ROOT"
rsync -a --delete dist/ "$FRONTEND_ROOT/"

echo "==> Reloading nginx"
nginx -t
systemctl reload nginx

echo "==> Done"
docker compose -f "$REPO_DIR/backend/docker-compose.prod.yml" ps
