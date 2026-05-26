#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
compose_file="$repo_root/infra/docker-compose.yml"
reset='\033[0m'
cyan='\033[36m'
green='\033[32m'
yellow='\033[33m'
magenta='\033[35m'
blue='\033[34m'
white='\033[37m'
red='\033[31m'

print_box() {
  local title="$1"
  local width=84
  local inner_width=$((width - 4))
  local border
  border="||$(printf '%0.s=' $(seq 1 $((width - 4))))||"
  printf '%s\n' "$border"
  printf '|| %-*s ||\n' "$inner_width" "$title"
  printf '%s\n\n' "$border"
}

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

print_box "Starting docker full stack:"
print_service "$red" "postgres" "PostgreSQL database on port 5432"
print_service "$magenta" "redis" "Redis cache/session store on port 6379"
print_service "$green" "backend" "Spring Boot API on port 8080"
print_service "$yellow" "prometheus" "Metrics scraper on port 9090"
print_service "$blue" "grafana" "Dashboards on port 3001"
print_service "$cyan" "frontend" "Vue SPA on port 8081"
printf '\n'

cd "$repo_root"
exec docker compose -f "$compose_file" up --build
