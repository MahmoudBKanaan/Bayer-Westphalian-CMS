<#
.SYNOPSIS
  Start the full production Compose stack (postgres, backup, backend, frontend, reverse-proxy).

.DESCRIPTION
  Ensures local TLS PEMs and .env.production TLS paths, validates compose, then
  docker compose up -d --build for the full stack.
  Optional -EvidenceLocal applies docker-compose.prod.evidence-local.yml (faster backup + scheduler).

.PARAMETER EnvFile
  Production env file (default: repo .env.production).

.PARAMETER EvidenceLocal
  Use local evidence overlay (shorter backup interval, every-minute reminder cron).

.PARAMETER Detached
  Do not stream logs after start (default streams reverse-proxy + backend briefly then shows ps).

.PARAMETER NoBuild
  Skip --build (use existing images).
#>
[CmdletBinding()]
param(
    [string]$EnvFile,
    [switch]$EvidenceLocal,
    [switch]$Detached,
    [switch]$NoBuild
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
if ([string]::IsNullOrWhiteSpace($EnvFile)) {
    $EnvFile = Join-Path $repoRoot ".env.production"
}

function Assert-DockerAvailable {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw "Docker is not installed or not on PATH."
    }
    docker info 1>$null 2>$null
    if ($LASTEXITCODE -ne 0) {
        throw "Docker daemon is not running. Start Docker Desktop, then retry."
    }
}

Assert-DockerAvailable

if (-not (Test-Path -LiteralPath $EnvFile)) {
    Write-Host "Missing $EnvFile — creating via Start-ProductionBackend helper..."
    & (Join-Path $PSScriptRoot "Start-ProductionBackend.ps1") -Detached -EnvFile $EnvFile
    # Stop backend-only so full stack owns the project cleanly
    Push-Location $repoRoot
    try {
        docker compose --env-file $EnvFile `
            -f docker-compose.prod.yml `
            -f docker-compose.prod.backend-local.yml `
            stop postgres backend 2>$null
    }
    finally {
        Pop-Location
    }
}

& (Join-Path $PSScriptRoot "Ensure-LocalProductionTls.ps1") -RepoRoot $repoRoot -EnvFile $EnvFile

$composeArgs = @(
    "compose",
    "--env-file", $EnvFile,
    "-f", (Join-Path $repoRoot "docker-compose.prod.yml")
)
if ($EvidenceLocal) {
    $composeArgs += @("-f", (Join-Path $repoRoot "docker-compose.prod.evidence-local.yml"))
    Write-Host "Evidence-local overlay enabled (faster backup + scheduler for screenshots)."
}

Write-Host "Validating full production compose..."
Push-Location $repoRoot
try {
    & docker @composeArgs config 1>$null
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose config failed. Check $EnvFile (DB_*, JWT_SECRET, CORS_ALLOWED_ORIGINS, TLS_*)."
    }

    # Build images one service at a time, then start without rebuilding.
    # Parallel `up --build` (Maven + Node + Spring Boot JVM) often OOM-kills the backend
    # on ~8 GiB Docker Desktop hosts (container exit code 137).
    if (-not $NoBuild) {
        Write-Host "Building backend image (sequential to reduce OOM risk)..."
        & docker @composeArgs build backend
        if ($LASTEXITCODE -ne 0) { throw "backend image build failed with exit code $LASTEXITCODE" }

        Write-Host "Building frontend image..."
        & docker @composeArgs build frontend
        if ($LASTEXITCODE -ne 0) { throw "frontend image build failed with exit code $LASTEXITCODE" }
    }

    Write-Host "Starting full production stack (no concurrent rebuild)..."
    & docker @composeArgs up -d --no-build
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose up failed with exit code $LASTEXITCODE"
    }

    Write-Host ""
    Write-Host "=== Service status (screenshot: Production Docker Compose / deployment running) ==="
    & docker @composeArgs ps

    Write-Host ""
    Write-Host "Public HTTPS (self-signed): https://localhost/  (browser will warn; proceed for local evidence)"
    Write-Host "Health (use -k for self-signed):"
    Write-Host "  curl.exe -k -i https://localhost/livez"
    Write-Host "  curl.exe -k -i https://localhost/readyz"
    Write-Host "  curl.exe -k -i https://localhost/healthz"
    Write-Host "Proxy liveness (HTTP): curl.exe -i http://localhost/proxy-healthz"
    Write-Host ""
    Write-Host "Logs:"
    Write-Host "  docker compose --env-file `"$EnvFile`" -f docker-compose.prod.yml logs --tail=80 backend"
    Write-Host "  docker compose --env-file `"$EnvFile`" -f docker-compose.prod.yml logs --tail=80 backend | Select-String schedulerEvent="
    Write-Host "  docker compose --env-file `"$EnvFile`" -f docker-compose.prod.yml logs --tail=40 database-backup"
    Write-Host ""
    Write-Host "Stop:"
    Write-Host "  docker compose --env-file `"$EnvFile`" -f docker-compose.prod.yml down"
    Write-Host ""
    Write-Host "Evidence helper: .\scripts\Capture-Sprint18DeploymentEvidence.ps1"
}
finally {
    Pop-Location
}
