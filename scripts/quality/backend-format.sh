#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
backend_dir="$repo_root/backend"

(
  cd "$backend_dir"
  ./mvnw spotless:apply
)
