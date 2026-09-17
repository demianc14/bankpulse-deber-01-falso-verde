#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

if [[ ! -f .env ]]; then
  cp .env.example .env
  echo "[BankPulse] Created a private .env from the laboratory template."
fi

echo "[BankPulse] Building Java services and the operations console..."
docker compose build
echo "[BankPulse] Build complete. The stack will start automatically."
