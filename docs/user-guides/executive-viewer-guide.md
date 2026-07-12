# Executive Viewer User Guide

This guide describes the MVP workflows available to employees with the `EXECUTIVE_VIEWER` role.

## Scope

Executive Viewers consume **high-level, read-only** business intelligence: platform KPIs, campaign
summaries, product performance outcomes, estimated ROI, and management-oriented CSV/PDF reports.
They support executive decision-making without operating CRM mutations, campaign lifecycle actions,
or compliance workflows.

KB role intent: **views high-level dashboards and management reports only**. Allowed functions:
view read-only dashboards, ROI, campaign summaries, and product performance reports.

Executive Viewers **cannot** manage customers, segments, products, campaigns, users, consent,
follow-ups, reminders, or audit administration. They **cannot** approve campaigns or launch
marketing. Backend authorization is authoritative; the frontend menu only reflects typical access.

## What Executive Viewers Can Do

| Capability | Typical screen / API |
| --- | --- |
| View platform KPIs (totals, active campaigns, audience, sent, rates, ROI) | Dashboard `/dashboard` — `GET /api/analytics/dashboard` |
| Review engagement charts and performance comparisons | Analytics `/analytics` |
| View **aggregated** management KPIs without raw contact detail | Executive `/executive` — `GET /api/analytics/executive` |
| Export management campaign **CSV** and **PDF** reports; review export history | Reports `/reports` — `/api/reports/**` |
| Read campaign-linked contact history where the UI exposes it for this role | Contact history |

Frontend menu access for `EXECUTIVE_VIEWER` typically includes: **Dashboard**, **Contact history**,
**Analytics**, **Executive**, and **Reports**. Navigation labels may vary slightly by release, but
these are the primary executive surfaces.

## What Executive Viewers Cannot Do

- Create, edit, soft-delete, or import **customers**.
- Create, edit, delete, or preview-manage **segments** as an owner.
- Create, submit, approve, reject, launch, pause, or archive **campaigns**.
- Create or change **products**, ownership, payment records, or product-change requests.
- Manage **users**, roles, or passwords.
- Record **consent**, opt-outs, or do-not-contact flags.
- Operate **follow-up** task assignment/completion or **reminder** generation as an owner role.
- Access **Audit** administration reserved for Admin / Compliance Officer / System Auditor.
- Bypass compliance or consent rules — viewers only consume already aggregated metrics and exports.

## Primary Screen: Executive Dashboard

The **Executive** screen (`/executive`) is the default management briefing surface for this role
(**COMP-010** / acceptance item **457**).

Use it to review:

| Area | What you see |
| --- | --- |
| Campaign inventory | Total, **active**, and **completed** campaign counts |
| Audience funnel | Total audience, eligible, excluded, messages sent |
| Engagement totals | Opened, clicked, replied, converted |
| Overall rates | Open, click, and conversion rates from **platform totals** |
| Financial summary | Estimated cost, estimated revenue, **estimated ROI** |
| Product outcomes | Embedded product performance summary rows |

### Aggregation rules (read this)

Executive payloads are **aggregates**:

- Counters are **sums** across campaigns (or product-linked campaigns).
- Rates are **Σ numerator ÷ Σ sent**, **not** averages of per-campaign rates.
- ROI uses **total cost and total revenue**, **not** the mean of per-campaign ROI values.
- The executive payload does **not** list raw contact events, recipient PII grids, or
  customer-level marketing history as the primary content (**COMP-010**).

API: `GET /api/analytics/executive`.

KPI formulas: [`KPI Definition Document`](../modules/kpi-definitions.md).  
Technical detail: [`Analytics Module Documentation`](../modules/analytics-module.md).

## Dashboard Workflow

Executive Viewers can use the general **Dashboard** (`/dashboard`) to:

- See **campaign totals** and **active campaigns** (FR-100, FR-101).
- Review **audience size**, **messages sent**, open/click/conversion rates, and **estimated ROI**
  (FR-102–FR-107).
- Navigate quickly to **Executive**, **Analytics**, and **Reports**.

API: `GET /api/analytics/dashboard`.

Prefer **Executive** when the audience is leadership and only aggregated outcomes should be shared.

## Analytics Workflows

On **Analytics** (`/analytics`), Executive Viewers can:

- Review charts and comparisons for engagement, conversion, and ROI (FR-108 / Recharts).
- Inspect a single campaign’s analytics summary when a campaign id is available
  (`GET /api/analytics/campaigns/{campaignId}`).
- Review **product performance** rows (`GET /api/analytics/products/performance`).

### Interpreting rates and ROI

- API rates are **fractions of messages sent** (scale 4), not stored percentages.
- When **sent = 0**, open/click/conversion rates are **0**.
- **Estimated ROI** = `(revenue − cost) / cost` when cost > 0; **null** if cost is missing; **0**
  if cost is zero (scale 2).
- Low send volume can make rates look extreme — always pair rates with **messages sent**.

## Reports Workflows

Executive Viewers can use **Reports** (`/reports`) for read-only **management exports**:

- Download campaign performance **CSV** (FR-109).
- Download campaign performance **PDF** (FR-110).
- Review **export history** (newest first), optionally limited to own exports.

APIs:

- `GET /api/reports/campaigns/{campaignId}/csv`
- `GET /api/reports/campaigns/{campaignId}/pdf`
- `GET /api/reports/exports` (optional `mine`, `status`)
- `GET /api/reports/exports/{exportId}`

Exports use the **same KPI definitions** as analytics. Unauthenticated requests receive
**401 Unauthorized**. Roles outside the report matrix receive **403 Forbidden** without generating
files (item **458**). `EXECUTIVE_VIEWER` is an allowed export role.

Details: [`Report Export Documentation`](../modules/report-export.md).

## Campaign And Product Summaries

Without operating campaign builder or product admin screens, Executive Viewers still see outcomes
via analytics:

| Summary need | Where to look |
| --- | --- |
| Campaign status and performance overview | Dashboard, Analytics campaign detail, Executive inventory |
| Product-level outcomes | Analytics product performance / Executive product rows |
| ROI and engagement impact | Executive financial + rate sections; Reports PDF/CSV |

Campaign lifecycle actions (create, submit, approve, launch) remain Campaign Manager / Compliance /
Admin workflows. Executive Viewers **cannot approve or launch campaigns**.

## Recommended Executive Viewer Practice

1. Open **Executive** first for a leadership-safe portfolio snapshot.
2. Check **active vs completed** campaign mix before interpreting engagement.
3. Read rates only with **sent** volume and audience funnel context.
4. Use **product performance** aggregates when comparing offers, not individual customer records.
5. Export **PDF** for board packs and **CSV** for offline tables; retain exports only as long as
   needed for business reporting.
6. Escalate operational issues (consent gaps, low eligibility, failed sends) to Campaign Managers or
   Compliance — do not attempt CRM fixes under this role.
7. Remember metrics are fed by launch and contact events (**BR-034**); stale or missing metrics
   usually mean a campaign was not launched or events were not recorded.

## Access And Error Handling

Backend authorization is authoritative. Frontend menus for `EXECUTIVE_VIEWER` hide operational
areas, but every protected API still checks roles server-side.

Permission helpers used by the SPA:

- `canViewAnalytics`
- `canViewExecutiveDashboard`
- `canViewReports` / `canExportReports`

Defined in `frontend/src/features/auth/permissions.ts` (matrix includes `EXECUTIVE_VIEWER`).

Expected responses:

- Missing authentication → **401 Unauthorized**.
- Authenticated user without analytics/report role → **403 Forbidden**.
- Unknown campaign id on detail or export → **404 Not Found** (when applicable).
- Empty campaign selection on Reports UI → client guidance before calling export APIs.

## Audit Expectations

Executive Viewers primarily perform **read** and **export** operations:

- Dashboard, analytics, and executive GETs are read-only.
- CSV/PDF generation may create **export history** rows (`report_exports`) for the requester.
- Customer mutations, campaign approval, user admin, and consent changes are out of scope for this
  role and remain auditable on the operators who perform them.

## Related Documentation

- [`Analytics Module Documentation`](../modules/analytics-module.md) — APIs and authorization (item 459)
- [`KPI Definition Document`](../modules/kpi-definitions.md) — formulas and aggregation rules (item 461)
- [`Report Export Documentation`](../modules/report-export.md) — CSV/PDF and history (item 460)
- [`BI Analyst User Guide`](bi-analyst-guide.md) — deeper analyst drill-down workflows (item 462)
- [`Role-Based Access Documentation`](../architecture/role-based-access.md) — role matrix

## KB Traceability

This guide preserves the KB Executive Viewer expectations:

- **Role description:** views high-level dashboards and management reports only.
- **Allowed functions:** view read-only dashboards, ROI, campaign summaries, and product
  performance reports.
- **Screens:** Login; Executive Dashboard (high-level KPIs); Reports (read-only management
  reports); campaign overview and product performance via analytics surfaces.
- **User story:** As an Executive, I want to view ROI and campaign summaries.
- **FR-100–FR-108:** dashboard KPIs and performance charts (read-only consumption).
- **FR-109–FR-110:** CSV and PDF report export for management reporting.
- **COMP-010:** reports/dashboards for executives should use **aggregated data** where possible.
- **BR-034:** underlying metrics update after contact events (source data; not mutated by this role).
- Item **443** / **457**: executive aggregate dashboard and acceptance of aggregated payload.
- Item **457**: Executive Dashboard acceptance requires aggregate management KPIs.
- Item **458**: unauthorized user cannot export restricted reports (`EXECUTIVE_VIEWER` is allowed).
- Item **463**: this Executive Viewer guide section.
