#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repo_root/backend"

if [[ -f "$repo_root/.env.local" ]]; then
  set -a
  # shellcheck disable=SC1091
  . "$repo_root/.env.local"
  set +a
fi

if [[ -z "${SONAR_TOKEN:-}" ]]; then
  read -r -s -p "Enter Sonar token: " SONAR_TOKEN
  printf '\n'
fi

if [[ -z "${SONAR_TOKEN:-}" ]]; then
  echo 'SONAR_TOKEN is required.' >&2
  exit 1
fi

./mvnw -Pci -ntp clean install sonar:sonar -Dsonar.token="$SONAR_TOKEN"
