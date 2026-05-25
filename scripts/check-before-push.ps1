$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent $PSScriptRoot
$backendStyleScript = Join-Path $repoRoot 'scripts\backend\check-style.ps1'
$backendCoverageScript = Join-Path $repoRoot 'scripts\backend\check-coverage.ps1'
$secretScanScript = Join-Path $repoRoot 'scripts\check-secrets.ps1'
$frontendStyleScript = Join-Path $repoRoot 'scripts\frontend\check-style.ps1'
$frontendTestScript = Join-Path $repoRoot 'scripts\frontend\test-frontend.ps1'
$frontendCoverageScript = Join-Path $repoRoot 'scripts\frontend\check-coverage.ps1'
$frontendDir = Join-Path $repoRoot 'frontend'
$backendCoverageCsv = Join-Path $repoRoot 'backend\coverage\target\site\jacoco-aggregate\jacoco.csv'
$frontendCoverageSummary = Join-Path $frontendDir 'coverage\coverage-summary.json'

function Invoke-Check {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Label,

        [Parameter(Mandatory = $true)]
        [scriptblock]$ScriptBlock
    )

    Write-BoxHeader $Label
    $logFile = [System.IO.Path]::GetTempFileName()
    try {
        & $ScriptBlock 2>&1 | Tee-Object -FilePath $logFile | Out-Host
        $pipelineSucceeded = $?
        $capturedText = Get-Content -Raw -LiteralPath $logFile
        $exitCode = $LASTEXITCODE
        if (-not $pipelineSucceeded -and $exitCode -eq 0) {
            $exitCode = 1
        }
        if ($exitCode -eq 0 -and $capturedText -match 'BUILD FAILURE|Coverage checks have not been met|Failed to execute goal') {
            $exitCode = 1
        }
    }
    finally {
        Remove-Item -LiteralPath $logFile -ErrorAction SilentlyContinue
    }
    return [pscustomobject]@{
        ExitCode = $exitCode
        Succeeded = ($exitCode -eq 0)
        Output = $capturedText
    }
}

function Write-BoxHeader {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Title
    )

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
        [string]$ScriptPath,

        [string[]]$Arguments = @()
    )

    $argumentList = @(
        '-NoProfile'
        '-ExecutionPolicy'
        'Bypass'
        '-File'
        $ScriptPath
    ) + $Arguments

    & powershell.exe @argumentList
}

function Get-TestSummary {
    param([string]$Output)

    $summary = [ordered]@{}

    if ($Output -match 'Tests run:\s+(?<run>\d+), Failures:\s+(?<failures>\d+), Errors:\s+(?<errors>\d+), Skipped:\s+(?<skipped>\d+)') {
        $summary['testsRun'] = [int]$Matches.run
        $summary['testsFailed'] = [int]$Matches.failures
        $summary['testsErrors'] = [int]$Matches.errors
        $summary['testsSkipped'] = [int]$Matches.skipped
    }

    if ($Output -match 'Test Files\s+(?<files>\d+)\s+passed.*?Tests\s+(?<tests>\d+)\s+passed') {
        $summary['testFiles'] = [int]$Matches.files
        $summary['tests'] = [int]$Matches.tests
    }

    return $summary
}

function Get-BackendCoverageSummary {
    if (-not (Test-Path -LiteralPath $backendCoverageCsv)) {
        return [ordered]@{}
    }

    $rows = Import-Csv -LiteralPath $backendCoverageCsv
    $totals = [ordered]@{
        instructionMissed = 0
        instructionCovered = 0
        branchMissed = 0
        branchCovered = 0
        lineMissed = 0
        lineCovered = 0
    }

    foreach ($row in $rows) {
        $totals.instructionMissed += [int]$row.INSTRUCTION_MISSED
        $totals.instructionCovered += [int]$row.INSTRUCTION_COVERED
        $totals.branchMissed += [int]$row.BRANCH_MISSED
        $totals.branchCovered += [int]$row.BRANCH_COVERED
        $totals.lineMissed += [int]$row.LINE_MISSED
        $totals.lineCovered += [int]$row.LINE_COVERED
    }

    $totals['branchPct'] = if (($totals.branchMissed + $totals.branchCovered) -gt 0) { [math]::Round(($totals.branchCovered / ($totals.branchMissed + $totals.branchCovered)) * 100, 2) } else { 0 }
    $totals['linePct'] = if (($totals.lineMissed + $totals.lineCovered) -gt 0) { [math]::Round(($totals.lineCovered / ($totals.lineMissed + $totals.lineCovered)) * 100, 2) } else { 0 }
    return $totals
}

function Get-FrontendCoverageSummary {
    if (-not (Test-Path -LiteralPath $frontendCoverageSummary)) {
        return [ordered]@{}
    }

    $summary = Get-Content -Raw -LiteralPath $frontendCoverageSummary | ConvertFrom-Json
    return [ordered]@{
        statements = [math]::Round([double]$summary.total.statements.pct, 2)
        branches = [math]::Round([double]$summary.total.branches.pct, 2)
        functions = [math]::Round([double]$summary.total.functions.pct, 2)
        lines = [math]::Round([double]$summary.total.lines.pct, 2)
    }
}

function Write-SummaryLine {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Label,

        [Parameter(Mandatory = $true)]
        [string]$Status,

        [string]$Details = ''
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

    if ($Details) {
        Write-Host ("  {0} {1} {2}" -f $badge, $Label, $Details) -ForegroundColor $color
    } else {
        Write-Host ("  {0} {1}" -f $badge, $Label) -ForegroundColor $color
    }
}

$results = [ordered]@{
    'backend style' = $null
    'backend quality' = $null
    'secret scan' = $null
    'frontend style' = $null
    'frontend tests' = $null
    'frontend coverage' = $null
    'frontend build' = $null
}

$results['backend style'] = Invoke-Check 'Running backend style checks...' { Invoke-PowerShellScript -ScriptPath $backendStyleScript }
if ($results['backend style'].ExitCode -eq 0) {
    $results['backend quality'] = Invoke-Check 'Running backend tests and coverage...' { Invoke-PowerShellScript -ScriptPath $backendCoverageScript }
}

if ($results['backend quality'] -and $results['backend quality'].ExitCode -eq 0) {
    $results['secret scan'] = Invoke-Check 'Running secret scan...' { Invoke-PowerShellScript -ScriptPath $secretScanScript }
}

if ($results['secret scan'] -and $results['secret scan'].ExitCode -eq 0) {
    $results['frontend style'] = Invoke-Check 'Running frontend style checks...' { Invoke-PowerShellScript -ScriptPath $frontendStyleScript }
}

if ($results['frontend style'] -and $results['frontend style'].ExitCode -eq 0) {
    $results['frontend tests'] = Invoke-Check 'Running frontend tests...' { Invoke-PowerShellScript -ScriptPath $frontendTestScript }
}

if ($results['frontend tests'] -and $results['frontend tests'].ExitCode -eq 0) {
    $results['frontend coverage'] = Invoke-Check 'Running frontend coverage gate...' { Invoke-PowerShellScript -ScriptPath $frontendCoverageScript }
}

if ($results['frontend coverage'] -and $results['frontend coverage'].ExitCode -eq 0) {
    Push-Location $frontendDir
    try {
        $results['frontend build'] = Invoke-Check 'Building frontend...' { npm run build }
    }
    finally {
        Pop-Location
    }
}

$backendQualitySummary = if ($results['backend quality']) { Get-TestSummary -Output $results['backend quality'].Output } else { [ordered]@{} }
$backendCoverageSummary = if ($results['backend quality']) { Get-BackendCoverageSummary } else { [ordered]@{} }
$frontendTestSummary = if ($results['frontend tests']) { Get-TestSummary -Output $results['frontend tests'].Output } else { [ordered]@{} }
$frontendCoverageSummaryValues = if ($results['frontend coverage']) { Get-FrontendCoverageSummary } else { [ordered]@{} }

Write-BoxHeader 'Summary'

if ($results['backend style']) {
    Write-SummaryLine -Label 'backend style' -Status ($(if ($results['backend style'].ExitCode -eq 0) { 'PASS' } else { 'FAIL' }))
} else {
    Write-SummaryLine -Label 'backend style' -Status 'SKIPPED'
}

if ($results['backend quality']) {
    $backendQualityDetails = ''
    if ($backendQualitySummary.Count -gt 0 -or $backendCoverageSummary.Count -gt 0) {
        $backendTestPart = ''
        if ($backendQualitySummary.Count -gt 0) {
            $backendTestPart = "$($backendQualitySummary.testsRun) run, $($backendQualitySummary.testsFailed) failed, $($backendQualitySummary.testsErrors) errors, $($backendQualitySummary.testsSkipped) skipped"
        }
        $backendCoveragePart = ''
        if ($backendCoverageSummary.Count -gt 0) {
            $backendCoveragePart = "$($backendCoverageSummary.linePct)% lines, $($backendCoverageSummary.branchPct)% branches"
        }
        $parts = @()
        if ($backendTestPart) { $parts += $backendTestPart }
        if ($backendCoveragePart) { $parts += $backendCoveragePart }
        if ($parts.Count -gt 0) {
            $backendQualityDetails = "($($parts -join '; '))"
        }
    }
    Write-SummaryLine -Label 'backend quality' -Status ($(if ($results['backend quality'].ExitCode -eq 0) { 'PASS' } else { 'FAIL' })) -Details (
        $backendQualityDetails
    )
} else {
    Write-SummaryLine -Label 'backend quality' -Status 'SKIPPED'
}

if ($results['secret scan']) {
    Write-SummaryLine -Label 'secret scan' -Status ($(if ($results['secret scan'].ExitCode -eq 0) { 'PASS' } else { 'FAIL' }))
} else {
    Write-SummaryLine -Label 'secret scan' -Status 'SKIPPED'
}

if ($results['frontend style']) {
    Write-SummaryLine -Label 'frontend style' -Status ($(if ($results['frontend style'].ExitCode -eq 0) { 'PASS' } else { 'FAIL' }))
} else {
    Write-SummaryLine -Label 'frontend style' -Status 'SKIPPED'
}

Write-Host ''
if ($results['frontend tests']) {
    $frontendTestsDetails = ''
    if ($frontendTestSummary.Count -gt 0) {
        $frontendTestsDetails = "($($frontendTestSummary.testFiles) files, $($frontendTestSummary.tests) tests)"
    }
    Write-SummaryLine -Label 'frontend tests' -Status ($(if ($results['frontend tests'].ExitCode -eq 0) { 'PASS' } else { 'FAIL' })) -Details (
        $frontendTestsDetails
    )
} else {
    Write-SummaryLine -Label 'frontend tests' -Status 'SKIPPED'
}

Write-Host ''
if ($results['frontend coverage']) {
    $frontendCoverageDetails = ''
    if ($frontendCoverageSummaryValues.Count -gt 0) {
        $frontendCoverageDetails = "($($frontendCoverageSummaryValues.lines)% lines, $($frontendCoverageSummaryValues.branches)% branches)"
    }
    Write-SummaryLine -Label 'frontend coverage' -Status ($(if ($results['frontend coverage'].ExitCode -eq 0) { 'PASS' } else { 'FAIL' })) -Details (
        $frontendCoverageDetails
    )
} else {
    Write-SummaryLine -Label 'frontend coverage' -Status 'SKIPPED'
}

Write-Host ''
if ($results['frontend build']) {
    Write-SummaryLine -Label 'frontend build' -Status ($(if ($results['frontend build'].ExitCode -eq 0) { 'PASS' } else { 'FAIL' }))
} else {
    Write-SummaryLine -Label 'frontend build' -Status 'SKIPPED'
}

if ($results.Values | Where-Object { $_ -and -not $_.Succeeded }) {
    exit 1
}
