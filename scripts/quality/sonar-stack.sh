#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repo_root"

docker compose -p weblink-pilot-sonar -f infra/sonar/docker-compose.yml up -d

printf '%s\n' 'SonarQube is starting at http://localhost:9001'
printf '%s\n' 'Default login: admin / admin'
printf '%s\n' 'After signing in, create a token and run the analysis from backend/'
printf '%s\n' '  ./mvnw -Pci clean install sonar:sonar -Dsonar.token=<your-token>'
