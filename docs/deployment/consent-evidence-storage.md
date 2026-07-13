# Consent Evidence File Storage

**Sprint 18 item 728** - Configure file storage for consent evidence.

The KB distinguishes the development local placeholder from secure production storage. Production
uses `FileSystemConsentEvidenceStorage` with a dedicated Docker-managed persistent volume. The
database stores only the opaque storage reference; file bytes remain outside PostgreSQL.

## Production contract

- `FILE_STORAGE_MODE=filesystem`
- `FILE_STORAGE_LOCAL_PATH=/app/data/consent-evidence`
- `FILE_STORAGE_MAX_BYTES=10485760` (10 MiB default)
- `CONSENT_EVIDENCE_VOLUME_NAME=bwc_consent_evidence`

The adapter accepts PDF, PNG, and JPEG content types and verifies their basic file signatures. It
ignores the supplied filename for storage, generates a UUID filename under a customer-specific
directory, normalizes paths, rejects references that escape the configured root, rejects
empty/oversized files, and writes through a temporary file before an atomic move where supported.

Storage references are internal identifiers such as
`consent-evidence/{customerUuid}/{generatedUuid}.pdf`. They are not public URLs and must not be
served directly by Nginx. Any future upload/download endpoint must enforce backend authorization,
customer access, audit logging, content verification, and safe response headers.

## Persistence and protection

The named volume is stable, labeled `backup-required`, and mounted only into the backend. It is not
published by the reverse proxy. Docker local volumes persist on one host but do not provide
encryption, replication, malware scanning, retention, or off-host disaster recovery. Use encrypted
host storage, least-privilege Docker access, and an approved backup destination.

Consent evidence may contain signatures, identity data, or guardian documentation. Treat it as
sensitive personal/compliance data. Do not put files, volume exports, filenames, or content in Git,
logs, screenshots, tickets, demo datasets, or public object storage.

## Operations

```powershell
docker volume inspect bwc_consent_evidence
docker compose --env-file .env.production -f docker-compose.prod.yml exec backend `
  sh -c "test -d /app/data/consent-evidence && test -w /app/data/consent-evidence"
```

Back up the evidence volume together with the PostgreSQL logical backup so database references and
files represent the same recovery point. Restore into a non-production environment first, verify
references resolve, and never use `docker compose down -v` in production.

Automated evidence: `FileSystemConsentEvidenceStorageTests` and
`ConsentEvidenceStorageDocumentationTests`.
