$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$backendDir = Join-Path $repoRoot 'backend'
$envFile = Join-Path $repoRoot '.env.local'
Push-Location $backendDir

function Import-LocalEnvFile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        return
    }

    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if ($trimmed.Length -eq 0 -or $trimmed.StartsWith('#')) {
            continue
        }

        $separatorIndex = $trimmed.IndexOf('=')
        if ($separatorIndex -lt 1) {
            continue
        }

        $name = $trimmed.Substring(0, $separatorIndex).Trim()
        $value = $trimmed.Substring($separatorIndex + 1).Trim()
        if ($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'")))) {
            $value = $value.Substring(1, $value.Length - 2)
        }

        if ($name -eq 'SONAR_TOKEN' -and -not [string]::IsNullOrWhiteSpace($value)) {
            $env:SONAR_TOKEN = $value
        }
    }
}

Import-LocalEnvFile -Path $envFile

if ([string]::IsNullOrWhiteSpace($env:SONAR_TOKEN)) {
    $env:SONAR_TOKEN = Read-Host 'Enter Sonar token'
}

if ([string]::IsNullOrWhiteSpace($env:SONAR_TOKEN)) {
    throw 'SONAR_TOKEN is required.'
}

$sonarArg = "-Dsonar.token=$($env:SONAR_TOKEN)"
try {
    & .\mvnw.cmd -Pci clean verify sonar:sonar $sonarArg
}
finally {
    Pop-Location
}
