#!/bin/sh
set -eu

: "${PGHOST:=postgres}"
: "${PGPORT:=5432}"
: "${PGDATABASE:?PGDATABASE is required}"
: "${PGUSER:?PGUSER is required}"
: "${BACKUP_DIR:=/backups}"
: "${BACKUP_INTERVAL_SECONDS:=86400}"
: "${BACKUP_RETENTION_DAYS:=7}"

case "$BACKUP_INTERVAL_SECONDS:$BACKUP_RETENTION_DAYS" in
  *[!0-9:]*|0:*|*:0) echo "Backup interval and retention must be positive integers" >&2; exit 1 ;;
esac

mkdir -p "$BACKUP_DIR"

create_backup() {
  timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
  final="$BACKUP_DIR/${PGDATABASE}-${timestamp}.dump"
  temporary="${final}.partial"

  trap 'rm -f "$temporary"' INT TERM EXIT
  echo "Starting PostgreSQL backup at $timestamp"
  pg_dump --format=custom --no-password --file="$temporary"
  pg_restore --list "$temporary" >/dev/null
  mv "$temporary" "$final"
  sha256sum "$final" >"${final}.sha256"
  touch "$BACKUP_DIR/.last-success"
  trap - INT TERM EXIT

  find "$BACKUP_DIR" -type f \( -name '*.dump' -o -name '*.dump.sha256' \) \
    -mtime "+$BACKUP_RETENTION_DAYS" -delete
  echo "PostgreSQL backup completed: $(basename "$final")"
}

while true; do
  create_backup
  sleep "$BACKUP_INTERVAL_SECONDS"
done
