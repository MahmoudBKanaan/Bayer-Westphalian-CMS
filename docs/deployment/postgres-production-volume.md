# PostgreSQL Production Volume

**Sprint 18 item 720** - Configure PostgreSQL production volume.

PostgreSQL production data is stored in the Docker-managed named volume
`bwc_postgres_prod_data`. The name is stable across Compose project-directory changes and can be
overridden with `POSTGRES_VOLUME_NAME` for a specific host or deployment.

## Storage contract

- The volume mounts at `/var/lib/postgresql/data` and `PGDATA` is the `pgdata` subdirectory.
- PostgreSQL initialization enables data checksums and UTF-8 for newly created volumes.
- Existing volumes are not reinitialized when initialization arguments change.
- PostgreSQL has a configurable shared-memory allocation and a 60-second graceful-stop allowance.
- The volume is labeled as production PostgreSQL state and backup-required.
- The database remains on the internal Compose network with no published host port.
- `docker compose down` preserves the volume; `docker compose down -v` deletes it and must not be
  used in production operations.

The Docker `local` volume driver provides persistence on one Docker host. It is not replication,
off-host backup, encryption, or disaster recovery. Host storage encryption and scheduled logical
backups remain operator responsibilities.

## Inspect

```powershell
docker volume inspect bwc_postgres_prod_data
docker compose --env-file .env.production -f docker-compose.prod.yml ps postgres
docker compose --env-file .env.production -f docker-compose.prod.yml exec postgres `
  psql -U bwc_app -d bwc_campaign -c "SHOW data_checksums;"
```

## Backup before maintenance

Create a logical dump outside the container and outside Git before upgrades or volume migration:

```powershell
New-Item -ItemType Directory -Force -Path C:\bwc-backups | Out-Null
docker compose --env-file .env.production -f docker-compose.prod.yml exec -T postgres `
  pg_dump -U bwc_app -d bwc_campaign -Fc > C:\bwc-backups\bwc_campaign.dump
```

Verify and restore dumps on a non-production database according to
[Backup and Restore Process](backup-and-restore.md). A volume snapshot must be taken only while
PostgreSQL is cleanly stopped or by storage tooling that guarantees an application-consistent
snapshot.

Automated static evidence: `PostgreSqlProductionVolumeDocumentationTests`.
