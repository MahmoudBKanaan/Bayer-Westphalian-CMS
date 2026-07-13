# Production Logging

**Sprint 18 item 729** - Configure production logging.

Production services write to standard output/error. Docker captures those streams with the `local`
logging driver and bounded rotation; application containers do not write log files into their
read-only filesystems.

## Application format and correlation

Backend lines use stable key-value fields: timestamp, level, service, request ID, thread, logger,
and message. `RequestCorrelationFilter` accepts a safe `X-Request-Id` or generates a UUID, returns it
to the client, places it in SLF4J MDC for the request, and clears MDC after completion. Unsafe,
oversized, or newline-containing IDs are replaced to prevent log injection.

Default levels are:

| Scope | Default |
| --- | --- |
| Root | `INFO` |
| Application package | `INFO` |
| Spring framework | `WARN` |
| Hibernate SQL | `WARN` |

Override with `LOG_LEVEL_ROOT`, `LOG_LEVEL_APPLICATION`, `LOG_LEVEL_SPRING`, and
`LOG_LEVEL_HIBERNATE_SQL`. Do not enable SQL parameter/value logging in production.

## Rotation and retention

All Compose services use Docker's `local` logging driver. Defaults are 10 MiB per file and five
files per container, configured by `LOG_MAX_SIZE` and `LOG_MAX_FILES`. These limits protect host
disk availability; they are not a compliance archive or centralized error-tracking system.

```powershell
docker compose --env-file .env.production -f docker-compose.prod.yml logs --tail 200 backend
docker compose --env-file .env.production -f docker-compose.prod.yml logs --since 30m reverse-proxy
docker inspect --format "{{json .HostConfig.LogConfig}}" bwc-production-backend-1
```

Container names can vary; obtain the actual name with `docker compose ps`.

## Security and operations

- Never log passwords, JWTs, provider keys, authorization/cookie headers, consent evidence bytes,
  complete customer records, or environment dumps.
- `SafeApiErrorLogger` records bounded request context and exception type without secret headers or
  request bodies.
- Operational logs do not replace immutable application `AuditLog` records for sensitive actions.
- Restrict Docker/log access to operators, avoid screenshots containing personal data, and use
  value-free incident evidence.
- For longer retention, ship stdout to an access-controlled centralized platform with documented
  retention, redaction, alerting, and deletion policies.

Scheduler-specific fields and operational queries: [Production Scheduler Logging](scheduler-logging.md)
(item **730**).

Automated evidence: `RequestCorrelationFilterTests` and
`ProductionLoggingConfigurationDocumentationTests`.

## Production Log Accessibility Verification (Item 758)

Run after deployment with the uncommitted production environment file:

```powershell
.\scripts\test-production-logs-accessible.ps1 -EnvFile .env.production -TailLines 50
```

The read-only verifier resolves each production container and reads a bounded tail for `postgres`,
`database-backup`, `backend`, `frontend`, and `reverse-proxy`. Scheduler logs are part of backend
stdout. It requires non-empty accessible output, scans in memory for common secret-assignment
patterns, discards raw lines, and prints only service name and line count. This scan is defense in
depth, not a substitute for application redaction or controlled log access.

Execution at `2026-07-13T00:29:40+03:00` is **BLOCKED**. Only development PostgreSQL is running;
its Docker logs are accessible, but no production backend/frontend/proxy/backup logs exist. Production
Compose also cannot resolve without required uncommitted environment values. Development database
logs are not release evidence and may contain SQL/test details, so their contents were not retained.

Item 758 passes only when all five deployed services have bounded logs accessible to an authorized
operator, scheduler events are retrievable from backend logs, access is denied to normal users, and
evidence contains no raw secrets/customer payloads. Record UTC time, environment, image digests,
service/count results, operator, access-control review, and approver.
