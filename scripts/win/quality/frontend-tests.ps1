$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
$frontendDir = Join-Path $repoRoot 'frontend'
Push-Location $frontendDir
try {
    npm test -- --run
}
finally {
    Pop-Location
}
