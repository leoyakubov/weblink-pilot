$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
$backendDir = Join-Path $repoRoot 'backend'
. (Join-Path $repoRoot 'scripts/win/lib/common.ps1')

$resolvedJavaHome = Resolve-JavaHome
if ($resolvedJavaHome) {
    $env:JAVA_HOME = $resolvedJavaHome
}

Push-Location $backendDir
try {
    $stdoutFile = [System.IO.Path]::GetTempFileName()
    $stderrFile = [System.IO.Path]::GetTempFileName()
    $process = $null
    try {
        $process = Start-Process -FilePath 'cmd.exe' -ArgumentList @('/c', 'call .\mvnw.cmd -Pci spotless:check checkstyle:check') -NoNewWindow -PassThru -RedirectStandardOutput $stdoutFile -RedirectStandardError $stderrFile
        $process.WaitForExit()
        $process.Refresh()

        $stdout = if (Test-Path -LiteralPath $stdoutFile) { Get-Content -Raw -LiteralPath $stdoutFile -ErrorAction SilentlyContinue } else { '' }
        $stderr = if (Test-Path -LiteralPath $stderrFile) { Get-Content -Raw -LiteralPath $stderrFile -ErrorAction SilentlyContinue } else { '' }
        $commandOutput = @($stdout, $stderr) -join "`n"

        if ($commandOutput) {
            Write-Host $commandOutput
        }
    }
    finally {
        if (Test-Path -LiteralPath $stdoutFile) {
            Remove-Item -LiteralPath $stdoutFile -ErrorAction SilentlyContinue
        }
        if (Test-Path -LiteralPath $stderrFile) {
            Remove-Item -LiteralPath $stderrFile -ErrorAction SilentlyContinue
        }
    }

    $combinedOutput = ($commandOutput | ForEach-Object { $_.ToString() }) -join "`n"
    $exitCode = if ($process) { $process.ExitCode } else { 1 }
    if ($exitCode -eq 0 -and $combinedOutput -match '(?m)^\[ERROR\]|BUILD FAILURE|Error loading java.security file|AccessDeniedException') {
        $exitCode = 1
    }
    if ($exitCode -ne 0) {
        exit $exitCode
    }
}
finally {
    Pop-Location
}
