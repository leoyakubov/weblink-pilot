#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
gitleaks_image="${GITLEAKS_IMAGE:-ghcr.io/gitleaks/gitleaks:v8.30.1}"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required to run the secret scan. Please install Docker first." >&2
  exit 1
fi

docker run --rm \
  -v "$repo_root:/repo" \
  -w /repo \
  "$gitleaks_image" \
  git --no-banner --redact .
