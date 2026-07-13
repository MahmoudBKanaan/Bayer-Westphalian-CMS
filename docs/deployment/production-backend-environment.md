# Production Backend Environment

**Sprint 18 item 718** - Configure production backend environment.

The backend production contract consists of `application-prod.yml`,
`backend/.env.production.example`, startup environment/secret validators, and the backend service in
`docker-compose.prod.yml`.

## Required values

`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, and `CORS_ALLOWED_ORIGINS` must be supplied by
the deployment environment. `DB_PASSWORD` and `JWT_SECRET` must satisfy
`SecretPresenceValidator`; CORS must contain explicit HTTPS origins and cannot contain a wildcard.
The application fails startup safely when required values are absent or unresolved.

Database provisioning and rotation: [Production Database Credentials](database-credentials.md)
(item **725**).

The example file intentionally leaves secrets and the deployment-specific CORS origin blank. Never
commit a populated `.env.production` file.

## Production defaults

- Spring profile is `prod`; Hibernate validates the schema and Flyway owns migrations.
- Flyway clean is disabled.
- Hikari connection-pool size and timeouts are bounded and environment-configurable.
- Graceful shutdown allows in-flight requests to finish within the configured timeout.
- Actuator exposes only `health` and `info`; health details are hidden from clients.
- OpenAPI JSON and Swagger UI are disabled unless an operator explicitly enables them.
- Stack traces, exception messages, and binding errors remain hidden.
- Operational stdout, request correlation, and retention are configured in
  [Production Logging](production-logging.md) (item **729**).
- HTTPS/HSTS and login-rate limiting remain enabled with conservative defaults.
- Real provider sending remains disabled until an approved delivery adapter is configured.
- Production email uses the explicit disabled provider described in
  [Production Email Provider](email-provider.md) (item **726**); mock email is dev/test only.
- Production SMS uses the explicit disabled provider described in
  [Production SMS Provider](sms-provider.md) (item **727**); mock SMS is dev/test only.

Use the production template as an inventory, then inject values through the host environment or a
secret manager:

```powershell
docker compose --env-file .env.production -f docker-compose.prod.yml config
docker compose --env-file .env.production -f docker-compose.prod.yml up -d --build backend
```

Automated static evidence: `ProductionBackendEnvironmentDocumentationTests`.
