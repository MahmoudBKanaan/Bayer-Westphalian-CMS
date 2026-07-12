# Environment Variable Template

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
| `PROVIDER_REAL_SENDING_ENABLED` | Backend | `false` locally |
| `EMAIL_PROVIDER_MODE` | Backend | `mock` locally |
| `SMTP_HOST` / `SMTP_PORT` / `SMTP_USERNAME` / `SMTP_PASSWORD` | Backend | empty/mock until real email configured |
| `SMS_PROVIDER_MODE` / `SMS_API_KEY` | Backend | mock / empty until real SMS configured |
| `FILE_STORAGE_MODE` | Backend | `local` |
| `FILE_STORAGE_LOCAL_PATH` | Backend | `./data/files` |

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

## Related backlog

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
| **542–543** / **665** | Runtime env / secret validation |
