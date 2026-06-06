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

Push-Location $backendDir
try {
    Import-DotEnv -Path $envFile

    if ([string]::IsNullOrWhiteSpace($env:SONAR_TOKEN)) {
        $env:SONAR_TOKEN = Read-Host 'Enter Sonar token'
    }

    if ([string]::IsNullOrWhiteSpace($env:SONAR_TOKEN)) {
        throw 'SONAR_TOKEN is required.'
    }

    if ([string]::IsNullOrWhiteSpace($env:SONAR_HOST_URL)) {
        $env:SONAR_HOST_URL = 'http://localhost:9001'
    }

    $sonarArg = "-Dsonar.token=$($env:SONAR_TOKEN)"
    $sonarHostArg = "-Dsonar.host.url=$($env:SONAR_HOST_URL)"
    & .\mvnw.cmd -Pci clean install sonar:sonar $sonarArg $sonarHostArg 2>&1 | Out-Host
}
finally {
    Pop-Location
}
