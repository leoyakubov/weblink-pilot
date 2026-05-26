#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
frontend_dir="$repo_root/frontend"

(
  cd "$frontend_dir"
  npm run format
)
