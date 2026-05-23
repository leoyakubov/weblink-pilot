#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
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
"$mvnw" -pl shared-contracts,url-module,analytics-module,application -am install -DskipTests
exec "$mvnw" -f "$backend_dir/application/pom.xml" spring-boot:run
