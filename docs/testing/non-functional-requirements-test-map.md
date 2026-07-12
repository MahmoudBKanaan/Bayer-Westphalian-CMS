# Non-Functional Requirements Test Map

**Backlog item 622 / item **622****: Map tests to non-functional requirements.

This document maps knowledge-base **Non-Functional Requirements (NFR-xxx)** to
primary automated evidence and operational documentation in the Bayer-Westphalian
Campaign Management Platform repository.

Code catalog (locked by Vitest):  
`frontend/src/features/testing/nonFunctionalRequirementsTestMap.ts`

Backend documentation anchor: `SecurityHardeningDocumentationTests`.

> Mapping and documentation only for this item — **do not run any tests** as
> part of item 622 delivery. Suite execution is covered by later Sprint 16 run
> items (e.g. **638–640**, **623+**, **617**).

Companion maps:

- [Functional Requirements Test Map](functional-requirements-test-map.md) (item **620**)
- [Business Rules Test Map](business-rules-test-map.md) (item **621**)

## Purpose

Provide NFR-to-test (and NFR-to-ops-doc) traceability so Sprint 16 QA can:

1. Show security, privacy, usability, accessibility, and testability evidence.
2. Link NFRs to critical tests (**663–665**) and run items (**638–640**, **666**).
3. Separate quality attributes from functional (FR) and business-rule (BR) maps.

## Scope

| In scope | Out of scope |
| --- | --- |
| NFR-001–NFR-014 (KB section 12) | FR map (**620**), BR map (**621**) |
| Primary JUnit / Vitest / Playwright anchors | Full suite execution (**623+**, **617**) |
| Deployment / migration docs where NFR is ops-level | Exhaustive listing of every related method |
| Smoke/performance *mapping* for NFR-003 | Formal load benchmarks (project-level smoke only) |

## How to read this map

| Column | Meaning |
| --- | --- |
| **ID** | KB non-functional requirement ID |
| **Name / Target** | KB requirement and target statement |
| **Backend tests** | Primary JUnit class simple names |
| **Frontend tests** | Paths under `frontend/` |
| **Docs** | Architecture, development, deployment, or testing docs |
| **Sprint 16** | Related run / critical / documentation items |

Some NFRs are **asymmetric**: accessibility is frontend-primary (**NFR-011**);
backup/recovery is ops/docs + migration integrity (**NFR-013**, no backup UI; item **666** runbook).

## Security and privacy (NFR-001–NFR-002)

| ID | Name | Target | Backend tests | Frontend tests | Docs | Sprint 16 |
| --- | --- | --- | --- | --- | --- | --- |
| NFR-001 | Security | Role-based access, password hashing, JWT/session security | `ProtectedEndpointSecurityTests`, **`DisabledUserCannotLogInTests`** (item **659**), **`ReportExportIsRestrictedToAuthorizedRolesTests`** (item **663**), **`ProductionProfileHidesStackTracesTests`** (item **664**), **`MissingSecretsAreDetectedTests`** (item **665**), **`ProductManagerCannotLaunchCampaignsTests`**, **`BiAnalystCannotEditCustomersTests`**, **`ComplianceOfficerCanApproveRejectCampaignsTests`**, method auth, security hardening | disabledUserCannotLogIn, reportExportIsRestrictedToAuthorizedRoles, productionProfileHidesStackTraces, missingSecretsAreDetected, permissions, productManagerCannotLaunchCampaigns, biAnalystCannotEditCustomers, complianceOfficerCanApproveRejectCampaigns | security-hardening, role-based-access, authentication-design, report-export, production-security-checklist | **627**, **640**, **653–655**, **659**, **663–665** |
| NFR-002 | Privacy | GDPR-aware consent, opt-out, data minimization | **`AiRecommendationCannotBypassConsentRulesTests`** (item **661**), `ConsentServiceTests`, eligibility, consent/opt-out audit, safe error/sensitive data tests | aiRecommendationCannotBypassConsentRules, consentUpdateFlow + integration + e2e, ConsentStatusBadge | consent-module, eligibility-rules, security-hardening, ai-limitations-and-human-approval | **628**, **649**, **661** |

## Performance and availability (NFR-003–NFR-004)

| ID | Name | Target | Backend tests | Frontend tests | Docs | Sprint 16 |
| --- | --- | --- | --- | --- | --- | --- |
| NFR-003 | Performance | Normal searches under 1 second for project dataset | **PerformanceSmokeTests** (customer/product search + dashboard aggregation &lt; 1000 ms); supporting search/dashboard endpoint tests; Flyway indexes | **performanceSmoke.test.ts**; CustomersPage, productSearch, dashboard flows | [performance-smoke.md](performance-smoke.md), migration-strategy, customer-module, analytics-module | **639** (perf smoke) |
| NFR-004 | Availability | 99% target for project-level deployment | `HealthEndpointIntegrationTests`, health controller, app context, PostgreSQL connection | App shell, ErrorState | production-security-checklist, docker README, developer-setup | **646**, **674** |

Project-level performance evidence is **smoke-oriented** (search/dashboard paths under
**639**), not a formal SLA harness. Availability is supported by health checks,
containerized Postgres, and deploy checklist—not continuous uptime monitoring in MVP.

## Usability and accessibility (NFR-005, NFR-011)

| ID | Name | Target | Backend tests | Frontend tests | Docs | Sprint 16 |
| --- | --- | --- | --- | --- | --- | --- |
| NFR-005 | Usability | Clear dashboards, forms, filters, validation | product search UI tests, customer/campaign user-guide documentation tests | dashboardReadability, form validation, empty/success/confirm, uiStyleNotes, businessUserWorkflowGate, workflow routes | ui-style-notes, sprint-15-production-gate, UI acceptance docs | **635–637** |
| NFR-011 | Accessibility | Labels, keyboard support, contrast | *(frontend-primary; no dedicated backend a11y suite)* | keyboardNavigationFlow, mainScreensAccessibility, accessibilityNotes, integration + Playwright specs, styles tokens | accessibility-notes, ui-keyboard-navigation, ui-main-screens-accessibility, ui-style-notes | **638** |

## Maintainability, scalability, and testability (NFR-006, NFR-007, NFR-010)

| ID | Name | Target | Backend tests | Frontend tests | Docs | Sprint 16 |
| --- | --- | --- | --- | --- | --- | --- |
| NFR-006 | Maintainability | Layered backend, reusable frontend components | package README, BaseEntity, exception handler, OpenAPI | frontendComponentInventory, shared badges/metric card, styles, frontendTestingNotes | initial-architecture, frontend-component-tests, developer-setup, READMEs | **619**, **667** |
| NFR-007 | Scalability | Pagination, indexes, async jobs | paginated customer APIs, PageResponse, Flyway indexes, reminder scheduler, send retry | CustomersPage/API, RemindersPage | migration-strategy, reminder-scheduler, customer-module | **624–625**, **630** |
| NFR-010 | Testability | Unit, integration, API, UI tests | application, health IT, Flyway IT, security IT baselines | frontend testing notes, FR/BR maps, happyPathFlow, component inventory, integration harness | frontend testing pyramid docs, FR/BR maps | **619–622**, **623**, **635–637**, **667**, **670** |

## Auditability and reliability (NFR-008–NFR-009)

| ID | Name | Target | Backend tests | Frontend tests | Docs | Sprint 16 |
| --- | --- | --- | --- | --- | --- | --- |
| NFR-008 | Auditability | Sensitive actions logged | **`AuditLogIsCreatedAfterConsentChangeTests`** (item **658**), `ConsentChangeCreatesAuditLogTests`, `AuditServiceTests`, controller/docs, campaign/product/user/report audit creation tests | auditLogIsCreatedAfterConsentChange, AuditPage, auditLogs API, AuditActionBadge | audit-logging, consent-module, campaign/product audit docs, system-auditor-guide | **633**, **658** |
| NFR-009 | Reliability | Failed sends can be retried | `SendRetryServiceTests`, configurable retry limit, communication + providers, reminder scheduler | contactEvents API, ContactHistoryPage, ErrorState | communication-tracking, system-settings, reminder-scheduler | **629–630** |

Aligns with business rule **BR-012** (max retries) on the [business rules map](business-rules-test-map.md).

## Data integrity, backup, and observability (NFR-012–NFR-014)

| ID | Name | Target | Backend tests | Frontend tests | Docs | Sprint 16 |
| --- | --- | --- | --- | --- | --- | --- |
| NFR-012 | Data integrity | Foreign keys, constraints, transactions | Flyway migration IT/resources, Postgres IT, soft-delete/base entity, campaign/segment/product entity IT | API client + domain client contract tests | migration-strategy, initial-architecture | **624–625** |
| NFR-013 | Backup/recovery | Database backup strategy | **`BackupAndRestoreProcessIsDocumentedAndTestableTests`** (item **666**), Flyway IT, Postgres IT, production security checklist documentation | backupAndRestoreProcessIsDocumentedAndTestable *(ops catalog; no backup UI)* | backup-and-restore, production-security-checklist, migration-strategy, docker README | **666** |
| NFR-014 | Observability | Logs, health endpoints, error tracking | health endpoint/controller/response, GlobalExceptionHandler, safe API error logger, secure errors, **`ProductionProfileHidesStackTracesTests`** (item **664**), `ProductionStackTraceHiddenTests`, AuditService | productionProfileHidesStackTraces, ErrorState, API client, App | security-hardening, production-security-checklist, audit-logging | **640**, **664** |

## Critical and run-item crosswalk

| Sprint 16 item | Focus | Primary NFR(s) |
| --- | --- | --- |
| **627** | Role-based access tests | NFR-001 |
| **653** | Product Manager cannot launch campaigns | NFR-001 (RBAC / least privilege) |
| **654** | BI Analyst cannot edit customers | NFR-001 (RBAC / least privilege) |
| **655** | Compliance Officer can approve/reject campaigns | NFR-001 (RBAC / least privilege) |
| **628** | Consent and eligibility tests | NFR-002 (+ BR eligibility) |
| **633** | Audit log tests | NFR-008 |
| **638** | Accessibility checks | NFR-011 |
| **639** | Performance smoke (search/dashboard) | NFR-003 |
| **640** | Security regression | NFR-001, NFR-014 |
| **658** | Audit after consent change | NFR-008 — `AuditLogIsCreatedAfterConsentChangeTests` |
| **659** | Disabled user cannot log in | NFR-001 — `DisabledUserCannotLogInTests` |
| **661** | AI recommendation cannot bypass consent rules | NFR-002 — `AiRecommendationCannotBypassConsentRulesTests` |
| **663** | Report export restricted | NFR-001 — `ReportExportIsRestrictedToAuthorizedRolesTests` |
| **664** | Production profile hides stack traces | NFR-001, NFR-014 — `ProductionProfileHidesStackTracesTests` |
| **665** | Missing secrets detected | NFR-001 — `MissingSecretsAreDetectedTests` |
| **666** | Backup/restore documented and testable | NFR-013 — `BackupAndRestoreProcessIsDocumentedAndTestableTests` |
| **670** | Requirement-to-test traceability matrix | NFR-010 |
| **674** | No RC if critical security/consent/auth/audit/approval fail | NFR-001, NFR-002, NFR-008 |

### Grouped NFR sets (catalog helpers)

| Set | IDs | Use |
| --- | --- | --- |
| Security hardening | NFR-001, NFR-002, NFR-014 | Hardening + prod safety |
| UX / accessibility | NFR-005, NFR-011 | Sprint 15 frontend quality |
| Testability / structure | NFR-006, NFR-010 | Pyramid + mapping docs |

## Coverage summary

| ID | Name | Domain |
| --- | --- | --- |
| NFR-001 | Security | security |
| NFR-002 | Privacy | privacy |
| NFR-003 | Performance | performance |
| NFR-004 | Availability | availability |
| NFR-005 | Usability | usability |
| NFR-006 | Maintainability | maintainability |
| NFR-007 | Scalability | scalability |
| NFR-008 | Auditability | auditability |
| NFR-009 | Reliability | reliability |
| NFR-010 | Testability | testability |
| NFR-011 | Accessibility | accessibility |
| NFR-012 | Data integrity | data-integrity |
| NFR-013 | Backup/recovery | backup-recovery |
| NFR-014 | Observability | observability |
| **Total** | | **14** |

## Related Sprint 16 items

| Item | Topic |
| --- | --- |
| **619** | Master test plan |
| **620** | Functional requirements test map |
| **621** | Business rules test map |
| **622** | This map (non-functional requirements) |
| **638–640** | A11y / performance smoke / security regression runs |
| **666** | Backup and restore documentation — `BackupAndRestoreProcessIsDocumentedAndTestableTests` |
| **670** | Requirement-to-test traceability matrix |
| **674** | Release candidate quality gate |
| **617** / **623+** | Run suites and fix failures |

## Acceptance (item 622)

Item **622** is complete when:

1. This document exists and states the backlog goal in KB language.
2. Every KB NFR-001–NFR-014 ID appears with mapped evidence and docs.
3. A code catalog (`nonFunctionalRequirementsTestMap.ts`) locks IDs, domains, and anchors.
4. Unit tests assert catalog completeness, special cases (NFR-011 frontend-primary, NFR-013 ops/docs backup runbook), doc snippets, and `docs/README.md` linkage.
5. The map does not claim suite execution; wording notes **do not run any tests** for this item.

Catalog path: `frontend/src/features/testing/nonFunctionalRequirementsTestMap.ts`  
Backend documentation test: `backend/src/test/java/.../support/NonFunctionalRequirementsTestMapDocumentationTests.java`
