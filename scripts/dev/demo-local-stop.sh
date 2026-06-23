#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
compose_file="$repo_root/infra/docker-compose.yml"
log_dir="${TMPDIR:-/tmp}/weblink-pilot-demo-local"

if command -v docker >/dev/null 2>&1 && [[ -f "$compose_file" ]]; then
  docker compose -p weblink-pilot -f "$compose_file" stop postgres redis >/dev/null 2>&1 || true
fi

for pid_file in "$log_dir/backend.pid" "$log_dir/frontend.pid"; do
  if [[ -f "$pid_file" ]]; then
    pid="$(tr -d '[:space:]' < "$pid_file")"
    if [[ "$pid" =~ ^[0-9]+$ ]] && kill -0 "$pid" >/dev/null 2>&1; then
      kill "$pid" >/dev/null 2>&1 || true
    fi
    rm -f "$pid_file"
  fi
done

echo "Demo local services stopped."
