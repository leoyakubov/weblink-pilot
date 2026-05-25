#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
if [[ -f "$repo_root/.env.local" ]]; then
  while IFS= read -r line || [[ -n "$line" ]]; do
    trimmed="${line#"${line%%[![:space:]]*}"}"
    trimmed="${trimmed%"${trimmed##*[![:space:]]}"}"
    if [[ -z "$trimmed" || "$trimmed" == \#* ]]; then
      continue
    fi

    name="${trimmed%%=*}"
    value="${trimmed#*=}"
    if [[ -z "$name" || "$name" == "$value" ]]; then
      continue
    fi

    value="${value#"${value%%[![:space:]]*}"}"
    value="${value%"${value##*[![:space:]]}"}"
    if [[ ( "$value" == \"*\" && "$value" == *\" ) || ( "$value" == \'*\' && "$value" == *\' ) ]]; then
      value="${value:1:${#value}-2}"
    fi

    if [[ -z "${!name:-}" ]]; then
      export "$name=$value"
    fi
  done < "$repo_root/.env.local"
fi

backend_health_url="${RENDER_HEALTH_URL:-}"
frontend_smoke_url="${FRONTEND_SMOKE_URL:-}"

if [ -z "$backend_health_url" ]; then
  echo 'RENDER_HEALTH_URL is not set.'
  exit 1
fi

if [ -z "$frontend_smoke_url" ]; then
  echo 'FRONTEND_SMOKE_URL is not set.'
  exit 1
fi

check_smoke() {
  local name="$1"
  local url="$2"
  local pattern="$3"

  printf 'Checking %s at %s...\n' "$name" "$url"
  local response
  response="$(curl --fail --silent --show-error --max-time 30 "$url")"

  if ! printf '%s' "$response" | grep -Eq "$pattern"; then
    printf 'Smoke check failed for %s.\n' "$name"
    exit 1
  fi
}

check_smoke 'backend health' "$backend_health_url" '"status"[[:space:]]*:[[:space:]]*"UP"'
check_smoke 'frontend home' "$frontend_smoke_url" '<title>[[:space:]]*WebLinkPilot[[:space:]]*</title>'

printf '%s\n' 'Deployment smoke checks passed.'
