# Docker

Docker resources for local development and deployment preparation.

Expected areas:

- PostgreSQL
- Nginx or Caddy reverse proxy
- setup and maintenance scripts

## Reverse Proxy Placeholders

Placeholder configs are available for future deployment hardening:

- `nginx/nginx.conf`
- `caddy/Caddyfile`

They route frontend assets, `/api/*`, and `/actuator/health`. Production deployment must add HTTPS, domain names, restricted CORS, and real container names before release.

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
