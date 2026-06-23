#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
source "$repo_root/scripts/lib/common.sh"
frontend_dir="$repo_root/frontend"

if ! command -v node >/dev/null 2>&1; then
  echo "Node.js is not available on PATH. Install Node.js 24.16.0 LTS before running this script." >&2
  exit 1
fi

if ! command -v npm >/dev/null 2>&1; then
  echo "npm is not available on PATH. Install npm 11.13.0 before running this script." >&2
  exit 1
fi

export VITE_API_BASE_URL="${VITE_API_BASE_URL:-http://localhost:8080/api/v1}"

if command -v lsof >/dev/null 2>&1; then
  while IFS= read -r pid; do
    if [[ -n "$pid" && "$pid" != "$$" ]]; then
      kill "$pid" >/dev/null 2>&1 || true
    fi
  done < <(lsof -ti tcp:8081 2>/dev/null || true)
fi

cd "$frontend_dir"
ensure_frontend_dependencies "$frontend_dir"

npm run build
npm run preview -- --host 0.0.0.0 --port 8081 --strictPort
