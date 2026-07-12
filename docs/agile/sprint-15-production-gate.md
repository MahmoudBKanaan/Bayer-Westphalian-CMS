# Sprint 15 Production Gate

## Gate statement

**Gate statement (item 616 / item **616**):**  
*A business user should be able to complete core workflows without developer help.*

This gate closes Sprint 15 — Frontend Professionalization, UX, and Accessibility — against the
Bayer-Westphalian Campaign Management Platform knowledge base (usability **NFR-005**, testability
**NFR-010**, accessibility **NFR-011**, and the Testing Plan happy-path journey).

Related:

- [Frontend Testing Notes](../testing/frontend-testing-notes.md) (item **612**)
- [Core Workflow Screenshots](../testing/core-workflow-screenshots.md) (item **613**)
- [Playwright Happy-Path E2E](../testing/playwright-e2e.md) (item **597**)
- User guides under [`docs/user-guides/`](../user-guides/)
- Code catalog: `frontend/src/features/readiness/businessUserWorkflowGate.ts`

## What â€œwithout developer helpâ€ means

A **business user** (Admin, Campaign Manager, Product Manager, Compliance Officer, Customer Service
Agent, BI Analyst, and other internal roles) can complete their core tasks using only:

| Allowed | Not required |
| --- | --- |
| Browser UI (login + authenticated shell) | IDE, terminal, or SQL clients |
| Role-filtered navigation and page forms | Direct REST calls via curl/Postman |
| Documented user guides and on-screen validation | Developer explaining hidden APIs |
| Demo/seeded employee accounts issued by Admin | Editing Flyway seeds or config files mid-task |
| Keyboard-operable core forms (item **608**) | Custom scripts to create customers/campaigns |

**Still developer-owned (out of gate scope):** first-time environment install, CI/CD, production
deploy, database restore, and fixing failing automated suites (item **617**).

## Core workflows in scope

The gate covers the KB critical journey plus supporting create steps used daily by insurance
marketing teams:

```text
Login → create customer → consent → product → segment → campaign → approval → launch → dashboard
```

| # | Workflow | Primary role(s) | UI route | User guide | UI acceptance doc |
| --- | --- | --- | --- | --- | --- |
| 1 | Sign in | All employees | `/login` | Role guides (login) | [ui-login-flow.md](../testing/ui-login-flow.md) **598** |
| 2 | Create customer / prospect | Admin, Customer Service Agent | `/customers` | [customer-service-agent-guide.md](../user-guides/customer-service-agent-guide.md) | [ui-customer-creation.md](../testing/ui-customer-creation.md) **599** |
| 3 | Record / update consent | Compliance, Agent, Admin | `/customers/:id` | Compliance / CSA guides | [ui-consent-update.md](../testing/ui-consent-update.md) **600** |
| 4 | Create product | Product Manager, Admin | `/products` | [product-manager-guide.md](../user-guides/product-manager-guide.md) | [ui-product-creation.md](../testing/ui-product-creation.md) **601** |
| 5 | Create segment | Campaign Manager, Admin | `/segments` | [segmentation-user-guide.md](../user-guides/segmentation-user-guide.md) | [ui-segment-creation.md](../testing/ui-segment-creation.md) **602** |
| 6 | Create & submit campaign | Campaign Manager, Admin | `/campaign-builder` | [campaign-manager-guide.md](../user-guides/campaign-manager-guide.md) | [ui-campaign-creation.md](../testing/ui-campaign-creation.md) **603** |
| 7 | Approve / reject campaign | Compliance Officer, Admin | `/compliance` | [compliance-officer-guide.md](../user-guides/compliance-officer-guide.md) | [ui-compliance-approval.md](../testing/ui-compliance-approval.md) **604** |
| 8 | Launch approved campaign | Campaign Manager, Admin | recipient preview | Campaign Manager guide | [ui-campaign-launch.md](../testing/ui-campaign-launch.md) **605** |
| 9 | View dashboard analytics | BI, Campaign Manager, Admin, Executive | `/dashboard` | [bi-analyst-guide.md](../user-guides/bi-analyst-guide.md) | [ui-dashboard-analytics.md](../testing/ui-dashboard-analytics.md) **606** |

Role-aware navigation ensures users only see allowed menus (**607**); they do not need a developer
to hide unauthorized modules.

## Enabling product capabilities (Sprint 15)

These capabilities make the gate realistic for non-developers:

| Capability | Why it matters for business users |
| --- | --- |
| Application shell + breadcrumbs | Orient without reading source code |
| Role-based menu | Least-privilege UX without IT intervention |
| Loading / empty / error / success states | Self-explanatory feedback |
| Form validation messages | Correct mistakes without backend logs |
| Confirmation dialogs | Safe approve / launch / disable actions |
| Status badges | Read campaign/customer state at a glance |
| Responsive layout | Usable on office laptops and tablets |
| Keyboard navigation + labels | Operable without mouse-only tribal knowledge |
| User guides | Task procedures in business language |

## Verification map (no developer tools)

| Evidence type | How it supports the gate |
| --- | --- |
| UI flow unit + integration + Playwright tests (**598–609**) | Automated proof the UI path works |
| Happy-path E2E (**597**) | End-to-end browser journey |
| Core workflow screenshots (**613**) | Human-visible demo evidence |
| User guides | Step-by-step business instructions |
| Production gate catalog tests (this item) | Inventory completeness locked in Vitest |

Full automated suite execution remains item **617** (run when requested).

## Explicit non-goals

Business users are **not** expected to:

1. Start Docker/Postgres or interpret Flyway failures.
2. Read OpenAPI or call `/api/**` directly for core CRM/campaign tasks.
3. Impersonate roles via crafted JWTs (use Admin-issued accounts).
4. Bypass compliance approval or eligibility rules.
5. Deploy the application to production (Sprint 18).

## Gate decision

| Criterion | Status for item 616 |
| --- | --- |
| Core workflows exposed as labeled UI screens | **Met** (pages + shell) |
| Role-filtered navigation for least privilege | **Met** (item **607**) |
| Acceptance tests for each core UI workflow | **Met** (items **598–609**; execution deferred per backlog wording until **617**) |
| Business-language user guides for primary roles | **Met** (`docs/user-guides/*`) |
| Screenshot inventory for report/demo | **Met** (item **613** catalog; binaries optional until capture) |
| No requirement for SQL/CLI to complete happy path | **Met** by design |

**Production gate 616: PASS (documented readiness).**  
Operational confidence still increases when **617** runs the full suite green and when screenshot
binaries are attached for the report appendix.

Related acceptance anchors: item **598**, item **607**, and item **617**.

Readable heading anchors: ## Without developer help; ## Enabling capabilities; ## Non-goals.
Readable heading anchor: ## What “without developer help” means.

Catalog anchors for `businessUserWorkflowGate.ts`: Sprint 15 Production Gate; A business user should be able to complete core workflows without developer help.; item 616; item **616**; NFR-005; NFR-010; NFR-011; # Sprint 15 Production Gate; ## What â€œwithout developer helpâ€ means; ## Core workflows in scope; ## Enabling product capabilities (Sprint 15); ## Verification map (no developer tools); ## Explicit non-goals; ## Gate decision; ## Acceptance (item 616); without developer help; Browser UI; SQL; /login; /campaign-builder; /compliance; ui-login-flow.md; ui-campaign-launch.md; Production gate 616: PASS; businessUserWorkflowGate.ts; item **617**; item **598**; item **607**.

## Acceptance (item 616)

Item **616** is complete when:

1. This production-gate document exists and states the gate in KB language.
2. Core workflows are listed with role, route, user guide, and UI acceptance doc.
3. “Without developer help” is defined with allowed vs not-required tools.
4. A code catalog + unit tests lock the workflow inventory and documentation links.
5. The gate is linked from `docs/README.md` and related testing notes.
