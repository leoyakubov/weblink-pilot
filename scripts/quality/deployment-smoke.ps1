$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$envFile = Join-Path $repoRoot '.env.local'
$backendHealthUrl = $env:RENDER_HEALTH_URL
$frontendSmokeUrl = $env:FRONTEND_SMOKE_URL

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

        if ($name -eq 'RENDER_HEALTH_URL' -and [string]::IsNullOrWhiteSpace($env:RENDER_HEALTH_URL)) {
            $env:RENDER_HEALTH_URL = $value
        }

        if ($name -eq 'FRONTEND_SMOKE_URL' -and [string]::IsNullOrWhiteSpace($env:FRONTEND_SMOKE_URL)) {
            $env:FRONTEND_SMOKE_URL = $value
        }
    }
}

Import-LocalEnvFile -Path $envFile

$backendHealthUrl = $env:RENDER_HEALTH_URL
$frontendSmokeUrl = $env:FRONTEND_SMOKE_URL

if ([string]::IsNullOrWhiteSpace($backendHealthUrl)) {
    throw 'RENDER_HEALTH_URL is not set.'
}

if ([string]::IsNullOrWhiteSpace($frontendSmokeUrl)) {
    throw 'FRONTEND_SMOKE_URL is not set.'
}

function Invoke-SmokeCheck {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name,

        [Parameter(Mandatory = $true)]
        [string]$Url,

        [Parameter(Mandatory = $true)]
        [string]$ExpectedPattern
    )

    Write-Host "Checking $Name at $Url..."
    $response = Invoke-WebRequest -Uri $Url -TimeoutSec 30 -MaximumRedirection 5
    if ($response.StatusCode -lt 200 -or $response.StatusCode -ge 300) {
        throw "$Name returned unexpected status code $($response.StatusCode)."
    }

    if ($response.Content -notmatch $ExpectedPattern) {
        throw "$Name response did not match the expected smoke pattern."
    }
}

Invoke-SmokeCheck -Name 'backend health' -Url $backendHealthUrl -ExpectedPattern '"status"\s*:\s*"UP"'
Invoke-SmokeCheck -Name 'frontend home' -Url $frontendSmokeUrl -ExpectedPattern '<title>\s*WebLinkPilot\s*</title>'

Write-Host 'Deployment smoke checks passed.'
