function Resolve-PhysicalPath {
    param([Parameter(Mandatory = $true)][string]$Path)

    try {
        $item = Get-Item -LiteralPath $Path -ErrorAction Stop
        if ($item.PSIsContainer -and $item.Target) {
            return $item.Target[0]
        }
        return $item.FullName
    } catch {
        return $Path
    }
}

function Resolve-JavaHome {
    param([string]$RepositoryRoot)

    $preferredMajorVersion = 21

    function Get-JavaMajorVersion {
        param([Parameter(Mandatory = $true)][string]$JavaExe)

        $previousErrorActionPreference = $ErrorActionPreference
        $nativeCommandPreference = Get-Variable -Name PSNativeCommandUseErrorActionPreference -Scope Global -ErrorAction SilentlyContinue
        $previousNativeCommandPreference = if ($nativeCommandPreference) { [bool] $nativeCommandPreference.Value } else { $false }
        try {
            $ErrorActionPreference = 'Continue'
            if ($nativeCommandPreference) {
                $PSNativeCommandUseErrorActionPreference = $false
            }

            $versionOutput = & $JavaExe -version 2>&1
            if ($LASTEXITCODE -ne 0) {
                return $null
            }

            foreach ($line in $versionOutput) {
                if ($line -match 'version\s+"(?<major>\d+)(?:\.(?<minor>\d+))?.*"') {
                    $major = [int]$matches.major
                    if ($major -eq 1 -and $matches.minor) {
                        return [int]$matches.minor
                    }

                    return $major
                }
            }

            return $null
        }
        finally {
            $ErrorActionPreference = $previousErrorActionPreference
            if ($nativeCommandPreference) {
                $PSNativeCommandUseErrorActionPreference = $previousNativeCommandPreference
            }
        }
    }

    function Test-JavaExecutable {
        param([Parameter(Mandatory = $true)][string]$JavaExe)

        return ($null -ne (Get-JavaMajorVersion -JavaExe $JavaExe))
    }

    function Get-PreferredJavaHome {
        param([Parameter(Mandatory = $true)][System.Collections.IEnumerable]$Candidates)

        foreach ($candidate in $Candidates) {
            $javaExe = Join-Path $candidate.FullName 'bin\java.exe'
            if ((Test-Path -LiteralPath $javaExe) -and ((Get-JavaMajorVersion -JavaExe $javaExe) -eq $preferredMajorVersion)) {
                return $candidate.FullName
            }
        }

        foreach ($candidate in $Candidates) {
            $javaExe = Join-Path $candidate.FullName 'bin\java.exe'
            if ((Test-Path -LiteralPath $javaExe) -and (Test-JavaExecutable -JavaExe $javaExe)) {
                return $candidate.FullName
            }
        }

        return $null
    }

    $preferredJavaPatterns = @(
        (Join-Path $env:ProgramFiles 'Eclipse Adoptium\jdk-21*'),
        (Join-Path $env:ProgramFiles 'Java\jdk-21*'),
        (Join-Path $env:ProgramFiles 'Adoptium\jdk-21*')
    )
    if ($env:ProgramFiles -and ${env:ProgramFiles(x86)}) {
        $preferredJavaPatterns += Join-Path ${env:ProgramFiles(x86)} 'Eclipse Adoptium\jdk-21*'
        $preferredJavaPatterns += Join-Path ${env:ProgramFiles(x86)} 'Java\jdk-21*'
    }

    foreach ($preferredPattern in $preferredJavaPatterns) {
        $preferredCandidates = Get-ChildItem -Path $preferredPattern -Directory -ErrorAction SilentlyContinue | Sort-Object Name
        foreach ($candidate in $preferredCandidates) {
            $javaExe = Join-Path $candidate.FullName 'bin\java.exe'
            if (Test-Path -LiteralPath $javaExe) {
                return Resolve-PhysicalPath -Path $candidate.FullName
            }
        }
    }

    if ($RepositoryRoot) {
        $workspaceRoot = Split-Path -Parent $RepositoryRoot
        $localJdkRoot = Join-Path $workspaceRoot '.local-jdk'
        if (Test-Path -LiteralPath $localJdkRoot) {
            $localCandidates = Get-ChildItem -LiteralPath $localJdkRoot -Directory -ErrorAction SilentlyContinue | Sort-Object Name
            $preferredLocalJdkHome = Get-PreferredJavaHome -Candidates $localCandidates
            if ($preferredLocalJdkHome) {
                return Resolve-PhysicalPath -Path $preferredLocalJdkHome
            }
        }
    }

    $commonJdkRoots = @(
        (Join-Path $env:ProgramFiles 'Eclipse Adoptium'),
        (Join-Path $env:ProgramFiles 'Java'),
        (Join-Path $env:ProgramFiles 'Adoptium'),
        (if ($env:ProgramFiles -and ${env:ProgramFiles(x86)}) { Join-Path ${env:ProgramFiles(x86)} 'Eclipse Adoptium' }),
        (if ($env:ProgramFiles -and ${env:ProgramFiles(x86)}) { Join-Path ${env:ProgramFiles(x86)} 'Java' })
    ) | Where-Object { $_ -and (Test-Path -LiteralPath $_) }

    foreach ($jdkRoot in $commonJdkRoots) {
        $commonCandidates = Get-ChildItem -LiteralPath $jdkRoot -Directory -ErrorAction SilentlyContinue | Sort-Object Name
        $preferredCommonJdkHome = Get-PreferredJavaHome -Candidates $commonCandidates
        if ($preferredCommonJdkHome) {
            return $preferredCommonJdkHome
        }
    }

    $scoopPersistJdkRoot = Join-Path $env:USERPROFILE 'scoop\persist\jabba\jdk'
    if (Test-Path -LiteralPath $scoopPersistJdkRoot) {
        $persistCandidates = Get-ChildItem -LiteralPath $scoopPersistJdkRoot -Directory -ErrorAction SilentlyContinue | Sort-Object Name
        $preferredPersistJdkHome = Get-PreferredJavaHome -Candidates $persistCandidates
        if ($preferredPersistJdkHome) {
            return $preferredPersistJdkHome
        }
    }

    if ($env:JAVA_HOME) {
        $resolvedJavaHome = Resolve-PhysicalPath -Path $env:JAVA_HOME
        $javaExe = Join-Path $resolvedJavaHome 'bin\java.exe'
        if ((Test-Path -LiteralPath $javaExe) -and ((Get-JavaMajorVersion -JavaExe $javaExe) -eq $preferredMajorVersion)) {
            return $resolvedJavaHome
        }
    }

    $javaCommand = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCommand -and $javaCommand.Source) {
        $commandJava = $javaCommand.Source
        if ((Test-Path -LiteralPath $commandJava) -and ((Get-JavaMajorVersion -JavaExe $commandJava) -eq $preferredMajorVersion)) {
            return Resolve-PhysicalPath -Path (Split-Path -Parent (Split-Path -Parent $commandJava))
        } elseif ($javaCommand.Path -and ((Get-JavaMajorVersion -JavaExe $javaCommand.Path) -eq $preferredMajorVersion)) {
            return Resolve-PhysicalPath -Path (Split-Path -Parent (Split-Path -Parent $javaCommand.Path))
        }
    }

    if ($RepositoryRoot) {
        $workspaceRoot = Split-Path -Parent $RepositoryRoot
        $localJdkRoot = Join-Path $workspaceRoot '.local-jdk'
        if (Test-Path -LiteralPath $localJdkRoot) {
            $localCandidates = Get-ChildItem -LiteralPath $localJdkRoot -Directory -ErrorAction SilentlyContinue | Sort-Object Name
            foreach ($candidate in $localCandidates) {
                $javaExe = Join-Path $candidate.FullName 'bin\java.exe'
                if ((Test-Path -LiteralPath $javaExe) -and (Test-JavaExecutable -JavaExe $javaExe)) {
                    return Resolve-PhysicalPath -Path $candidate.FullName
                }
            }
        }
    }

    if (Test-Path -LiteralPath $scoopPersistJdkRoot) {
        $persistCandidates = Get-ChildItem -LiteralPath $scoopPersistJdkRoot -Directory -ErrorAction SilentlyContinue | Sort-Object Name
        foreach ($candidate in $persistCandidates) {
            $javaExe = Join-Path $candidate.FullName 'bin\java.exe'
            if ((Test-Path -LiteralPath $javaExe) -and (Test-JavaExecutable -JavaExe $javaExe)) {
                return $candidate.FullName
            }
        }
    }

    if ($env:JAVA_HOME) {
        $resolvedJavaHome = Resolve-PhysicalPath -Path $env:JAVA_HOME
        $javaExe = Join-Path $resolvedJavaHome 'bin\java.exe'
        if ((Test-Path -LiteralPath $javaExe) -and (Test-JavaExecutable -JavaExe $javaExe)) {
            return $resolvedJavaHome
        }
    }

    if ($javaCommand -and $javaCommand.Source) {
        $commandJava = $javaCommand.Source
        if ((Test-Path -LiteralPath $commandJava) -and (Test-JavaExecutable -JavaExe $commandJava)) {
            return Resolve-PhysicalPath -Path (Split-Path -Parent (Split-Path -Parent $commandJava))
        } elseif ($javaCommand.Path -and (Test-JavaExecutable -JavaExe $javaCommand.Path)) {
            return Resolve-PhysicalPath -Path (Split-Path -Parent (Split-Path -Parent $javaCommand.Path))
        }
    }

    return $null
}

function Get-JavaSecurityPropertiesOverride {
    param([Parameter(Mandatory = $true)][string]$JavaHome)

    $securitySource = Join-Path $JavaHome 'conf\security\java.security'
    if (-not (Test-Path -LiteralPath $securitySource)) {
        return $null
    }

    $overrideDirectory = Join-Path $env:TEMP 'weblink-pilot-java-security'
    New-Item -ItemType Directory -Path $overrideDirectory -Force | Out-Null

    $overridePath = Join-Path $overrideDirectory 'java.security'
    Copy-Item -LiteralPath $securitySource -Destination $overridePath -Force

    return $overridePath
}

function Enter-JavaSecurityOverride {
    param([Parameter(Mandatory = $true)][string]$JavaHome)

    return $env:JAVA_TOOL_OPTIONS
}

function Exit-JavaSecurityOverride {
    param([string]$PreviousJavaToolOptions)

    if ($null -ne $PreviousJavaToolOptions -and $PreviousJavaToolOptions.Length -gt 0) {
        $env:JAVA_TOOL_OPTIONS = $PreviousJavaToolOptions
    } else {
        Remove-Item Env:JAVA_TOOL_OPTIONS -ErrorAction SilentlyContinue
    }
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
