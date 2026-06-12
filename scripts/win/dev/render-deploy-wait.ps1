$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
. (Join-Path $repoRoot 'scripts/win/lib/common.ps1')
$envFile = Join-Path $repoRoot 'infra/.env.local'

if (Test-Path -LiteralPath $envFile) {
    Import-DotEnv -Path $envFile
}

$apiBase = if ([string]::IsNullOrWhiteSpace($env:RENDER_API_BASE_URL)) { 'https://api.render.com/v1' } else { $env:RENDER_API_BASE_URL }
$apiKey = $env:RENDER_API_KEY
$serviceId = $env:RENDER_BACKEND_SERVICE_ID
$baselineDeployId = $env:BASELINE_DEPLOY_ID
$attempt = 1
$maxAttempts = if ([string]::IsNullOrWhiteSpace($env:RENDER_DEPLOY_WAIT_ATTEMPTS)) { 60 } else { [int]$env:RENDER_DEPLOY_WAIT_ATTEMPTS }
$delaySeconds = if ([string]::IsNullOrWhiteSpace($env:RENDER_DEPLOY_WAIT_DELAY_SECONDS)) { 10 } else { [int]$env:RENDER_DEPLOY_WAIT_DELAY_SECONDS }

if ([string]::IsNullOrWhiteSpace($apiKey)) {
    throw 'RENDER_API_KEY is required.'
}

if ([string]::IsNullOrWhiteSpace($serviceId)) {
    throw 'RENDER_BACKEND_SERVICE_ID is required.'
}

if ($serviceId -match '^(http|https)://') {
    throw 'RENDER_BACKEND_SERVICE_ID must be the Render service ID (for example srv-...), not the public URL.'
}

function Get-LatestDeployIdFromResponse {
    param([Parameter(Mandatory = $true)][string]$Response)

    try {
        $payload = $Response | ConvertFrom-Json
    }
    catch {
        return $null
    }

    $items = @()
    if ($payload -is [System.Array]) {
        $items = $payload
    }
    elseif ($payload.PSObject.Properties.Name -contains 'deploys') {
        $items = @($payload.deploys)
    }
    elseif ($payload.PSObject.Properties.Name -contains 'items') {
        $items = @($payload.items)
    }
    elseif ($payload.PSObject.Properties.Name -contains 'data') {
        $items = @($payload.data)
    }
    elseif ($payload.PSObject.Properties.Name -contains 'result') {
        $items = @($payload.result)
    }
    elseif ($payload.PSObject.Properties.Name -contains 'deployments') {
        $items = @($payload.deployments)
    }
    elseif ($payload.PSObject.Properties.Name -contains 'service') {
        $service = $payload.service
        if ($service.PSObject.Properties.Name -contains 'deploys') {
            $items = @($service.deploys)
        }
        elseif ($service.PSObject.Properties.Name -contains 'items') {
            $items = @($service.items)
        }
    }

    foreach ($item in $items) {
        if ($null -eq $item) { continue }
        if ($item.PSObject.Properties.Name -contains 'deploy' -and $item.deploy -isnot [string] -and $item.deploy) {
            $item = $item.deploy
        }

        if ($item.PSObject.Properties.Name -contains 'id' -and $item.id) {
            return [string]$item.id
        }

        if ($item.PSObject.Properties.Name -contains 'deployId' -and $item.deployId) {
            return [string]$item.deployId
        }
    }

    return $null
}

function Get-LatestDeployStatusFromResponse {
    param([Parameter(Mandatory = $true)][string]$Response)

    try {
        $payload = $Response | ConvertFrom-Json
    }
    catch {
        return $null
    }

    $items = @()
    if ($payload -is [System.Array]) {
        $items = $payload
    }
    elseif ($payload.PSObject.Properties.Name -contains 'deploys') {
        $items = @($payload.deploys)
    }
    elseif ($payload.PSObject.Properties.Name -contains 'items') {
        $items = @($payload.items)
    }
    elseif ($payload.PSObject.Properties.Name -contains 'data') {
        $items = @($payload.data)
    }
    elseif ($payload.PSObject.Properties.Name -contains 'result') {
        $items = @($payload.result)
    }
    elseif ($payload.PSObject.Properties.Name -contains 'deployments') {
        $items = @($payload.deployments)
    }
    elseif ($payload.PSObject.Properties.Name -contains 'service') {
        $service = $payload.service
        if ($service.PSObject.Properties.Name -contains 'deploys') {
            $items = @($service.deploys)
        }
        elseif ($service.PSObject.Properties.Name -contains 'items') {
            $items = @($service.items)
        }
    }

    foreach ($item in $items) {
        if ($null -eq $item) { continue }
        if ($item.PSObject.Properties.Name -contains 'deploy' -and $item.deploy -isnot [string] -and $item.deploy) {
            $item = $item.deploy
        }

        if ($item.PSObject.Properties.Name -contains 'status' -and $item.status) {
            return [string]$item.status
        }

        if ($item.PSObject.Properties.Name -contains 'statusText' -and $item.statusText) {
            return [string]$item.statusText
        }
    }

    return $null
}

while ($attempt -le $maxAttempts) {
    try {
        $response = (& curl.exe --silent --show-error --fail --header "Authorization: Bearer $apiKey" --header 'Accept: application/json' "$apiBase/services/$serviceId/deploys").Trim()
        $latestDeployId = Get-LatestDeployIdFromResponse -Response $response
        $status = Get-LatestDeployStatusFromResponse -Response $response
        $statusLabel = if ([string]::IsNullOrWhiteSpace($status)) { 'unknown' } else { $status }

        if ([string]::IsNullOrWhiteSpace($latestDeployId)) {
            Write-Host 'Backend Render deploy response did not include a deploy id'
        }
        elseif (-not [string]::IsNullOrWhiteSpace($baselineDeployId) -and $latestDeployId -eq $baselineDeployId) {
            Write-Host "Backend Render latest deploy is still the previous one ($latestDeployId, status $statusLabel)"
        }
        else {
            switch ($status) {
                'live' {
                    Write-Host "Backend deployment is live on Render ($latestDeployId)" -ForegroundColor Green
                    return
                }
                'failed' { throw "Backend deployment ended with status 'failed' ($latestDeployId)" }
                'update_failed' { throw "Backend deployment ended with status 'update_failed' ($latestDeployId)" }
                'canceled' { throw "Backend deployment ended with status 'canceled' ($latestDeployId)" }
                'deactivated' { throw "Backend deployment ended with status 'deactivated' ($latestDeployId)" }
                Default {
                    Write-Host "Backend Render deploy status: $statusLabel ($latestDeployId)"
                }
            }
        }
    }
    catch {
        Write-Host "Waiting for backend deployment status from Render (attempt $attempt/$maxAttempts)..."
    }

    $attempt++
    Start-Sleep -Seconds $delaySeconds
}

throw 'Backend deployment did not become live on Render'
