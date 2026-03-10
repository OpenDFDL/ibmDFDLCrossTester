<#
.SYNOPSIS
    Validates a data file against an IBM DFDL schema using the ibmDFDLCrossTester rig.

.PARAMETER Schema
    Full path to the DFDL schema (.xsd).

.PARAMETER Data
    Full path to the data file to validate.

.PARAMETER Root
    Root element name. Auto-detected from the schema if omitted
    (looks for ibmSchExtn:docRoot="true" or ibmDfdlExtn:docRoot="true").

.PARAMETER AceVersion
    ACE major version: "12" (default) or "13". Ignored when -AcePath is supplied.

.PARAMETER AcePath
    Full path to the ACE installation root. Overrides -AceVersion.

.PARAMETER Trace
    Attach IBM DFDL's service trace listener. Prints info/error/fatal trace lines to
    stderr, including schema path and byte offset for each parse decision. Useful for
    diagnosing parse failures.

.PARAMETER SbtPath
    Full path to sbt.bat. Defaults to "C:\Program Files (x86)\sbt\bin\sbt.bat".

.EXAMPLE
    .\validate.ps1 -Schema "C:\path\to\MySchema.xsd" -Data "C:\path\to\mydata.txt"
    .\validate.ps1 -Schema "..." -Data "..." -Root "MyRootElement"
    .\validate.ps1 -Schema "..." -Data "..." -AceVersion 13
    .\validate.ps1 -Schema "..." -Data "..." -AcePath "C:\Program Files\IBM\ACE\12.0.12.17"
    .\validate.ps1 -Schema "..." -Data "..." -Trace
    .\validate.ps1 -Schema "..." -Data "..." -SbtPath "D:\tools\sbt\bin\sbt.bat"
#>
param(
    [Parameter(Mandatory=$true)]  [string]$Schema,
    [Parameter(Mandatory=$true)]  [string]$Data,
    [string]$Root       = "",
    [string]$AceVersion = "12",
    [string]$AcePath    = "",
    [string]$SbtPath    = "C:\Program Files (x86)\sbt\bin\sbt.bat",
    [switch]$Trace
)

$scriptDir = $PSScriptRoot

# --- Resolve ACE root ---
if ($AcePath -ne "") {
    $aceRoot  = $AcePath
    $aceLabel = $AcePath
} else {
    $acePaths = @{
        "12" = "C:\Program Files\IBM\ACE\12.0.12.17"
        "13" = "C:\Program Files\IBM\ACE\13.0.6.0"
    }
    if (-not $acePaths.ContainsKey($AceVersion)) {
        Write-Error "Unsupported AceVersion '$AceVersion'. Use -AcePath for custom installs."
        exit 1
    }
    $aceRoot  = $acePaths[$AceVersion]
    $aceLabel = "ACE $AceVersion"
}

if (-not (Test-Path $aceRoot)) {
    Write-Error "ACE install not found at: $aceRoot"
    exit 1
}

$java17Home = Join-Path $aceRoot "common\java17"

# --- Validate inputs ---
if (-not (Test-Path $Schema)) {
    Write-Error "Schema not found: $Schema"
    exit 1
}
if (-not (Test-Path $Data)) {
    Write-Error "Data file not found: $Data"
    exit 1
}

# --- Ensure IBMdefined/ is next to the schema ---
# Schemas that import IBMdefined/RecordSeparatedFieldFormat.xsd use a relative path,
# so the IBMdefined/ folder must exist in the same directory as the schema.
$schemaDir           = Split-Path $Schema -Parent
$ibmDefinedAtSchema  = Join-Path $schemaDir "IBMdefined"
$ibmDefinedInProject = Join-Path $scriptDir "src\test\resources\IBMdefined"

if (-not (Test-Path $ibmDefinedAtSchema)) {
    if (Test-Path $ibmDefinedInProject) {
        Write-Host "Copying IBMdefined/ to schema directory ..." -ForegroundColor Cyan
        Copy-Item -Recurse -Force $ibmDefinedInProject $schemaDir
    } else {
        Write-Warning "IBMdefined/ not found in project. Run setup-ace-jars.ps1 first if the schema imports RecordSeparatedFieldFormat.xsd."
    }
}

# --- Build sbt arguments ---
$sbtArgs = @(
    "-java-home", $java17Home,
    "-Dvalidate.schema=$Schema",
    "-Dvalidate.data=$Data"
)
if ($Root -ne "") {
    $sbtArgs += "-Dvalidate.root=$Root"
}
if ($Trace) {
    $sbtArgs += "-Dvalidate.verbose=true"
}
$sbtArgs += "testOnly io.github.openDFDL.ValidateFile"

# --- Run ---
Write-Host ""
$header = "=== IBM DFDL Validator ($aceLabel)"
if ($Trace) { $header += " [verbose]" }
$header += " ==="
Write-Host $header -ForegroundColor Cyan
Write-Host "Schema : $Schema"
Write-Host "Data   : $Data"
if ($Root -ne "") { Write-Host "Root   : $Root" }
Write-Host ""

if (-not (Test-Path $SbtPath)) {
    Write-Error "sbt not found at: $SbtPath. Use -SbtPath to specify the correct location."
    exit 1
}
& $SbtPath @sbtArgs
exit $LASTEXITCODE
