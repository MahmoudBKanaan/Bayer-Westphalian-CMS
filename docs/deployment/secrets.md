# Secrets Documentation

**Sprint 17 item 689** — Add secrets documentation.

The Bayer-Westphalian Campaign Management Platform treats credentials and signing keys as **secrets**.
This document is the ops-facing guide for what must stay secret, where secrets live, how production
validates them, and what CI must never do. It complements the [environment variable
template](environment-variables.md) (item **688**), which only ships **placeholders**.

## Principles

1. **Never commit real secret values** to Git (`.env`, PEM files, keystores, cloud keys).
2. **Never bake secrets into Docker images** or GitHub Actions workflow YAML.
3. **Supply secrets at runtime** from the deployment environment or a secret manager.
4. **Fail closed in production** when secrets are missing, weak, or known placeholders.
5. **Error messages name keys only** — never log or return secret *values*.

## What is a secret in this system?

| Secret env name | Purpose | Production rules (summary) |
| --- | --- | --- |
| `JWT_SECRET` | Signs/verifies JWT access tokens | Required; **≥ 32** characters; not a known placeholder |
| `DB_PASSWORD` | PostgreSQL password for the app | Required; **≥ 8** characters; not a known placeholder |
| `POSTGRES_PASSWORD` | Compose/host Postgres bootstrap password | Treat as secret; align with DB credentials in prod |
| `SMTP_PASSWORD` | SMTP auth when real email is enabled | Required when real sending + SMTP mode |
| `SMS_API_KEY` | SMS provider key when real SMS is enabled | Required when real sending + provider mode (min 8) |

Non-secret but sensitive configuration (still not public):

| Name | Notes |
| --- | --- |
| `DB_URL` / `DB_USERNAME` | Required in prod; not “signing secrets” but not for public repos |
| `CORS_ALLOWED_ORIGINS` | Required in prod; must be explicit `https://` origins |

Full non-secret catalog: [environment-variables.md](environment-variables.md).

Runtime validation details: [security-hardening.md](../architecture/security-hardening.md) (items
**542–543**), critical test item **665**.

## Where secrets live

| Location | Allowed content |
| --- | --- |
| Secret manager / host env / orchestrator secrets | **Real production values** |
| Local `backend/.env`, `frontend/.env` (gitignored) | Local/dev only; never prod production keys in shared machines without policy |
| `.env.example`, `backend/.env.example`, `frontend/.env.example` | **Placeholders only** |
| GitHub Actions workflow YAML | **No secret values**; use GitHub Secrets / OIDC when deploy needs them (later items) |
| Docker image layers | **No secrets**; pass via `docker run -e` / Compose `environment` / orchestrator |
| Database backups | DB data only; **re-apply** secrets from the secret store after restore ([backup-and-restore.md](backup-and-restore.md)) |

`.gitignore` excludes `.env`, `.env.*` (with `!.env.example` exceptions), `*.pem`, `*.key`,
`secrets/`.

## Local development

1. Copy templates (item **688**):

   ```powershell
   Copy-Item backend\.env.example backend\.env
   Copy-Item frontend\.env.example frontend\.env
   ```

2. Use **local placeholders** from the templates (e.g. Compose default `bwc_app` password).
3. Do **not** put production `JWT_SECRET` or production DB passwords on developer laptops unless
   your org policy requires a separate break-glass process.
4. Prefer `EMAIL_PROVIDER_MODE=mock` / `SMS_PROVIDER_MODE=mock` and
   `PROVIDER_REAL_SENDING_ENABLED=false` so provider secrets are unused.

## Production deployment checklist (secrets)

Before enabling the `prod` profile against real customer data:

1. Generate a strong `JWT_SECRET` (cryptographically random, ≥ 32 characters; prefer longer).
2. Set a strong unique `DB_PASSWORD` (and matching DB user credentials).
3. Inject secrets via the platform secret store or host environment — **not** files in the Git tree.
4. Confirm `SPRING_PROFILES_ACTIVE=prod`.
5. Confirm startup **fails** if you deliberately omit `JWT_SECRET` or `DB_PASSWORD` (item **665**).
6. Confirm logs/error responses never echo secret values.
7. If enabling real email/SMS (`PROVIDER_REAL_SENDING_ENABLED=true`), set `SMTP_PASSWORD` and/or
   `SMS_API_KEY` as required by mode.
8. Rotate secrets on staff change, suspected leak, or scheduled policy; invalidate sessions after
   JWT secret rotation.

Also complete [production-security-checklist.md](production-security-checklist.md).

## Application enforcement

| Component | Role |
| --- | --- |
| `EnvironmentVariableValidator` | Prod required env shape (item **542**) |
| `SecretPresenceValidator` | Prod secret presence/strength (item **543**) |
| `ProductionEnvironmentPostProcessor` | Fail fast during environment preparation |
| `productionSecretPresenceValidationRunner` | `@Profile("prod")` re-check at startup |

### Expected failure shape

```text
IllegalStateException: Production secret presence validation failed: ...
```

Messages may include names such as `JWT_SECRET` or `DB_PASSWORD` but **must not** include the
configured values.

### Automated tests (do not print secrets)

| Suite | Role |
| --- | --- |
| `MissingSecretsAreDetectedTests` | Critical item **665** |
| `SecretPresenceValidatorTests` | Unit rules for secret strength |
| `ProductionEnvironmentPostProcessorTests` | Startup post-processor behavior |
| Frontend catalog `missingSecretsAreDetected.ts` | Companion catalog |

## CI / CD rules

| Rule | Rationale |
| --- | --- |
| No `JWT_SECRET:` / `DB_PASSWORD:` **values** in `.github/workflows/*` | Prevents leak in logs and fork PRs |
| Docker build jobs must not `ARG`/`ENV` production secrets | Layers are inspectable |
| CI may use throwaway test secrets only inside ephemeral runners if a future job needs them | Prefer Testcontainers and non-prod profiles |
| Permissions stay least-privilege (`contents: read` for current CI) | Reduces blast radius |

See [ci-cd.md](ci-cd.md) security notes (items **677+**).

## Rotation and incident response (minimum)

1. **Suspect leak** (commit, log, screenshot, shared chat): rotate `JWT_SECRET` and DB password;
   revoke sessions; review audit logs.
2. **Accidental commit**: remove from history if required by policy, rotate immediately, treat the
   value as compromised even if “deleted” later.
3. **Provider key leak**: rotate at the provider console; disable real sending until keys are
   replaced.

## Automated documentation evidence

| Item | Backend test | Frontend catalog |
| --- | --- | --- |
| **689** | `SecretsDocumentationTests` | `secretsDocumentation.ts` |

## Related backlog and docs

| Item / doc | Topic |
| --- | --- |
| **688** | [Environment variable template](environment-variables.md) |
| **689** | This secrets documentation |
| **690** | [Production config validation step in CI](ci-cd.md#production-config-validation-step-item-690) |
| **691** | [Release artifact generation](ci-cd.md#release-artifact-generation-item-691) |
| **692** | [CI badge on README](ci-cd.md#ci-badge-on-readme-item-692) |
| **693** | [Verify pipeline fails when tests fail](ci-cd.md#verify-pipeline-fails-when-tests-fail-item-693) |
| **694** | [Verify pipeline passes on clean main branch](ci-cd.md#verify-pipeline-passes-on-clean-main-branch-item-694) |
| **695** | [Branch protection recommendation](branch-protection.md) |
| **696** | [Release tagging process](release-tagging.md) |
| **697** | [Deployment workflow placeholder](ci-cd.md#deployment-workflow-placeholder-item-697) |
| **542–543** / **665** | Runtime validation and critical tests |
| [security-hardening.md](../architecture/security-hardening.md) | Technical validation detail |
| [production-security-checklist.md](production-security-checklist.md) | Pre-release checklist |
| [backup-and-restore.md](backup-and-restore.md) | Secrets not in DB dumps |
