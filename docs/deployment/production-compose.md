# Production Docker Compose

**Sprint 18 item 717** - Create production Docker Compose file.

The production stack is defined in [`docker-compose.prod.yml`](../../docker-compose.prod.yml). It
is separate from the local PostgreSQL-only `docker-compose.yml` and contains `postgres`, `backend`,
and `frontend` services.

## Safety and topology

- PostgreSQL is reachable only through the internal `database` network and has no host port.
- The backend uses the `prod` Spring profile and waits for healthy PostgreSQL.
- Backend liveness/readiness is defined in [Production Health Endpoints](health-endpoints.md)
  (item **731**).
- The frontend waits for a healthy backend and remains private to the application network.
- The Nginx reverse proxy is the only service publishing an HTTP host port; see
  [Production Reverse Proxy](reverse-proxy.md) (item **721**).
- Database data and consent evidence use named persistent volumes.
- Consent files use the protected volume and adapter described in
  [Consent Evidence File Storage](consent-evidence-storage.md) (item **728**).
- PostgreSQL uses the stable production volume described in
  [PostgreSQL Production Volume](postgres-production-volume.md) (item **720**).
- Required production values use Compose required interpolation and have no committed fallback:
  `DB_PASSWORD`, `JWT_SECRET`, and `CORS_ALLOWED_ORIGINS`.
- Containers use restart policies, health checks, `no-new-privileges`, and read-only filesystems
  where the image supports them.
- Provider sending defaults to disabled/mock. Enabling a real provider requires its corresponding
  credentials and the production security checklist.

## Validate and start

Provide required values through the deployment environment or an uncommitted env file. Never put
real credentials in this Compose file.

```powershell
docker compose --env-file .env.production -f docker-compose.prod.yml config
docker compose --env-file .env.production -f docker-compose.prod.yml up -d --build
docker compose --env-file .env.production -f docker-compose.prod.yml ps
```

The reverse proxy routes the SPA and API and terminates [HTTPS](https.md) (item **722**).
`HTTPS_REQUIRED` remains enabled by default, certificates are injected as read-only host mounts,
and production API traffic arrives through the trusted HTTPS-aware proxy.

Automated static evidence: `ProductionDockerComposeDocumentationTests`.
