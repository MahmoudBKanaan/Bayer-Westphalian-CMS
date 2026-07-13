# Environment Variable Template

**Sprint 17 item 709** - Environment variable documentation.

**Sprint 17 item 688** — Add environment variable template.

The Bayer-Westphalian Campaign Management Platform is configured primarily through **environment
variables** (KB DevOps / deployment preparation). This document is the human-readable catalog for
the checked-in templates. **Never commit real production secrets** — see
[secrets.md](secrets.md) (item **689**).

## Template files (checked into Git)

| Path | Scope |
| --- | --- |
| [`.env.example`](../../.env.example) | Full-stack reference template (backend + frontend + Compose) |
| [`backend/.env.example`](../../backend/.env.example) | Spring Boot backend |
| [`frontend/.env.example`](../../frontend/.env.example) | Vite React frontend |

Local setup (from developer guide):

```powershell
Copy-Item backend\.env.example backend\.env
Copy-Item frontend\.env.example frontend\.env
# Optional: use root .env.example as a checklist when configuring deployment hosts
```

Real `.env` files must stay out of version control.

## Automated documentation evidence

| Item | Backend test | Frontend catalog |
| --- | --- | --- |
| **688** | `EnvironmentVariableTemplateDocumentationTests` | `environmentVariableTemplate.ts` |
| **709** | `EnvironmentVariableDocumentationTests` | `ENVIRONMENT_VARIABLE_DOC_REQUIRED_MARKERS` / `environmentVariableDocDefinesRequiredMarkers` |

## Classification

| Class | Meaning | Examples | Handling |
| --- | --- | --- | --- |
| Required production variables | App cannot safely run in `prod` without these values | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS` | Supply through deployment environment or secret manager before startup |
| Secret variables | Values grant access or sign tokens | `DB_PASSWORD`, `JWT_SECRET`, `SMTP_PASSWORD`, `SMS_API_KEY`, `POSTGRES_PASSWORD` | Never commit real values; rotate through the secret manager |
| Operational tuning | Non-secret runtime behavior | `CONTACT_MONTHLY_LIMIT`, `CONTACT_RETRY_LIMIT`, `REMINDER_PROCESSING_CRON` | Change via environment, document reason in release notes |
| Frontend build/runtime | Public client configuration | `VITE_API_BASE_URL`, `VITE_APP_ENV` | Safe to expose, but must point at the intended API/environment |

## Required production variables

Production must provide at least these variables:

| Variable | Required because |
| --- | --- |
| `DB_URL` | Selects the production database endpoint |
| `DB_USERNAME` | Authenticates the application database user |
| `DB_PASSWORD` | Authenticates the application database user; secret |
| `JWT_SECRET` | Signs and verifies access/refresh tokens; secret, minimum strength enforced |
| `CORS_ALLOWED_ORIGINS` | Restricts browser clients to approved HTTPS origins |

Optional provider secrets such as `SMTP_PASSWORD` and `SMS_API_KEY` become required when the
corresponding real provider mode is enabled.

## Secret variables

Secret variables are documented by name only. Store values in the deployment environment or a
secret manager, not in Git, Docker images, CI YAML, screenshots, or exported reports. See
[secrets.md](secrets.md) for rotation ownership and incident handling.

## Variable catalog

### Spring / process

| Variable | Used by | Local default / notes |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | Backend | `dev` (`test` / `prod` for those profiles) |
| `SERVER_PORT` | Backend | `8080` |

### Database

| Variable | Used by | Local default / notes |
| --- | --- | --- |
| `DB_URL` | Backend | `jdbc:postgresql://localhost:5432/bwc_campaign` — **required in prod** |
| `DB_USERNAME` | Backend | `bwc_app` — **required in prod** |
| `DB_PASSWORD` | Backend | local only placeholder — **required in prod** (secret) |
| `POSTGRES_DB` | Docker Compose | `bwc_campaign` |
| `POSTGRES_USER` | Docker Compose | `bwc_app` |
| `POSTGRES_PASSWORD` | Docker Compose | local only placeholder (secret) |
| `POSTGRES_PORT` | Docker Compose | `5432` |

### Security / JWT / CORS / HTTPS

| Variable | Used by | Local default / notes |
| --- | --- | --- |
| `JWT_SECRET` | Backend | local placeholder — **required in prod** (secret; strength rules in SecretPresenceValidator) |
| `JWT_ISSUER` | Backend | app issuer string |
| `JWT_ACCESS_TOKEN_MINUTES` | Backend | `15` |
| `JWT_REFRESH_TOKEN_DAYS` | Backend | `7` |
| `CORS_ALLOWED_ORIGINS` | Backend | localhost Vite origins in dev; **required https:// origins in prod** |
| `HTTPS_REQUIRED` | Backend | `false` in dev; prod profile defaults `true` |
| `HTTPS_HSTS_ENABLED` | Backend | `false` in dev; prod defaults `true` |
| `HTTPS_HSTS_MAX_AGE_SECONDS` | Backend | `31536000` |
| `LOGIN_RATE_LIMIT_MAX_FAILURES` | Backend | `5` |
| `LOGIN_RATE_LIMIT_FAILURE_WINDOW_MINUTES` | Backend | `15` |
| `LOGIN_RATE_LIMIT_LOCKOUT_MINUTES` | Backend | `15` |

### Campaign / providers / storage

| Variable | Used by | Local default / notes |
| --- | --- | --- |
| `CONTACT_MONTHLY_LIMIT` | Backend | `3` |
| `CONTACT_RETRY_LIMIT` | Backend | `3` |
| `UNINTERESTED_EXCLUSION_DAYS` | Backend | `90` |
| `REMINDER_PROCESSING_CRON` | Backend | every 15 minutes |
| `REMINDER_PROCESSING_ZONE` | Backend | `UTC` in production |
| `LOG_LEVEL_SCHEDULER` | Backend | `INFO` |
| `PROVIDER_REAL_SENDING_ENABLED` | Backend | `false` locally |
| `EMAIL_PROVIDER_MODE` | Backend | `mock` locally |
| `SMTP_HOST` / `SMTP_PORT` / `SMTP_USERNAME` / `SMTP_PASSWORD` | Backend | empty/mock until real email configured |
| `SMS_PROVIDER_MODE` / `SMS_API_KEY` | Backend | `mock` in dev/test; `disabled` / empty in production until a real provider is implemented |
| `FILE_STORAGE_MODE` | Backend | `local` in dev; `filesystem` in production |
| `FILE_STORAGE_LOCAL_PATH` | Backend | `./data/files` in dev; `/app/data/consent-evidence` in production |
| `FILE_STORAGE_MAX_BYTES` | Backend | `10485760` (10 MiB production default) |
| `CONSENT_EVIDENCE_VOLUME_NAME` | Docker Compose | `bwc_consent_evidence` |

### Frontend (Vite)

| Variable | Used by | Local default / notes |
| --- | --- | --- |
| `VITE_API_BASE_URL` | Frontend | `http://localhost:8080/api` |
| `VITE_APP_ENV` | Frontend | `dev` |

## Production notes

- Production must supply secrets via the deployment environment or a secret manager — **not** via
  committed files (see [Security Hardening](../architecture/security-hardening.md) items **542–543**
  and the production security checklist).
- Required production keys include at least: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`,
  `CORS_ALLOWED_ORIGINS` (https origins).
- CI must not embed production secret **values** in workflow YAML (item **677+** security notes).

## Validation and startup behavior

Production validation is split across:

- `EnvironmentVariableValidator` for required names such as `DB_URL`, `DB_USERNAME`,
  `DB_PASSWORD`, `JWT_SECRET`, and `CORS_ALLOWED_ORIGINS`.
- `SecretPresenceValidator` for minimum secret strength (`JWT_SECRET` and database password).
- `ProductionEnvironmentPostProcessor` for invoking validation during production startup.
- The CI production configuration step in [ci-cd.md](ci-cd.md), which statically checks templates,
  validators, `application-prod.yml`, secrets docs, and the production checklist without loading
  real secret values.

Missing required production values should fail startup or produce a safe configuration error. The
error must name the missing variable but must not print secret values.

## Change management

When adding, renaming, or removing an environment variable:

1. Update `.env.example`, `backend/.env.example`, or `frontend/.env.example`.
2. Update this guide's variable catalog, classification, and required production variables if
   applicable.
3. Update [secrets.md](secrets.md) if the variable is secret.
4. Update [production-security-checklist.md](production-security-checklist.md) if production
   operators must verify it.
5. Update CI/static tests that lock the expected variable set.

## Rotation notes

- Rotate `JWT_SECRET` with a token/session impact plan because existing tokens may become invalid.
- Rotate `DB_PASSWORD` together with database user credentials and deployment environment updates.
- Rotate `SMTP_PASSWORD` and `SMS_API_KEY` in the provider console and the secret manager.
- After rotation, verify production startup and provider connectivity without logging secret values.

## Troubleshooting

| Symptom | Likely variable area | First check |
| --- | --- | --- |
| Production startup fails fast | Required production variables | Confirm `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS` are present |
| Login tokens fail validation | JWT settings | Check `JWT_SECRET`, `JWT_ISSUER`, access/refresh durations |
| Browser calls blocked | CORS settings | Check `CORS_ALLOWED_ORIGINS` contains the deployed frontend origin |
| Provider sends fail | Email/SMS provider settings | Check provider mode and matching secret/API key |
| Frontend points to wrong API | Vite variables | Check `VITE_API_BASE_URL` for the deployed environment |

## Related backlog

Additional item: **709** — this environment variable documentation. Item **710** expands the
[secrets documentation](secrets.md).
Evidence tests: `EnvironmentVariableDocumentationTests`.

| Item | Topic |
| --- | --- |
| **688** | This environment variable template |
| **689** | [Secrets documentation](secrets.md) (ops process) |
| **690** | Production config validation step in CI — [ci-cd.md](ci-cd.md) |
| **691** | Release artifact generation (JAR + `dist/` upload) — [ci-cd.md](ci-cd.md) |
| **692** | CI badge on README — [ci-cd.md](ci-cd.md) |
| **693** | Verify pipeline fails when tests fail — [ci-cd.md](ci-cd.md) |
| **694** | Verify pipeline passes on clean main branch — [ci-cd.md](ci-cd.md) |
| **695** | Branch protection recommendation — [branch-protection.md](branch-protection.md) |
| **696** | Release tagging process — [release-tagging.md](release-tagging.md) |
| **697** | Deployment workflow placeholder — [ci-cd.md](ci-cd.md#deployment-workflow-placeholder-item-697) |
| **698** | CI runs on pull request — [ci-cd.md](ci-cd.md#ci-runs-on-pull-request-item-698) |
| **699** | CI runs on main branch — [ci-cd.md](ci-cd.md#ci-runs-on-main-branch-item-699) |
| **700** | Backend build passes — [ci-cd.md](ci-cd.md#backend-build-passes-item-700) |
| **701** | Backend tests pass — [ci-cd.md](ci-cd.md#backend-tests-pass-item-701) |
| **542–543** / **665** | Runtime env / secret validation |
