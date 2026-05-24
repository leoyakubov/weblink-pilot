#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
backend_dir="$repo_root/backend"
mvnw="$backend_dir/mvnw"

if ! command -v java >/dev/null 2>&1; then
  echo "java is not available on PATH. Install Java 21 before running this script." >&2
  exit 1
fi

if [[ ! -f "$mvnw" ]]; then
  echo "Maven wrapper not found at $mvnw" >&2
  exit 1
fi

cd "$backend_dir"
"$mvnw" -Pdev -pl shared-contracts,url,analytics,app -am install -DskipTests
exec "$mvnw" -Pdev -f "$backend_dir/app/pom.xml" spring-boot:run
