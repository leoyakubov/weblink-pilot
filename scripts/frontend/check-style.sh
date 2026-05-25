#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
frontend_dir="$repo_root/frontend"

(
  cd "$frontend_dir"
  npm run lint
  npm run format:check
)
