$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$frontendDir = Join-Path $repoRoot 'frontend'

if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
    throw 'Node.js is not available on PATH. Install Node 22+ before running this script.'
}

if (-not (Get-Command npm -ErrorAction SilentlyContinue)) {
    throw 'npm is not available on PATH. Install Node.js 22+ before running this script.'
}

Set-Location $frontendDir
if (-not (Test-Path (Join-Path $frontendDir 'node_modules'))) {
    npm install
}

npm run dev
