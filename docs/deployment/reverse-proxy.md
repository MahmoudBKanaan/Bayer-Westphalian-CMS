# Production Reverse Proxy

**Sprint 18 item 721** - Configure reverse proxy with Nginx.

The `reverse-proxy` service in `docker-compose.prod.yml` is the production stack's only published
HTTP entry point. PostgreSQL, backend, and frontend ports remain private to their Compose networks.

## Routing

| Public path | Upstream | Purpose |
| --- | --- | --- |
| `/api/*` | `backend:8080` | Spring REST API without stripping the `/api` prefix |
| `/healthz`, `/readyz` | `backend:8080/actuator/health/readiness` | Application readiness check |
| `/livez` | `backend:8080/actuator/health/liveness` | Backend process liveness |
| `/proxy-healthz` | Nginx | Edge-process liveness check |
| All other paths | `frontend:80` | React SPA and static assets |

Nginx forwards `Host`, client IP, `X-Forwarded-For`, `X-Forwarded-Proto`,
`X-Forwarded-Host`, and `X-Forwarded-Port`. Spring uses
`server.forward-headers-strategy=framework`, so production HTTPS enforcement sees the original
scheme after TLS termination is added.

The proxy also applies baseline browser security headers, a 20 MB request-body limit for supported
uploads, bounded upstream timeouts, upstream keepalive, a read-only filesystem, and
`no-new-privileges`.

## Validate and operate

```powershell
docker compose --env-file .env.production -f docker-compose.prod.yml config
docker compose --env-file .env.production -f docker-compose.prod.yml up -d --build
docker compose --env-file .env.production -f docker-compose.prod.yml ps reverse-proxy
docker compose --env-file .env.production -f docker-compose.prod.yml logs reverse-proxy
```

Item **722** adds [HTTPS/TLS](https.md) to this proxy. Plain HTTP user traffic redirects to HTTPS,
and the TLS listener forwards `X-Forwarded-Proto: https`. With `HTTPS_REQUIRED=true`, backend API
requests that do not carry secure transport evidence remain intentionally rejected.

Automated static evidence: `ProductionReverseProxyDocumentationTests`.
