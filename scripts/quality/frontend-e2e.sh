#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
source "$repo_root/scripts/lib/common.sh"
cd "$repo_root/frontend"
ensure_frontend_dependencies "$repo_root/frontend"
npm run test:e2e
