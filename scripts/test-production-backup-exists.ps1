param(
    [string]$BackupVolume = "bwc_postgres_backups",
    [int]$MaximumAgeHours = 26
)

$ErrorActionPreference = "Stop"
if ($BackupVolume -notmatch '^[A-Za-z0-9][A-Za-z0-9_.-]+$') {
    throw "BackupVolume contains unsupported characters"
}
if ($MaximumAgeHours -lt 1) {
    throw "MaximumAgeHours must be positive"
}

docker volume inspect $BackupVolume *> $null
if ($LASTEXITCODE -ne 0) {
    throw "Production backup volume does not exist: $BackupVolume"
}

$maximumAgeSeconds = $MaximumAgeHours * 3600
$validationScript = @'
file="$(find /backups -maxdepth 1 -type f -name '*.dump' ! -name '*.partial' -printf '%T@ %p\n' | sort -n | tail -1 | cut -d' ' -f2-)"
test -n "$file"
test -s "$file"
test -s "$file.sha256"
sha256sum -c "$file.sha256" >/dev/null
pg_restore --list "$file" >/dev/null
modified="$(stat -c '%Y' "$file")"
now="$(date +%s)"
age="$((now - modified))"
test "$age" -ge 0
test "$age" -le "$MAXIMUM_AGE_SECONDS"
printf '%s\n%s\n' "$(basename "$file")" "$(stat -c '%s' "$file")"
'@
$result = & docker run --rm `
    --network none `
    --mount "type=volume,src=$BackupVolume,dst=/backups,readonly" `
    --env "MAXIMUM_AGE_SECONDS=$maximumAgeSeconds" `
    postgres:16-alpine sh -eu -c $validationScript 2>$null

if ($LASTEXITCODE -ne 0) {
    throw "No fresh, non-empty, checksum-valid, pg_restore-readable backup exists"
}
$lines = @($result | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
if ($lines.Count -ne 2 -or [long]$lines[1] -le 0) {
    throw "Backup verification returned invalid sanitized evidence"
}

Write-Host "Production backup existence verification passed."
Write-Host "Artifact: $($lines[0])"
Write-Host "Size: $($lines[1]) bytes"
Write-Host "Freshness: within $MaximumAgeHours hours"
Write-Host "Integrity: SHA-256 verified and pg_restore readable"
Write-Host "Access: backup volume mounted read-only with networking disabled"
