$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$composeFile = Join-Path $projectRoot "docker-compose.yml"

docker compose -f $composeFile up -d postgres
if ($LASTEXITCODE -ne 0) {
    throw "docker compose up -d postgres failed"
}

$containerId = docker compose -f $composeFile ps -q postgres
if ([string]::IsNullOrWhiteSpace($containerId)) {
    throw "Could not resolve the Docker Compose postgres container ID"
}

$healthy = $false
for ($attempt = 1; $attempt -le 30; $attempt++) {
    $status = docker inspect --format="{{.State.Health.Status}}" $containerId 2>$null
    if ($status -eq "healthy") {
        $healthy = $true
        break
    }

    Start-Sleep -Seconds 2
}

if (-not $healthy) {
    docker logs --tail 80 $containerId
    throw "Docker Compose postgres service did not become healthy"
}

docker exec $containerId pg_isready -U bwc_app -d bwc_campaign
if ($LASTEXITCODE -ne 0) {
    throw "pg_isready failed for bwc_campaign"
}

$query = "select current_database() as database, current_user as username;"
$result = docker exec $containerId psql -U bwc_app -d bwc_campaign -t -A -F "," -c $query
if ($LASTEXITCODE -ne 0) {
    throw "PostgreSQL smoke query failed"
}

if ($result.Trim() -ne "bwc_campaign,bwc_app") {
    throw "Unexpected PostgreSQL query result: $result"
}

Write-Host "Docker Compose PostgreSQL integration test passed."
