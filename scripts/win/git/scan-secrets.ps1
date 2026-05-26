$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
$gitleaksImage = if ($env:GITLEAKS_IMAGE) { $env:GITLEAKS_IMAGE } else { 'ghcr.io/gitleaks/gitleaks:v8.30.1' }

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'Docker is required to run the secret scan. Please install Docker Desktop or another Docker-compatible engine.'
}

& docker run --rm `
    -v "${repoRoot}:/repo" `
    -w /repo `
    $gitleaksImage `
    git --no-banner --redact .
