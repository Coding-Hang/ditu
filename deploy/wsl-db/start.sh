#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

if [[ ! -f .env ]]; then
  cp .env.example .env
fi
set -a
# shellcheck disable=SC1091
source .env
set +a

if ! docker version >/dev/null 2>&1; then
  echo "Docker daemon is not running. Trying to start WSL Docker service."
  if command -v service >/dev/null 2>&1; then
    if [[ -n "${SUDO_PASSWORD:-}" ]]; then
      printf '%s\n' "$SUDO_PASSWORD" | sudo -S service docker start || true
    else
      sudo service docker start || true
    fi
  fi
fi

if ! docker version >/dev/null 2>&1; then
  echo "Docker daemon is still unavailable."
  echo "Run one of:"
  echo "  cd /data/install/db && ./start.sh"
  echo "  cd /data/install/db && SUDO_PASSWORD='your-wsl-sudo-password' ./start.sh"
  echo "  sudo service docker start"
  exit 1
fi

docker compose --env-file .env -f docker-compose.yml up -d

echo "Waiting for PostgreSQL health check..."
for i in $(seq 1 60); do
  status="$(docker inspect -f '{{.State.Health.Status}}' ditu-postgres 2>/dev/null || true)"
  if [[ "$status" == "healthy" ]]; then
    break
  fi
  sleep 2
done

docker compose --env-file .env -f docker-compose.yml ps
echo
echo "PostgreSQL started: localhost:${POSTGRES_PORT:-5432}"
echo "JDBC: jdbc:postgresql://localhost:${POSTGRES_PORT:-5432}/${POSTGRES_DB:-ditu}"
