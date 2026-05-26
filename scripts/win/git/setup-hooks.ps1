$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
Push-Location $repoRoot
try {
    git config core.hooksPath .githooks
    Write-Host 'Git hooks path set to .githooks'
}
finally {
    Pop-Location
}
