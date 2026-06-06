$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
$backendDir = Join-Path $repoRoot 'backend'
. (Join-Path $repoRoot 'scripts/win/lib/common.ps1')

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw 'java is not available on PATH. Install Java 21 or newer before running this script.'
}

$previousJavaHome = $env:JAVA_HOME
$javaHome = Resolve-JavaHome -RepositoryRoot $repoRoot
if (-not $javaHome) {
    throw 'No compatible Java home found for backend-only.'
}
$env:JAVA_HOME = $javaHome
$previousJavaToolOptions = Enter-JavaSecurityOverride -JavaHome $javaHome

$mvnw = Join-Path $backendDir 'mvnw.cmd'
if (-not (Test-Path $mvnw)) {
    throw "Maven wrapper not found at $mvnw"
}

Import-DotEnv (Join-Path $repoRoot '.env.local')

Push-Location $backendDir
try {
    & $mvnw -Pdev -pl shared-contracts,links,analytics,application -am install -DskipTests 2>&1 | Out-Host
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }

    & (Join-Path $javaHome 'bin\java.exe') -jar (Join-Path $backendDir 'application/target/application-0.1.0-SNAPSHOT.jar')
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
