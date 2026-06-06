$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
$backendDir = Join-Path $repoRoot 'backend'
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw 'java is not available on PATH. Install Java 25 before running this script.'
}

$mvnw = Join-Path $backendDir 'mvnw.cmd'
if (-not (Test-Path $mvnw)) {
    throw "Maven wrapper not found at $mvnw"
}

function Import-DotEnv {
    param([string]$Path)

    if (-not (Test-Path $Path)) {
        return
    }

    Get-Content -LiteralPath $Path | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith('#')) {
            return
        }

        $index = $line.IndexOf('=')
        if ($index -lt 1) {
            return
        }

        $name = $line.Substring(0, $index).Trim()
        $value = $line.Substring($index + 1).Trim()
        if ($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'")))) {
            $value = $value.Substring(1, $value.Length - 2)
        }

        Set-Item -Path "Env:$name" -Value $value
    }
}

Import-DotEnv (Join-Path $repoRoot '.env.local')

Push-Location $backendDir
try {
    & $mvnw -Pdev -pl shared-contracts,url,analytics,app -am install -DskipTests 2>&1 | Out-Host
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }

    & $mvnw -Pdev -f (Join-Path $backendDir 'app/pom.xml') spring-boot:run 2>&1 | Out-Host
}
finally {
    Pop-Location
}
