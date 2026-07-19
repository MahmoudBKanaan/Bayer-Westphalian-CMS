<#
.SYNOPSIS
  Create local self-signed TLS PEMs for production Compose reverse-proxy (evidence / local only).

.DESCRIPTION
  Generates docker/tls/fullchain.pem and privkey.pem if missing (or -Force).
  Updates .env.production TLS_* paths to absolute Docker-friendly paths.
  PEMs are gitignored (*.pem). Never use these certs for a real public deployment.
#>
[CmdletBinding()]
param(
    [string]$RepoRoot,
    [string]$EnvFile,
    [switch]$Force
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($RepoRoot)) {
    $RepoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
}
if ([string]::IsNullOrWhiteSpace($EnvFile)) {
    $EnvFile = Join-Path $RepoRoot ".env.production"
}

$tlsDir = Join-Path $RepoRoot "docker\tls"
$certPath = Join-Path $tlsDir "fullchain.pem"
$keyPath = Join-Path $tlsDir "privkey.pem"

function Find-OpenSsl {
    $candidates = @(
        "C:\Program Files\Git\usr\bin\openssl.exe",
        "C:\Program Files\OpenSSL-Win64\bin\openssl.exe",
        "openssl"
    )
    foreach ($c in $candidates) {
        if ($c -eq "openssl") {
            $cmd = Get-Command openssl -ErrorAction SilentlyContinue
            if ($cmd) { return $cmd.Source }
        }
        elseif (Test-Path -LiteralPath $c) {
            return $c
        }
    }
    return $null
}

New-Item -ItemType Directory -Force -Path $tlsDir | Out-Null

$needGenerate = $Force -or -not (Test-Path -LiteralPath $certPath) -or -not (Test-Path -LiteralPath $keyPath)
if ($needGenerate) {
    $openssl = Find-OpenSsl
    if (-not $openssl) {
        throw @"
OpenSSL not found. Install Git for Windows (includes openssl) or OpenSSL, then retry.

Git openssl path (if installed):
  & `"C:\Program Files\Git\usr\bin\openssl.exe`" version
"@
    }
    Write-Host "Generating self-signed TLS material with: $openssl"
    & $openssl req -x509 -nodes -newkey rsa:2048 -days 365 `
        -keyout $keyPath `
        -out $certPath `
        -subj "/CN=localhost"
    if ($LASTEXITCODE -ne 0) {
        throw "openssl failed with exit code $LASTEXITCODE"
    }
}
else {
    Write-Host "Using existing TLS files under docker\tls"
}

$certAbs = ((Resolve-Path -LiteralPath $certPath).Path) -replace "\\", "/"
$keyAbs = ((Resolve-Path -LiteralPath $keyPath).Path) -replace "\\", "/"

if (-not (Test-Path -LiteralPath $EnvFile)) {
    Write-Warning "Env file not found: $EnvFile (create via Start-ProductionBackend.ps1 or copy from backend/.env.production.example)"
    Write-Host "TLS_CERTIFICATE_PATH=$certAbs"
    Write-Host "TLS_PRIVATE_KEY_PATH=$keyAbs"
    return
}

$raw = [System.IO.File]::ReadAllText($EnvFile)
$raw = [regex]::Replace($raw, "(?m)^TLS_CERTIFICATE_PATH=.*$", "TLS_CERTIFICATE_PATH=$certAbs")
$raw = [regex]::Replace($raw, "(?m)^TLS_PRIVATE_KEY_PATH=.*$", "TLS_PRIVATE_KEY_PATH=$keyAbs")
if ($raw -notmatch "(?m)^TLS_CERTIFICATE_PATH=") {
    $raw = $raw.TrimEnd() + "`r`nTLS_CERTIFICATE_PATH=$certAbs`r`n"
}
if ($raw -notmatch "(?m)^TLS_PRIVATE_KEY_PATH=") {
    $raw = $raw.TrimEnd() + "`r`nTLS_PRIVATE_KEY_PATH=$keyAbs`r`n"
}
# Production CORS forbids localhost/127.0.0.1. Strip them if present.
if ($raw -match "(?m)^CORS_ALLOWED_ORIGINS=") {
    $m = [regex]::Match($raw, "(?m)^CORS_ALLOWED_ORIGINS=(.*)$")
    $origins = $m.Groups[1].Value.Trim()
    $parts = $origins.Split(",") | ForEach-Object { $_.Trim() } | Where-Object {
        $_ -and $_ -notmatch '(?i)localhost|127\.0\.0\.1'
    }
    if ($parts.Count -eq 0) {
        $parts = @("https://campaign.bayer-westphalian.example")
    }
    $newOrigins = ($parts -join ",")
    $raw = [regex]::Replace($raw, "(?m)^CORS_ALLOWED_ORIGINS=.*$", "CORS_ALLOWED_ORIGINS=$newOrigins")
}
elseif ($raw -notmatch "(?m)^CORS_ALLOWED_ORIGINS=") {
    $raw = $raw.TrimEnd() + "`r`nCORS_ALLOWED_ORIGINS=https://campaign.bayer-westphalian.example`r`n"
}

$utf8 = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText((Resolve-Path -LiteralPath $EnvFile), $raw, $utf8)

Write-Host "Updated $EnvFile TLS paths (absolute)."
Write-Host "  TLS_CERTIFICATE_PATH=$certAbs"
Write-Host "  TLS_PRIVATE_KEY_PATH=$keyAbs"
Write-Host "  CORS must be non-localhost HTTPS origins only (prod rule)."
