#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

if [[ ! -f .env ]]; then
  cp .env.example .env
fi

echo "[BankPulse] Starting the distributed platform..."
docker compose up -d --wait --wait-timeout 360
docker compose ps
echo "[BankPulse] Ready. Open the forwarded port named 'BankPulse Operations Console'."
