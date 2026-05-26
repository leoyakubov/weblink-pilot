$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
$frontendDir = Join-Path $repoRoot 'frontend'
Push-Location $frontendDir
try {
    npm test -- --run --reporter=default --reporter=json --outputFile=.vite/vitest/results.json
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}
finally {
    Pop-Location
}
