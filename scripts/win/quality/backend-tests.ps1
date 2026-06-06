$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
$backendDir = Join-Path $repoRoot 'backend'
. (Join-Path $repoRoot 'scripts/win/lib/common.ps1')

$previousJavaHome = $env:JAVA_HOME
$javaHome = Resolve-JavaHome -RepositoryRoot $repoRoot
if (-not $javaHome) {
    throw 'No compatible Java home found for backend tests.'
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
        $commandOutput = & cmd.exe /c "call .\mvnw.cmd -pl application -am clean test" 2>&1 | Tee-Object -Variable commandOutput | Out-Host
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
        if ($nativeCommandPreference) {
            $PSNativeCommandUseErrorActionPreference = $previousNativeCommandPreference
        }
    }
    $combinedOutput = ($commandOutput | ForEach-Object { $_.ToString() }) -join "`n"
    $exitCode = $LASTEXITCODE
    if ($exitCode -eq 0 -and $combinedOutput -match '(?m)^\[ERROR\]|BUILD FAILURE|Non-resolvable import POM|Could not transfer artifact|Could not resolve') {
        $exitCode = 1
    }
    if ($exitCode -ne 0) {
        exit $exitCode
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
