<#
.SYNOPSIS
  Run / guide Sprint 18 deployment evidence capture (item 743 DEP-01..09 + related screenshots).

.DESCRIPTION
  Optionally starts the full production stack with evidence-local overlay, then prints
  exact commands and document paths for each required screenshot. Does not open .env or secrets.

.PARAMETER StartStack
  Start (or rebuild) the full stack before printing capture steps.

.PARAMETER SkipStart
  Only print capture commands (assume stack already running).

.PARAMETER WaitSeconds
  Seconds to wait after start for health/backup/scheduler (default 120).
#>
[CmdletBinding()]
param(
    [switch]$StartStack,
    [switch]$SkipStart,
    [int]$WaitSeconds = 120,
    [string]$EnvFile
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
if ([string]::IsNullOrWhiteSpace($EnvFile)) {
    $EnvFile = Join-Path $repoRoot ".env.production"
}

function Write-Section([string]$Title) {
    Write-Host ""
    Write-Host "======== $Title ========" -ForegroundColor Cyan
}

Push-Location $repoRoot
try {
    if ($StartStack -and -not $SkipStart) {
        Write-Section "Starting full production stack (evidence-local)"
        & (Join-Path $PSScriptRoot "Start-ProductionStack.ps1") -EvidenceLocal -EnvFile $EnvFile
        Write-Host "Waiting $WaitSeconds seconds for health / first backup / scheduler..."
        Start-Sleep -Seconds $WaitSeconds
    }
    elseif (-not $StartStack) {
        Write-Host "Stack not started by this script (pass -StartStack to boot). Showing live capture commands..."
    }

    $composeBase = @(
        "compose", "--env-file", $EnvFile,
        "-f", "docker-compose.prod.yml",
        "-f", "docker-compose.prod.evidence-local.yml"
    )

    Write-Section "1) Production Docker Compose file (config note)"
    Write-Host "Open in editor and screenshot the service list (no secrets):"
    Write-Host "  $repoRoot\docker-compose.prod.yml"
    Write-Host "Or terminal:"
    Write-Host "  docker compose --env-file .env.production -f docker-compose.prod.yml config --services"
    docker compose --env-file $EnvFile -f docker-compose.prod.yml config --services

    Write-Section "2) Deployment running (DEP-01) — SCREENSHOT THIS OUTPUT"
    Write-Host "Command:"
    Write-Host "  docker compose --env-file .env.production -f docker-compose.prod.yml -f docker-compose.prod.evidence-local.yml ps"
    & docker @composeBase ps
    Write-Host ""
    Write-Host "Expect services: postgres, database-backup, backend, frontend, reverse-proxy"
    Write-Host "Names look like bwc-production-*-1 (project name bwc-production), not campaign-*"

    Write-Section "3) Reverse proxy / HTTPS (DEP-02 or config note)"
    Write-Host "Browser (self-signed warning is OK for local evidence):"
    Write-Host "  https://localhost/"
    Write-Host "Or terminal TLS/headers:"
    Write-Host "  curl.exe -k -I https://localhost/"
    Write-Host "  curl.exe -k -I https://localhost/ | findstr /i Strict-Transport"
    Write-Host "Config note alternative: docs\deployment\https.md + docker\nginx\nginx.conf"
    try {
        curl.exe -k -I --max-time 15 https://localhost/ 2>&1 | Select-Object -First 20
    }
    catch {
        Write-Warning "HTTPS curl failed — is reverse-proxy up? $($_.Exception.Message)"
    }

    Write-Section "4) Health endpoints (DEP-03) — SCREENSHOT"
    Write-Host "  curl.exe -k -i https://localhost/livez"
    Write-Host "  curl.exe -k -i https://localhost/readyz"
    Write-Host "  curl.exe -k -i https://localhost/healthz"
    Write-Host "  curl.exe -i http://localhost/proxy-healthz"
    foreach ($path in @("/livez", "/readyz", "/healthz")) {
        Write-Host "--- https://localhost$path ---"
        curl.exe -k -s -i --max-time 15 "https://localhost$path" 2>&1 | Select-Object -First 15
    }
    Write-Host "--- http://localhost/proxy-healthz ---"
    curl.exe -s -i --max-time 10 "http://localhost/proxy-healthz" 2>&1 | Select-Object -First 12

    Write-Section "5) Application logs (DEP-05) — SCREENSHOT (no secrets)"
    Write-Host "  docker compose ... logs --tail=60 backend"
    & docker @composeBase logs --tail=60 backend 2>&1 |
        Select-String -Pattern "Started CampaignApplication|Tomcat started|schedulerEvent|ready|error|ERROR" -CaseSensitive:$false |
        Select-Object -Last 40

    Write-Host ""
    Write-Host "Full tail for screenshot:"
    & docker @composeBase logs --tail=40 backend

    Write-Section "6) Scheduler logs (DEP-06) — SCREENSHOT"
    Write-Host "With evidence-local overlay, cron is every minute. Filter:"
    Write-Host "  docker compose ... logs backend | Select-String schedulerEvent="
    $sched = & docker @composeBase logs --since 30m backend 2>&1 | Select-String "schedulerEvent="
    if ($sched) {
        $sched | Select-Object -Last 30
    }
    else {
        Write-Warning "No schedulerEvent lines yet. Wait ~60s and re-run:"
        Write-Host "  docker compose --env-file .env.production -f docker-compose.prod.yml -f docker-compose.prod.evidence-local.yml logs --since 10m backend | Select-String schedulerEvent="
    }

    Write-Section "7) Backup evidence (DEP-07) — SCREENSHOT"
    Write-Host "  docker compose ... ps database-backup"
    Write-Host "  docker compose ... logs --tail=30 database-backup"
    & docker @composeBase ps database-backup
    & docker @composeBase logs --tail=30 database-backup

    Write-Section "8) Smoke test checklist (DEP-08) — DOCUMENT SCREENSHOT"
    $smoke = Join-Path $repoRoot "docs\deployment\production-smoke-test-checklist.md"
    Write-Host "Open and screenshot header + execution record + sample checks:"
    Write-Host "  $smoke"
    Write-Host "Execution history folder: docs\deployment\smoke-test-executions\"
    if (Test-Path $smoke) { Write-Host "File exists." } else { Write-Warning "Missing smoke checklist file." }

    Write-Section "9) Rollback plan (DEP-09) — DOCUMENT SCREENSHOT"
    $rollback = Join-Path $repoRoot "docs\deployment\rollback-plan.md"
    Write-Host "Open and screenshot objectives + rollback record fields:"
    Write-Host "  $rollback"
    if (Test-Path $rollback) { Write-Host "File exists." }

    Write-Section "10) Release notes screenshot"
    $release = Join-Path $repoRoot "docs\releases\v1.0-draft.md"
    Write-Host "Open and screenshot title + gate table (status may be BLOCKED — that is honest evidence):"
    Write-Host "  $release"
    if (Test-Path $release) { Write-Host "File exists." }

    Write-Section "11) Compose project images (optional digest evidence)"
    & docker @composeBase images

    Write-Section "Capture tips"
    Write-Host @"
- Filenames: 743-01-compose-ps-<UTC>.png etc. (see docs/deployment/deployment-screenshot-evidence.md)
- Never screenshot .env.production, secret managers, dump contents, or real customer data.
- Self-signed browser padlock may show 'Not secure' — for local evidence, pair with curl -k HTTPS headers + nginx.conf note.
- Real release evidence still needs green main CI, approved hostnames, and item 770 gates.
- Manifest template is in deployment-screenshot-evidence.md
"@

    Write-Section "Done"
    Write-Host "Re-run status only:  docker compose --env-file .env.production -f docker-compose.prod.yml ps"
    Write-Host "Stop stack:         docker compose --env-file .env.production -f docker-compose.prod.yml down"
}
finally {
    Pop-Location
}
