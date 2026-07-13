# Docker

Docker resources for local development and deployment preparation.

Expected areas:

- PostgreSQL
- Nginx or Caddy reverse proxy
- setup and maintenance scripts
- Application images (backend **685**, frontend **686**)

## Backend application image (item **685**)

Multi-stage Dockerfile for the Spring Boot API:

| Path | Purpose |
| --- | --- |
| [`backend/Dockerfile`](../backend/Dockerfile) | Maven package (Java 21) → JRE 21 runtime image |
| [`backend/.dockerignore`](../backend/.dockerignore) | Excludes `target/`, logs, local env files from build context |

CI job `docker-backend` in [`.github/workflows/ci.yml`](../.github/workflows/ci.yml) builds
`bwc-backend:ci` without pushing. Local build:

```powershell
docker build -t bwc-backend:ci -f backend/Dockerfile backend
docker image inspect bwc-backend:ci
```

Do **not** bake production secrets into the image. Pass `SPRING_PROFILES_ACTIVE`, `JWT_SECRET`,
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and related vars at container start.

## Frontend application image (item **686**)

Multi-stage Dockerfile for the React/Vite SPA:

| Path | Purpose |
| --- | --- |
| [`frontend/Dockerfile`](../frontend/Dockerfile) | Node 22 `npm run build` → nginx static SPA |
| [`frontend/nginx.docker.conf`](../frontend/nginx.docker.conf) | SPA `try_files` config for the image |
| [`frontend/.dockerignore`](../frontend/.dockerignore) | Excludes `node_modules/`, `dist/`, logs, env files |

CI job `docker-frontend` builds `bwc-frontend:ci` without pushing. Local build:

```powershell
docker build -t bwc-frontend:ci -f frontend/Dockerfile frontend
docker image inspect bwc-frontend:ci
```

The frontend image serves static assets only. API reverse-proxy to the backend remains a Compose /
host-proxy concern (`nginx/nginx.conf`, `caddy/Caddyfile`).

## Docker Compose validation (item **687**)

Root [`docker-compose.yml`](../docker-compose.yml) defines local PostgreSQL (`postgres:16-alpine`),
named volume `bwc_postgres_data`, network `bwc_local`, and a `pg_isready` healthcheck.

CI job `docker-compose-validate` runs **`docker compose config`** (model validation only — no
`compose up`). Local parity:

```powershell
.\scripts\test-docker-compose-config.ps1
docker compose -f docker-compose.yml config
```

Integration start/smoke of Postgres remains `.\scripts\test-docker-compose-postgres.ps1` (not the
CI validate job).

## Production Reverse Proxy

As of item **721**, `nginx/nginx.conf` is no longer a placeholder: `docker-compose.prod.yml` runs it
as the production `reverse-proxy` service for frontend, `/api/*`, and health routing. It is the only
service publishing an application port. The Caddy file remains an unused alternative example.
See [`docs/deployment/reverse-proxy.md`](../docs/deployment/reverse-proxy.md) and the configured
[production HTTPS guide](../docs/deployment/https.md) (item **722**).

Proxy configuration files:

- `nginx/nginx.conf`
- `caddy/Caddyfile`

They route frontend assets, `/api/*`, and `/actuator/health`. Production deployment must add
**HTTPS** (TLS at the proxy), domain names, restricted CORS (`CORS_ALLOWED_ORIGINS` with
`https://` origins only), and real container names before release (items **540–541**).

### Production HTTPS (item 541)

1. Terminate TLS on Caddy/nginx (port 443); do not expose the Spring Boot port publicly.
2. Reverse-proxy to `backend:8080` and set **`X-Forwarded-Proto: https`** (and `X-Forwarded-Host`).
3. Run the backend with `SPRING_PROFILES_ACTIVE=prod` so `HttpsEnforcementFilter` requires HTTPS
   (or the forwarded proto) for `/api/**` (health endpoints remain HTTP-reachable for probes).
4. Point `CORS_ALLOWED_ORIGINS` at the public **https://** frontend origin(s).

## Local PostgreSQL

The root `docker-compose.yml` starts PostgreSQL for local development.

```bash
docker compose up -d postgres
docker compose ps
docker compose logs -f postgres
docker compose down
```

Default local settings:

| Setting | Value |
| --- | --- |
| Image | `postgres:16-alpine` |
| Database | `bwc_campaign` |
| User | `bwc_app` |
| Password | `bwc_app` |
| Host port | `5432` |
| JDBC URL | `jdbc:postgresql://localhost:5432/bwc_campaign` |

Docker resources:

- network: `bwc_local`
- volume: `bwc_postgres_data`

The Compose service does not pin a global `container_name`. This allows clean checkouts or temporary verification copies to start their own Compose project without colliding with another local checkout.

Override the host port when `5432` is already in use:

```powershell
$env:POSTGRES_PORT = "5433"
docker compose up -d postgres
```

Reset local PostgreSQL data only when a full database wipe is intended:

```bash
docker compose down -v
```

**Warning:** `docker compose down -v` removes the named volume `bwc_postgres_data` and destroys
local data. Take a logical backup first when the data matters.

### Backup and restore (item **666** / NFR-013)

Operator runbook (logical `pg_dump` / `pg_restore`, volume notes, Flyway after restore, testability):

- [`docs/deployment/backup-and-restore.md`](../docs/deployment/backup-and-restore.md)

Critical suite: `BackupAndRestoreProcessIsDocumentedAndTestableTests`.

## Verification

Docker Compose checks are available in `scripts/`:

```powershell
.\scripts\test-docker-compose-config.ps1
.\scripts\test-docker-compose-postgres.ps1
```

| Check | Type | Purpose |
| --- | --- | --- |
| `test-docker-compose-config.ps1` | Unit-style config test | Validates the Compose model for the PostgreSQL service, named volume, named network, and health check |
| `test-docker-compose-postgres.ps1` | Integration test | Starts PostgreSQL, waits for container health, runs `pg_isready`, and executes a SQL smoke query |

The backend PostgreSQL integration test also uses this local database when port `5432` is available.
