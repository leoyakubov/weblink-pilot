#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
smoke_target="${SMOKE_TARGET:-local}"
smoke_target="${smoke_target,,}"
smoke_check="${SMOKE_CHECK:-all}"
smoke_check="${smoke_check,,}"
checks_to_run=()

case "$smoke_check" in
  all)
    checks_to_run=('backend' 'frontend')
    ;;
  backend|frontend)
    checks_to_run=("$smoke_check")
    ;;
  *)
    echo "SMOKE_CHECK must be one of: all, backend, frontend. Got '$smoke_check'."
    exit 1
    ;;
esac

load_env_file() {
  local env_file="$1"
  if [[ ! -f "$env_file" ]]; then
    return
  fi

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
  done < "$env_file"
}

if [[ "$smoke_target" == demo ]]; then
  load_env_file "$repo_root/infra/.env.local"
fi

backend_health_url="${RENDER_HEALTH_URL:-}"
frontend_smoke_url="${FRONTEND_SMOKE_URL:-}"

case "$smoke_check" in
  all)
    if [ "$smoke_target" = 'demo' ]; then
      printf '\n=== Deployment smoke tests starting ===\n\n'
    else
      printf '\n=== Local smoke tests starting ===\n\n'
    fi
    ;;
  backend)
    if [ "$smoke_target" = 'demo' ]; then
      printf '\n=== Backend deployment smoke tests starting ===\n\n'
    else
      printf '\n=== Backend local smoke tests starting ===\n\n'
    fi
    ;;
  frontend)
    if [ "$smoke_target" = 'demo' ]; then
      printf '\n=== Frontend deployment smoke tests starting ===\n\n'
    else
      printf '\n=== Frontend local smoke tests starting ===\n\n'
    fi
    ;;
esac

if [[ " ${checks_to_run[*]} " == *" backend "* ]]; then
  if [ "$smoke_target" = 'demo' ]; then
    if [ -z "$backend_health_url" ]; then
      echo 'RENDER_HEALTH_URL is not set.'
      exit 1
    fi
  else
    backend_health_url='http://localhost:8080/actuator/health'
  fi
fi

if [[ " ${checks_to_run[*]} " == *" frontend "* ]]; then
  if [ "$smoke_target" = 'demo' ]; then
    if [ -z "$frontend_smoke_url" ]; then
      echo 'FRONTEND_SMOKE_URL is not set.'
      exit 1
    fi
  else
    frontend_smoke_url='http://localhost:8081'
  fi
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
    if [ "$smoke_target" != 'demo' ] && [[ "$url" == http://localhost:* ]]; then
      printf 'Local smoke hint: start the Docker stack first with ./scripts/unix/dev/docker-full-stack.sh\n'
    fi
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

if [[ " ${checks_to_run[*]} " == *" backend "* ]]; then
  check_smoke 'backend health' "$backend_health_url" '"status"[[:space:]]*:[[:space:]]*"UP"'
fi

if [ "$smoke_check" = 'all' ]; then
  printf '\n\n'
fi

if [[ " ${checks_to_run[*]} " == *" frontend "* ]]; then
  check_smoke 'frontend home' "$frontend_smoke_url" 'id="app"'
fi

case "$smoke_check" in
  backend)
    if [ "$smoke_target" = 'demo' ]; then
      printf '\n=== Backend deployment smoke tests passed ===\n'
    else
      printf '\n=== Backend local smoke tests passed ===\n'
    fi
    ;;
  frontend)
    if [ "$smoke_target" = 'demo' ]; then
      printf '\n=== Frontend deployment smoke tests passed ===\n'
    else
      printf '\n=== Frontend local smoke tests passed ===\n'
    fi
    ;;
  *)
    if [ "$smoke_target" = 'demo' ]; then
      printf '\n=== Deployment smoke tests passed ===\n'
    else
      printf '\n=== Local smoke tests passed ===\n'
    fi
    ;;
esac
