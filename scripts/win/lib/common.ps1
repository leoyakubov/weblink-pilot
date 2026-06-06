function Resolve-JavaHome {
    $jabbaCurrent = 'C:\Users\dev\scoop\apps\jabba\current'
    if (Test-Path -LiteralPath $jabbaCurrent) {
        $jabbaRoot = (Get-Item -LiteralPath $jabbaCurrent).Target
        $jdkRoot = Join-Path $jabbaRoot 'jdk'
        if (Test-Path -LiteralPath $jdkRoot) {
            $candidates = Get-ChildItem -LiteralPath $jdkRoot -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending
            foreach ($candidate in $candidates) {
                if (Test-Path -LiteralPath (Join-Path $candidate.FullName 'bin\java.exe')) {
                    return $candidate.FullName
                }
            }
        }
    }

    return $env:JAVA_HOME
}

function Import-DotEnv {
    param([Parameter(Mandatory = $true)][string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        return
    }

    Get-Content -LiteralPath $Path | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith('#')) {
            return
        }

        $index = $line.IndexOf('=')
        if ($index -lt 1) {
            return
        }

        $name = $line.Substring(0, $index).Trim()
        $value = $line.Substring($index + 1).Trim()
        if ($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'")))) {
            $value = $value.Substring(1, $value.Length - 2)
        }

        Set-Item -Path "Env:$name" -Value $value
    }
}

function Write-BoxHeader {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Title,

        [int]$Width = 112,

        [ConsoleColor]$Color = [ConsoleColor]::Cyan
    )

    $innerWidth = $Width - 4
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

    $borderLine = '||' + ('=' * ($Width - 4)) + '||'
    Write-Host ''
    Write-Host $borderLine -ForegroundColor $Color
    foreach ($line in $titleLines) {
        Write-Host ('|| {0} ||' -f $line.PadRight($innerWidth)) -ForegroundColor $Color
    }
    Write-Host $borderLine -ForegroundColor $Color
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
