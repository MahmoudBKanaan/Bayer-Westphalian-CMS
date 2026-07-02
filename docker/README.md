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

Docker resources:

- network: `bwc_local`
- volume: `bwc_postgres_data`
