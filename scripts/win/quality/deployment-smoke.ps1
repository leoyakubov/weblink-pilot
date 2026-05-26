$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
$envFile = Join-Path $repoRoot '.env.local'
$smokeTarget = if ([string]::IsNullOrWhiteSpace($env:SMOKE_TARGET)) { 'local' } else { $env:SMOKE_TARGET.Trim().ToLowerInvariant() }
$smokeCheck = if ([string]::IsNullOrWhiteSpace($env:SMOKE_CHECK)) { 'all' } else { $env:SMOKE_CHECK.Trim().ToLowerInvariant() }
$checksToRun = if ($smokeCheck -eq 'all') { @('backend', 'frontend') } else { @($smokeCheck) }
$backendHealthUrl = ''
$frontendSmokeUrl = ''

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

if ($smokeTarget -eq 'demo') {
    Import-LocalEnvFile -Path $envFile
    if ($checksToRun -contains 'backend') {
        $backendHealthUrl = $env:RENDER_HEALTH_URL
    }
    if ($checksToRun -contains 'frontend') {
        $frontendSmokeUrl = $env:FRONTEND_SMOKE_URL
    }
} else {
    $backendHealthUrl = 'http://localhost:8080/actuator/health'
    $frontendSmokeUrl = 'http://localhost:8081'
}

if ($smokeTarget -eq 'demo') {
    if ($checksToRun -contains 'backend' -and [string]::IsNullOrWhiteSpace($backendHealthUrl)) {
        throw 'RENDER_HEALTH_URL is not set.'
    }

    if ($checksToRun -contains 'frontend' -and [string]::IsNullOrWhiteSpace($frontendSmokeUrl)) {
        throw 'FRONTEND_SMOKE_URL is not set.'
    }
}

if ($smokeCheck -notin @('all', 'backend', 'frontend')) {
    throw "SMOKE_CHECK must be one of: all, backend, frontend. Got '$smokeCheck'."
}

Write-Host ''
switch ($smokeCheck) {
    'backend' {
        if ($smokeTarget -eq 'demo') {
            Write-Host '=== Backend deployment smoke tests starting ===' -ForegroundColor Magenta
        } else {
            Write-Host '=== Backend local smoke tests starting ===' -ForegroundColor Magenta
        }
    }
    'frontend' {
        if ($smokeTarget -eq 'demo') {
            Write-Host '=== Frontend deployment smoke tests starting ===' -ForegroundColor Magenta
        } else {
            Write-Host '=== Frontend local smoke tests starting ===' -ForegroundColor Magenta
        }
    }
    Default {
        if ($smokeTarget -eq 'demo') {
            Write-Host '=== Deployment smoke tests starting ===' -ForegroundColor Magenta
        } else {
            Write-Host '=== Local smoke tests starting ===' -ForegroundColor Magenta
        }
    }
}
Write-Host ''

function Invoke-SmokeCheck {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name,

        [Parameter(Mandatory = $true)]
        [string]$Url,

        [string]$ExpectedPattern = ''
    )

    $color = if ($Name -eq 'backend health') { 'Cyan' } else { 'Green' }
    Write-Host ''
    Write-Host "Checking $Name at $Url..." -ForegroundColor $color
    $bodyFile = [System.IO.Path]::GetTempFileName()
    try {
        $statusCode = (& curl.exe --silent --show-error --location --max-time 30 --output $bodyFile --write-out '%{http_code}' $Url).Trim()
        if ($LASTEXITCODE -ne 0) {
            if ($smokeTarget -ne 'demo' -and $Url -like 'http://localhost:*') {
                Write-Host 'Local smoke hint: start the Docker stack first with .\scripts\win\dev\docker-full-stack.ps1' -ForegroundColor DarkYellow
            }
            throw "$Name request failed."
        }

        $responseText = Get-Content -Raw -LiteralPath $bodyFile
        $responseSnippet = if ($responseText.Length -gt 500) { $responseText.Substring(0, 500) } else { $responseText }
        if ([string]::IsNullOrWhiteSpace($statusCode)) {
            throw "$Name did not return an HTTP status code."
        }

        if ($Name -eq 'backend health') {
            try {
                $health = $responseText | ConvertFrom-Json
            }
            catch {
                Write-Host "$Name response:" -ForegroundColor DarkYellow
                Write-Host $responseSnippet -ForegroundColor DarkYellow
                throw "$Name response was not valid JSON."
            }

            if ([string]::IsNullOrWhiteSpace($health.status) -or $health.status.ToString().ToUpperInvariant() -ne 'UP') {
                Write-Host "$Name response:" -ForegroundColor DarkYellow
                Write-Host $responseSnippet -ForegroundColor DarkYellow
                throw "$Name response status was not UP."
            }

            Write-Host "$Name HTTP $statusCode status $($health.status)" -ForegroundColor $color
            return
        }

        if ($ExpectedPattern -and $responseText -notmatch $ExpectedPattern) {
            Write-Host "$Name response snippet:" -ForegroundColor DarkYellow
            Write-Host $responseSnippet -ForegroundColor DarkYellow
            throw "$Name response did not match the expected smoke pattern."
        }

        Write-Host "$Name HTTP $statusCode app shell present" -ForegroundColor $color
    }
    finally {
        Remove-Item -LiteralPath $bodyFile -ErrorAction SilentlyContinue
    }
}

if ($checksToRun -contains 'backend') {
    Invoke-SmokeCheck -Name 'backend health' -Url $backendHealthUrl -ExpectedPattern ''
}

if ($smokeCheck -eq 'all') {
    Write-Host ''
    Write-Host ''
}

if ($checksToRun -contains 'frontend') {
    Invoke-SmokeCheck -Name 'frontend home' -Url $frontendSmokeUrl -ExpectedPattern 'id="app"'
}

Write-Host ''
switch ($smokeCheck) {
    'backend' {
        if ($smokeTarget -eq 'demo') {
            Write-Host '=== Backend deployment smoke tests passed ===' -ForegroundColor Magenta
        } else {
            Write-Host '=== Backend local smoke tests passed ===' -ForegroundColor Magenta
        }
    }
    'frontend' {
        if ($smokeTarget -eq 'demo') {
            Write-Host '=== Frontend deployment smoke tests passed ===' -ForegroundColor Magenta
        } else {
            Write-Host '=== Frontend local smoke tests passed ===' -ForegroundColor Magenta
        }
    }
    Default {
        if ($smokeTarget -eq 'demo') {
            Write-Host '=== Deployment smoke tests passed ===' -ForegroundColor Magenta
        } else {
            Write-Host '=== Local smoke tests passed ===' -ForegroundColor Magenta
        }
    }
}
