# Performance Smoke Checks

**Backlog item 639:** Run performance smoke checks for search and dashboard.  
**KB requirement:** **NFR-003** — *Normal searches under 1 second for project dataset.*

Code:

- Backend: `backend/src/test/java/.../performance/PerformanceSmokeTests.java`
- Frontend catalog: `frontend/src/features/testing/performanceSmoke.ts`

> For mapping/documentation delivery, suites may be written with **do not run any tests** backlog
> wording. This document defines what *exists* to be run under item **639**.

## Purpose

Provide lightweight, deterministic **smoke** evidence that:

1. **Customer-style search** over a project-scale in-memory catalog finishes under **1000 ms**.
2. **Product-style search** (term + type + active filters) finishes under **1000 ms**.
3. **Dashboard KPI aggregation** over a project-scale campaign metrics set finishes under **1000 ms**.

These smokes protect NFR-003 without a formal load-test harness. They are not a production capacity
study and do not replace integration tests with Testcontainers for correctness.

## Scope

| In scope | Out of scope |
| --- | --- |
| Wall-clock budget **&lt; 1000 ms** (NFR-003) | Hard multi-tenant SLA / cloud load tests |
| Project-scale synthetic datasets (thousands of rows) | Million-row enterprise stress |
| Customer multi-field search semantics (FR-014) | Full Elasticsearch / external search engines |
| Product search + filters (FR-044) | Network latency to remote APIs |
| Dashboard aggregation via `AnalyticsService` (FR-100–FR-107) | Browser paint/FPS measurements |

## Budgets and dataset sizes

| Constant | Value | Meaning |
| --- | --- | --- |
| `NFR_003_BUDGET_MS` | **1000** | KB “under 1 second” target |
| `PROJECT_DATASET_SIZE` | **5000** | Synthetic customers / products for search smokes |
| `DASHBOARD_CAMPAIGN_COUNT` | **2000** | Synthetic campaign metrics rows for dashboard smoke |

Sizes reflect an insurance marketing **project dataset** (MVP course/demo scale), not big-data volumes.

## Surfaces under test

| Surface | KB refs | Backend | Frontend |
| --- | --- | --- | --- |
| `customer-search` | Customer / prospect search | FR-014, NFR-003 | `PerformanceSmokeTests` customer-style search | `filterCustomersSmoke` |
| `product-search` | Product catalog search | FR-044, NFR-003 | `PerformanceSmokeTests` product-style search | `filterProductsSmoke` + productSearch helpers |
| `dashboard` | Dashboard KPIs | FR-100–FR-108, NFR-003 | `AnalyticsService.getDashboard()` + `AnalyticsCalculations` totals | `dashboardKpiModelSmoke` / `buildDashboardKpiGroups` |

### Search semantics (aligned with KB)

**Customer search** matches across profile fields used by the platform search design:

- first name, last name, email, city, country, phone, source

**Product search** combines:

- free-text term (name / description / type)
- product type filter
- active flag filter

### Dashboard semantics

Dashboard smoke builds a project-scale list of campaigns and `CampaignMetrics`, then:

1. Calls `AnalyticsService.getDashboard()` (mocked repositories, real aggregation path).
2. Recomputes totals with `AnalyticsCalculations` (audience size, messages sent, etc.).

Frontend smoke repeatedly builds readability KPI groups so UI post-processing stays cheap after the
API returns.

## How to run (when execution is requested)

```text
# Backend performance smokes only
cd backend
mvn test -Dtest=PerformanceSmokeTests,PerformanceSmokeDocumentationTests

# Frontend performance smokes only
cd frontend
npx vitest run src/features/testing/performanceSmoke.test.ts
```

Do **not** treat a single overloaded CI agent as a formal SLA measurement; re-run locally if a
machine is heavily contended. Failures that exceed 1000 ms on a quiet laptop warrant algorithm or
query review.

## Relationship to other items

| Item | Relationship |
| --- | --- |
| **622** | NFR map references NFR-003 → item 639 |
| **631** | Analytics correctness suites (not timed) |
| **635–637** | FE component/integration/E2E (functional) |
| **617 / 642** | Full suite execution gates |

## Acceptance (item 639)

Item **639** is complete for *existence* when:

1. Backend `PerformanceSmokeTests` assert customer search, product search, and dashboard aggregation
   finish under **1000 ms** on the project-scale synthetic datasets.
2. Frontend `performanceSmoke.ts` / `.test.ts` lock the same budget and surfaces.
3. This document exists and is linked from `docs/README.md`.
4. Documentation tests lock key NFR-003 / item **639** wording.

Pass/fail of a live suite run is a separate execution step when the backlog asks to **run** tests.
