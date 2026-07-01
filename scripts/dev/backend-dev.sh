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

# Host-run dev uses the Docker Mailpit service on localhost. Keep this after
# loading backend/.env so demo SMTP credentials cannot leak into dev runs.
export APP_AUTH_MAIL_DELIVERY_MODE="SMTP"
export SPRING_MAIL_HOST="localhost"
export SPRING_MAIL_PORT="1025"
export SPRING_MAIL_USERNAME=""
export SPRING_MAIL_PASSWORD=""
export SPRING_MAIL_SMTP_AUTH="false"
export SPRING_MAIL_SMTP_STARTTLS="false"
export APP_AI_PROVIDER="ollama"
export APP_AI_OLLAMA_BASE_URL="${APP_AI_OLLAMA_BASE_URL:-http://localhost:11434}"
export APP_AI_OLLAMA_MODEL="${APP_AI_OLLAMA_MODEL:-llama3.2:1b}"
export APP_AI_OLLAMA_TIMEOUT="${APP_AI_OLLAMA_TIMEOUT:-60s}"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is not available on PATH. Install Docker Desktop and make sure the daemon is running." >&2
  exit 1
fi

if [[ ! -f "$compose_file" ]]; then
  echo "Docker Compose file not found at $compose_file" >&2
  exit 1
fi

cd "$repo_root"
docker compose -p weblink-pilot -f "$compose_file" up -d postgres redis mailpit ollama
docker exec weblink-pilot-ollama ollama pull "$APP_AI_OLLAMA_MODEL"
docker exec weblink-pilot-ollama ollama run "$APP_AI_OLLAMA_MODEL" "Return only: OK" >/dev/null

if ! command -v java >/dev/null 2>&1; then
  echo "java is not available on PATH. Install Java 21 before running this script." >&2
  exit 1
fi

if [[ ! -f "$mvnw" ]]; then
  echo "Maven wrapper not found at $mvnw" >&2
  exit 1
fi

cd "$backend_dir"
"$mvnw" -Pdev -pl shared,links,analytics,application -am install -DskipTests
"$mvnw" -Pdev -f "$backend_dir/application/pom.xml" spring-boot:run
