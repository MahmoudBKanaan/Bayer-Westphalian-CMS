# Production Security Checklist

KB item **563** records the production security checklist for the Bayer-Westphalian Campaign
Management Platform. Complete this checklist before a production candidate is exposed to real
users or real customer data. Acceptance item **566** is satisfied only when sensitive actions are
auditable and production does not expose secrets, stack traces, or unsafe configuration.

## Required Production Profile

- Set `SPRING_PROFILES_ACTIVE=prod`.
- Confirm `application-prod.yml` is loaded.
- Confirm demo-only behavior is not used for consent, opt-outs, eligibility, campaign approval,
  audit logs, permissions, or customer communication history.

## Required Environment And Secrets

Production startup must fail fast unless all required values are present and safe:

| Variable | Requirement |
| --- | --- |
| `DB_URL` | Required; JDBC URL |
| `DB_USERNAME` | Required; non-blank |
| `DB_PASSWORD` | Required; non-blank and secret-presence validated |
| `JWT_SECRET` | Required; strong value, at least 32 characters for secret validation |
| `CORS_ALLOWED_ORIGINS` | Required; explicit `https://` frontend origins only |
| `SMTP_PASSWORD` | Required when `PROVIDER_REAL_SENDING_ENABLED=true` and `EMAIL_PROVIDER_MODE=smtp` |
| `SMS_API_KEY` | Required when `PROVIDER_REAL_SENDING_ENABLED=true` and `SMS_PROVIDER_MODE=provider` |

Environment variable **templates** (placeholders only) live under `.env.example`,
`backend/.env.example`, and `frontend/.env.example` (item **688** —
[environment-variables.md](environment-variables.md)).

Ops **secrets** process (item **689**): [secrets.md](secrets.md).

CI **production config validation** (item **690**): job `production-config-validate` in
[ci-cd.md](ci-cd.md) statically checks `application-prod.yml`, validators, and templates.

Validation components:

- `EnvironmentVariableValidator`
- `SecretPresenceValidator`
- `ProductionEnvironmentPostProcessor`

Sprint 16 critical item **665** (*Missing secrets are detected*):
`MissingSecretsAreDetectedTests` (companion `SecretPresenceValidatorTests`).

Safe failure rule: configuration errors may name missing keys, but must not print configured secret
values.

## HTTPS And Transport Security

- Set `HTTPS_REQUIRED=true` for production unless an emergency break-glass deployment is formally
  documented.
- Terminate TLS at the reverse proxy and forward `X-Forwarded-Proto: https`.
- Confirm plain HTTP API calls without forwarded HTTPS are rejected with `HTTPS_REQUIRED`.
- Confirm health endpoints remain available for internal probes.
- Confirm `Strict-Transport-Security` is sent only on secure or forwarded-HTTPS production
  responses.
- Confirm frontend URLs and `CORS_ALLOWED_ORIGINS` use `https://`.

Primary components:

- `HttpsEnforcementFilter`
- `ProductionHttpsProperties`
- `SecurityConfiguration`

## Production CORS

- `CORS_ALLOWED_ORIGINS` must be explicit.
- Wildcards such as `*` or `https://*.example.com` are forbidden.
- `localhost` and `127.0.0.1` are forbidden in production.
- Plain `http://` origins are forbidden in production.
- `allowCredentials=true` is allowed only with the explicit origin list.
- Preflight `maxAge` remains 3600 seconds.

## Secure Errors And Stack Traces

- `server.error.include-stacktrace: never`
- `server.error.include-message: never`
- `server.error.include-binding-errors: never`
- `server.error.include-exception: false`
- Sprint 16 critical item **664** (*Production profile hides stack traces*):
  `ProductionProfileHidesStackTracesTests` (companion `ProductionStackTraceHiddenTests`).
- API unexpected errors return `INTERNAL_ERROR` and `Unexpected server error`.
- Client responses must not include `trace`, `stackTrace`, `exception`, Java class names, or stack
  frames.

Primary components:

- `GlobalExceptionHandler`
- `SecureErrorResponses`
- `ProductionErrorSafetyConfiguration`

## Authentication And Rate Limiting

- Passwords are stored with BCrypt.
- JWT signing uses the production `JWT_SECRET`.
- Login lockout is enabled with `LOGIN_RATE_LIMIT_MAX_FAILURES`,
  `LOGIN_RATE_LIMIT_FAILURE_WINDOW_MINUTES`, and `LOGIN_RATE_LIMIT_LOCKOUT_MINUTES`.
- Lockout responses use `LOGIN_RATE_LIMITED` and `Retry-After` without exposing whether the email
  exists.
- Role-based access remains enforced by backend security and method authorization.

## Security Headers

Confirm API responses include:

- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `Referrer-Policy: no-referrer`
- `Permissions-Policy`
- `Content-Security-Policy`
- `X-Permitted-Cross-Domain-Policies: none`
- `Cache-Control: no-store, no-cache, must-revalidate, max-age=0`
- `Pragma: no-cache`

Primary components:

- `SecurityConfiguration.configureSecurityHeaders`
- `ApiSecurityHeadersFilter`

## Safe Logging

- API error logs use `SafeApiErrorLogger`.
- Logs must not include raw `Authorization`, `Cookie`, bearer tokens, JWT strings, passwords,
  refresh tokens, API keys, or request bodies.
- Validation logs record counts and context, not rejected secret values.
- Stack traces may be logged server-side for unexpected errors, but never returned to clients.

## Audit And Accountability

- Sensitive actions write immutable `AuditLog` rows.
- Audit log API and UI remain read-only.
- `GET /api/audit-logs` and entity history are restricted to `ADMIN`, `COMPLIANCE_OFFICER`, and
  `SYSTEM_AUDITOR`.
- Normal business roles cannot view audit logs.
- Audit report export is restricted to audit roles and writes an `EXPORT_REPORT` audit entry.
- System Auditor guide documents read-only audit, consent history, campaign approval history, user
  activity history, and audit export workflows.

## Backup And Restore (Item **666** / NFR-013)

- Follow [`backup-and-restore.md`](backup-and-restore.md) for scheduled PostgreSQL logical backups
  (`pg_dump` / `pg_restore`), restore practice, and Flyway post-restore checks.
- Confirm at least one successful dump exists before release-candidate cutover demos that wipe
  environments.
- Secrets remain outside dumps’ Git history — re-apply from the secret store after restore (item
  **665**).
- Automated evidence: `BackupAndRestoreProcessIsDocumentedAndTestableTests` (companions: Flyway /
  Postgres integration tests).

## Go / No-Go Evidence

Before sign-off, collect evidence for:

- Environment validation failure when required variables are missing.
- Secret validation failure without leaking values.
- HTTPS enforcement and HSTS behavior.
- Production CORS rejection for wildcard, localhost, and plain HTTP origins.
- Production errors without stack traces.
- Login lockout response and `Retry-After`.
- Security headers on API responses.
- Audit log access denial for unauthorized users.
- System Auditor access to audit logs and audit export only.
- Backup/restore runbook present and practiced on a non-production database (item **666**).

Related documentation:

- [`../architecture/security-hardening.md`](../architecture/security-hardening.md)
- [`../modules/audit-logging.md`](../modules/audit-logging.md)
- [`backup-and-restore.md`](backup-and-restore.md)
- [`../user-guides/system-auditor-guide.md`](../user-guides/system-auditor-guide.md)
