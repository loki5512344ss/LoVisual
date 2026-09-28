#!/bin/bash
# One-time first-install setup for LoVisual on this VDS. Safe to re-run.
# Assumes: repo already cloned to /opt/lovisual, backend/.env already created (chmod 600).
set -euo pipefail

REPO_DIR=/opt/lovisual

echo "==> nginx site configs"
cp "$REPO_DIR/backend/deploy/nginx-visual.loki-code.dev.conf" /etc/nginx/sites-available/visual.loki-code.dev
cp "$REPO_DIR/backend/deploy/nginx-bekend.loki-code.dev.conf" /etc/nginx/sites-available/bekend.loki-code.dev
ln -sf /etc/nginx/sites-available/visual.loki-code.dev /etc/nginx/sites-enabled/visual.loki-code.dev
ln -sf /etc/nginx/sites-available/bekend.loki-code.dev /etc/nginx/sites-enabled/bekend.loki-code.dev
mkdir -p /var/www/visual.loki-code.dev/html
nginx -t
systemctl reload nginx

echo "==> certbot"
certbot --nginx -d visual.loki-code.dev --non-interactive --agree-tos -m mauhhhh7@gmail.com --redirect || true
certbot --nginx -d bekend.loki-code.dev --non-interactive --agree-tos -m mauhhhh7@gmail.com --redirect || true

echo "==> First deploy"
"$REPO_DIR/backend/deploy/deploy.sh"
