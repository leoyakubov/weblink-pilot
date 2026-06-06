$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
$backendDir = Join-Path $repoRoot 'backend'

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

    $exitCode = if ($process) { $process.ExitCode } else { 1 }
    if ($exitCode -eq 0 -and $commandOutput -match '(?m)^\[ERROR\]|BUILD FAILURE|Non-resolvable import POM|Could not transfer artifact') {
        $exitCode = 1
    }
    if ($exitCode -ne 0) {
        exit $exitCode
    }
}
finally {
    Pop-Location
}
