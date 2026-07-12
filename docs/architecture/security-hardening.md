# Security Hardening Documentation

Production accountability and safe API behavior for the Bayer-Westphalian Campaign Management
Platform (Sprint 14 / BWCO-23). This document covers **item 538** secure error responses,
**item 539** production stack-trace hiding, **item 540** production CORS, **item 541** HTTPS
production requirements, **item 542** environment variable validation, **item 543** secret
presence validation, **item 544** login rate limiting / lockout, **item 545** backend security
headers, **item 546** API error logging without leaking secrets, and documentation item **560**.

Acceptance item **566** is covered by these controls: production must not expose secrets, stack
traces, or unsafe configuration details to clients or startup logs.

## Secure Error Responses (Item 538)

API failures return a structured JSON body and never expose stack traces or internal exception
details to clients.

### Response contract

`ErrorResponse` fields:

| Field | Purpose |
| --- | --- |
| `status` | HTTP status code |
| `error` | HTTP reason phrase |
| `code` | Stable machine-readable code (e.g. `VALIDATION_FAILED`, `INTERNAL_ERROR`) |
| `message` | Safe human-readable message |
| `path` | Request path |
| `details` | Optional non-sensitive detail strings |
| `validationErrors` | Field-level validation (rejected values sanitized) |
| `timestamp` | Server time |
| `requestId` | Optional `X-Request-Id` echo |

### Sources of secure errors

| Layer | Component | Behavior |
| --- | --- | --- |
| Controllers / services | `GlobalExceptionHandler` | Maps domain and framework exceptions to `ErrorResponse` |
| Unexpected failures | `Exception` handler | Logs full stack server-side; client gets `INTERNAL_ERROR` / “Unexpected server error” |
| Malformed JSON | `HttpMessageNotReadableException` | `MALFORMED_REQUEST` without parse internals |
| JWT filter | `JwtAuthenticationFilter` + `SecureErrorResponses` | `401` JSON; does **not** echo JWT validation messages |
| Security filter chain | `authenticationEntryPoint` / `accessDeniedHandler` | `401` / `403` JSON via `SecureErrorResponses` |
| Container error pages | `server.error.*` | `include-stacktrace: never`, no exception/message/binding dumps |

### Sensitive validation values

`SecureErrorResponses.sanitizeRejectedValue` redacts rejected values for fields whose names contain
sensitive markers (`password`, `token`, `secret`, `apiKey`, etc.) so temporary passwords and secrets
are never reflected in error JSON.

### Configuration

`application.yml` and `application-prod.yml`:

```yaml
server:
  error:
    include-stacktrace: never
    include-message: never
    include-binding-errors: never
    include-exception: false
```

## Hide Stack Traces in Production (Item 539)

When `SPRING_PROFILES_ACTIVE=prod` (or equivalent), clients must never receive:

- Java stack traces (`trace` / `stackTrace` fields)
- Exception class names (`exception`)
- Raw exception messages from unhandled failures
- Binding/validation dumps on container error pages

### Production configuration (`application-prod.yml`)

```yaml
server:
  error:
    include-stacktrace: never
    include-message: never
    include-binding-errors: never
    include-exception: false
```

These settings apply to Spring Boot’s default `/error` handling. They are also set in the base
`application.yml` so non-prod environments default to the same safe container behavior.

### Production-only bean

`ProductionErrorSafetyConfiguration` (`@Profile("prod")`) replaces `ErrorAttributes` so that even
if `server.error.include-stacktrace` were mis-set, production still:

1. Requests only `STATUS`, `ERROR`, and `TIMESTAMP` includes from Spring.
2. Strips any residual `trace` / `exception` / `message` / binding keys before returning attributes.

Server logs still record full stack traces via `GlobalExceptionHandler` for operations/debugging;
logging is not a client channel.

### API layer (all profiles, including prod)

`GlobalExceptionHandler` maps unexpected exceptions to:

```json
{
  "status": 500,
  "code": "INTERNAL_ERROR",
  "message": "Unexpected server error"
}
```

No stack frames, exception type names, or root-cause text appear in the JSON body.

### Acceptance (item 554 / Sprint 16 critical **664**)

Primary suite: `ProductionProfileHidesStackTracesTests` (companion:
`ProductionStackTraceHiddenTests`). Frontend catalog:
`frontend/src/features/security/productionProfileHidesStackTraces.ts`.

| Check | Expected |
| --- | --- |
| Prod profile active | `include-stacktrace: never` |
| Unhandled exception via API | `INTERNAL_ERROR` without stack |
| Container `/error` attributes under prod | no `trace` / `exception` |

### Related backlog

| Item | Topic |
| --- | --- |
| **538** | Secure error responses |
| **539** | Hide stack traces in production (this section) |
| **540** | Production CORS configuration (next section) |
| **546** | API error logging without leaking secrets |
| **554** | Acceptance: production error does not expose stack trace |
| **664** | Production profile hides stack traces — `ProductionProfileHidesStackTracesTests` |

## Production CORS Configuration (Item 540)

Browser clients may call the API only from **explicitly allow-listed** frontend origins. Production
must not fall back to local Vite defaults or permit `*`.

### Environment variable

| Variable | Profile | Required | Example |
| --- | --- | --- | --- |
| `CORS_ALLOWED_ORIGINS` | `prod` | **Yes** | `https://campaign.example.com,https://www.example.com` |
| `CORS_ALLOWED_ORIGINS` | `dev` | No | Defaults to `http://localhost:5173,http://127.0.0.1:5173` |

Property binding: `app.cors.allowed-origins` (see `application.yml` / `application-prod.yml`).

### Production rules (enforced in `SecurityConfiguration`)

When the active profile includes `prod`:

1. At least one non-blank origin is required (missing/blank → **startup failure**).
2. Wildcards (`*`, `https://*.example.com`) are rejected.
3. `localhost` / `127.0.0.1` origins are rejected.
4. Origins must be absolute **`https://`** URLs only (item **541** — no plain `http://` frontends).
5. Credentials are allowed only with this explicit list (`allowCredentials=true`).
6. Preflight `maxAge` is 3600 seconds.

Non-production profiles keep convenient local defaults for Vite.

### Configuration snippets

`application-prod.yml`:

```yaml
app:
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS}
```

`SecurityConfiguration` registers CORS for `/api/**` and `/**` with methods
`GET, POST, PUT, PATCH, DELETE, OPTIONS` and headers `Authorization`, `Content-Type`,
`X-Request-Id`.

### Acceptance (item 556 / 895)

| Check | Expected |
| --- | --- |
| Prod without `CORS_ALLOWED_ORIGINS` | Application fails to start / bean creation error |
| Prod with `*` | Startup fails |
| Prod with explicit HTTPS origins | CORS allow-list matches env |
| Dev without env | Localhost Vite origins |

## HTTPS Production Requirement (Item 541)

Production traffic must use HTTPS at the edge. The Spring Boot app typically runs plain HTTP
behind a TLS-terminating reverse proxy (Caddy or nginx). The backend still **requires** that
client-facing API calls are marked secure.

### Properties (`app.security.https`)

| Property | Prod default | Env override | Meaning |
| --- | --- | --- | --- |
| `required` | `true` | `HTTPS_REQUIRED` | Enforce HTTPS / forwarded HTTPS |
| `hsts-enabled` | `true` | `HTTPS_HSTS_ENABLED` | Send `Strict-Transport-Security` |
| `hsts-max-age-seconds` | `31536000` | `HTTPS_HSTS_MAX_AGE_SECONDS` | HSTS max-age (1 year) |

Non-prod defaults set `required: false` so local HTTP development keeps working.

### Enforcement (`HttpsEnforcementFilter`)

When profile includes **`prod`** and `app.security.https.required=true`:

| Request | Result |
| --- | --- |
| `request.isSecure() == true` | Allowed; HSTS header added when enabled |
| `X-Forwarded-Proto: https` (first hop) | Allowed (reverse proxy termination) |
| Plain HTTP API call without forwarded proto | **403** `HTTPS_REQUIRED` secure JSON error |
| `/api/health/**`, `/actuator/health/**` | Always allowed over HTTP (internal probes) |

Also relies on:

```yaml
server:
  forward-headers-strategy: framework
```

so Spring can honor proxy headers when the platform is configured to trust them.

### Reverse proxy

- Terminate TLS on Caddy/nginx (public port 443).
- Proxy to `backend:8080` with `X-Forwarded-Proto: https` (and host headers).
- Frontend and `CORS_ALLOWED_ORIGINS` must use `https://` origins.

### Acceptance (item 896)

| Check | Expected |
| --- | --- |
| Prod HTTP API without proxy headers | 403 `HTTPS_REQUIRED` |
| Prod with `X-Forwarded-Proto: https` | Request proceeds |
| Prod health endpoint over HTTP | 200 path allowed |
| Prod CORS `http://` origin | Startup validation fails |

## Environment Variable Validation (Item 542)

Production deployments must fail fast when required environment variables are missing, blank,
unresolved, or obviously invalid. Validation does **not** log secret values.

### Components

| Component | Role |
| --- | --- |
| `EnvironmentVariableValidator` | Pure rules for required keys and formats |
| `ProductionEnvironmentPostProcessor` | Runs at environment preparation (`META-INF/spring/...EnvironmentPostProcessor`) |
| `ProductionEnvironmentValidationConfiguration` | `@Profile("prod")` `ApplicationRunner` re-check + success log |

### Required production variables

| Env name | Spring property | Rules |
| --- | --- | --- |
| `DB_URL` | `spring.datasource.url` | Required; must start with `jdbc:` |
| `DB_USERNAME` | `spring.datasource.username` | Required; non-blank |
| `DB_PASSWORD` | `spring.datasource.password` | Required; non-blank |
| `JWT_SECRET` | `app.security.jwt.secret` | Required; ≥16 chars; not a known dev placeholder |
| `CORS_ALLOWED_ORIGINS` | `app.cors.allowed-origins` | Required; no `*`; at least one `https://` origin |

When present, login lockout integers (`LOGIN_RATE_LIMIT_*`) must be positive.

### Failure behavior

```text
IllegalStateException: Production environment variable validation failed: ...
```

Messages name the variable (e.g. `JWT_SECRET`) but never print the secret contents.

### Related

| Item | Topic |
| --- | --- |
| **542** | Environment variable validation (previous section) |
| **543** | Secret presence validation on startup (next section) |
| **555** | Missing secret fails startup (acceptance) |
| **665** | Missing secrets are detected — `MissingSecretsAreDetectedTests` |

## Secret Presence Validation on Startup (Item 543)

Production must not start without real secrets for JWT signing and the database password. Optional
provider secrets are required only when real sending is enabled.

### Component

`SecretPresenceValidator` — pure rules; invoked by:

1. `ProductionEnvironmentPostProcessor` (environment preparation, with item 542)
2. `@Profile("prod")` `ApplicationRunner` `productionSecretPresenceValidationRunner`

### Required secrets

| Secret | Min length | Rules |
| --- | --- | --- |
| `JWT_SECRET` / `app.security.jwt.secret` | **32** | Present; not blank; not a known placeholder (`dev-only-change-me`, `changeme`, …) |
| `DB_PASSWORD` / `spring.datasource.password` | **8** | Present; not blank; not a known placeholder |

### Conditional provider secrets

When `PROVIDER_REAL_SENDING_ENABLED=true` (or `app.providers.real-sending-enabled=true`):

| Condition | Required secret |
| --- | --- |
| `EMAIL_PROVIDER_MODE=smtp` | `SMTP_PASSWORD` / `app.providers.email.smtp-password` |
| `SMS_PROVIDER_MODE=provider` | `SMS_API_KEY` / `app.providers.sms.api-key` (min 8) |

### Failure behavior

```text
IllegalStateException: Production secret presence validation failed: ...
```

Messages identify the secret **name** only (e.g. `JWT_SECRET is required`) and never print values.

### Difference from item 542

| Concern | 542 | 543 |
| --- | --- | --- |
| General env vars (DB_URL, CORS, …) | Yes | No |
| JWT min length | 16 | **32** |
| DB password min length | non-blank | **8** |
| Provider secrets when real sending | No | Yes |

### Acceptance (item 555 / Sprint 16 critical **665**)

Primary suite: `MissingSecretsAreDetectedTests` (companions: `SecretPresenceValidatorTests`,
`ProductionEnvironmentPostProcessorTests`). Frontend catalog:
`frontend/src/features/security/missingSecretsAreDetected.ts`.

Ops guide for secret storage, CI rules, and rotation (Sprint 17 item **689**):
[secrets.md](../deployment/secrets.md).

| Check | Expected |
| --- | --- |
| Prod without `JWT_SECRET` | Startup fails |
| Prod with `JWT_SECRET=dev-only-change-me` | Startup fails |
| Prod with short JWT secret (&lt;32) | Startup fails |
| Prod with strong secrets | Startup succeeds |
| Error messages | Name secret keys only; never print secret values |

## Login Rate Limiting / Lockout Strategy (Item 544)

Failed password logins are rate-limited per account (and optional client IP) to slow credential
stuffing and brute-force attacks.

### Configuration (`app.security.login-rate-limit`)

| Property | Env | Default | Meaning |
| --- | --- | --- | --- |
| `max-failures` | `LOGIN_RATE_LIMIT_MAX_FAILURES` | `5` | Failures before lockout |
| `failure-window-minutes` | `LOGIN_RATE_LIMIT_FAILURE_WINDOW_MINUTES` | `15` | Sliding window for counting failures |
| `lockout-minutes` | `LOGIN_RATE_LIMIT_LOCKOUT_MINUTES` | `15` | Lockout duration after threshold |

### Components

| Component | Role |
| --- | --- |
| `LoginAttemptTracker` | In-memory counters; lockout after N failures in window |
| `LoginLockoutException` | HTTP **429**, code `LOGIN_RATE_LIMITED`, optional `Retry-After` seconds |
| `AuthService.validateCredentials` | `ensureAllowed` before lookup; `recordFailure` / `recordSuccess` |
| `AuthController` | Passes client IP (`X-Forwarded-For` first hop or remote addr) |
| `GlobalExceptionHandler` | Adds `Retry-After` header for lockouts |

### Behavior

1. On each login, check lockout for `email` (+ optional IP).
2. Invalid credentials → increment failure count (generic `401 Invalid email or password`).
3. At `max-failures` within the window → lock for `lockout-minutes`.
4. Further attempts during lockout → **429** `LOGIN_RATE_LIMITED` with the same safe message.
5. Successful login → clear counters for that principal.
6. After the failure window and lockout expire → counters reset.

### Notes

- Store is **in-memory** (per JVM instance). Multi-node deployments need a shared store in a later
  hardening iteration if sticky sessions are not used.
- Lockout message does not confirm whether the email exists beyond the rate-limit signal itself.

## Backend Security Headers (Item 545)

The API is a **stateless JSON** service. Headers focus on reducing clickjacking, MIME sniffing, and
accidental caching of authenticated responses. They are applied on every response.

### Headers applied

| Header | Value | Purpose |
| --- | --- | --- |
| `X-Content-Type-Options` | `nosniff` | Disable MIME sniffing |
| `X-Frame-Options` | `DENY` | Block framing / clickjacking |
| `Referrer-Policy` | `no-referrer` | Avoid leaking URLs in Referer |
| `Permissions-Policy` | `camera=(), microphone=(), …` | Disable unused browser features |
| `Content-Security-Policy` | `default-src 'none'; frame-ancestors 'none'; …` | Restrictive API CSP |
| `X-Permitted-Cross-Domain-Policies` | `none` | Block Flash/PDF cross-domain policy |
| `Cache-Control` | `no-store, no-cache, must-revalidate, max-age=0` | Avoid caching API payloads |
| `Pragma` | `no-cache` | HTTP/1.0 cache compatibility |

### Implementation

| Component | Role |
| --- | --- |
| `SecurityConfiguration.configureSecurityHeaders` | Spring Security `headers()` writers |
| `ApiSecurityHeadersFilter` | Ensures headers on all responses (including early filter paths) |
| `HttpsEnforcementFilter` (item 541) | Production **HSTS** only on secure / forwarded-HTTPS responses |

HSTS is **not** set by the generic headers filter so plain-HTTP internal health checks are not
marked with `Strict-Transport-Security`.

### Not applied (intentionally)

| Header / feature | Reason |
| --- | --- |
| Browser XSS auditor header | Deprecated; not useful for JSON APIs |
| `Cross-Origin-Resource-Policy: same-origin` | Would break SPA calling the API from another origin via CORS |
| Global HSTS on all responses | Handled only under production HTTPS enforcement |

## API Error Logging Without Leaking Secrets (Item 546)

Server logs must support operations and debugging without writing passwords, bearer tokens, API
keys, or other secrets.

### Component

`SafeApiErrorLogger` — used by `GlobalExceptionHandler` for all mapped API errors.

### What is logged

| Event | Level | Content |
| --- | --- | --- |
| Application errors (4xx security) | WARN | code, status, method, path, requestId, **sanitized** message |
| Application errors (other 4xx) | INFO | same structured fields |
| Application errors (5xx) | ERROR | same structured fields |
| Validation failures | INFO | method, path, requestId, **field error count only** (no rejected values) |
| Access denied | WARN | method, path, requestId |
| Unexpected exceptions | ERROR | exception **type**, context, sanitized message, stack trace |

### What is never logged

- Raw `Authorization` / `Cookie` / API-key header values
- Request bodies (including login password JSON)
- Unredacted bearer tokens or JWT-shaped strings in free text
- JSON/form fields named `password`, `secret`, `token`, `accessToken`, `refreshToken`, `apiKey`, etc.

### Sanitization

`SafeApiErrorLogger.sanitizeForLog(String)` replaces sensitive patterns with `[REDACTED]` before
messages are written. Client-facing `ErrorResponse` bodies remain separately controlled by items
538–539 (no stack traces to clients).

### Example log line (safe)

```text
API security error code=UNAUTHORIZED status=401 method=POST path=/api/auth/login requestId=req-1 message=Invalid email or password
```

## Related Documentation

- [`authentication-design.md`](authentication-design.md)
- [`role-based-access.md`](role-based-access.md)
- [`../deployment` guides when present](../deployment/)
