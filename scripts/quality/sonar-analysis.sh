#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repo_root/backend"

load_env_file() {
  local env_file="$1"
  if [[ -f "$env_file" ]]; then
    set -a
    # shellcheck disable=SC1090
    . "$env_file"
    set +a
  fi
}

load_env_file "$repo_root/backend/.env"
load_env_file "$repo_root/infra/sonar/.env"

if [[ -z "${SONAR_TOKEN:-}" ]]; then
  read -r -s -p "Enter Sonar token: " SONAR_TOKEN
  printf '\n'
fi

if [[ -z "${SONAR_TOKEN:-}" ]]; then
  echo 'SONAR_TOKEN is required.' >&2
  exit 1
fi

SONAR_HOST_URL="${SONAR_HOST_URL:-http://localhost:9001}"

./mvnw -Pci clean install sonar:sonar -Dsonar.token="$SONAR_TOKEN" -Dsonar.host.url="$SONAR_HOST_URL"
