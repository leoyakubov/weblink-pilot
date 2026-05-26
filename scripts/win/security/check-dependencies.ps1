$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
$backendScript = Join-Path $repoRoot 'scripts\win\security\backend-vulnerabilities.ps1'
$frontendScript = Join-Path $repoRoot 'scripts\win\security\frontend-vulnerabilities.ps1'

function Write-BoxHeader {
    param([Parameter(Mandatory = $true)][string]$Title)

    $width = 62
    $innerWidth = $width - 4
    $titleText = " $Title "
    if ($titleText.Length -gt $innerWidth) {
        $titleText = $titleText.Substring(0, $innerWidth)
    }
    $titleLine = ('||{0}||' -f $titleText.PadRight($innerWidth))
    $borderLine = '||' + ('=' * ($width - 4)) + '||'

    Write-Host ''
    Write-Host $borderLine -ForegroundColor Cyan
    Write-Host $titleLine -ForegroundColor Cyan
    Write-Host $borderLine -ForegroundColor Cyan
    Write-Host ''
}

function Invoke-PowerShellScript {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ScriptPath
    )

    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $ScriptPath 2>&1 | Out-Host
    return $LASTEXITCODE
}

function Write-SummaryLine {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Label,

        [Parameter(Mandatory = $true)]
        [string]$Status
    )

    $color = switch ($Status) {
        'PASS' { 'Green' }
        'FAIL' { 'Red' }
        'SKIPPED' { 'DarkYellow' }
        default { 'DarkYellow' }
    }
    $badge = switch ($Status) {
        'PASS' { '[PASS]' }
        'FAIL' { '[FAIL]' }
        'SKIPPED' { '[SKIP]' }
        default { "[${Status}]" }
    }

    Write-Host ("  {0} {1}" -f $badge, $Label) -ForegroundColor $color
}

$results = [ordered]@{
    'backend vulnerabilities' = 'SKIPPED'
    'frontend vulnerabilities' = 'SKIPPED'
}

Write-BoxHeader 'Running backend dependency checks...'
$backendExitCode = Invoke-PowerShellScript -ScriptPath $backendScript
if ($backendExitCode -eq 0) {
    $results['backend vulnerabilities'] = 'PASS'
} else {
    $results['backend vulnerabilities'] = 'FAIL'
}

if ($backendExitCode -eq 0) {
    Write-BoxHeader 'Running frontend dependency checks...'
    $frontendExitCode = Invoke-PowerShellScript -ScriptPath $frontendScript
    if ($frontendExitCode -eq 0) {
        $results['frontend vulnerabilities'] = 'PASS'
    } else {
        $results['frontend vulnerabilities'] = 'FAIL'
    }
}

Write-BoxHeader 'Summary'
Write-SummaryLine -Label 'backend vulnerabilities' -Status $results['backend vulnerabilities']
Write-Host ''
Write-SummaryLine -Label 'frontend vulnerabilities' -Status $results['frontend vulnerabilities']

if ($results.Values -contains 'FAIL') {
    exit 1
}
