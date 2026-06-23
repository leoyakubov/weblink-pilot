#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
source "$repo_root/scripts/lib/common.sh"
compose_file="$repo_root/infra/monitoring/docker-compose.monitoring.yml"
reset='\033[0m'
cyan='\033[36m'
green='\033[32m'
yellow='\033[33m'
magenta='\033[35m'
blue='\033[34m'

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
  weblink-pilot-prometheus \
  weblink-pilot-grafana

print_box "Starting monitoring stack:"
print_service "$yellow" "prometheus" "Metrics scraper on port 9090"
print_service "$blue" "grafana" "Dashboards on port 3001"
printf '\n'

cd "$repo_root"
exec docker compose -p weblink-pilot-monitoring -f "$compose_file" up --build
