$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
$commonScript = Join-Path $repoRoot 'scripts/win/lib/common.ps1'
. $commonScript
$composeFile = Join-Path $repoRoot 'infra/docker-compose.monitoring.yml'

function Write-StackService {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name,

        [Parameter(Mandatory = $true)]
        [string]$Description,

        [Parameter(Mandatory = $true)]
        [string]$Color
    )

    Write-Host ("  - {0,-12} {1}" -f $Name, $Description) -ForegroundColor $Color
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'Docker is not available on PATH. Install Docker Desktop and make sure the daemon is running.'
}

if (-not (Test-Path $composeFile)) {
    throw "Docker Compose file not found at $composeFile"
}

Remove-StaleDockerContainers -ContainerNames @(
    'weblink-pilot-prometheus',
    'weblink-pilot-grafana'
)

Write-BoxHeader 'Starting monitoring stack:'
Write-StackService -Name 'prometheus' -Description 'Metrics scraper on port 9090' -Color 'Yellow'
Write-StackService -Name 'grafana' -Description 'Dashboards on port 3001' -Color 'Blue'
Write-Host ''

Push-Location $repoRoot
try {
    docker compose -p weblink-pilot -f $composeFile up --build
}
finally {
    Pop-Location
}
