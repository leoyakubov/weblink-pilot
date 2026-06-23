#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
backend_dir="$repo_root/backend"
compose_file="$repo_root/infra/docker-compose.yml"
mvnw="$backend_dir/mvnw"

if [[ -f "$repo_root/backend/.env.local" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$repo_root/backend/.env.local"
  set +a
fi

if [[ -f "$repo_root/backend/.env.smtp.local" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$repo_root/backend/.env.smtp.local"
  set +a
fi

if [[ ! -f "$compose_file" ]]; then
  echo "Docker Compose file not found at $compose_file" >&2
  exit 1
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is not available on PATH. Install Docker Desktop and make sure the daemon is running." >&2
  exit 1
fi

if ! command -v java >/dev/null 2>&1; then
  echo "java is not available on PATH. Install Java 21 or newer before running this script." >&2
  exit 1
fi

if [[ ! -f "$mvnw" ]]; then
  echo "Maven wrapper not found at $mvnw" >&2
  exit 1
fi

export SPRING_PROFILES_ACTIVE=demo
export SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL:-jdbc:postgresql://localhost:5432/weblinkpilot}"
export SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME:-weblinkpilot}"
export SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-weblinkpilot}"
export REDIS_URL="${REDIS_URL:-redis://localhost:6379}"
export APP_PUBLIC_BASE_URL="${APP_PUBLIC_BASE_URL:-http://localhost:8080}"
export FRONTEND_BASE_URL="${FRONTEND_BASE_URL:-http://localhost:8081}"
export APP_CORS_ALLOWED_ORIGIN_PATTERNS="${APP_CORS_ALLOWED_ORIGIN_PATTERNS:-http://localhost:8081,http://127.0.0.1:8081}"
export APP_AUTH_MAIL_DELIVERY_MODE="SMTP"
export SPRING_MAIL_HOST="smtp-relay.brevo.com"
export SPRING_MAIL_PORT="587"
export SPRING_MAIL_SMTP_AUTH="true"
export SPRING_MAIL_SMTP_STARTTLS="true"

if [[ -z "${SPRING_MAIL_USERNAME:-}" || -z "${SPRING_MAIL_PASSWORD:-}" ]]; then
  echo "Brevo SMTP username/password are required for demo mode." >&2
  echo "Copy backend/.env.smtp.local.example to backend/.env.smtp.local and fill in the credentials." >&2
  exit 1
fi

printf '\n'
printf '||============================================================================================================||\n'
printf '|| Starting backend in demo mode:                                                                             ||\n'
printf '||============================================================================================================||\n\n'
printf '  - %-12s %s\n' "postgres" "PostgreSQL database on port 5432"
printf '  - %-12s %s\n' "redis" "Redis cache/session store on port 6379"
printf '  - %-12s %s\n' "backend" "Spring Boot API on port 8080 in demo profile"
printf '  - %-12s %s\n' "mail" "Brevo SMTP for demo verification and reset links"
printf '\n'

cd "$repo_root"
docker compose -p weblink-pilot -f "$compose_file" up -d postgres redis

cd "$backend_dir"
"$mvnw" -pl application -am package -DskipTests
exec java -jar "$backend_dir/application/target/application-0.1.0-SNAPSHOT.jar"
