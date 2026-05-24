#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
if [[ -f "$repo_root/.env.local" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$repo_root/.env.local"
  set +a
fi
cd "$repo_root/backend"
./mvnw -pl app -am test
