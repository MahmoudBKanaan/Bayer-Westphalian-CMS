# Backend Package Structure

The backend is organized as a modular monolith under:

```text
com.bayerwestphalian.campaign
```

Each top-level package owns one business capability from the Knowledge Base. Modules should keep
their controllers, services, domain objects, repositories, DTOs, validation, and adapters inside
their own package unless a class is intentionally shared through `common`.

## Module Map

| Package | Responsibility |
| --- | --- |
| `auth` | Internal employee login, logout, token/session handling, and authenticated user context. |
| `user` | Employee accounts, role assignment, disable/lock workflows, and admin user management. |
| `customer` | Customers, prospects, profile search, filters, status changes, imports, and soft delete. |
| `beneficiary` | Policyholder-to-beneficiary links, relationship metadata, and guardian-consent requirements. |
| `consent` | Marketing consent, opt-out evidence, guardian consent, consent validity, and compliance checks. |
| `product` | Insurance/investment products, ownership, expiration, product-change requests, and product lifecycle. |
| `campaign` | Campaign creation, approval workflow, campaign products, recipient preview, launch, and lifecycle status. |
| `segment` | Saved audience definitions, segment criteria, preview rules, and reusable targeting logic. |
| `schedule` | Reminder schedules, follow-up task timing, payment-due reminders, and product-expiration reminders. |
| `communication` | Contact events, email/SMS/phone history, outcomes, replies, and replaceable provider adapters. |
| `analytics` | Campaign metrics, dashboards, conversion, ROI, and BI-facing aggregate views. |
| `audit` | Sensitive action history, audit evidence, approval history, and compliance/auditor visibility. |
| `ai` | AI-assisted recommendations, explanations, risk/copy suggestions, and human approval tracking. |
| `report` | Report export requests, CSV/PDF export state, export history, and report file references. |
| `common` | Shared API contracts, exceptions, base entities, validation, OpenAPI configuration, and cross-module utilities. |

## Dependency Rules

- Modules may depend on `common`.
- Modules should avoid direct dependencies on unrelated modules unless the dependency represents a real KB workflow.
- Cross-module workflows should be coordinated through services instead of reaching into another module's persistence details.
- Backend validation is authoritative; frontend validation is only a user-experience layer.
- Security, consent, campaign eligibility, audit, and customer-data rules remain backend-owned.
- External integrations such as email, SMS, AI, and file storage should be replaceable adapters.
  Mock providers are allowed only for development and testing.

## Data And Migration Boundaries

- PostgreSQL is the system of record.
- Flyway owns all schema and seed-data changes.
- Production migrations live in `src/main/resources/db/migration`.
- Controlled demonstration data lives separately in `src/main/resources/db/demo` and is only enabled by dev/test profiles.

## Common Layer

The `common` package is intentionally small. It contains shared building blocks that all modules can
use without creating circular dependencies:

- API response contracts such as `ApiResponse`, `PageResponse`, `ErrorResponse`, and `ValidationError`.
- `GlobalExceptionHandler` for consistent REST error output.
- Base persistence classes such as `BaseEntity` and `SoftDeletableEntity`.
- Common exception classes such as `ApplicationException`, `ValidationException`,
  `ResourceNotFoundException`, and access/business-rule exceptions.
- OpenAPI configuration and other cross-module configuration.

## Implementation Guidance

New features should start inside the package that owns the KB capability. Add shared code to
`common` only when more than one module genuinely needs it. Keep package-level APIs explicit, keep
business rules close to their owning module, and prefer focused tests that verify module behavior
without depending on unrelated packages.
