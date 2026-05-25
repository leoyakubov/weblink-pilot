#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
backend_dir="$repo_root/backend"

(
  cd "$backend_dir"
  ./mvnw -pl app,url,analytics -am spotless:check checkstyle:check spotbugs:check
)
