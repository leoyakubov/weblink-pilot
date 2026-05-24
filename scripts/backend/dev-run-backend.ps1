$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$backendDir = Join-Path $repoRoot 'backend'
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw 'java is not available on PATH. Install Java 21 before running this script.'
}

$mvnw = Join-Path $backendDir 'mvnw.cmd'
if (-not (Test-Path $mvnw)) {
    throw "Maven wrapper not found at $mvnw"
}

Set-Location $backendDir
& $mvnw -Pdev -pl shared-contracts,url,analytics,app -am install -DskipTests
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

& $mvnw -Pdev -f (Join-Path $backendDir 'app/pom.xml') spring-boot:run
