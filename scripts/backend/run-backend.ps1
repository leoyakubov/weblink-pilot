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
& $mvnw -pl app -am package -DskipTests
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
java -jar (Join-Path $backendDir 'app/target/app-0.1.0-SNAPSHOT.jar')
