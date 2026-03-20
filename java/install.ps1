#Requires -Version 5.1
<#
.SYNOPSIS
    Install script for ia-dev-env CLI on Windows.

.DESCRIPTION
    Installs ia-dev-env CLI to %LOCALAPPDATA%\Programs\ia-dev-env\
    and adds it to the user PATH.

.PARAMETER Uninstall
    Remove ia-dev-env installation.

.PARAMETER Prefix
    Custom installation directory.

.PARAMETER JarPath
    Path to a pre-built JAR file.

.PARAMETER SkipBuild
    Skip Maven build (requires JAR in target/).

.EXAMPLE
    .\install.ps1
    # Install to default location

.EXAMPLE
    .\install.ps1 -Uninstall
    # Remove installation

.EXAMPLE
    .\install.ps1 -JarPath .\my-jar.jar
    # Install using a pre-built JAR
#>

[CmdletBinding()]
param(
    [switch]$Uninstall,
    [string]$Prefix,
    [string]$JarPath,
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

$Version = "2.0.0-SNAPSHOT"
$JarName = "ia-dev-env-$Version.jar"
$InstalledJarName = "ia-dev-env.jar"
$RequiredJavaVersion = 21
$ProgramName = "ia-dev-env"

# --- Functions ---

function Write-Info {
    param([string]$Message)
    Write-Host "==> " -ForegroundColor Blue -NoNewline
    Write-Host $Message
}

function Write-Ok {
    param([string]$Message)
    Write-Host "==> " -ForegroundColor Green -NoNewline
    Write-Host $Message
}

function Write-Warn {
    param([string]$Message)
    Write-Host "WARNING: " -ForegroundColor Yellow -NoNewline
    Write-Host $Message
}

function Write-Err {
    param([string]$Message)
    Write-Host "ERROR: " -ForegroundColor Red -NoNewline
    Write-Host $Message
}

function Get-InstallDir {
    if ($Prefix) {
        return $Prefix
    }
    return Join-Path $env:LOCALAPPDATA "Programs\$ProgramName"
}

function Find-Java {
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
        return Join-Path $env:JAVA_HOME "bin\java.exe"
    }
    $javaCmd = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCmd) {
        return $javaCmd.Source
    }
    return $null
}

function Get-JavaMajorVersion {
    param([string]$JavaCmd)
    $output = & $JavaCmd -version 2>&1 | Out-String
    if ($output -match '"(\d+)[\._]') {
        $major = $Matches[1]
        if ($major -eq "1") {
            if ($output -match '"1\.(\d+)') {
                return [int]$Matches[1]
            }
        }
        return [int]$major
    }
    return 0
}

function Test-Java {
    Write-Info "Checking Java installation..."
    $javaCmd = Find-Java
    if (-not $javaCmd) {
        Write-Err "Java not found."
        Write-Host ""
        Write-Host "ia-dev-env requires Java $RequiredJavaVersion or later. Install it via:"
        Write-Host ""
        Write-Host "  SDKMAN:   sdk install java 21-tem"
        Write-Host "  Scoop:    scoop install temurin21-jdk"
        Write-Host "  Manual:   https://adoptium.net/temurin/releases/"
        Write-Host ""
        Write-Host "Then set JAVA_HOME or add java to your PATH."
        exit 1
    }

    $majorVersion = Get-JavaMajorVersion $javaCmd
    if ($majorVersion -lt $RequiredJavaVersion) {
        Write-Err "Java $RequiredJavaVersion or later is required, but found Java $majorVersion."
        exit 1
    }

    Write-Ok "Found Java $majorVersion ($javaCmd)"
    return $javaCmd
}

function Test-Maven {
    if ($SkipBuild -or $JarPath) {
        return
    }

    Write-Info "Checking Maven installation..."
    $mvnCmd = Get-Command mvn -ErrorAction SilentlyContinue
    if (-not $mvnCmd) {
        Write-Err "Maven not found. Install Maven 3.9+ or use -JarPath to provide a pre-built JAR."
        exit 1
    }

    $mvnVersion = & mvn --version 2>&1 | Select-Object -First 1
    Write-Ok "Found $mvnVersion"
}

function Build-Jar {
    $scriptDir = Split-Path -Parent $MyInvocation.ScriptName
    if (-not $scriptDir) {
        $scriptDir = $PSScriptRoot
    }

    if ($JarPath) {
        if (-not (Test-Path $JarPath)) {
            Write-Err "JAR not found at: $JarPath"
            exit 1
        }
        Write-Ok "Using pre-built JAR: $JarPath"
        return $JarPath
    }

    $targetJar = Join-Path $scriptDir "target\$JarName"

    if ($SkipBuild) {
        if (-not (Test-Path $targetJar)) {
            Write-Err "JAR not found at $targetJar. Run 'mvn package' first or remove -SkipBuild."
            exit 1
        }
        Write-Ok "Using existing JAR: $targetJar"
        return $targetJar
    }

    $pomFile = Join-Path $scriptDir "pom.xml"
    if (-not (Test-Path $pomFile)) {
        Write-Err "pom.xml not found in $scriptDir. Run this script from the java\ directory or use -JarPath."
        exit 1
    }

    Write-Info "Building fat JAR (this may take a minute)..."
    Push-Location $scriptDir
    try {
        & mvn clean package -DskipTests -q
        if ($LASTEXITCODE -ne 0) {
            Write-Err "Maven build failed."
            exit 1
        }
    }
    finally {
        Pop-Location
    }

    if (-not (Test-Path $targetJar)) {
        Write-Err "Build succeeded but JAR not found at $targetJar"
        exit 1
    }

    Write-Ok "Built $targetJar"
    return $targetJar
}

function Install-Files {
    param([string]$BuiltJarPath)

    $installDir = Get-InstallDir

    Write-Info "Installing to $installDir..."

    # Create directory
    if (-not (Test-Path $installDir)) {
        New-Item -ItemType Directory -Path $installDir -Force | Out-Null
    }

    # Copy JAR
    Copy-Item -Path $BuiltJarPath -Destination (Join-Path $installDir $InstalledJarName) -Force

    # Write VERSION file
    Set-Content -Path (Join-Path $installDir "VERSION") -Value $Version -NoNewline

    # Generate .cmd wrapper
    $wrapperPath = Join-Path $installDir "$ProgramName.cmd"
    $jarFullPath = Join-Path $installDir $InstalledJarName
    $wrapperContent = @"
@echo off
REM ia-dev-env wrapper (installed)
REM Generated by install.ps1 — do not edit manually.

setlocal

set "JAR_PATH=$jarFullPath"

REM Find java
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
        goto :check_version
    )
)

where java >nul 2>&1
if %ERRORLEVEL% equ 0 (
    set "JAVA_CMD=java"
    goto :check_version
)

echo ERROR: Java not found. Install Java $RequiredJavaVersion+. >&2
exit /b 1

:check_version
if not exist "%JAR_PATH%" (
    echo ERROR: JAR not found at %JAR_PATH% >&2
    echo Reinstall with: powershell .\install.ps1 >&2
    exit /b 1
)

"%JAVA_CMD%" %IA_DEV_ENV_JAVA_OPTS% -jar "%JAR_PATH%" %*
"@
    Set-Content -Path $wrapperPath -Value $wrapperContent -Encoding ASCII

    Write-Ok "Installed $ProgramName to $installDir"
    return $installDir
}

function Add-ToPath {
    param([string]$InstallDir)

    $currentPath = [Environment]::GetEnvironmentVariable("Path", "User")
    if ($currentPath -split ";" | Where-Object { $_ -eq $InstallDir }) {
        return
    }

    Write-Info "Adding $InstallDir to user PATH..."
    $newPath = "$InstallDir;$currentPath"
    [Environment]::SetEnvironmentVariable("Path", $newPath, "User")

    # Update current session
    $env:Path = "$InstallDir;$env:Path"

    Write-Warn "Restart your terminal for PATH changes to take effect in new sessions."
}

function Invoke-Uninstall {
    $installDir = Get-InstallDir

    Write-Info "Uninstalling ia-dev-env..."

    $found = $false

    if (Test-Path $installDir) {
        Remove-Item -Path $installDir -Recurse -Force
        Write-Ok "Removed $installDir"
        $found = $true
    }

    # Remove from PATH
    $currentPath = [Environment]::GetEnvironmentVariable("Path", "User")
    if ($currentPath -and ($currentPath -split ";" | Where-Object { $_ -eq $installDir })) {
        $newPath = ($currentPath -split ";" | Where-Object { $_ -ne $installDir }) -join ";"
        [Environment]::SetEnvironmentVariable("Path", $newPath, "User")
        Write-Ok "Removed $installDir from user PATH"
        $found = $true
    }

    if (-not $found) {
        Write-Warn "No installation found at $installDir"
    }
    else {
        Write-Ok "ia-dev-env uninstalled successfully."
    }
}

function Write-Success {
    param([string]$InstallDir)

    $jarFile = Join-Path $InstallDir $InstalledJarName
    $jarSize = ""
    if (Test-Path $jarFile) {
        $size = (Get-Item $jarFile).Length
        $jarSize = "{0:N1} MB" -f ($size / 1MB)
    }

    Write-Host ""
    Write-Host "ia-dev-env installed successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "  Version:  $Version"
    Write-Host "  JAR:      $jarFile ($jarSize)"
    Write-Host "  Wrapper:  $(Join-Path $InstallDir "$ProgramName.cmd")"
    Write-Host ""
    Write-Host "Quick start:" -ForegroundColor White
    Write-Host "  $ProgramName --version"
    Write-Host "  $ProgramName generate --stack java-quarkus --output my-project/"
    Write-Host ""
    Write-Host "Uninstall:" -ForegroundColor White
    Write-Host "  powershell .\install.ps1 -Uninstall"
    Write-Host ""
}

# --- Main ---

if ($Uninstall) {
    Invoke-Uninstall
    exit 0
}

$null = Test-Java
Test-Maven
$builtJar = Build-Jar
$installDir = Install-Files -BuiltJarPath $builtJar
Add-ToPath -InstallDir $installDir
Write-Success -InstallDir $installDir
