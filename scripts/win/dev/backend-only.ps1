$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
$backendDir = Join-Path $repoRoot 'backend'
. (Join-Path $repoRoot 'scripts/win/lib/common.ps1')

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw 'java is not available on PATH. Install Java 25 before running this script.'
}

$resolvedJavaHome = Resolve-JavaHome
if ($resolvedJavaHome) {
    $env:JAVA_HOME = $resolvedJavaHome
}

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

    & $mvnw -Pdev -f (Join-Path $backendDir 'application/pom.xml') spring-boot:run 2>&1 | Out-Host
}
finally {
    Pop-Location
}
