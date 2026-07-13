[CmdletBinding()]
param(
    [ValidateRange(24, 256)]
    [int]$ByteLength = 32,

    [string]$OutputPath
)

$ErrorActionPreference = "Stop"

# Sprint 18 item 725: generate a cryptographically random database password.
$bytes = New-Object byte[] $ByteLength
$generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $generator.GetBytes($bytes)
}
finally {
    $generator.Dispose()
}
$password = [Convert]::ToBase64String($bytes)

if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    Write-Output $password
    Write-Warning "Store this value in the production secret manager; terminal output is sensitive."
    return
}

$repositoryRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
$absoluteOutputPath = [System.IO.Path]::GetFullPath($OutputPath)
if ($absoluteOutputPath.StartsWith($repositoryRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Refusing to write production database credentials inside the Git repository."
}
if ([System.IO.File]::Exists($absoluteOutputPath)) {
    throw "Refusing to overwrite an existing credential file. Choose a new path."
}

$parent = Split-Path -Parent $absoluteOutputPath
if (-not [string]::IsNullOrWhiteSpace($parent)) {
    New-Item -ItemType Directory -Force -Path $parent | Out-Null
}

[System.IO.File]::WriteAllText($absoluteOutputPath, "DB_PASSWORD=$password`n")
Write-Host "Database password written outside Git. Restrict access and import it into the secret manager."
