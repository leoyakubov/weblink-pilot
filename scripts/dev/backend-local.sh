#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
backend_dir="$repo_root/backend"
mvnw="$backend_dir/mvnw"
compose_file="$repo_root/infra/docker-compose.yml"

if [[ -f "$repo_root/backend/.env" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$repo_root/backend/.env"
  set +a
fi

# Local development must always use Mailpit, even if backend/.env contains Brevo
# credentials for demo-local runs.
export APP_AUTH_MAIL_DELIVERY_MODE="SMTP"
export SPRING_MAIL_HOST="localhost"
export SPRING_MAIL_PORT="1025"
export SPRING_MAIL_USERNAME=""
export SPRING_MAIL_PASSWORD=""
export SPRING_MAIL_SMTP_AUTH="false"
export SPRING_MAIL_SMTP_STARTTLS="false"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is not available on PATH. Install Docker Desktop and make sure the daemon is running." >&2
  exit 1
fi

if [[ ! -f "$compose_file" ]]; then
  echo "Docker Compose file not found at $compose_file" >&2
  exit 1
fi

printf '\n'
printf '||============================================================================================================||\n'
printf '|| Starting Mailpit for local backend:                                                                         ||\n'
printf '||============================================================================================================||\n\n'
printf '  - %-12s %s\n' "mailpit" "SMTP catcher on port 1025, inbox UI on port 8025"
printf '\n'

cd "$repo_root"
docker compose -p weblink-pilot -f "$compose_file" up -d mailpit

if ! command -v java >/dev/null 2>&1; then
  echo "java is not available on PATH. Install Java 21 or newer before running this script." >&2
  exit 1
fi

if [[ ! -f "$mvnw" ]]; then
  echo "Maven wrapper not found at $mvnw" >&2
  exit 1
fi

cd "$backend_dir"
"$mvnw" -pl application -am package -DskipTests
export SPRING_PROFILES_ACTIVE=local
exec java -jar "$backend_dir/application/target/application-0.1.0-SNAPSHOT.jar"
