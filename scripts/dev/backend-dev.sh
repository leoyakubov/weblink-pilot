#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
backend_dir="$repo_root/backend"
mvnw="$backend_dir/mvnw"

if [[ -f "$repo_root/backend/.env" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$repo_root/backend/.env"
  set +a
fi

# Host-run dev uses the Docker Mailpit service on localhost. Keep this after
# loading backend/.env so demo SMTP credentials cannot leak into dev runs.
export APP_AUTH_MAIL_DELIVERY_MODE="SMTP"
export SPRING_MAIL_HOST="localhost"
export SPRING_MAIL_PORT="1025"
export SPRING_MAIL_USERNAME=""
export SPRING_MAIL_PASSWORD=""
export SPRING_MAIL_SMTP_AUTH="false"
export SPRING_MAIL_SMTP_STARTTLS="false"

if ! command -v java >/dev/null 2>&1; then
  echo "java is not available on PATH. Install Java 21 or newer before running this script." >&2
  exit 1
fi

if [[ ! -f "$mvnw" ]]; then
  echo "Maven wrapper not found at $mvnw" >&2
  exit 1
fi

cd "$backend_dir"
"$mvnw" -Pdev -pl shared-contracts,links,analytics,application -am install -DskipTests
"$mvnw" -Pdev -f "$backend_dir/application/pom.xml" spring-boot:run
