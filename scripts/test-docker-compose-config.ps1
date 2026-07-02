$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$composeFile = Join-Path $projectRoot "docker-compose.yml"

if (-not (Test-Path $composeFile)) {
    throw "docker-compose.yml was not found at $composeFile"
}

$configJson = docker compose -f $composeFile config --format json
if ($LASTEXITCODE -ne 0) {
    throw "docker compose config failed"
}

$config = $configJson | ConvertFrom-Json
$postgres = $config.services.postgres

if ($null -eq $postgres) {
    throw "Expected a postgres service in docker-compose.yml"
}

if ($postgres.image -ne "postgres:16-alpine") {
    throw "Expected postgres image postgres:16-alpine, found $($postgres.image)"
}

if (-not ($postgres.networks.PSObject.Properties.Name -contains "bwc_local")) {
    throw "Expected postgres service to join bwc_local network"
}

if (-not ($config.volumes.PSObject.Properties.Name -contains "bwc_postgres_data")) {
    throw "Expected bwc_postgres_data named volume"
}

if (-not ($config.networks.PSObject.Properties.Name -contains "bwc_local")) {
    throw "Expected bwc_local named network"
}

if ($null -eq $postgres.healthcheck) {
    throw "Expected postgres service healthcheck"
}

Write-Host "Docker Compose config test passed."
