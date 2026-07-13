# Production Frontend Environment

**Sprint 18 item 719** - Configure production frontend environment.

Vite variables are compile-time browser configuration. They are passed to the frontend Docker
build by `docker-compose.prod.yml`; they are not runtime secrets and are visible in the generated
JavaScript bundle.

## Production contract

- `VITE_APP_ENV=prod` identifies the production frontend build.
- `VITE_API_BASE_URL=/api` uses a same-origin API path. The production reverse proxy must route
  `/api` to the backend service.
- An explicit API origin is allowed only when it uses HTTPS.
- Production builds reject localhost, loopback, and plain HTTP API URLs.
- The production Dockerfile defaults to `/api`, preventing the development localhost fallback from
  being baked into the image.
- `index.html` is not cached, while fingerprinted `/assets/` files receive immutable caching.
- Nginx emits baseline browser security headers and hides its version token.

The safe template is `frontend/.env.production.example`. Do not place credentials, JWT secrets,
provider keys, or any other confidential values in variables prefixed with `VITE_`.

```powershell
docker compose --env-file .env.production -f docker-compose.prod.yml build frontend
docker compose --env-file .env.production -f docker-compose.prod.yml up -d frontend
```

The `/api` forwarding and public HTTP entry point are configured by the
[production reverse proxy](reverse-proxy.md) (item **721**). The external listener uses
[HTTPS](https.md) (item **722**) and redirects plain HTTP traffic.

Automated evidence: `productionFrontendEnvironment.test.ts` and
`ProductionFrontendEnvironmentDocumentationTests`.
