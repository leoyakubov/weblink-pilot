#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
source "$repo_root/scripts/unix/lib/common.sh"

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
printf '  %s %s\n' "$(status_badge "$backend_status")" 'backend vulnerabilities'
printf '\n'
printf '  %s %s\n' "$(status_badge "$frontend_status")" 'frontend vulnerabilities'

if [ "$backend_status" != 'PASS' ] || [ "$frontend_status" != 'PASS' ]; then
  exit 1
fi
