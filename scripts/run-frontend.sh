#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
frontend_dir="$repo_root/frontend"

if ! command -v node >/dev/null 2>&1; then
  echo "Node.js is not available on PATH. Install Node 22+ before running this script." >&2
  exit 1
fi

if ! command -v npm >/dev/null 2>&1; then
  echo "npm is not available on PATH. Install Node.js 22+ before running this script." >&2
  exit 1
fi

cd "$frontend_dir"
if [[ ! -d node_modules ]]; then
  npm install
fi

exec npm run dev
