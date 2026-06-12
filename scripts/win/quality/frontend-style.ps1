$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
$frontendDir = Join-Path $repoRoot 'frontend'

Push-Location $frontendDir
try {
    $lintOutput = & npm run lint 2>&1
    $lintExitCode = $LASTEXITCODE
    $lintOutput | Out-Host
    if ($lintExitCode -ne 0) {
        exit $lintExitCode
    }

    $formatOutput = & npm run format:check 2>&1
    $formatExitCode = $LASTEXITCODE
    $formatOutput | Out-Host
    if ($formatExitCode -ne 0) {
        exit $formatExitCode
    }
}
finally {
    Pop-Location
}
