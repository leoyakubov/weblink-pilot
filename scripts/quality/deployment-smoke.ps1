$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

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
