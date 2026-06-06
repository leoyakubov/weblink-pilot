$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
$backendDir = Join-Path $repoRoot 'backend'
. (Join-Path $repoRoot 'scripts/win/lib/common.ps1')

$previousJavaHome = $env:JAVA_HOME
$javaHome = Resolve-JavaHome -RepositoryRoot $repoRoot
if (-not $javaHome) {
    throw 'No compatible Java home found for backend coverage.'
}
$env:JAVA_HOME = $javaHome
$previousJavaToolOptions = Enter-JavaSecurityOverride -JavaHome $javaHome

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
        & .\mvnw.cmd -Pci clean verify spotbugs:check 2>&1 | Out-Host
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
    Exit-JavaSecurityOverride -PreviousJavaToolOptions $previousJavaToolOptions
    if ($null -ne $previousJavaHome) {
        $env:JAVA_HOME = $previousJavaHome
    } else {
        Remove-Item Env:JAVA_HOME -ErrorAction SilentlyContinue
    }
}
