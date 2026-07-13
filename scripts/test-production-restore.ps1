param(
    [string]$BackupVolume = "bwc_postgres_backups",
    [string]$DumpName = "",
    [int]$TimeoutSeconds = 120
)

$ErrorActionPreference = "Stop"
if ($BackupVolume -notmatch '^[A-Za-z0-9][A-Za-z0-9_.-]+$') {
    throw "BackupVolume contains unsupported characters"
}
if ($DumpName -and $DumpName -notmatch '^[A-Za-z0-9_.-]+\.dump$') {
    throw "DumpName must be a basename ending in .dump"
}
if ($TimeoutSeconds -lt 30) {
    throw "TimeoutSeconds must be at least 30"
}

$containerName = "bwc-restore-rehearsal-$([Guid]::NewGuid().ToString('N').Substring(0, 12))"
$databaseName = "bwc_restore_rehearsal"
$databaseUser = "bwc_restore_operator"
$databasePassword = "Restore-$([Guid]::NewGuid().ToString('N'))-A1"

try {
    docker volume inspect $BackupVolume *> $null
    if ($LASTEXITCODE -ne 0) {
        throw "Backup volume does not exist: $BackupVolume"
    }

    if (-not $DumpName) {
        $DumpName = (& docker run --rm --mount "type=volume,src=$BackupVolume,dst=/backups,readonly" `
            postgres:16-alpine sh -eu -c "find /backups -maxdepth 1 -type f -name '*.dump' -printf '%f`n' | sort | tail -1").Trim()
        if ($LASTEXITCODE -ne 0 -or -not $DumpName) {
            throw "No completed PostgreSQL dump exists in volume $BackupVolume"
        }
    }

    & docker run --rm --mount "type=volume,src=$BackupVolume,dst=/backups,readonly" `
        postgres:16-alpine sh -eu -c `
        'file="/backups/$1"; test -s "$file"; test -s "$file.sha256"; sha256sum -c "$file.sha256" >/dev/null; pg_restore --list "$file" >/dev/null' `
        -- $DumpName
    if ($LASTEXITCODE -ne 0) {
        throw "Backup preflight validation failed: $DumpName"
    }

    & docker run -d --name $containerName `
        --label "com.bayer-westphalian.purpose=restore-rehearsal" `
        --network none `
        --tmpfs "/var/lib/postgresql/data:rw,noexec,nosuid,size=1g" `
        --mount "type=volume,src=$BackupVolume,dst=/backups,readonly" `
        --env "POSTGRES_DB=$databaseName" `
        --env "POSTGRES_USER=$databaseUser" `
        --env "POSTGRES_PASSWORD=$databasePassword" `
        postgres:16-alpine *> $null
    if ($LASTEXITCODE -ne 0) {
        throw "Could not start isolated PostgreSQL restore container"
    }

    $deadline = [DateTimeOffset]::UtcNow.AddSeconds($TimeoutSeconds)
    do {
        Start-Sleep -Seconds 2
        & docker exec $containerName pg_isready -U $databaseUser -d $databaseName *> $null
        $ready = $LASTEXITCODE -eq 0
    } while (-not $ready -and [DateTimeOffset]::UtcNow -lt $deadline)
    if (-not $ready) {
        & docker logs --tail 100 $containerName
        throw "Isolated restore database did not become ready"
    }

    & docker exec $containerName sh -eu -c `
        'pg_restore --exit-on-error --no-owner --no-privileges -U "$POSTGRES_USER" -d "$POSTGRES_DB" "/backups/$1"' `
        -- $DumpName
    if ($LASTEXITCODE -ne 0) {
        throw "pg_restore failed for $DumpName"
    }

    $validationSql = @'
SELECT count(*) FROM flyway_schema_history WHERE success = true;
SELECT to_regclass('public.users') IS NOT NULL;
SELECT to_regclass('public.customers') IS NOT NULL;
SELECT to_regclass('public.campaigns') IS NOT NULL;
SELECT to_regclass('public.audit_logs') IS NOT NULL;
'@
    $validation = & docker exec $containerName psql -X -v ON_ERROR_STOP=1 -U $databaseUser `
        -d $databaseName -t -A -c $validationSql
    if ($LASTEXITCODE -ne 0) {
        throw "Post-restore schema validation failed"
    }

    $values = @($validation | ForEach-Object { $_.Trim() } | Where-Object { $_ })
    if ($values.Count -lt 5 -or [int]$values[0] -le 0 -or ($values[1..4] | Where-Object { $_ -ne "t" })) {
        throw "Restored database is missing successful Flyway history or required core tables"
    }

    Write-Host "Non-production restore rehearsal passed."
    Write-Host "Artifact: $DumpName"
    Write-Host "Flyway migrations: $($values[0]) successful entries"
    Write-Host "Core schema: users, customers, campaigns, audit_logs present"
    Write-Host "Isolation: temporary container, no network, tmpfs data, read-only backup mount"
}
finally {
    & docker rm -f $containerName *> $null
}
