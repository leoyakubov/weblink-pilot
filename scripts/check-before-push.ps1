$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent $PSScriptRoot
$backendTestScript = Join-Path $repoRoot 'scripts\backend\test-backend.ps1'
$backendCoverageScript = Join-Path $repoRoot 'scripts\backend\check-coverage.ps1'
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

    Write-Host $Label
    $logFile = [System.IO.Path]::GetTempFileName()
    try {
        & $ScriptBlock 2>&1 | Tee-Object -FilePath $logFile | Out-Host
        $capturedText = Get-Content -Raw -LiteralPath $logFile
        $exitCode = $LASTEXITCODE
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
        default { 'DarkYellow' }
    }

    if ($Details) {
        Write-Host ("- {0}: {1} {2}" -f $Label, $Status, $Details) -ForegroundColor $color
    } else {
        Write-Host ("- {0}: {1}" -f $Label, $Status) -ForegroundColor $color
    }
}

$results = [ordered]@{
    'backend tests' = $null
    'backend coverage' = $null
    'frontend tests' = $null
    'frontend coverage' = $null
    'frontend build' = $null
}

$results['backend tests'] = Invoke-Check 'Running backend tests...' { & $backendTestScript }
if ($results['backend tests'].ExitCode -eq 0) {
    $results['backend coverage'] = Invoke-Check 'Running backend coverage gate...' { & $backendCoverageScript }
}

if ($results['backend coverage'].ExitCode -eq 0) {
    $results['frontend tests'] = Invoke-Check 'Running frontend tests...' { & $frontendTestScript }
}

if ($results['frontend tests'].ExitCode -eq 0) {
    $results['frontend coverage'] = Invoke-Check 'Running frontend coverage gate...' { & $frontendCoverageScript }
}

if ($results['frontend coverage'].ExitCode -eq 0) {
    Push-Location $frontendDir
    try {
        $results['frontend build'] = Invoke-Check 'Building frontend...' { npm run build }
    }
    finally {
        Pop-Location
    }
}

$backendTestSummary = if ($results['backend tests']) { Get-TestSummary -Output $results['backend tests'].Output } else { [ordered]@{} }
$backendCoverageSummary = if ($results['backend coverage']) { Get-BackendCoverageSummary } else { [ordered]@{} }
$frontendTestSummary = if ($results['frontend tests']) { Get-TestSummary -Output $results['frontend tests'].Output } else { [ordered]@{} }
$frontendCoverageSummaryValues = if ($results['frontend coverage']) { Get-FrontendCoverageSummary } else { [ordered]@{} }

Write-Host ''
Write-Host 'Summary:'

if ($results['backend tests']) {
    $backendTestsDetails = ''
    if ($backendTestSummary.Count -gt 0) {
        $backendTestsDetails = "($($backendTestSummary.testsRun) run, $($backendTestSummary.testsFailed) failed, $($backendTestSummary.testsErrors) errors, $($backendTestSummary.testsSkipped) skipped)"
    }
    Write-SummaryLine -Label 'backend tests' -Status ($(if ($results['backend tests'].ExitCode -eq 0) { 'PASS' } else { 'FAIL' })) -Details (
        $backendTestsDetails
    )
} else {
    Write-SummaryLine -Label 'backend tests' -Status 'SKIPPED'
}

if ($results['backend coverage']) {
    $backendCoverageDetails = ''
    if ($backendCoverageSummary.Count -gt 0) {
        $backendCoverageDetails = "($($backendCoverageSummary.linePct)% lines, $($backendCoverageSummary.branchPct)% branches)"
    }
    Write-SummaryLine -Label 'backend coverage' -Status ($(if ($results['backend coverage'].ExitCode -eq 0) { 'PASS' } else { 'FAIL' })) -Details (
        $backendCoverageDetails
    )
} else {
    Write-SummaryLine -Label 'backend coverage' -Status 'SKIPPED'
}

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

if ($results['frontend build']) {
    Write-SummaryLine -Label 'frontend build' -Status ($(if ($results['frontend build'].ExitCode -eq 0) { 'PASS' } else { 'FAIL' }))
} else {
    Write-SummaryLine -Label 'frontend build' -Status 'SKIPPED'
}

if ($results.Values | Where-Object { $_ -and -not $_.Succeeded }) {
    exit 1
}
