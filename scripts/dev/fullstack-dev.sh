#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
source "$repo_root/scripts/lib/common.sh"
compose_file="$repo_root/infra/docker-compose.yml"
reset='\033[0m'
cyan='\033[36m'
green='\033[32m'
yellow='\033[33m'
magenta='\033[35m'
blue='\033[34m'
white='\033[37m'
red='\033[31m'

print_service() {
  local color="$1"
  local name="$2"
  local description="$3"
  printf '%b  - %-12s %s%b\n' "$color" "$name" "$description" "$reset"
}

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is not available on PATH. Install Docker Desktop and make sure the daemon is running." >&2
  exit 1
fi

if [[ ! -f "$compose_file" ]]; then
  echo "Docker Compose file not found at $compose_file" >&2
  exit 1
fi

remove_stale_docker_containers \
  weblink-pilot-postgres \
  weblink-pilot-redis \
  weblink-pilot-mailpit \
  weblink-pilot-ollama \
  weblink-pilot-ollama-pull \
  weblink-pilot-backend \
  weblink-pilot-frontend

print_box "Starting docker full stack:"
print_service "$red" "postgres" "PostgreSQL database on port 5432"
print_service "$magenta" "redis" "Redis cache/session store on port 6379"
print_service "$yellow" "mailpit" "SMTP catcher on port 1025, inbox UI on port 8025"
print_service "$blue" "ollama" "Local AI provider on port 11434"
print_service "$green" "backend" "Spring Boot API on port 8080"
print_service "$cyan" "frontend" "Vue SPA on port 8081"
printf '\n'

cd "$repo_root"
exec docker compose -p weblink-pilot -f "$compose_file" up --build
