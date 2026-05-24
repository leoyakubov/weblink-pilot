$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$composeFile = Join-Path $repoRoot 'infra/docker-compose.yml'

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'Docker is not available on PATH. Install Docker Desktop and make sure the daemon is running.'
}

if (-not (Test-Path $composeFile)) {
    throw "Docker Compose file not found at $composeFile"
}

Set-Location $repoRoot
docker compose -f $composeFile up --build
