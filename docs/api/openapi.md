# REST API and OpenAPI Guide

**Item 779** documents the internal REST API for the Bayer-Westphalian Campaign Management
Platform. The generated OpenAPI document is the schema-level reference; this guide explains shared
conventions, authorization, endpoint families, and business workflow constraints.

The API is for authorized employee clients. It is not a public signup/customer API.

## Base URLs and generated documentation

| Resource | Local development URL |
| --- | --- |
| API base | `http://localhost:8080/api` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| Actuator health | `http://localhost:8080/actuator/health` |

Springdoc scans `com.bayerwestphalian.campaign`. Metadata is configured in
`backend/src/main/java/com/bayerwestphalian/campaign/common/OpenApiConfiguration.java`; paths and
enablement are configured in `application.yml` and `application-prod.yml`.

Production disables OpenAPI and Swagger by default through `OPENAPI_ENABLED=false` and
`SWAGGER_UI_ENABLED=false`. Do not expose interactive documentation publicly without an approved
access/security decision.

## Exported OpenAPI artifact

**Item 780** exports the running backend contract to
[`docs/api/openapi.json`](openapi.json). Start the intended local backend release, then run from the
repository root:

```powershell
.\scripts\export-openapi.ps1
```

The exporter accepts local endpoints only, validates OpenAPI 3.x metadata and critical API paths,
writes a `.partial` file, and atomically publishes only a valid JSON document. To export a different
local port or destination:

```powershell
.\scripts\export-openapi.ps1 `
  -ApiDocsUrl http://localhost:8081/v3/api-docs `
  -OutputFile docs/api/openapi.json
```

Regenerate after controller/request/response changes and before release documentation review. The
artifact contains schemas and example metadata, not runtime customer records or credentials. Review
the diff for unintended internal details. Production Swagger remains disabled by default; do not
enable it merely to export from an exposed production host.

## Authentication

Most `/api/**` endpoints require a JWT access token:

```http
Authorization: Bearer <access-token>
Content-Type: application/json
Accept: application/json
```

Authentication endpoints:

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/auth/login` | Exchange employee email/password for user and access/refresh tokens |
| `POST` | `/api/auth/refresh` | Exchange a valid refresh token for a refreshed session |
| `POST` | `/api/auth/logout` | Revoke/logout the presented access-token session |
| `GET` | `/api/auth/me` | Return the current authenticated employee |

Do not log, persist in URLs, or place access/refresh tokens in screenshots, tickets, source control,
or API examples. Clients should clear local session state after logout or an unrecoverable auth
failure. Repeated invalid login can produce rate limiting and `Retry-After`.

## Authorization

Spring Security and method authorization are authoritative. Frontend menu visibility is not an API
permission. Typical ownership:

| API area | Primary authorized roles |
| --- | --- |
| Users, roles, system settings | `ADMIN` |
| Customer writes | `ADMIN`, `CUSTOMER_SERVICE_AGENT` (specific consent/compliance operations vary) |
| Product management | `ADMIN`, `PRODUCT_MANAGER` |
| Segment management | `ADMIN`, `CAMPAIGN_MANAGER`; BI/compliance have narrower read/preview access |
| Campaign drafts/lifecycle | `CAMPAIGN_MANAGER` (and explicitly authorized Admin paths) |
| Campaign approval/rejection | `COMPLIANCE_OFFICER` |
| Analytics/reports | Authorized analyst, campaign, marketing, executive, and Admin roles by endpoint |
| Audit reads | `ADMIN`, `COMPLIANCE_OFFICER`, `SYSTEM_AUDITOR` |

Consult [Role-Based Access](../architecture/role-based-access.md) and generated OpenAPI for the
current endpoint contract. Unauthenticated requests receive `401`; authenticated insufficient roles
receive `403`. Clients must not retry with alternate URLs to bypass a denial.

## Common response contracts

Most successful JSON endpoints return `ApiResponse<T>`:

```json
{
  "success": true,
  "message": "Customer loaded",
  "data": {},
  "errors": [],
  "timestamp": "2026-07-13T10:00:00Z"
}
```

Paged searches wrap `PageResponse<T>` inside `data`:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true,
  "empty": true
}
```

Errors use `ErrorResponse`, not the success envelope:

```json
{
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "path": "/api/customers",
  "details": ["email: must be a well-formed email address"],
  "validationErrors": [
    { "field": "email", "message": "must be a well-formed email address" }
  ],
  "timestamp": "2026-07-13T10:00:00Z",
  "requestId": "safe-correlation-id"
}
```

Production errors exclude stack traces, exception classes, SQL details, filesystem paths, secrets,
and request payloads. Preserve `requestId`, UTC time, method/path, and safe error code for support.

Common HTTP statuses:

| Status | Meaning |
| --- | --- |
| `200` | Successful read/update/workflow action |
| `201` | Resource created/import accepted |
| `400` | Validation, malformed input, or business-rule failure |
| `401` | Missing, invalid, expired, or revoked authentication |
| `403` | Authenticated but role/ownership is insufficient |
| `404` | Resource not found or unavailable under the request contract |
| `409` | Conflict such as duplicate/current-state collision where used |
| `429` | Login/request rate limit where configured |
| `500` | Safe unexpected server error; use `requestId` for investigation |

## Data conventions

- Resource IDs are RFC 4122 UUID strings such as `40000000-0000-0000-0000-000000000101`.
- Server-generated IDs must not be invented by clients unless a request schema explicitly requires
  an existing related UUID.
- JSON property names use lower camel case. Enum values use uppercase snake case.
- Instants use ISO-8601 UTC (`2026-07-13T10:00:00Z`); dates use `YYYY-MM-DD`.
- Search pagination is zero-based (`page=0`) with endpoint-specific defaults/limits.
- Send `Content-Type: application/json` for JSON bodies; customer CSV import uses
  `multipart/form-data`; report exports return `text/csv` or `application/pdf`.
- Omitted and explicit `null` can have different update semantics. Follow the generated request
  schema rather than guessing partial-update behavior.

## Endpoint catalog

### Authentication and administration

| Family | Endpoints |
| --- | --- |
| Authentication | `POST /api/auth/login`, `/refresh`, `/logout`; `GET /api/auth/me` |
| Users | `GET/POST /api/users`; `GET/PUT /api/users/{id}`; `PATCH /{id}/disable`, `/{id}/password`; `POST /{id}/roles` |
| Settings | `GET/PUT /api/system-settings` |
| Audit | `GET /api/audit-logs`; `/entity-history`; `/entities/{entityType}/{entityId}` |

User creation, disable, and role assignment are Admin-only and auditable. Passwords/hashes are never
returned. Audit endpoints are read-only for authorized audit roles.

### Customers, beneficiaries, and consent

| Family | Endpoints |
| --- | --- |
| Customers | `GET/POST /api/customers`; `GET/PUT/DELETE /api/customers/{id}` |
| Customer import | `POST /api/customers/import` (`file` multipart field) |
| Beneficiaries | `GET/POST /api/beneficiaries`; `GET/PUT/DELETE /api/beneficiaries/{id}` |
| Consent | `GET/POST /api/consents`; `GET /status`, `/eligibility`; `POST /withdraw` |

Customer search supports `term`, `customerType`, `status`, `city`, `country`, `contactable`, `page`,
and `size`. `DELETE /customers/{id}` is a soft-delete workflow, not physical erasure. Consent and
do-not-contact history must not be rewritten to bypass eligibility.

### Products, ownership, payments, and change requests

| Family | Endpoints |
| --- | --- |
| Products | `GET/POST /api/products`; `GET/PUT/DELETE /api/products/{id}`; `PATCH /{id}/disable` |
| Ownerships | `GET/POST /api/product-ownerships`; `PUT /api/product-ownerships/{id}` |
| Payments | `GET/POST /api/payment-records`; `PUT /{id}`; `PATCH /{id}/mark-paid`, `/mark-overdue`, `/increment-reminder` |
| Product changes | `GET/POST /api/product-change-requests`; `PUT /{id}`; `PATCH /{id}/approve`, `/reject`, `/mark-implemented` |

Product mutation is restricted to Product Manager/Admin paths. Payment history feeds reminders and
AI default-risk recommendations and must represent actual business events.

### Segments and campaigns

| Family | Endpoints |
| --- | --- |
| Segments | `GET/POST /api/segments`; `GET/PUT/DELETE /api/segments/{id}`; `POST /api/segments/preview` |
| Campaign CRUD | `GET/POST /api/campaigns`; `GET/PUT /api/campaigns/{id}` |
| Campaign targeting | `GET/PUT /api/campaigns/{id}/products`; `GET/PUT /api/campaigns/{id}/segment` |
| Recipients | `GET /api/campaigns/{id}/recipients/preview`, `/eligible`, `/excluded`, `/summary` |
| Review | `POST /api/campaigns/{id}/submit`, `/approve`, `/reject`; `PUT /compliance-review-notes` |
| Lifecycle | `POST /api/campaigns/{id}/launch`, `/pause`, `/complete`, `/archive` |

Segment IDs are generated valid UUIDs. Segment preview/match does not authorize contact. Campaign
recipient preview applies `EligibilityService` consent, do-not-contact, opt-out, guardian, duplicate,
frequency, retry, and uninterested rules.

Campaign Manager creates/edits `DRAFT` or `REJECTED` campaigns and submits them. Compliance Officer
performs human approval/rejection; rejection requires a reason. Only `APPROVED` campaigns can launch.
AI cannot call or substitute for approval and cannot bypass eligibility.

### Communications, follow-ups, and reminders

| Family | Endpoints |
| --- | --- |
| Contact events | `GET /api/contact-events/timeline`; `POST /api/contact-events` |
| Follow-ups | `GET/POST /api/follow-up-tasks`; `PUT /{id}`, `/{id}/assign`, `/{id}/complete`, `/{id}/status` |
| Reminders | `GET /api/reminders`; `POST /payment`, `/payment/generate`, `/expiration`, expiration generate paths, `/due/send`, `/due/manual-trigger`; `PUT /{id}/sent`, `/{id}/cancel` |

Manual reminder triggering is restricted to approved admin/test use and does not bypass consent,
eligibility, contact limits, idempotency, or retry policy. Provider sending is approved real
configuration or explicitly disabled in production.

### Analytics, reports, and AI

| Family | Endpoints |
| --- | --- |
| Analytics | `GET /api/analytics/dashboard`, `/campaigns/{campaignId}`, `/products/performance`, `/executive` |
| Reports | `GET /api/reports/campaigns/{campaignId}/csv`, `/pdf`, `/exports`, `/exports/{exportId}` |
| AI search | `GET /api/ai/customer-search` |
| AI recommendations | `POST /api/ai/segment-suggestions`, `/product-recommendations`, `/duplicate-contact-warning`, `/campaign-copy` |
| AI copy approval | `POST /api/ai/campaign-copy/{recommendationId}/approve` (human-authenticated approval) |

AI responses include explanations and confidence when available; stored recommendations remain
decision support. AI cannot approve a campaign, approve its own copy, override consent/do-not-contact,
or bypass authorization/`EligibilityService`.

### Health

`GET /api/health` provides the application health contract used by the frontend. Actuator
liveness/readiness paths are documented in [Health Endpoints](../deployment/health-endpoints.md).
Do not expose internal health details publicly.

## Example requests

Login:

```powershell
$session = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/auth/login" `
  -ContentType "application/json" `
  -Body (@{ email = "employee@example.invalid"; password = "<prompted-secret>" } | ConvertTo-Json)
```

Do not put a real password directly in shell history. For actual use, read it from a secure prompt.

Authenticated customer search:

```powershell
$headers = @{ Authorization = "Bearer <access-token>" }
Invoke-RestMethod -Headers $headers `
  -Uri "http://localhost:8080/api/customers?term=Ada&page=0&size=20"
```

Create a draft campaign (request fields must match current generated schema):

```http
POST /api/campaigns HTTP/1.1
Authorization: Bearer <campaign-manager-access-token>
Content-Type: application/json

{
  "name": "Synthetic renewal outreach",
  "objective": "Test the approved renewal workflow",
  "channel": "EMAIL",
  "messageSubject": "Synthetic subject",
  "messageBody": "Synthetic test content",
  "startDate": "2026-09-01",
  "endDate": "2026-09-30",
  "productIds": []
}
```

Examples are illustrative and use synthetic data. Generated OpenAPI controls required fields and
types for the running release.

## Compatibility and client guidance

- The current base path is `/api`; no separate public version prefix is defined.
- Treat request/response schema changes as release changes and regenerate/compare OpenAPI.
- Clients must ignore unknown response fields where practical and must not depend on message text for
  logic; use HTTP status, stable error `code`, fields, and documented enum values.
- Use idempotency-aware application behavior: verify uncertain mutation outcomes before retrying.
- Never automate campaign approval, launch, consent changes, provider sends, or destructive actions
  without their required human/business controls.

## Related documentation

- [Role-Based Access](../architecture/role-based-access.md)
- [Authentication Design](../architecture/authentication-design.md)
- [Eligibility Rules](../architecture/eligibility-rules.md)
- [Module Documentation](../README.md#modules)
- [Production Security Checklist](../deployment/production-security-checklist.md)

Automated documentation evidence: `ApiDocumentationGuideTests`.
Export evidence: `OpenApiExportDocumentationTests`.
