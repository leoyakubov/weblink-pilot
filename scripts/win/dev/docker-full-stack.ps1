$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
$composeFile = Join-Path $repoRoot 'infra/docker-compose.yml'

function Write-BoxHeader {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Title
    )

    $width = 84
    $innerWidth = $width - 4
    $titleText = " $Title "
    if ($titleText.Length -gt $innerWidth) {
        $titleText = $titleText.Substring(0, $innerWidth)
    }
    $titleLine = ('||{0}||' -f $titleText.PadRight($innerWidth))
    $borderLine = '||' + ('=' * ($width - 4)) + '||'

    Write-Host $borderLine -ForegroundColor Cyan
    Write-Host $titleLine -ForegroundColor Cyan
    Write-Host $borderLine -ForegroundColor Cyan
    Write-Host ''
}

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

Write-BoxHeader 'Starting docker full stack:'
Write-StackService -Name 'postgres' -Description 'PostgreSQL database on port 5432' -Color 'Red'
Write-StackService -Name 'redis' -Description 'Redis cache/session store on port 6379' -Color 'Magenta'
Write-StackService -Name 'mailpit' -Description 'SMTP catcher on port 1025, inbox UI on port 8025' -Color 'DarkYellow'
Write-StackService -Name 'backend' -Description 'Spring Boot API on port 8080' -Color 'Green'
Write-StackService -Name 'prometheus' -Description 'Metrics scraper on port 9090' -Color 'Yellow'
Write-StackService -Name 'grafana' -Description 'Dashboards on port 3001' -Color 'Blue'
Write-StackService -Name 'frontend' -Description 'Vue SPA on port 8081' -Color 'Cyan'
Write-Host ''

Push-Location $repoRoot
try {
    docker compose -f $composeFile up --build
}
finally {
    Pop-Location
}
