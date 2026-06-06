$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
$backendStyleScript = Join-Path $repoRoot 'scripts\win\quality\backend-style.ps1'
$backendTestsScript = Join-Path $repoRoot 'scripts\win\quality\backend-tests.ps1'
$secretScanScript = Join-Path $repoRoot 'scripts\win\git\scan-secrets.ps1'
$frontendStyleScript = Join-Path $repoRoot 'scripts\win\quality\frontend-style.ps1'
$frontendTestScript = Join-Path $repoRoot 'scripts\win\quality\frontend-tests.ps1'
$frontendDir = Join-Path $repoRoot 'frontend'

function Invoke-Check {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Label,

        [Parameter(Mandatory = $true)]
        [scriptblock]$ScriptBlock
    )

    Write-BoxHeader $Label
    try {
        $scriptResult = & $ScriptBlock
        if ($null -ne $scriptResult -and $scriptResult.PSObject.Properties.Match('ExitCode').Count -gt 0) {
            $exitCode = [int]$scriptResult.ExitCode
            $capturedText = [string]$scriptResult.Output
        } else {
            $capturedText = [string]$scriptResult
            $exitCode = $LASTEXITCODE
        }

        if ($exitCode -eq 0 -and $capturedText -match '(?m)^\[ERROR\]|BUILD FAILURE|Coverage checks have not been met|Failed to execute goal|Non-resolvable import POM|Could not transfer artifact|failed to load config|Access is denied') {
            $exitCode = 1
        }
    }
    finally {
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

    $width = 84
    $innerWidth = $width - 4
    $words = $Title -split '\s+'
    $titleLines = New-Object System.Collections.Generic.List[string]
    $current = ''
    foreach ($word in $words) {
        if ([string]::IsNullOrWhiteSpace($word)) { continue }
        if (-not $current) {
            $current = $word
            continue
        }
        if (($current.Length + 1 + $word.Length) -le $innerWidth) {
            $current = "$current $word"
        } else {
            [void]$titleLines.Add($current)
            $current = $word
        }
    }
    if ($current) {
        [void]$titleLines.Add($current)
    }
    if ($titleLines.Count -eq 0) {
        [void]$titleLines.Add('')
    }
    $borderLine = '||' + ('=' * ($width - 4)) + '||'

    Write-Host ''
    Write-Host $borderLine -ForegroundColor Cyan
    foreach ($line in $titleLines) {
        Write-Host ('|| {0} ||' -f $line.PadRight($innerWidth)) -ForegroundColor Cyan
    }
    Write-Host $borderLine -ForegroundColor Cyan
    Write-Host ''
}

function Get-StatusColor {
    param([Parameter(Mandatory = $true)][string]$Status)

    switch ($Status) {
        'PASS' { 'Green' }
        'FAIL' { 'Red' }
        'SKIPPED' { 'DarkYellow' }
        default { 'DarkYellow' }
    }
}

function Get-StatusBadge {
    param([Parameter(Mandatory = $true)][string]$Status)

    switch ($Status) {
        'PASS' { '[PASS]' }
        'FAIL' { '[FAIL]' }
        'SKIPPED' { '[SKIP]' }
        default { "[${Status}]" }
    }
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

    $stdoutFile = [System.IO.Path]::GetTempFileName()
    $stderrFile = [System.IO.Path]::GetTempFileName()
    $capturedOutput = [System.Text.StringBuilder]::new()
    $stdoutLength = 0
    $stderrLength = 0
    $process = $null

    function Write-NewText {
        param(
            [Parameter(Mandatory = $true)]
            [string]$Path,

            [Parameter(Mandatory = $true)]
            [ref]$KnownLength
        )

        if (-not (Test-Path -LiteralPath $Path)) {
            return
        }

        $text = Get-Content -Raw -LiteralPath $Path -ErrorAction SilentlyContinue
        if ([string]::IsNullOrEmpty($text)) {
            return
        }

        if ($text.Length -le $KnownLength.Value) {
            return
        }

        $chunk = $text.Substring($KnownLength.Value)
        if ($chunk) {
            Write-Host $chunk -NoNewline
            [void]$capturedOutput.Append($chunk)
            $KnownLength.Value = $text.Length
        }
    }

    try {
        $pathValue = [System.Environment]::GetEnvironmentVariable('Path', 'Process')
        if ($pathValue) {
            [System.Environment]::SetEnvironmentVariable('PATH', $null, 'Process')
            [System.Environment]::SetEnvironmentVariable('Path', $pathValue, 'Process')
        }

        $process = Start-Process -FilePath 'powershell.exe' -ArgumentList $argumentList -NoNewWindow -PassThru -RedirectStandardOutput $stdoutFile -RedirectStandardError $stderrFile

        while (-not $process.HasExited) {
            Write-NewText -Path $stdoutFile -KnownLength ([ref]$stdoutLength)
            Write-NewText -Path $stderrFile -KnownLength ([ref]$stderrLength)
            Start-Sleep -Milliseconds 200
        }

        $process.WaitForExit()
        $process.Refresh()
        Write-NewText -Path $stdoutFile -KnownLength ([ref]$stdoutLength)
        Write-NewText -Path $stderrFile -KnownLength ([ref]$stderrLength)
        $global:LASTEXITCODE = $process.ExitCode
    }
    finally {
        Remove-Item -LiteralPath $stdoutFile, $stderrFile -ErrorAction SilentlyContinue
    }

    return [pscustomobject]@{
        ExitCode = $process.ExitCode
        Output = $capturedOutput.ToString()
    }
}

function Get-TestSummary {
    param([string]$Output)

    $summary = [ordered]@{}
    $normalizedOutput = [regex]::Replace($Output, "`e\[[0-9;]*[A-Za-z]", '')
    $normalizedOutput = $normalizedOutput -replace "`r", ''

    if ($normalizedOutput -match '(?s)Tests run:\s+(?<run>\d+), Failures:\s+(?<failures>\d+), Errors:\s+(?<errors>\d+), Skipped:\s+(?<skipped>\d+)') {
        $summary['testsRun'] = [int]$Matches.run
        $summary['testsFailed'] = [int]$Matches.failures
        $summary['testsErrors'] = [int]$Matches.errors
        $summary['testsSkipped'] = [int]$Matches.skipped
    }

    if ($normalizedOutput -match '(?s)Test Files\s+(?<files>\d+)\s+passed.*?Tests\s+(?<tests>\d+)\s+passed') {
        $summary['testFiles'] = [int]$Matches.files
        $summary['tests'] = [int]$Matches.tests
    }

    return $summary
}

function Get-BackendTestSummary {
    $summary = [ordered]@{
        testsRun = 0
        testsFailed = 0
        testsErrors = 0
        testsSkipped = 0
    }

    $reportRoot = Join-Path $repoRoot 'backend'
    $reportFiles = Get-ChildItem -Path $reportRoot -Recurse -File -Filter 'TEST-*.xml' -ErrorAction SilentlyContinue
    foreach ($reportFile in $reportFiles) {
        try {
            [xml]$xml = Get-Content -Raw -LiteralPath $reportFile.FullName
            $suite = $xml.testsuite
            if ($null -eq $suite) {
                continue
            }

            $summary.testsRun += [int]$suite.tests
            $summary.testsFailed += [int]$suite.failures
            $summary.testsErrors += [int]$suite.errors
            $summary.testsSkipped += [int]$suite.skipped
        } catch {
            continue
        }
    }

    if (($summary.testsRun + $summary.testsFailed + $summary.testsErrors + $summary.testsSkipped) -eq 0) {
        return [ordered]@{}
    }

    return $summary
}

function Get-FrontendTestSummary {
    param(
        [string]$Output,

        [string]$ReportPath
    )

    $summary = [ordered]@{}

    $normalizedOutput = [regex]::Replace($Output, "(`e|\x1B)\[[0-9;]*[A-Za-z]", '')
    $normalizedOutput = $normalizedOutput -replace "`r", ''

    if ($normalizedOutput -match '(?s)Test Files\D+(?<filesPassed>\d+)\s+passed(?:\D+\((?<filesTotal>\d+)\))?.*?Tests\D+(?<testsPassed>\d+)\s+passed(?:\D+\((?<testsTotal>\d+)\))?') {
        $filesTotal = if ($Matches.filesTotal) { $Matches.filesTotal } else { $Matches.filesPassed }
        $testsTotal = if ($Matches.testsTotal) { $Matches.testsTotal } else { $Matches.testsPassed }
        $summary['totalTestSuites'] = [int]$filesTotal
        $summary['passedTestSuites'] = [int]$Matches.filesPassed
        $summary['totalTests'] = [int]$testsTotal
        $summary['passedTests'] = [int]$Matches.testsPassed
        return $summary
    }

    if (Test-Path -LiteralPath $ReportPath) {
        try {
            $json = Get-Content -Raw -LiteralPath $ReportPath | ConvertFrom-Json
            $summary['totalTestSuites'] = [int]$json.numTotalTestSuites
            $summary['passedTestSuites'] = [int]$json.numPassedTestSuites
            $summary['totalTests'] = [int]$json.numTotalTests
            $summary['passedTests'] = [int]$json.numPassedTests
        }
        catch {
        }
    }

    return $summary
}

function Write-SummaryLine {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Label,

        [Parameter(Mandatory = $true)]
        [string]$Status,

        [string]$Details = ''
    )

    $color = Get-StatusColor -Status $Status
    $badge = Get-StatusBadge -Status $Status

    if ($Details) {
        Write-Host ("  {0} {1} {2}" -f $badge, $Label, $Details) -ForegroundColor $color
    } else {
        Write-Host ("  {0} {1}" -f $badge, $Label) -ForegroundColor $color
    }
}

function Write-SummaryTable {
    param(
        [Parameter(Mandatory = $true)]
        [array]$Rows,

        [string]$TableColor = 'Cyan'
    )

    $width = 132
    $stepWidth = 30
    $statusWidth = 10
    $detailsWidth = $width - 15 - $stepWidth - $statusWidth
    $borderLine = '||' + ('=' * ($width - 4)) + '||'
    $dividerLine = '||' + ('-' * ($width - 4)) + '||'
    $headerLine = ('|| {0,-30} | {1,-10} | {2,-73} ||' -f 'Step', 'Status', 'Details')
    Write-Host $borderLine -ForegroundColor $TableColor
    Write-Host $headerLine -ForegroundColor $TableColor
    Write-Host $dividerLine -ForegroundColor $TableColor

    foreach ($row in $Rows) {
        $badge = Get-StatusBadge -Status $row.Status
        $details = [string]$row.Details
        if ($details.Length -gt $detailsWidth) {
            $details = $details.Substring(0, [Math]::Max(0, $detailsWidth - 3)) + '...'
        }

        $rowColor = Get-StatusColor -Status $row.Status
        $rowLine = ('|| {0,-30} | {1,-10} | {2,-73} ||' -f $row.Label, $badge, $details)
        Write-Host $rowLine -ForegroundColor $rowColor
    }

    Write-Host $borderLine -ForegroundColor $TableColor
}

$results = [ordered]@{
    'backend style' = $null
    'backend tests' = $null
    'secret scan' = $null
    'frontend style' = $null
    'frontend tests' = $null
    'frontend build' = $null
}

$results['backend style'] = Invoke-Check 'Running backend style: formatting (Spotless), API checks (Checkstyle)...' { Invoke-PowerShellScript -ScriptPath $backendStyleScript }
if ($results['backend style'].ExitCode -eq 0) {
    $results['backend tests'] = Invoke-Check 'Running backend tests: unit tests (JUnit, Mockito), integration tests (Testcontainers, Docker)...' { Invoke-PowerShellScript -ScriptPath $backendTestsScript }
}

if ($results['backend tests'] -and $results['backend tests'].ExitCode -eq 0) {
    $results['secret scan'] = Invoke-Check 'Running secret scan: repository secrets scan (Gitleaks)...' { Invoke-PowerShellScript -ScriptPath $secretScanScript }
}

if ($results['secret scan'] -and $results['secret scan'].ExitCode -eq 0) {
    $results['frontend style'] = Invoke-Check 'Running frontend style: linting (ESLint), formatting (Prettier)...' { Invoke-PowerShellScript -ScriptPath $frontendStyleScript }
}

if ($results['frontend style'] -and $results['frontend style'].ExitCode -eq 0) {
    $results['frontend tests'] = Invoke-Check 'Running frontend tests: component tests (Vitest, Vue Test Utils, JSDOM)...' { Invoke-PowerShellScript -ScriptPath $frontendTestScript }
}

if ($results['frontend tests'] -and $results['frontend tests'].ExitCode -eq 0) {
    Push-Location $frontendDir
    try {
        $results['frontend build'] = Invoke-Check 'Building frontend: typecheck and production bundle (Vue TSC, Vite)...' { npm run build }
    }
    finally {
        Pop-Location
    }
}

$backendTestSummary = if ($results['backend tests']) { Get-BackendTestSummary } else { [ordered]@{} }
$frontendTestReport = Join-Path $frontendDir '.vite\vitest\results.json'
$frontendTestSummary = if ($results['frontend tests']) { Get-FrontendTestSummary -Output $results['frontend tests'].Output -ReportPath $frontendTestReport } else { [ordered]@{} }

Write-BoxHeader 'Summary'

$backendTestDetails = ''
if ($backendTestSummary.Count -gt 0) {
    $backendTestDetails = "$($backendTestSummary.testsRun) run, $($backendTestSummary.testsFailed) failed, $($backendTestSummary.testsErrors) errors, $($backendTestSummary.testsSkipped) skipped"
}

$frontendTestsDetails = ''
if ($frontendTestSummary.Count -gt 0) {
    $frontendTestsDetails = "$($frontendTestSummary.passedTestSuites) files, $($frontendTestSummary.passedTests) tests"
}

$summaryRows = @(
    [pscustomobject]@{
        Label   = 'backend style'
        Status  = if ($results['backend style']) { if ($results['backend style'].ExitCode -eq 0) { 'PASS' } else { 'FAIL' } } else { 'SKIPPED' }
        Details = ''
    }
    [pscustomobject]@{
        Label   = 'backend tests'
        Status  = if ($results['backend tests']) { if ($results['backend tests'].ExitCode -eq 0) { 'PASS' } else { 'FAIL' } } else { 'SKIPPED' }
        Details = $backendTestDetails
    }
    [pscustomobject]@{
        Label   = 'secret scan'
        Status  = if ($results['secret scan']) { if ($results['secret scan'].ExitCode -eq 0) { 'PASS' } else { 'FAIL' } } else { 'SKIPPED' }
        Details = ''
    }
    [pscustomobject]@{
        Label   = 'frontend style'
        Status  = if ($results['frontend style']) { if ($results['frontend style'].ExitCode -eq 0) { 'PASS' } else { 'FAIL' } } else { 'SKIPPED' }
        Details = ''
    }
    [pscustomobject]@{
        Label   = 'frontend tests'
        Status  = if ($results['frontend tests']) { if ($results['frontend tests'].ExitCode -eq 0) { 'PASS' } else { 'FAIL' } } else { 'SKIPPED' }
        Details = $frontendTestsDetails
    }
    [pscustomobject]@{
        Label   = 'frontend build'
        Status  = if ($results['frontend build']) { if ($results['frontend build'].ExitCode -eq 0) { 'PASS' } else { 'FAIL' } } else { 'SKIPPED' }
        Details = ''
    }
)

$summaryColor = 'Green'
if ($summaryRows.Status -contains 'FAIL') {
    $summaryColor = 'Red'
} elseif ($summaryRows.Status -contains 'SKIPPED') {
    $summaryColor = 'DarkYellow'
}

Write-SummaryTable -Rows $summaryRows -TableColor $summaryColor

if ($results.Values | Where-Object { $_ -and -not $_.Succeeded }) {
    [System.Environment]::Exit(1)
}
