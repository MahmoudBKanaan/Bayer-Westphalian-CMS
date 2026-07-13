# Production Health Endpoints

**Sprint 18 item 731** - Enable health endpoint.

Spring Boot Actuator health probes are enabled on the main backend port and exposed through Nginx
without authentication. Responses never show component names or details in production.

## Endpoint contract

| Public endpoint | Backend target | Meaning |
| --- | --- | --- |
| `/livez` | `/actuator/health/liveness` | Process is alive; excludes external dependencies |
| `/readyz` | `/actuator/health/readiness` | Ready to receive traffic |
| `/healthz` | `/actuator/health/readiness` | Compatibility alias for readiness |
| `/proxy-healthz` | Nginx local response | Reverse-proxy process liveness |
| `/api/health` | Custom application response | Service identity/timestamp; not dependency readiness |

Readiness includes PostgreSQL, disk space, Spring readiness state, and the writable
consent-evidence storage mount. The backend container health check uses readiness. A database or
evidence-volume outage therefore makes the container unhealthy rather than reporting a false UP.

Liveness intentionally excludes PostgreSQL and storage. Do not restart a healthy process merely
because a dependency is temporarily unavailable; remove it from traffic using readiness while the
dependency is repaired.

## Security

- Health endpoints return only aggregate `UP`/`DOWN` status in production.
- They do not expose database URLs, usernames, filesystem paths, exception messages, versions,
  secrets, customer data, scheduler state, or stack traces.
- HTTPS remains the public transport. Backend plain-HTTP health access is limited to internal
  container/proxy probes.
- Health status is operational evidence, not proof that business workflows or backups work.

## Verification

```powershell
curl.exe -i https://campaign.example.com/livez
curl.exe -i https://campaign.example.com/readyz
curl.exe -i https://campaign.example.com/healthz
docker compose --env-file .env.production -f docker-compose.prod.yml ps backend
```

Expect HTTP 200 and `{"status":"UP"}` when healthy. Alert on sustained readiness failure, any
liveness failure, or repeated transitions. Use correlated backend, database, storage, and proxy logs
to diagnose without changing health responses to reveal details.

Automated evidence: `HealthEndpointIntegrationTests`, `HealthControllerTests`,
`ConsentEvidenceStorageHealthIndicatorTests`, and `ProductionHealthEndpointDocumentationTests`.
Operational thresholds and external-monitor requirements are defined in
[Production Operational Monitoring Notes](operational-monitoring-notes.md) (item **741**).
