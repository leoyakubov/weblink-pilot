$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
$frontendDir = Join-Path $repoRoot 'frontend'

Push-Location $frontendDir
try {
    $previousErrorActionPreference = $ErrorActionPreference
    $nativeCommandPreference = Get-Variable -Name PSNativeCommandUseErrorActionPreference -Scope Global -ErrorAction SilentlyContinue
    $previousNativeCommandPreference = if ($nativeCommandPreference) { [bool] $nativeCommandPreference.Value } else { $false }
    $ErrorActionPreference = 'Continue'
    if ($nativeCommandPreference) {
        $PSNativeCommandUseErrorActionPreference = $false
    }
    try {
        & npm run lint 2>&1 | Out-Host
        if ($LASTEXITCODE -eq 0) {
            & npm run format:check 2>&1 | Out-Host
        }
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
        if ($nativeCommandPreference) {
            $PSNativeCommandUseErrorActionPreference = $previousNativeCommandPreference
        }
    }
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}
finally {
    Pop-Location
}
