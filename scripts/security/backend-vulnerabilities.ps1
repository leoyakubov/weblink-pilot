$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$backendDir = Join-Path $repoRoot 'backend'

Push-Location $backendDir
try {
    $previousErrorActionPreference = $ErrorActionPreference
    $nativeCommandPreference = Get-Variable -Name PSNativeCommandUseErrorActionPreference -Scope Global -ErrorAction SilentlyContinue
    $previousNativeCommandPreference = if ($nativeCommandPreference) { [bool] $nativeCommandPreference.Value } else { $false }
    $ErrorActionPreference = 'Continue'
    if ($nativeCommandPreference) {
        $PSNativeCommandUseErrorActionPreference = $false
    }
    try {
        & .\mvnw.cmd -U -Pci dependency-check:aggregate 2>&1 | Out-Host
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
