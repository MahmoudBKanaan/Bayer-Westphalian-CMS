# KPI Definition Document

This document is the formal catalog of **campaign performance KPIs** for the Bayer-Westphalian
Campaign Management Platform (KB epic **E19**, dashboard **FR-100–FR-108**, reports **FR-109–FR-110**,
**BR-034**, **COMP-010**).

It defines formulas, units, aggregation rules, storage, and traceability so analytics dashboards,
executive aggregates, CSV/PDF exports, and acceptance tests share one definition of each metric.

Primary implementation:

```text
com.bayerwestphalian.campaign.campaign.CampaignMetrics
com.bayerwestphalian.campaign.analytics.AnalyticsCalculations
com.bayerwestphalian.campaign.analytics.AnalyticsRates
com.bayerwestphalian.campaign.analytics.AnalyticsService
```

Module context: [Analytics Module Documentation](analytics-module.md).  
Report consumers: [Report Export Documentation](report-export.md).

## Purpose

| Audience | Use of this catalog |
| --- | --- |
| BI Analyst / Marketing Analyst | Interpret open, click, conversion, and ROI correctly |
| Executive Viewer | Understand that platform rates are aggregate totals, not averages of campaign rates |
| Developers / testers | Align implementations and acceptance suites (items 417–430, 446–454, 457) |
| Report consumers | CSV/PDF columns map 1:1 to the same KPI formulas |

Exports (**FR-109**, **FR-110**) **must not invent separate KPI formulas**; they serialize values
from `CampaignAnalyticsView` / `CampaignMetricsView` produced by analytics.

## KB Traceability

| KB / FR / rule | KPI surface |
| --- | --- |
| Epic **E19** | Analytics (dashboards, KPIs, product performance) |
| Epic **E20** | Reports that export the same KPIs |
| **FR-100** | Dashboard campaign totals (inventory counts) |
| **FR-101** | Active campaign count |
| **FR-102** | Audience size |
| **FR-103** | Messages sent |
| **FR-104** | Open rate |
| **FR-105** | Click rate |
| **FR-106** | Conversion rate |
| **FR-107** | Estimated ROI |
| **FR-108** | Performance charts (same underlying KPIs) |
| **FR-109** / **FR-110** | CSV / PDF export of campaign KPIs |
| **BR-034** | Campaign metrics update after contact events (Sprint 16 item **656**: `ContactEventsUpdateAnalyticsTests`) |
| **COMP-010** | Executive reports use aggregated data where possible |
| Item **461** | This KPI definition document |

## Precision and Rounding

| Kind | Scale | Rounding | Java constants |
| --- | --- | --- | --- |
| Engagement rates (open / click / conversion) | **Scale 4** | `HALF_UP` | `CampaignMetrics.RATE_SCALE = 4` |
| Money (cost, revenue) and **estimated ROI** | **Scale 2** | `HALF_UP` | `CampaignMetrics.MONEY_SCALE = 2` |
| Integer counters | Whole numbers | Non-negative | `int` / `long` on metrics and totals |

Rates are **ratios** (not percentages). UI may format them as percentages for display; storage and
API decimal values remain fraction form at scale 4 (for example `0.2500` = 25% open rate).

## Aggregation Rules (Mandatory)

These rules apply to **dashboard**, **executive**, and **product performance** multi-campaign views:

1. **Sum counters first** (audience, eligible, excluded, sent, opened, clicked, replied, converted,
   cost, revenue) across the relevant campaign metrics rows.
2. **Compute rates from aggregate numerators and aggregate denominators**  
   (`Σ numerator ÷ Σ sent`), **not** as the arithmetic mean of per-campaign rates.
3. **Compute platform ROI from total estimated cost and total estimated revenue**, **not** as the
   average of per-campaign ROI values.
4. Prefer **derived** audience size (`eligible + excluded`) over a possibly stale stored
   `audience_size` column when recalculating.

Acceptance suites encode these rules (items **446–454**, **457**).

## Source of Truth and Traceability

All counters live on table **`campaign_metrics`** (entity `CampaignMetrics`), one row per campaign.

```text
campaign_recipients (ELIGIBLE / EXCLUDED)
        │  launch
        ▼
contact_events (SENT, OPENED, CLICKED, REPLIED, conversion outcomes)
        │  BR-034
        ▼
campaign_metrics  (stored counters + optional cost/revenue/ROI)
        │
        ├── AnalyticsService → Dashboard / Campaign / Product / Executive views
        └── ReportService → CSV / PDF (same KPI values)
```

| Data origin | What updates | When |
| --- | --- | --- |
| Recipient generation / launch | eligible, excluded, audience, sent | `CampaignService.launchCampaign` |
| Contact events (**BR-034**) | opened, clicked, replied, converted, optional sent | `CommunicationService` increments |
| Financial estimates | cost, revenue, stored ROI | metrics financial update / recalculate |

Item **466** (traceability): reports and dashboards should remain reconcilable to
`campaign_recipients` and `contact_events` through `campaign_metrics`. Formalized by
`ReportsUseAggregatedDataAndMetricsAreTraceableTests`.

## Inventory KPIs (Dashboard)

Not stored on `campaign_metrics`; derived from the `campaigns` table.

| KPI | Definition | FR / notes |
| --- | --- | --- |
| Total campaigns | Count of campaign rows in scope | **FR-100** |
| Active campaigns | Count where status is `ACTIVE` | **FR-101** |
| Completed campaigns (executive) | Count where status is `COMPLETED` | Executive inventory only |

## Count KPIs

| KPI | Formula / definition | Storage column | Item / FR |
| --- | --- | --- | --- |
| **Eligible count** | Recipients with status `ELIGIBLE` | `eligible_count` | 418 / **447** |
| **Excluded count** | Recipients with status `EXCLUDED` | `excluded_count` | 419 / **448** |
| **Audience size** | `eligible_count + excluded_count` | `audience_size` (also derived) | 417 / **446** / **FR-102** |
| **Sent count** | Messages sent (`SENT` contact events at launch) | `sent_count` | 420 / **449** / **FR-103** |
| **Opened count** | `OPENED` contact events | `opened_count` | 421 / **450** |
| **Clicked count** | `CLICKED` contact events | `clicked_count` | 422 / **450** |
| **Replied count** | `REPLIED` contact events | `replied_count` | 423 / **450** |
| **Converted count** | Conversion outcomes on contact events | `converted_count` | 424 / **450** |

### Edge cases (counts)

- All counts are **non-negative**.
- Dashboard / executive **totals** = sum of per-campaign counts.
- Audience size used in analytics prefers **eligible + excluded** so aggregates stay consistent if
  `audience_size` was not refreshed.

## Rate KPIs

Denominator for engagement rates is **messages sent** (`sent_count` / aggregate sent).

| KPI | Formula | When sent = 0 | Scale | Item / FR |
| --- | --- | --- | --- | --- |
| **Open rate** | `opened_count / sent_count` | `0` (scale 4) | 4 | 425 / **451** / **FR-104** |
| **Click rate** | `clicked_count / sent_count` | `0` (scale 4) | 4 | 426 / **452** / **FR-105** |
| **Conversion rate** | `converted_count / sent_count` | `0` (scale 4) | 4 | 427 / **453** / **FR-106** |

Helpers: `CampaignMetrics.calculateOpenRate|ClickRate|ConversionRate`,
`AnalyticsCalculations.calculate*Rate`, `AnalyticsRates.openRate|clickRate|conversionRate`.

### Multi-campaign rates (not averages)

| View | Rate computation |
| --- | --- |
| Single campaign | Per-campaign opened|clicked|converted ÷ per-campaign sent |
| Dashboard | Σ opened|clicked|converted ÷ Σ sent |
| Executive | Same aggregate rule (**COMP-010** / item **457**) |
| Product performance | Sum metrics of campaigns linked to the product, then ÷ product-level sent |

**Incorrect:** average of each campaign’s open rate.  
**Correct:** total opens across campaigns divided by total sends across campaigns.

## Financial KPIs

| KPI | Formula / definition | Null / zero rules | Scale | Item / FR |
| --- | --- | --- | --- | --- |
| **Estimated cost** | Optional non-negative monetary estimate | `null` if not set | 2 | 428 |
| **Estimated revenue** | Optional non-negative monetary estimate | `null` if not set | 2 | 429 |
| **Estimated ROI** | `(revenue − cost) / cost` when cost > 0 | **null** if cost missing; **0** if cost is zero | 2 | 430 / **454** / **FR-107** |

Helpers: `CampaignMetrics.calculateEstimatedCost|Revenue|Roi`,
`AnalyticsCalculations.calculateEstimated*` / `totalEstimated*` / `totalEstimatedRoi`,
`AnalyticsRates.roi`.

### Multi-campaign financials

1. Sum non-null per-campaign costs → total cost (null only if no campaign has cost).
2. Sum non-null per-campaign revenues → total revenue.
3. ROI = `(total revenue − total cost) / total cost` with the same null/zero rules.

**Incorrect:** mean of per-campaign ROI values.  
**Correct:** ROI of the summed cost and revenue.

## Product Performance KPIs

`GET /api/analytics/products/performance` groups campaign–product links by product, sums the metrics
of linked campaigns, and applies the same rate and ROI definitions at product grain.

Typical product row fields (aligned with `ProductPerformanceView`): product identity, linked
campaign participation, summed audience/sent/engagement, open/click/conversion rates, estimated
cost/revenue/ROI from product-level totals.

## Surfaces That Display KPIs

| Surface | API / export | KPI scope |
| --- | --- | --- |
| Platform dashboard | `GET /api/analytics/dashboard` | Inventory + platform totals + rates + ROI + recent campaign rows |
| Campaign analytics | `GET /api/analytics/campaigns/{id}` | Single campaign identity + optional metrics |
| Product performance | `GET /api/analytics/products/performance` | Per-product aggregates |
| Executive dashboard | `GET /api/analytics/executive` | Platform aggregates only (**COMP-010**); no raw contact-event lists |
| Campaign CSV | `GET /api/reports/campaigns/{id}/csv` | Same campaign KPIs (**FR-109**) |
| Campaign PDF | `GET /api/reports/campaigns/{id}/pdf` | Same narrative KPIs (**FR-110**) |
| Charts | Frontend Recharts (**FR-108**) | Same underlying dashboard / analytics numbers |

## Worked Examples

### Example 1 — Open rate (single campaign)

- Sent = 200, opened = 50  
- Open rate = `50 / 200` = **0.2500** (scale 4)

### Example 2 — Aggregate open rate (two campaigns)

| Campaign | Sent | Opened | Per-campaign open rate |
| --- | --- | --- | --- |
| A | 100 | 50 | 0.5000 |
| B | 100 | 10 | 0.1000 |

- **Wrong** average of rates: `(0.5 + 0.1) / 2` = 0.3000  
- **Correct** platform open rate: `(50 + 10) / (100 + 100)` = **0.3000** in this case coincides numerically,
  but change B to sent 300 / opened 30:
  - Wrong average: `(0.5 + 0.1) / 2` = 0.3000  
  - Correct: `(50 + 30) / (100 + 300)` = **0.2000**

### Example 3 — Estimated ROI

- Cost = 100.00, revenue = 150.00  
- ROI = `(150 − 100) / 100` = **0.50** (scale 2)  
- Cost missing → ROI **null**  
- Cost = 0.00 → ROI **0.00**

### Example 4 — Audience size

- Eligible = 80, excluded = 20  
- Audience size = **100** (not a separate independent formula)

## Acceptance Mapping

| Item | Acceptance statement | Primary helper / suite |
| --- | --- | --- |
| 446 | Audience size is calculated correctly | `AudienceSizeIsCalculatedCorrectlyTests` |
| 447 | Eligible count is calculated correctly | `EligibleCountIsCalculatedCorrectlyTests` |
| 448 | Excluded count is calculated correctly | `ExcludedCountIsCalculatedCorrectlyTests` |
| 449 | Sent count updates after launch | `SentCountUpdatesAfterLaunchTests` |
| 450 | Open/click/reply/conversion update from contact events | `EngagementCountsUpdateFromContactEventsTests` |
| 451 | Open rate is calculated correctly | `OpenRateIsCalculatedCorrectlyTests` |
| 452 | Click rate is calculated correctly | `ClickRateIsCalculatedCorrectlyTests` |
| 453 | Conversion rate is calculated correctly | `ConversionRateIsCalculatedCorrectlyTests` |
| 454 | ROI is calculated correctly | `RoiIsCalculatedCorrectlyTests` |
| 457 | Executive report uses aggregated data | `ExecutiveReportUsesAggregatedDataTests` |
| 466 | Reports use aggregates; metrics traceable to recipients and contact events | `ReportsUseAggregatedDataAndMetricsAreTraceableTests` |

## Related Documentation

- [Analytics Module Documentation](analytics-module.md) — APIs, authorization, screens (item 459)
- [Report Export Documentation](report-export.md) — CSV/PDF using these KPIs (item 460)
- [Communication Tracking Module](communication-tracking.md) — contact events and BR-034
- [Campaign Launch Documentation](campaign-launch.md) — launch metrics and sent counts
- [Role-Based Access Documentation](../architecture/role-based-access.md) — who can view analytics/reports
- [BI Analyst User Guide](../user-guides/bi-analyst-guide.md) — how analysts use these KPIs (item 462)
- [Executive Viewer User Guide](../user-guides/executive-viewer-guide.md) — executive aggregate consumption (item 463)

## Implementation Evidence

| Area | Location |
| --- | --- |
| Metrics entity | `backend/.../campaign/CampaignMetrics.java` |
| Calculation helpers | `backend/.../analytics/AnalyticsCalculations.java` |
| Rate helpers | `backend/.../analytics/AnalyticsRates.java` |
| Service aggregation | `backend/.../analytics/AnalyticsService.java` |
| Contact-event → metrics | `backend/.../communication/CommunicationService.java` |
| Report serialization | `backend/.../report/CampaignReportDocument.java` |
| This document | `docs/modules/kpi-definitions.md` (item **461**) |
| Documentation tests | `KpiDefinitionDocumentationTests` |
