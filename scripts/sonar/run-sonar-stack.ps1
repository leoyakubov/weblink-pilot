$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
Push-Location $repoRoot
try {
    docker compose -f infra/sonar/docker-compose.yml up -d

    Write-Host 'SonarQube is starting at http://localhost:9001'
    Write-Host 'Default login: admin / admin'
    Write-Host 'After signing in, create a token and run the analysis from backend/'
    Write-Host '  .\mvnw.cmd clean verify sonar:sonar -Dsonar.token=<your-token>'
}
finally {
    Pop-Location
}
