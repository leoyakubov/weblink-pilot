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

printf '\n=== Deployment smoke tests starting ===\n\n'

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
  local body_file
  body_file="$(mktemp)"

  printf 'Checking %s at %s...\n' "$name" "$url"
  local status_code
  if ! status_code="$(curl --silent --show-error --location --max-time 30 --output "$body_file" --write-out '%{http_code}' "$url")"; then
    rm -f "$body_file"
    printf 'Smoke check failed for %s.\n' "$name"
    exit 1
  fi

  local response
  response="$(cat "$body_file")"
  rm -f "$body_file"
  local response_snippet
  response_snippet="$(printf '%s' "$response" | cut -c 1-500)"

  if [ "$name" = 'backend health' ]; then
    if ! printf '%s' "$response" | grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"'; then
      printf '%s response:\n%s\n' "$name" "$response_snippet"
      printf 'Smoke check failed for %s.\n' "$name"
      exit 1
    fi

    printf '\n'
    printf '\033[36m%s HTTP %s status UP\033[0m\n' "$name" "$status_code"
    return
  fi

  if ! printf '%s' "$response" | grep -Eq "$pattern"; then
    printf '%s response snippet:\n%s\n' "$name" "$response_snippet"
    printf 'Smoke check failed for %s.\n' "$name"
    exit 1
  fi

  printf '\n'
  printf '\033[32m%s HTTP %s app shell present\033[0m\n' "$name" "$status_code"
}

check_smoke 'backend health' "$backend_health_url" '"status"[[:space:]]*:[[:space:]]*"UP"'
printf '\n\n'
check_smoke 'frontend home' "$frontend_smoke_url" 'id="app"'

printf '\n=== Deployment smoke tests passed ===\n'
