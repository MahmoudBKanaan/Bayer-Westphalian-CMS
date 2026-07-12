# BI Analyst User Guide

This guide describes the MVP workflows available to employees with the `BI_ANALYST` role.

## Scope

BI Analysts evaluate campaign performance, audience behavior, and product outcomes. They can view
analytics dashboards, campaign and product performance metrics, executive aggregates, CSV/PDF
reports, segmentation insights, and read-only customer/product context needed for analysis.

BI Analysts **cannot** edit customer records, create or launch campaigns, approve compliance
reviews, manage employee users, assign roles, or mutate product catalog data unless they also hold
an additional authorized role.

Primary value for this role: accurate, role-scoped insight into **open rate**, **click rate**,
**conversion rate**, **estimated ROI**, audience funnel counts, and exportable campaign reports
(KB allowed functions: view analytics, reports, segmentation insights, audience counts, campaign
performance, and product performance).

## What BI Analysts Can Do

| Capability | Typical screen / API |
| --- | --- |
| View platform KPIs (campaign totals, active campaigns, audience, sent, rates, ROI) | Dashboard `/dashboard` — `GET /api/analytics/dashboard` |
| Analyze campaign and product performance charts | Analytics `/analytics` |
| View executive aggregated management KPIs | Executive `/executive` — `GET /api/analytics/executive` |
| Export campaign CSV and PDF reports; list export history | Reports `/reports` — `/api/reports/**` |
| Read and preview segments for insight (not ownership) | Segments `/segments` |
| Read customers/prospects for analytical context | Customers `/customers` |
| Read products and product-change request status | Products, Change requests |
| Read campaign-linked contact history where permitted | Contact history |

Frontend menu access for `BI_ANALYST` typically includes: **Dashboard**, **Customers**,
**Products**, **Change requests**, **Segments**, **Contact history**, **Analytics**,
**Executive**, and **Reports**. Backend authorization remains authoritative.

## What BI Analysts Cannot Do Alone

- **Edit customers** — create, update, soft-delete, or CSV import (KB **TC-009** / Sprint 16 item
  **654**: BI Analyst cannot edit customers; `BiAnalystCannotEditCustomersTests`).
- **Create, edit, or delete reusable segments** unless also granted `CAMPAIGN_MANAGER` or `ADMIN`
  (item **200**; see [Segmentation User Guide](segmentation-user-guide.md)).
- **Create, submit, approve, reject, or launch campaigns**.
- **Manage products** (create/edit/disable) or assign product ownership.
- **Manage users**, roles, or passwords.
- **View full audit administration** reserved for Admin / Compliance Officer / System Auditor.
- **Bypass consent, eligibility, or compliance gates** — analytics only consume already recorded
  metrics and events.

## Dashboard Workflow

BI Analysts can use the **Dashboard** (`/dashboard`) to:

- View **campaign totals** and **active campaigns** (FR-100, FR-101).
- Review **audience size**, **messages sent**, **open rate**, **click rate**, **conversion rate**,
  and **estimated ROI** (FR-102–FR-107).
- Spot engagement or ROI outliers that need deeper campaign or product analysis.
- Navigate to **Analytics**, **Executive**, **Reports**, and **Segments**.

Dashboard data comes from `GET /api/analytics/dashboard`. KPI formulas are defined in the
[`KPI Definition Document`](../modules/kpi-definitions.md).

## Analytics Workflows

BI Analysts can use the **Analytics** screen (`/analytics`) to:

- Inspect platform KPIs alongside **campaign analytics** detail for a selected campaign
  (`GET /api/analytics/campaigns/{campaignId}`).
- Compare **product performance** rows (`GET /api/analytics/products/performance`).
- Use **Recharts** visualizations (FR-108) for engagement, conversion, and ROI comparisons.

### Interpreting rates correctly

- Rates are **fractions of messages sent** (open/click/conversion ÷ sent), not percentages stored in
  the API.
- Platform-level rates use **aggregate numerators ÷ aggregate sent**, not averages of per-campaign
  rates.
- When **sent = 0**, open/click/conversion rates are **0**.

See [`Analytics Module Documentation`](../modules/analytics-module.md) and the
[`KPI Definition Document`](../modules/kpi-definitions.md).

## Executive Dashboard Workflow

BI Analysts can open **Executive** (`/executive`) for high-level **aggregated** KPIs only
(**COMP-010** / item **457**):

- Campaign inventory (total, active, completed).
- Audience funnel (audience, eligible, excluded, sent).
- Engagement totals (opened, clicked, replied, converted).
- Overall rates and estimated cost / revenue / ROI from **sums**, not raw contact-event lists.
- Embedded product performance summary.

Use this view for management briefings. Prefer it when personal data detail is not required.

API: `GET /api/analytics/executive`.

## Reports Workflows

BI Analysts can use the **Reports** screen (`/reports`) to:

- Select a campaign and download a **CSV** performance report (FR-109).
- Download a **PDF** performance report (FR-110).
- Review **export history** (newest first), including own exports when filtered.

APIs:

- `GET /api/reports/campaigns/{campaignId}/csv`
- `GET /api/reports/campaigns/{campaignId}/pdf`
- `GET /api/reports/exports` (optional `mine`, `status` query parameters)
- `GET /api/reports/exports/{exportId}`

Report files use the **same KPI formulas** as analytics; they do not invent separate metrics.
Export is restricted: unauthenticated callers receive **401 Unauthorized**; roles outside the
report matrix receive **403 Forbidden** without generating files (item **458**).

Details: [`Report Export Documentation`](../modules/report-export.md).

## Segmentation Insight Workflows

BI Analysts use **Segments** for **insight**, not definition ownership (unless dual-roled):

- Browse saved segments and inspect criteria patterns.
- Run **Preview** to study total vs **eligible** vs **excluded** counts and exclusion reasons.
- Use **Segmentation insights** panels for analytical summaries.
- Brief Campaign Managers on compliance attrition (eligible vs total) and exclusion codes such as
  `DO_NOT_CONTACT`, `MARKETING_OPT_OUT`, or `INVALID_CONSENT`.

BI Analysts **cannot edit segments unless allowed** (dual-role with Campaign Manager or Admin).
UI hides create/edit controls; backend also rejects unauthorized mutations.

End-user detail: [Segmentation User Guide](segmentation-user-guide.md) — BI Analyst Workflows.

## Customer And Product Context (Read-Only)

### Customers

BI Analysts can:

- Search and open customer profiles for analytical context.
- View status, type, location, and related history where the UI exposes it for read roles.

BI Analysts **cannot**:

- Create, edit, soft-delete, or import customers (**TC-009**).
- Mark consent, opt-outs, or do-not-contact flags (Customer Service / Compliance workflows).

### Products

BI Analysts can:

- View the product catalog and product performance context used in campaigns.
- Observe product-change request status for planning analysis.

BI Analysts **cannot** create, edit, or disable products or assign ownership.

## Recommended BI Analyst Practice

1. Start on **Dashboard** for portfolio health (active campaigns, overall rates, ROI).
2. Drill into **Analytics** for a campaign or product that under/over-performs.
3. Confirm rates against **sent** volume before drawing conclusions (low sent distorts rates).
4. Use **Executive** for management-safe aggregates without recipient-level detail.
5. Export **CSV/PDF** for offline briefings; keep exports only as long as needed for business use.
6. Cross-check audience quality on **Segments** preview (eligible vs excluded) before recommending
   targeting changes to Campaign Managers.
7. Treat metrics as **traceable** to launch and contact events (`campaign_metrics` / BR-034); report
   data quality issues when counters and events diverge.

## Access And Error Handling

Backend authorization is authoritative. Frontend role-based menus improve usability, but the
backend enforces `BI_ANALYST` (and other allowed roles) on every protected analytics and report call.

Permission helpers: `canViewAnalytics`, `canViewExecutiveDashboard`, `canViewReports`,
`canExportReports` in `frontend/src/features/auth/permissions.ts` (roles include `BI_ANALYST`).

Expected responses:

- Missing authentication → **401 Unauthorized**.
- Authenticated user without analytics/report role → **403 Forbidden**.
- Unknown campaign id on analytics or export → **404 Not Found** (when applicable).
- Validation or empty selection on export UI → client guidance before calling the API.

BI Analysts cannot edit customers. Unauthorized export attempts do not create completed export
history for restricted roles.

## Audit Expectations

BI Analysts primarily perform **read** and **export** operations:

- Campaign CSV/PDF generation may record **export history** rows (`report_exports`) for the
  requester.
- Customer mutations, campaign approval, and user administration remain outside BI Analyst scope.
- Metrics themselves are updated by launch and communication services (**BR-034**), not by BI
  Analyst clicks on dashboards.

## Related Documentation

- [`Analytics Module Documentation`](../modules/analytics-module.md) — APIs and authorization (item 459)
- [`KPI Definition Document`](../modules/kpi-definitions.md) — formulas and aggregation rules (item 461)
- [`Report Export Documentation`](../modules/report-export.md) — CSV/PDF and history (item 460)
- [`Segmentation User Guide`](segmentation-user-guide.md) — BI Analyst segment insight workflows
- [`Role-Based Access Documentation`](../architecture/role-based-access.md) — role matrix
- [`Executive Viewer User Guide`](executive-viewer-guide.md) — leadership aggregate-only workflows (item 463)

## KB Traceability

This guide preserves the KB BI Analyst expectations:

- **Role description:** views dashboards, reports, customer analytics, segmentation insights, and
  performance data.
- **Allowed functions:** view analytics, reports, segmentation insights, audience counts, campaign
  performance, product performance; may use analytical segment drafts only if dual-role policy
  allows (MVP: create/edit segment requires Campaign Manager or Admin).
- **Screens:** Login, Dashboard, Customers (read), Products (read), Segmentation (insight/preview),
  Analytics, Executive Dashboard, Reports.
- **FR-100–FR-108:** dashboard KPIs and performance charts.
- **FR-109–FR-110:** CSV and PDF report export.
- **BR-034:** metrics update after contact events (source data for analysis).
- **COMP-010:** executive aggregates prefer aggregated data.
- **TC-009:** BI Analyst cannot edit customers.
- Item **200:** BI Analyst cannot edit segments unless allowed. Item **200** is the segment
  ownership and role boundary reference for this guide.
- Item **458:** unauthorized user cannot export restricted reports. Item **458** is the restricted
  report export authorization reference.
- Item **462:** this BI Analyst user guide section. Item **462** is the BI Analyst guide
  acceptance evidence.
