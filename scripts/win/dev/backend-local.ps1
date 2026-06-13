$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
$backendDir = Join-Path $repoRoot 'backend'
$composeFile = Join-Path $repoRoot 'infra/docker-compose.yml'
. (Join-Path $repoRoot 'scripts/win/lib/common.ps1')
$envFile = Join-Path $repoRoot 'backend/.env.local'
$smtpEnvFile = Join-Path $repoRoot 'backend/.env.smtp.local'

$previousJavaHome = $env:JAVA_HOME
$javaHome = Resolve-JavaHome -RepositoryRoot $repoRoot
if (-not $javaHome) {
    throw 'No compatible Java home found for backend-local.'
}
$env:JAVA_HOME = $javaHome
$previousJavaToolOptions = Enter-JavaSecurityOverride -JavaHome $javaHome

Import-DotEnv $envFile
if (Test-Path $smtpEnvFile) {
    Import-DotEnv $smtpEnvFile
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'Docker is not available on PATH. Install Docker Desktop and make sure the daemon is running.'
}

if (-not (Test-Path -LiteralPath $composeFile)) {
    throw "Docker Compose file not found at $composeFile"
}

Write-BoxHeader 'Starting Mailpit for local backend:'
Write-Host '  - mailpit      SMTP catcher on port 1025, inbox UI on port 8025' -ForegroundColor DarkYellow
Write-Host ''

Push-Location $repoRoot
try {
    docker compose -p weblink-pilot -f $composeFile up -d mailpit
}
finally {
    Pop-Location
}

Push-Location $backendDir
try {
    & .\mvnw.cmd -pl application -am package -DskipTests 2>&1 | Out-Host
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }

    $env:SPRING_PROFILES_ACTIVE = 'local'
    try {
        & (Join-Path $javaHome 'bin\java.exe') -jar (Join-Path $backendDir 'application/target/application-0.1.0-SNAPSHOT.jar')
    }
    finally {
        Remove-Item Env:SPRING_PROFILES_ACTIVE -ErrorAction SilentlyContinue
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
