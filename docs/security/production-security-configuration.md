# Production Security Configuration

Operational and configuration notes for running the Bayer-Westphalian Campaign Management Platform
under the **`prod`** Spring profile. This document consolidates production security settings,
required environment variables, transport controls (CORS/HTTPS), error safety, headers, logging,
and accountability expectations.

It complements (does not replace):

| Document | Focus |
| --- | --- |
| [Security Hardening](../architecture/security-hardening.md) | Design detail for items **538–546**, **560** |
| [Production Security Checklist](../deployment/production-security-checklist.md) | Pre-release checklist (item **563**) |
| [Production HTTPS](../deployment/https.md) | TLS edge, certificates (item **722**) |
| [Production CORS](../deployment/production-cors.md) | Origin allow-list ops (item **723**) |
| [Secrets](../deployment/secrets.md) | Secret storage and rotation (item **689**) |
| [Environment Variables](../deployment/environment-variables.md) | Templates and catalog (item **688**) |

**Acceptance theme (item 566):** sensitive actions remain auditable, and production must not expose
secrets, stack traces, or unsafe configuration to clients or startup logs.

---

## 1. Scope and non-goals

**In scope**

- Backend production profile and security-related configuration.
- Browser CORS allow-list and HTTPS/HSTS enforcement.
- Secure error responses, security headers, and safe API logging.
- Login rate limiting, JWT secret strength, and RBAC enforcement expectations.
- Pointers to audit, backup, and smoke-test evidence.

**Out of scope**

- Full reverse-proxy certificate issuance procedures (see [https.md](../deployment/https.md)).
- Day-to-day business RBAC matrices (see [role-based-access.md](../architecture/role-based-access.md)).
- AI decision-support policy (AI never bypasses compliance; see
  [ai-limitations-and-human-approval.md](../modules/ai-limitations-and-human-approval.md)).

---

## 2. Required production profile

| Setting | Production expectation |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | Must include `prod` |
| Config files | `application.yml` + `application-prod.yml` |
| Demo data | Dev-only repeatable Flyway seeds must not be treated as production data |
| Local host access | Full public production stack uses HTTPS + restricted CORS; local backend overlays are for operator testing only |

Do not enable demo-only shortcuts for consent, opt-out, eligibility, campaign approval, audit, or
customer communication history in production.

Primary security entry: `com.bayerwestphalian.campaign.auth.SecurityConfiguration`.

---

## 3. Required environment variables and secrets

Production startup **fails fast** when required values are missing, blank, or unsafe.
Validators name **keys only** and never print secret values.

### 3.1 Always required

| Variable | Rules |
| --- | --- |
| `DB_URL` | JDBC URL; must start with `jdbc:` |
| `DB_USERNAME` | Non-blank |
| `DB_PASSWORD` | Non-blank; secret presence min length **8**; not a known placeholder |
| `JWT_SECRET` | Min length **32** for secret presence; not a known placeholder (`changeme`, `dev-only-change-me`, …) |
| `CORS_ALLOWED_ORIGINS` | Explicit comma-separated **`https://`** frontend origins only |

### 3.2 Conditional provider secrets

When `PROVIDER_REAL_SENDING_ENABLED=true`:

| Condition | Required secret |
| --- | --- |
| `EMAIL_PROVIDER_MODE=smtp` | `SMTP_PASSWORD` |
| `SMS_PROVIDER_MODE=provider` | `SMS_API_KEY` (min length **8**) |

### 3.3 Validation components

| Component | Role |
| --- | --- |
| `EnvironmentVariableValidator` | Required keys and format rules (item **542**) |
| `SecretPresenceValidator` | Strength / presence of secrets (item **543**) |
| `ProductionEnvironmentPostProcessor` | Early fail at environment preparation |
| `@Profile("prod")` ApplicationRunners | Re-check at startup + success log |

Automated evidence: `MissingSecretsAreDetectedTests`, `SecretPresenceValidatorTests`,
`EnvironmentVariableValidatorTests`, `ProductionEnvironmentPostProcessorTests`.

Ops detail: [secrets.md](../deployment/secrets.md),
[environment-variables.md](../deployment/environment-variables.md),
[jwt-secret.md](../deployment/jwt-secret.md).

---

## 4. Secure errors and hidden stack traces

Production clients must never receive stack traces, exception class names, or internal binding dumps.

| Control | Setting / behavior |
| --- | --- |
| Container errors | `server.error.include-stacktrace: never`, `include-message: never`, `include-binding-errors: never`, `include-exception: false` |
| API mapping | `GlobalExceptionHandler` → structured `ErrorResponse` |
| Unexpected failures | Client: `INTERNAL_ERROR` / “Unexpected server error”; full stack **server-side only** |
| Prod hardening bean | `ProductionErrorSafetyConfiguration` strips residual `trace` / `exception` attributes |
| JWT / security filter errors | `SecureErrorResponses` JSON `401` / `403` without raw JWT messages |

Example controlled client error (missing resource after authentication):

```json
{
  "status": 404,
  "error": "Not Found",
  "code": "RESOURCE_NOT_FOUND",
  "message": "Customer was not found: 00000000-0000-0000-0000-000000000099",
  "path": "/api/customers/00000000-0000-0000-0000-000000000099",
  "details": [],
  "validationErrors": [],
  "timestamp": "2026-07-16T14:12:17.698357232Z",
  "requestId": "2371e6c4-fb31-40e4-b6d8-71d97d132942"
}
```

Automated evidence: `ProductionProfileHidesStackTracesTests` (item **664** / **554**).

---

## 5. Authentication, lockout, and authorization

| Topic | Production configuration |
| --- | --- |
| Password storage | BCrypt via `PasswordHashingService` |
| Tokens | JWT access + refresh; signing secret from `JWT_SECRET` |
| Login lockout | `LOGIN_RATE_LIMIT_MAX_FAILURES` (default 5), `LOGIN_RATE_LIMIT_FAILURE_WINDOW_MINUTES` (15), `LOGIN_RATE_LIMIT_LOCKOUT_MINUTES` (15) |
| Lockout response | HTTP **429**, code `LOGIN_RATE_LIMITED`, optional `Retry-After` |
| Identity | Employee accounts only; Admin-managed users (no public signup) |
| Authorization | Spring Security + method security (`@PreAuthorize` / `@authz.*`); frontend route protection is UX only |

Components: `JwtService`, `JwtAuthenticationFilter`, `LoginAttemptTracker`, `AuthService`,
`AuthorizationExpressions`.

Note: lockout counters are **in-memory per JVM**; multi-node production may need a shared store in a
later hardening iteration.

---

## 6. CORS/HTTPS configuration notes

This section is the consolidated production **CORS and HTTPS** configuration note (items **540**,
**541**, **722**, **723**). Prefer dedicated ops guides for certificate paths and curl evidence
samples.

### 6.1 HTTPS (transport)

| Property / env | Prod expectation |
| --- | --- |
| `HTTPS_REQUIRED` / `app.security.https.required` | **`true`** (unless formally documented break-glass) |
| `HTTPS_HSTS_ENABLED` | **`true`** |
| `HTTPS_HSTS_MAX_AGE_SECONDS` | **`31536000`** (1 year) |
| Edge TLS | Nginx (or equivalent) terminates TLS on public **443** |
| Proxy headers | Forward `X-Forwarded-Proto: https` (and host/port) to the backend |
| Backend listen | Typically plain HTTP on the private Docker network (`backend:8080`) |
| Health exceptions | `/api/health/**` and `/actuator/health/**` may remain probeable over HTTP |

**Enforcement:** `HttpsEnforcementFilter` + `ProductionHttpsProperties`.

| Request shape | Result |
| --- | --- |
| Secure request or `X-Forwarded-Proto: https` | Allowed; HSTS added when enabled |
| Plain HTTP API without forwarded HTTPS | **403** `HTTPS_REQUIRED` |
| Health probe paths | Allowed over HTTP for internal probes |

Spring Boot should use `server.forward-headers-strategy: framework` so proxy headers are honored
when the platform trusts them.

Public certificate configuration, HSTS at the edge, and off-host verification:
[Production HTTPS](../deployment/https.md). Script: `scripts/test-production-https.ps1`.

**Local backend-only overlay** (`docker-compose.prod.backend-local.yml` +
`Start-ProductionBackend.ps1`) may relax HTTPS for host-port testing. That is **not** production
HTTPS evidence and must not be confused with the full public stack.

### 6.2 CORS (browser origins)

| Variable | Binding |
| --- | --- |
| `CORS_ALLOWED_ORIGINS` | `app.cors.allowed-origins` |

**Production rules** (enforced by `ProductionCorsOrigins` / `SecurityConfiguration` under `prod`):

1. At least one non-blank origin is required (missing/blank → **startup failure**).
2. Wildcards (`*`, `https://*.example.com`) are **rejected**.
3. `localhost` and `127.0.0.1` origins are **rejected**.
4. Origins must be absolute **`https://`** URLs only (no plain `http://`).
5. Each entry is an **origin only** (scheme + host [+ port]); no path, trailing slash, query, or fragment.
6. Credentials (`allowCredentials=true`) are allowed only with this explicit list.
7. Allowed methods: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`.
8. Allowed request headers: `Authorization`, `Content-Type`, `X-Request-Id` (expose `X-Request-Id`).
9. Preflight `maxAge` is **3600** seconds.
10. Changing the allow-list requires a **backend restart**.

Example:

```dotenv
CORS_ALLOWED_ORIGINS=https://campaign.example.com,https://www.campaign.example.com
```

Do **not** put API paths (e.g. `/api`) in the CORS list; browsers compare page origin, not API URL.

**Browser login implication:** a Vite dev server on `http://localhost:5173` cannot call a
production-profile API that forbids localhost CORS. For local UI against a prod-profile API, use a
same-origin proxy (e.g. `VITE_API_BASE_URL=/api` → proxy to `:8080`) or a deployed HTTPS frontend
origin listed in `CORS_ALLOWED_ORIGINS`. Do not widen production CORS to localhost.

Ops detail and preflight curl samples: [Production CORS](../deployment/production-cors.md).

Automated evidence: `ProductionCorsConfigurationTests`, `EnvironmentVariableValidatorTests`,
`ProductionCorsDeploymentDocumentationTests`, `ProductionHttpsConfigurationDocumentationTests`.

### 6.3 CORS/HTTPS go/no-go checks

Before treating a deployment as production-ready for transport security:

- [ ] `HTTPS_REQUIRED=true` and plain HTTP API without proxy headers returns `HTTPS_REQUIRED`.
- [ ] Public HTTPS serves application traffic with HSTS where expected.
- [ ] HTTP → HTTPS redirect works at the edge for the public host.
- [ ] `CORS_ALLOWED_ORIGINS` lists only approved `https://` origins.
- [ ] Unapproved origin preflight does **not** receive `Access-Control-Allow-Origin` for that origin.
- [ ] Startup fails for `*`, localhost, or `http://` CORS entries under `prod`.

---

## 7. Security headers

API responses include (via `SecurityConfiguration` and `ApiSecurityHeadersFilter`):

| Header | Typical value / purpose |
| --- | --- |
| `X-Content-Type-Options` | `nosniff` |
| `X-Frame-Options` | `DENY` |
| `Referrer-Policy` | `no-referrer` |
| `Permissions-Policy` | Restrict unused browser features |
| `Content-Security-Policy` | Restrictive API CSP (`default-src 'none'`, …) |
| `X-Permitted-Cross-Domain-Policies` | `none` |
| `Cache-Control` | `no-store, no-cache, must-revalidate, max-age=0` |
| `Pragma` | `no-cache` |

**HSTS** is applied by `HttpsEnforcementFilter` only on secure / forwarded-HTTPS responses so
internal HTTP health probes are not marked with `Strict-Transport-Security`.

---

## 8. Safe API error logging

`SafeApiErrorLogger` (used by `GlobalExceptionHandler`) structures logs without leaking secrets.

| Must log | Must not log |
| --- | --- |
| Method, path, status, error code, requestId | Raw `Authorization` / `Cookie` values |
| Sanitized safe messages | Request bodies (including login passwords) |
| Validation **field error counts** | Unredacted tokens / JWT-shaped strings |
| Exception **type** + stack for unexpected 5xx (server only) | Raw secret field values |

Client 4xx controlled failures (e.g. `RESOURCE_NOT_FOUND`) are often logged at **INFO**, not ERROR.
Capture HTTP response bodies with `curl -i` for evidence when log level is quiet.

Production log ops: [production-logging.md](../deployment/production-logging.md).

---

## 9. Audit and accountability

| Expectation | Detail |
| --- | --- |
| Immutable audit rows | Sensitive actions write `AuditLog` entries |
| Read access | `ADMIN`, `COMPLIANCE_OFFICER`, `SYSTEM_AUDITOR` only |
| Export | Restricted; records `EXPORT_REPORT` (or equivalent) audit entry |
| UI | Read-only audit screens; no business-role mutation of audit history |

Module guide: [audit-logging.md](../modules/audit-logging.md).
Role guide: [system-auditor-guide.md](../user-guides/system-auditor-guide.md).

---

## 10. Key implementation map

| Concern | Primary types / files |
| --- | --- |
| Security filter chain & CORS | `auth/SecurityConfiguration.java` |
| JWT filter | `auth/JwtAuthenticationFilter.java` |
| HTTPS enforcement | `auth/HttpsEnforcementFilter.java`, `ProductionHttpsProperties` |
| API headers | `auth/ApiSecurityHeadersFilter.java` |
| Login lockout | `auth/LoginAttemptTracker.java` |
| Errors | `common/api/GlobalExceptionHandler.java`, `SecureErrorResponses` |
| Safe logs | `common/api/SafeApiErrorLogger.java` |
| Prod error attributes | `common/api/ProductionErrorSafetyConfiguration.java` |
| Env / secrets | `EnvironmentVariableValidator`, `SecretPresenceValidator`, `ProductionEnvironmentPostProcessor` |
| Prod YAML | `backend/src/main/resources/application-prod.yml` |
| Compose model | `docker-compose.prod.yml`, `docker/nginx/` |

---

## 11. Verification and evidence

Use the [Production Security Checklist](../deployment/production-security-checklist.md) and
[Production Smoke Test Checklist](../deployment/production-smoke-test-checklist.md).

Minimum configuration evidence themes:

1. Missing / weak secrets fail startup without printing values.
2. HTTPS enforcement and HSTS behavior.
3. CORS rejection of wildcard, localhost, and plain HTTP origins.
4. Client errors without stack traces.
5. Login lockout + `Retry-After`.
6. Security headers present on API responses.
7. Unauthorized audit access denied; authorized auditor access works.
8. Backup/restore process documented and practiced on non-production data.

Release gate context: [production-release-gate.md](../deployment/production-release-gate.md)
(item **770**). Incident handling: [incident-response-notes.md](../deployment/incident-response-notes.md).

---

## 12. Related documentation

- [Security Hardening](../architecture/security-hardening.md)
- [Authentication Design](../architecture/authentication-design.md)
- [Role-Based Access](../architecture/role-based-access.md)
- [Production Security Checklist](../deployment/production-security-checklist.md)
- [Production HTTPS](../deployment/https.md)
- [Production CORS](../deployment/production-cors.md)
- [Production Logging](../deployment/production-logging.md)
- [JWT Secret](../deployment/jwt-secret.md)
- [Secrets](../deployment/secrets.md)
- [Environment Variables](../deployment/environment-variables.md)
- [Reverse Proxy](../deployment/reverse-proxy.md)
- [Production Compose](../deployment/production-compose.md)
- [Audit Logging](../modules/audit-logging.md)
