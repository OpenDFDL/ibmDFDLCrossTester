<#
.SYNOPSIS
    Copies IBM DFDL jars and sample files from an ACE installation into the cross-tester project.

.PARAMETER AceVersion
    The ACE major version to use. Supported values: "12" (default), "13".

.EXAMPLE
    .\setup-ace-jars.ps1
    .\setup-ace-jars.ps1 -AceVersion 13
#>
param(
    [string]$AceVersion = "12"
)

$acePaths = @{
    "12" = "C:\Program Files\IBM\ACE\12.0.12.17"
    "13" = "C:\Program Files\IBM\ACE\13.0.6.0"
}

if (-not $acePaths.ContainsKey($AceVersion)) {
    Write-Error "Unsupported AceVersion '$AceVersion'. Supported: $($acePaths.Keys -join ', ')"
    exit 1
}

$aceRoot   = $acePaths[$AceVersion]
$scriptDir = $PSScriptRoot

if (-not (Test-Path $aceRoot)) {
    Write-Error "ACE $AceVersion install not found at: $aceRoot"
    exit 1
}

$dfdlLibDir   = Join-Path $aceRoot "server\dfdl\lib"
$sampleDfdlDir = Join-Path $aceRoot "server\sample\dfdl"

$libDir         = Join-Path $scriptDir "lib"
$testResDir     = Join-Path $scriptDir "src\test\resources"
$ibmDefinedDir  = Join-Path $testResDir "IBMdefined"

# Ensure destination directories exist
New-Item -ItemType Directory -Force -Path $libDir        | Out-Null
New-Item -ItemType Directory -Force -Path $testResDir    | Out-Null
New-Item -ItemType Directory -Force -Path $ibmDefinedDir | Out-Null

$copied = @()
$errors = @()

function Copy-File($src, $dst) {
    if (Test-Path $src) {
        Copy-Item -Force $src $dst
        $script:copied += "  $src -> $dst"
    } else {
        $script:errors += "  NOT FOUND: $src"
    }
}

Write-Host ""
Write-Host "=== IBM DFDL Cross-Tester Setup (ACE $AceVersion) ===" -ForegroundColor Cyan
Write-Host "ACE root : $aceRoot"
Write-Host ""

# 1. Copy all DFDL runtime jars from server/dfdl/lib/
Write-Host "Copying DFDL runtime jars from $dfdlLibDir ..."
Get-ChildItem -Path $dfdlLibDir -Filter "*.jar" -ErrorAction Stop | ForEach-Object {
    $dst = Join-Path $libDir $_.Name
    Copy-Item -Force $_.FullName $dst
    $copied += "  $($_.FullName) -> $dst"
}

# 2. Copy dfdlsample_java.jar from sample directory
Write-Host "Copying dfdlsample_java.jar ..."
Copy-File (Join-Path $sampleDfdlDir "dfdlsample_java.jar") $libDir

# 3. Copy company sample files
Write-Host "Copying company sample files ..."
foreach ($name in @("company.txt", "company.xml", "company.xsd")) {
    Copy-File (Join-Path $sampleDfdlDir $name) $testResDir
}

# 4. Copy IBMdefined schema
Write-Host "Copying RecordSeparatedFieldFormat.xsd ..."
Copy-File (Join-Path $sampleDfdlDir "IBMdefined\RecordSeparatedFieldFormat.xsd") $ibmDefinedDir

# Summary
Write-Host ""
if ($copied.Count -gt 0) {
    Write-Host "Copied ($($copied.Count) files):" -ForegroundColor Green
    $copied | ForEach-Object { Write-Host $_ }
}
if ($errors.Count -gt 0) {
    Write-Host ""
    Write-Host "Warnings - files not found:" -ForegroundColor Yellow
    $errors | ForEach-Object { Write-Host $_ }
}

Write-Host ""
Write-Host "lib/ contents:" -ForegroundColor Cyan
Get-ChildItem -Path $libDir -Filter "*.jar" | Select-Object -ExpandProperty Name | ForEach-Object { Write-Host "  $_" }
Write-Host ""
Write-Host "Setup complete. Run 'sbt compile' then 'sbt test' to verify." -ForegroundColor Green
