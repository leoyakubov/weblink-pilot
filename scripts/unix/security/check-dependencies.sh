#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"

print_box() {
  local title="$1"
  local width=62
  local inner_width=$((width - 4))
  local border
  border="||$(printf '%0.s=' $(seq 1 $((width - 4))))||"
  printf '\n%s\n' "$border"
  printf '|| %-*s ||\n' "$inner_width" "$title"
  printf '%s\n\n' "$border"
}

print_status() {
  local label="$1"
  local status="$2"
  local badge
  case "$status" in
    PASS) badge='[PASS]' ;;
    FAIL) badge='[FAIL]' ;;
    SKIPPED) badge='[SKIP]' ;;
    *) badge="[$status]" ;;
  esac
  printf '  %s %s\n' "$badge" "$label"
}

backend_status='SKIPPED'
frontend_status='SKIPPED'

print_box "Running backend dependency checks..."
if "$repo_root/scripts/unix/security/backend-vulnerabilities.sh"; then
  backend_status='PASS'
else
  backend_status='FAIL'
fi

if [ "$backend_status" = 'PASS' ]; then
  print_box "Running frontend dependency checks..."
  if "$repo_root/scripts/unix/security/frontend-vulnerabilities.sh"; then
    frontend_status='PASS'
  else
    frontend_status='FAIL'
  fi
fi

print_box "Summary"
print_status 'backend vulnerabilities' "$backend_status"
printf '\n'
print_status 'frontend vulnerabilities' "$frontend_status"

if [ "$backend_status" != 'PASS' ] || [ "$frontend_status" != 'PASS' ]; then
  exit 1
fi
