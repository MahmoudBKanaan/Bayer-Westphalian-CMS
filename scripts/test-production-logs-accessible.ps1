param(
    [string]$EnvFile = ".env.production",
    [string]$ComposeFile = "docker-compose.prod.yml",
    [int]$TailLines = 50
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$envPath = [System.IO.Path]::GetFullPath((Join-Path $projectRoot $EnvFile))
$composePath = [System.IO.Path]::GetFullPath((Join-Path $projectRoot $ComposeFile))
if (-not (Test-Path -LiteralPath $envPath -PathType Leaf)) { throw "Environment file not found" }
if (-not (Test-Path -LiteralPath $composePath -PathType Leaf)) { throw "Compose file not found" }
if ($TailLines -lt 1 -or $TailLines -gt 500) { throw "TailLines must be between 1 and 500" }

$services = @("postgres", "database-backup", "backend", "frontend", "reverse-proxy")
$secretPattern = '(?i)(authorization|cookie|set-cookie|password|jwt_secret|api[_-]?key)\s*[:=]\s*\S+'
$results = @()

foreach ($service in $services) {
    $containerId = & docker compose --env-file $envPath -f $composePath ps -q $service 2>$null
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($containerId)) {
        throw "Production service is not running: $service"
    }

    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $lines = @(& docker logs --tail $TailLines $containerId 2>&1 | ForEach-Object { "$_" })
    $exitCode = $LASTEXITCODE
    $ErrorActionPreference = $previousPreference
    if ($exitCode -ne 0) { throw "Logs are not accessible for service: $service" }
    if ($lines.Count -lt 1) { throw "Accessible logs are unexpectedly empty for service: $service" }
    if (($lines -join "`n") -match $secretPattern) {
        throw "Potential secret-bearing log output detected for service: $service"
    }

    $results += [pscustomobject]@{ Service = $service; Lines = $lines.Count }
    $lines = $null
}

Write-Host "Production log accessibility verification passed."
foreach ($result in $results) {
    Write-Host "Service: $($result.Service); bounded lines accessible: $($result.Lines)"
}
Write-Host "Scheduler: accessible through bounded backend logs"
Write-Host "Privacy: no configured secret-assignment pattern detected"
