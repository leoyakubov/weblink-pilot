#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
source "$repo_root/scripts/lib/common.sh"
backend_dir="$repo_root/backend"
frontend_dir="$repo_root/frontend"
compose_file="$repo_root/infra/docker-compose.yml"
log_dir="${TMPDIR:-/tmp}/weblink-pilot-demo-local"
backend_log="$log_dir/backend.log"
frontend_log="$log_dir/frontend.log"
mvnw="$backend_dir/mvnw"
reset='\033[0m'
cyan='\033[36m'
green='\033[32m'
yellow='\033[33m'
magenta='\033[35m'
red='\033[31m'

print_service() {
  local color="$1"
  local name="$2"
  local description="$3"
  printf '%b  - %-12s %s%b\n' "$color" "$name" "$description" "$reset"
}

set_default_env() {
  local name="$1"
  local value="$2"
  if [[ -z "${!name:-}" ]]; then
    export "$name=$value"
  fi
}

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is not available on PATH. Install Docker Desktop and make sure the daemon is running." >&2
  exit 1
fi

if ! command -v node >/dev/null 2>&1; then
  echo "Node.js is not available on PATH. Install Node.js 24.16.0 LTS before running this script." >&2
  exit 1
fi

if ! command -v npm >/dev/null 2>&1; then
  echo "npm is not available on PATH. Install npm 11.13.0 before running this script." >&2
  exit 1
fi

if [[ ! -f "$compose_file" ]]; then
  echo "Docker Compose file not found at $compose_file" >&2
  exit 1
fi

if [[ ! -f "$mvnw" ]]; then
  echo "Maven wrapper not found at $mvnw" >&2
  exit 1
fi

if ! command -v java >/dev/null 2>&1; then
  echo "java is not available on PATH. Install Java 21 or newer before running this script." >&2
  exit 1
fi

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

stop_script="$repo_root/scripts/dev/fullstack-demo-local-stop.sh"
if [[ -f "$stop_script" ]]; then
  bash "$stop_script" >/dev/null 2>&1 || true
fi

remove_stale_docker_containers \
  weblink-pilot-postgres \
  weblink-pilot-redis \
  weblink-pilot-mailpit \
  weblink-pilot-backend \
  weblink-pilot-frontend

set_default_env SPRING_DATASOURCE_URL "jdbc:postgresql://localhost:5432/weblinkpilot"
set_default_env SPRING_DATASOURCE_USERNAME "weblinkpilot"
set_default_env SPRING_DATASOURCE_PASSWORD "weblinkpilot"
set_default_env REDIS_URL "redis://localhost:6379"
set_default_env APP_PUBLIC_BASE_URL "http://localhost:8080"
set_default_env FRONTEND_BASE_URL "http://localhost:8081"
set_default_env APP_CORS_ALLOWED_ORIGIN_PATTERNS "http://localhost:8081,http://127.0.0.1:8081"
set_default_env VITE_API_BASE_URL "http://localhost:8080/api/v1"
set_default_env SPRING_PROFILES_ACTIVE "demo"

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

mkdir -p "$log_dir"

print_box "Starting local demo stack:"
print_service "$red" "postgres" "PostgreSQL database on port 5432"
print_service "$magenta" "redis" "Redis cache/session store on port 6379"
print_service "$green" "backend" "Spring Boot API on port 8080 in demo profile"
print_service "$cyan" "frontend" "Production-like Vue preview on port 8081"
printf '\n'

cd "$repo_root"
docker compose -p weblink-pilot -f "$compose_file" up -d postgres redis

cd "$backend_dir"
"$mvnw" -pl application -am package -DskipTests

cd "$frontend_dir"
ensure_frontend_dependencies "$frontend_dir"
npm run build

(
  cd "$backend_dir/application"
  SPRING_PROFILES_ACTIVE="$SPRING_PROFILES_ACTIVE" \
  SPRING_DATASOURCE_URL="$SPRING_DATASOURCE_URL" \
  SPRING_DATASOURCE_USERNAME="$SPRING_DATASOURCE_USERNAME" \
  SPRING_DATASOURCE_PASSWORD="$SPRING_DATASOURCE_PASSWORD" \
  REDIS_URL="$REDIS_URL" \
  APP_PUBLIC_BASE_URL="$APP_PUBLIC_BASE_URL" \
  FRONTEND_BASE_URL="$FRONTEND_BASE_URL" \
  APP_CORS_ALLOWED_ORIGIN_PATTERNS="$APP_CORS_ALLOWED_ORIGIN_PATTERNS" \
  SPRING_MAIL_HOST="$SPRING_MAIL_HOST" \
  SPRING_MAIL_PORT="$SPRING_MAIL_PORT" \
  SPRING_MAIL_USERNAME="$SPRING_MAIL_USERNAME" \
  SPRING_MAIL_PASSWORD="$SPRING_MAIL_PASSWORD" \
  SPRING_MAIL_SMTP_AUTH="$SPRING_MAIL_SMTP_AUTH" \
  SPRING_MAIL_SMTP_STARTTLS="$SPRING_MAIL_SMTP_STARTTLS" \
  nohup java -jar "$backend_dir/application/target/application-0.1.0-SNAPSHOT.jar" >"$backend_log" 2>&1 &
  echo $! >"$log_dir/backend.pid"
)

(
  cd "$frontend_dir"
  VITE_API_BASE_URL="$VITE_API_BASE_URL" nohup npm run preview -- --host 0.0.0.0 --port 8081 --strictPort >"$frontend_log" 2>&1 &
  echo $! >"$log_dir/frontend.pid"
)

echo "Demo local services are starting."
echo "  Backend log:  $backend_log"
echo "  Frontend log: $frontend_log"
echo
echo "Open the app at http://localhost:8081"
echo "Backend is available at http://localhost:8080"
echo "Mail flow uses Brevo SMTP in the demo profile."
echo "Put your Brevo SMTP username/password in backend/.env.smtp.local before starting."
echo
echo "Backend PID: $(cat "$log_dir/backend.pid")"
echo "Frontend PID: $(cat "$log_dir/frontend.pid")"
echo "Press Ctrl+C to stop this script; terminate the child processes if needed."

wait
