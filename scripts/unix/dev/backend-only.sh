#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
backend_dir="$repo_root/backend"
mvnw="$backend_dir/mvnw"

if [[ -f "$repo_root/.env.local" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$repo_root/.env.local"
  set +a
fi

if ! command -v java >/dev/null 2>&1; then
  echo "java is not available on PATH. Install Java 25 before running this script." >&2
  exit 1
fi

if [[ ! -f "$mvnw" ]]; then
  echo "Maven wrapper not found at $mvnw" >&2
  exit 1
fi

cd "$backend_dir"
"$mvnw" -Pdev -pl shared-contracts,url,analytics,app -am install -DskipTests
"$mvnw" -Pdev -f "$backend_dir/app/pom.xml" spring-boot:run
