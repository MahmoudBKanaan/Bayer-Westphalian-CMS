# Production JWT Secret

**Sprint 18 item 724** - Configure secure JWT/session secret.

The application uses signed JWT access and refresh tokens rather than a server-side HTTP session.
Both token types are signed with `JWT_SECRET`, so this value is the production session-signing
secret required by KB rule **SEC-004**.

## Production contract

- `JWT_SECRET` is mandatory in `application-prod.yml` and `docker-compose.prod.yml`.
- Production startup fails when it is missing, unresolved, shorter than 32 characters, or a known
  placeholder.
- The JWT signing secret must be cryptographically random and must not equal `DB_PASSWORD`.
- It is injected at container runtime from the deployment environment or secret manager. It is not
  accepted as a Docker build argument and must never appear in source, images, CI logs, screenshots,
  tickets, frontend `VITE_*` variables, or database backups.
- Access-token and refresh-token lifetimes remain independently configurable; changing durations
  does not reduce the need for a strong signing secret.

Generate at least 32 random bytes; the repository helper defaults to 48 random bytes:

```powershell
.\scripts\New-ProductionJwtSecret.ps1
```

The command prints sensitive output. Prefer directing it to an access-controlled path outside the
repository and importing that value into the secret manager:

```powershell
.\scripts\New-ProductionJwtSecret.ps1 -OutputPath C:\secure\bwc-jwt.env
```

Delete the temporary file securely after import. Do not copy its value into
`backend/.env.production.example`; that file intentionally keeps `JWT_SECRET=` blank.

## Rotation

Rotate on suspected disclosure, staff/access changes, or the organization schedule. Generate and
store the replacement first, update the deployment secret, and recreate the backend container.
Rotation immediately invalidates every access and refresh token signed with the previous value, so
users must sign in again. Record the rotation time, operator, environment, and reason without
recording either secret value. There is no dual-key grace period in this MVP.

If disclosure is suspected, treat the old value as compromised even after it is removed from Git or
logs. Rotate immediately, review authentication/audit activity, and preserve only value-free
incident evidence.

Automated evidence: `SecretPresenceValidatorTests` and
`ProductionJwtSecretConfigurationDocumentationTests`.
