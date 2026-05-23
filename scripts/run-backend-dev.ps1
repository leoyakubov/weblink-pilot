$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$backendDir = Join-Path $repoRoot 'backend'
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw 'java is not available on PATH. Install Java 21 before running this script.'
}

$mvnw = Join-Path $backendDir 'mvnw.cmd'
if (-not (Test-Path $mvnw)) {
    throw "Maven wrapper not found at $mvnw"
}

Set-Location $backendDir
& $mvnw -pl shared-contracts,url-module,analytics-module,application -am install -DskipTests
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

& $mvnw -f (Join-Path $backendDir 'application/pom.xml') spring-boot:run
