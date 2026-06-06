$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
. (Join-Path $repoRoot 'scripts/win/lib/common.ps1')
$backendScript = Join-Path $repoRoot 'scripts\win\security\backend-vulnerabilities.ps1'
$frontendScript = Join-Path $repoRoot 'scripts\win\security\frontend-vulnerabilities.ps1'

function Write-SummaryLine {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Label,

        [Parameter(Mandatory = $true)]
        [string]$Status
    )

    $color = Get-StatusColor $Status
    $badge = Get-StatusBadge $Status
    Write-Host ("  {0} {1}" -f $badge, $Label) -ForegroundColor $color
}

function Invoke-PowerShellChildScript {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ScriptPath
    )

    (Invoke-PowerShellScript -ScriptPath $ScriptPath).ExitCode
}

$results = [ordered]@{
    'backend vulnerabilities' = 'SKIPPED'
    'frontend vulnerabilities' = 'SKIPPED'
}

Write-BoxHeader 'Running backend dependency checks...'
$backendExitCode = Invoke-PowerShellChildScript -ScriptPath $backendScript
if ($backendExitCode -eq 0) {
    $results['backend vulnerabilities'] = 'PASS'
} else {
    $results['backend vulnerabilities'] = 'FAIL'
}

if ($backendExitCode -eq 0) {
    Write-BoxHeader 'Running frontend dependency checks...'
    $frontendExitCode = Invoke-PowerShellChildScript -ScriptPath $frontendScript
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
