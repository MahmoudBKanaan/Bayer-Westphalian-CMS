[CmdletBinding()]
param(
    [ValidateRange(32, 256)]
    [int]$ByteLength = 48,

    [string]$OutputPath
)

$ErrorActionPreference = "Stop"

# Sprint 18 item 724: generate a cryptographically random JWT signing secret.
$bytes = New-Object byte[] $ByteLength
$generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $generator.GetBytes($bytes)
}
finally {
    $generator.Dispose()
}
$secret = [Convert]::ToBase64String($bytes)

if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    Write-Output $secret
    Write-Warning "Store this value in the production secret manager; terminal output is sensitive."
    return
}

$repositoryRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
$absoluteOutputPath = [System.IO.Path]::GetFullPath($OutputPath)
if ($absoluteOutputPath.StartsWith($repositoryRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Refusing to write a production secret inside the Git repository."
}
if ([System.IO.File]::Exists($absoluteOutputPath)) {
    throw "Refusing to overwrite an existing secret file. Choose a new path."
}

$parent = Split-Path -Parent $absoluteOutputPath
if (-not [string]::IsNullOrWhiteSpace($parent)) {
    New-Item -ItemType Directory -Force -Path $parent | Out-Null
}

[System.IO.File]::WriteAllText($absoluteOutputPath, "JWT_SECRET=$secret`n")
Write-Host "JWT secret written to $absoluteOutputPath. Keep this file outside Git and restrict access."
