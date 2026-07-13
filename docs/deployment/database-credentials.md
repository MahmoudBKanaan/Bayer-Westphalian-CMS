# Production Database Credentials

**Sprint 18 item 725** - Configure database credentials through environment variables.

Spring and PostgreSQL receive database connection settings only at container startup. Production
configuration has no datasource credential fallback.

## Required variables

| Variable | Purpose | Rule |
| --- | --- | --- |
| `DB_URL` | Spring JDBC endpoint | Required PostgreSQL JDBC URL with host and database name |
| `DB_USERNAME` | Application database role | Required; use a dedicated least-privilege role |
| `DB_PASSWORD` | PostgreSQL/application credential | Required secret; generated randomly and stored outside Git |

Keep credentials out of `DB_URL`. Use
`jdbc:postgresql://postgres:5432/bwc_campaign`, not a URL containing `user:password@host` or
`?password=...`. Startup validation rejects embedded credentials so errors and diagnostics can
refer to the URL without exposing a password.

In `docker-compose.prod.yml`, PostgreSQL bootstrap and the backend use the same `DB_USERNAME` and
`DB_PASSWORD` inputs. The backend receives all three values explicitly. PostgreSQL remains on the
internal database network and has no published host port.

## Provisioning

Create the dedicated database role and password in the deployment secret manager. The helper below
generates 32 cryptographically random bytes and does not run automatically:

```powershell
.\scripts\New-ProductionDatabasePassword.ps1
```

Prefer an access-controlled output path outside the repository, import it into the secret manager,
then securely remove the temporary file:

```powershell
.\scripts\New-ProductionDatabasePassword.ps1 -OutputPath C:\secure\bwc-db.env
```

Do not reuse `JWT_SECRET`, an administrator/superuser password, or credentials from development,
test, or another application.

## Rotation

1. Take and verify a current logical backup.
2. Generate a replacement and update the PostgreSQL role password through an authenticated admin
   connection.
3. Update `DB_PASSWORD` in the deployment secret store.
4. Recreate the backend and PostgreSQL services in a controlled window.
5. Verify Flyway, `/healthz`, login, and a read/write application workflow.
6. Revoke the old credential and record value-free audit evidence.

Changing only the application environment before PostgreSQL causes authentication failure. Rotate
the database role and deployment secret as one operation. Never put credential values in commands
that are retained in shell history.

Automated evidence: `EnvironmentVariableValidatorTests`, `SecretPresenceValidatorTests`, and
`ProductionDatabaseCredentialsDocumentationTests`.
