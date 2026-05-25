#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repo_root/backend"
./mvnw -Pci -pl shared-contracts,url,analytics,app,coverage -am clean verify
