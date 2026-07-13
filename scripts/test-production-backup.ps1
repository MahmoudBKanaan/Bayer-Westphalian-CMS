param(
    [string]$EnvFile = ".env.production",
    [string]$ComposeFile = "docker-compose.prod.yml",
    [int]$TimeoutSeconds = 180
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$envPath = [System.IO.Path]::GetFullPath((Join-Path $projectRoot $EnvFile))
$composePath = [System.IO.Path]::GetFullPath((Join-Path $projectRoot $ComposeFile))

if (-not (Test-Path -LiteralPath $envPath -PathType Leaf)) {
    throw "Production environment file not found: $envPath"
}
if (-not (Test-Path -LiteralPath $composePath -PathType Leaf)) {
    throw "Production Compose file not found: $composePath"
}
if ($TimeoutSeconds -lt 30) {
    throw "TimeoutSeconds must be at least 30"
}

function Invoke-Compose {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments)

    & docker compose --env-file $envPath -f $composePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose command failed: $($Arguments -join ' ')"
    }
}

function Get-BackupNames {
    $output = & docker compose --env-file $envPath -f $composePath run --rm --no-deps `
        --entrypoint sh database-backup -c "find /backups -maxdepth 1 -type f -name '*.dump' -printf '%f`n' | sort"
    if ($LASTEXITCODE -ne 0) {
        throw "Could not inspect the production backup volume"
    }
    return @($output | Where-Object { $_ -match '\.dump$' })
}

$before = @(Get-BackupNames)
Invoke-Compose up -d postgres
Invoke-Compose up -d database-backup
Invoke-Compose restart database-backup

$deadline = [DateTimeOffset]::UtcNow.AddSeconds($TimeoutSeconds)
$created = $null
do {
    Start-Sleep -Seconds 2
    $current = @(Get-BackupNames)
    $created = $current | Where-Object { $_ -notin $before } | Select-Object -Last 1
} while (-not $created -and [DateTimeOffset]::UtcNow -lt $deadline)

if (-not $created) {
    Invoke-Compose logs --tail 100 database-backup
    throw "No new PostgreSQL dump was created within $TimeoutSeconds seconds"
}

$validation = & docker compose --env-file $envPath -f $composePath exec -T database-backup `
    sh -eu -c 'file="/backups/$1"; test -s "$file"; test -s "$file.sha256"; sha256sum -c "$file.sha256" >/dev/null; pg_restore --list "$file" >/dev/null; stat -c "%s" "$file"' -- $created
if ($LASTEXITCODE -ne 0) {
    throw "Backup artifact validation failed for $created"
}

$size = [long]($validation | Select-Object -Last 1)
if ($size -le 0) {
    throw "Backup artifact is empty: $created"
}

Write-Host "Production backup creation test passed."
Write-Host "Artifact: $created"
Write-Host "Size: $size bytes"
Write-Host "Validation: non-empty, SHA-256 verified, pg_restore readable"
