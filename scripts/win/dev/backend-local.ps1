$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
$backendDir = Join-Path $repoRoot 'backend'
. (Join-Path $repoRoot 'scripts/win/lib/common.ps1')
$envFile = Join-Path $repoRoot '.env.local'

$resolvedJavaHome = Resolve-JavaHome
if ($resolvedJavaHome) {
    $env:JAVA_HOME = $resolvedJavaHome
}

Import-DotEnv $envFile

Push-Location $backendDir
try {
    & .\mvnw.cmd -pl application -am package -DskipTests 2>&1 | Out-Host
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }

    $env:SPRING_PROFILES_ACTIVE = 'local'
    try {
        java -jar (Join-Path $backendDir 'application/target/application-0.1.0-SNAPSHOT.jar')
    }
    finally {
        Remove-Item Env:SPRING_PROFILES_ACTIVE -ErrorAction SilentlyContinue
    }
}
finally {
    Pop-Location
}
