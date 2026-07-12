# Analytics Module Documentation

The analytics module provides campaign KPIs, engagement and conversion metrics, estimated ROI,
product performance comparisons, and executive-level aggregated dashboards for the
Bayer-Westphalian Campaign Management Platform (KB epic **E19**).

Metrics are derived primarily from `campaign_metrics` rows that are updated at campaign launch and
when contact events are recorded (**BR-034**). Analytics APIs expose calculated rates and totals; they
do not return raw contact-event streams on the executive surface (**COMP-010**).

## Package Boundary

Primary backend package:

```text
com.bayerwestphalian.campaign.analytics
```

Related domain package (metrics entity and contact events):

```text
com.bayerwestphalian.campaign.campaign
```

The analytics package contains:

| Component | Responsibility |
| --- | --- |
| `AnalyticsController` | REST API under `/api/analytics` |
| `AnalyticsService` | Aggregation of campaign inventory and `CampaignMetrics` into DTO views |
| `AnalyticsCalculations` | Shared KPI helpers (counts, rates, cost/revenue/ROI totals) |
| `AnalyticsRates` | Thin rate/ROI helpers used by DTOs |
| `DashboardView` | Platform dashboard payload (FR-100–FR-107) |
| `CampaignAnalyticsView` | Single-campaign analytics detail |
| `CampaignMetricsView` | Per-campaign counters and calculated rates |
| `ProductPerformanceView` | Product-level performance aggregates |
| `ExecutiveDashboardView` | Platform-level executive aggregates (COMP-010) |

Frontend surfaces (item 440–444, readability item **591** / NFR-005):

- `DashboardPage` — platform KPIs and charts with grouped inventory / engagement / financial KPI sections
- `AnalyticsPage` — campaign and product analytics
- `ExecutiveDashboardPage` — management aggregates with the same grouped KPI layout
- API client: `frontend/src/api/analytics.ts`
- Chart helpers under `frontend/src/components/charts` and `frontend/src/features/analytics`
- Readability helpers: `frontend/src/features/dashboard/dashboardReadability.ts`

## KB Traceability

| KB / FR / rule | Analytics capability |
| --- | --- |
| Epic **E19** | Analytics module (dashboards, KPIs, product performance) |
| **FR-100** | Dashboard shows campaign totals |
| **FR-101** | Dashboard shows active campaigns |
| **FR-102** | Dashboard shows audience size |
| **FR-103** | Dashboard shows messages sent |
| **FR-104** | Dashboard shows open rate |
| **FR-105** | Dashboard shows click rate |
| **FR-106** | Dashboard shows conversion rate |
| **FR-107** | Dashboard shows estimated ROI |
| **FR-108** | Users can view performance charts (frontend Recharts) |
| **BR-034** | Campaign metrics update after contact events (Sprint 16 item **656**: `ContactEventsUpdateAnalyticsTests`) |
| **COMP-010** | Executive reports use aggregated data where possible |

Acceptance suites formalize KPI correctness (items 446–454), executive aggregation (item 457), and
report aggregation plus metrics traceability to recipients/contact events (item **466**).

## REST API Surface

Base path: `/api/analytics`

| Method | Path | Description | Item / FR |
| --- | --- | --- | --- |
| `GET` | `/api/analytics/dashboard` | Platform KPIs + recent campaign metrics | 431 / FR-100–107 |
| `GET` | `/api/analytics/campaigns/{campaignId}` | Campaign identity + optional metrics | 432 |
| `GET` | `/api/analytics/products/performance` | Product performance rows | 433 |
| `GET` | `/api/analytics/executive` | Executive aggregate dashboard | 434 / 457 / COMP-010 |

All endpoints require authentication and an analytics-read role (see [Authorization](#authorization)).

Responses use the standard `ApiResponse` envelope (`success`, `message`, `data`).

## KPI Definitions

Formal catalog (formulas, precision, aggregation, worked examples):  
[KPI Definition Document](kpi-definitions.md) (item **461**).

Counts and rates are computed in `CampaignMetrics` and re-exported via `AnalyticsCalculations` /
`AnalyticsRates`. Dashboard and executive rates use **aggregate numerators ÷ aggregate denominators**,
not averages of per-campaign rates.

| KPI | Formula / definition | Notes |
| --- | --- | --- |
| Audience size | `eligible_count + excluded_count` | Prefer derived value over a stale stored column |
| Eligible count | Recipients with status `ELIGIBLE` | Updated at launch |
| Excluded count | Recipients with status `EXCLUDED` | Updated at launch |
| Sent count | Messages sent (`SENT` contact events at launch) | FR-103; item 449 |
| Opened / clicked / replied | OPENED / CLICKED / REPLIED contact events | BR-034; item 450 |
| Converted count | Conversion outcomes on contact events | BR-034 |
| Open rate | `opened_count / sent_count` when sent > 0; else 0 | Scale 4, HALF_UP; FR-104 |
| Click rate | `clicked_count / sent_count` when sent > 0; else 0 | Scale 4, HALF_UP; FR-105 |
| Conversion rate | `converted_count / sent_count` when sent > 0; else 0 | Scale 4, HALF_UP; FR-106 |
| Estimated cost | Optional non-negative monetary amount | Scale 2 |
| Estimated revenue | Optional non-negative monetary amount | Scale 2 |
| Estimated ROI | `(revenue − cost) / cost` when cost > 0 | Null if cost missing; 0 if cost is 0; scale 2; FR-107 |

### Dashboard aggregation

`AnalyticsService.getDashboard()`:

1. Loads all campaigns and all `CampaignMetrics` rows.
2. Counts total and **ACTIVE** campaigns (FR-100, FR-101).
3. Sums audience, eligible, excluded, sent, engagement, and financial fields via
   `AnalyticsCalculations.total*`.
4. Computes open/click/conversion rates from **total** opened|clicked|converted over **total** sent.
5. Computes ROI from **total** estimated cost and revenue.
6. Returns up to 10 most recently updated `CampaignMetricsView` rows as `recentCampaignMetrics`.

### Executive aggregation (COMP-010)

`AnalyticsService.getExecutiveDashboard()` returns platform-level aggregates only:

- Campaign inventory: total, active, completed
- Audience funnel: total audience, eligible, excluded, sent
- Engagement totals: opened, clicked, replied, converted
- Overall rates from aggregate opened|clicked|converted ÷ sent
- Total estimated cost, revenue, and overall ROI
- Embedded product performance summary rows (same aggregation as product performance endpoint)

The executive payload does **not** expose raw contact-event lists, recipient detail, or customer PII
rows. This satisfies **COMP-010** (item 457).

### Product performance

`GET /api/analytics/products/performance` groups campaign–product links by product, sums metrics of
linked campaigns, and derives rates/ROI from product-level totals.

### Campaign analytics detail

`GET /api/analytics/campaigns/{campaignId}` returns campaign identity (name, objective, status,
channel, schedule, owner) and optional `CampaignMetricsView`. Metrics may be `null` when the campaign
has not been launched / has no metrics row yet. Unknown campaign ids yield `404`.

## Metrics Lifecycle (Traceability)

```text
Recipient preview / launch
        │
        ▼
campaign_recipients (ELIGIBLE / EXCLUDED)
        │
        ▼ launch
contact_events (SENT) ──► campaign_metrics.sent_count (+ audience/eligible/excluded)
        │
        ▼ BR-034
contact_events (OPENED / CLICKED / REPLIED / CONVERTED)
        │
        ▼ CommunicationService increments
campaign_metrics opened/clicked/replied/converted
        │
        ▼ AnalyticsService
Dashboard / Campaign / Product / Executive views
```

- Launch: `CampaignService.launchCampaign` records launch counts on `CampaignMetrics`.
- Engagement: `CommunicationService` applies `incrementOpened`, `incrementClicked`,
  `incrementReplied`, conversion, and optional `incrementSent` after campaign-linked contact events
  are saved (**BR-034** / item 450).

Reports (CSV/PDF) consume the same analytics views; see
[Report Export Documentation](report-export.md) (item 460).

## Authorization

Analytics endpoints are restricted by Spring Security path rules and method-level `@PreAuthorize`.

**Allowed roles** (matches `SecurityConfiguration` `/api/analytics/**`):

- `ADMIN`
- `BI_ANALYST`
- `CAMPAIGN_MANAGER`
- `MARKETING_ANALYST`
- `EXECUTIVE_VIEWER`

**Not allowed** (examples): `PRODUCT_MANAGER`, `COMPLIANCE_OFFICER`, `CUSTOMER_SERVICE_AGENT`,
`SALES_AGENT`, `SYSTEM_AUDITOR`.

Unauthenticated requests receive `401 Unauthorized`. Authenticated users with insufficient roles
receive `403 Forbidden`. Backend authorization is authoritative; frontend navigation only improves UX.

Frontend permissions: `canViewAnalytics` / related helpers in
`frontend/src/features/auth/permissions.ts`. Routes: `/dashboard`, `/analytics`, `/executive`.

## Frontend Screens

| Screen | Route | Data source |
| --- | --- | --- |
| Dashboard | `/dashboard` | `GET /api/analytics/dashboard` |
| Analytics | `/analytics` | dashboard + campaign detail + product performance |
| Executive dashboard | `/executive` | `GET /api/analytics/executive` |

Charts (FR-108) use Recharts components (bar, pie, line) fed from dashboard and analytics adapters.

## Related Documentation

- [Communication Tracking Module](communication-tracking.md) — contact events and BR-034 source data
  (critical suite item **656** / `ContactEventsUpdateAnalyticsTests`)
- [Campaign Launch Documentation](campaign-launch.md) — launch metrics and sent counts
- [Role-Based Access Documentation](../architecture/role-based-access.md) — analytics role matrix
- [Report Export Documentation](report-export.md) — CSV/PDF using campaign analytics KPIs (item 460)
- [KPI Definition Document](kpi-definitions.md) — formal KPI catalog (item 461)
- [BI Analyst User Guide](../user-guides/bi-analyst-guide.md) — BI Analyst screen workflows (item 462)
- [Executive Viewer User Guide](../user-guides/executive-viewer-guide.md) — executive aggregate workflows (item 463)

## Implementation Evidence

| Area | Location |
| --- | --- |
| Package | `backend/.../analytics/` |
| Metrics entity | `backend/.../campaign/CampaignMetrics.java` |
| Contact-event → metrics | `backend/.../communication/CommunicationService.java` |
| REST controller | `AnalyticsController` |
| Service | `AnalyticsService` |
| Calculations | `AnalyticsCalculations`, `AnalyticsRates` |
| Acceptance tests | `*IsCalculatedCorrectlyTests`, `ExecutiveReportUsesAggregatedDataTests`, `ReportsUseAggregatedDataAndMetricsAreTraceableTests` (item 466, report package) |
| Endpoint tests | `DashboardEndpointTests`, `CampaignAnalyticsEndpointTests`, `ProductPerformanceEndpointTests`, `ExecutiveDashboardEndpointTests` |
| Frontend API | `frontend/src/api/analytics.ts` |
| Frontend pages | `DashboardPage`, `AnalyticsPage`, `ExecutiveDashboardPage` |
