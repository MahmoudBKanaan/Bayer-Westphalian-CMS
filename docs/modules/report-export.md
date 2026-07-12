# Report Export Documentation

The report export module generates downloadable campaign performance reports (CSV and PDF),
persists export request history, and enforces role-based access to restricted report endpoints (KB
Reports epic **E20**, **FR-109**, **FR-110**).

Campaign report content is built from the same analytics KPIs exposed by the analytics module
(`CampaignAnalyticsView` / `CampaignMetricsView`). Exports do not invent separate KPI formulas;
they serialize existing aggregated campaign metrics.

## Package Boundary

Primary backend package:

```text
com.bayerwestphalian.campaign.report
```

| Component | Responsibility |
| --- | --- |
| `ReportController` | REST API under `/api/reports` (CSV/PDF downloads, export history) |
| `ReportService` | Orchestrates generation, history persistence, and authorization |
| `CampaignReportDocument` | Builds CSV and minimal PDF 1.4 document bytes |
| `ReportExport` | JPA entity mapped to `report_exports` |
| `ReportExportRepository` | History queries (by requester, status, newest first) |
| `ReportExportType` | `CSV` / `PDF` |
| `ReportExportStatus` | `REQUESTED` / `COMPLETED` / `FAILED` |
| `ReportExportView` | API view of a history row |
| `ReportFile` | Filename, content type, bytes, and history view for HTTP attachments |

Depends on:

- `AnalyticsService.getCampaignAnalytics` for KPI payload
- `CampaignRepository` to validate campaign existence
- `UserRepository` to attach optional requester on history rows
- `AuditService` for service-level audit history CSV export (`exportAuditReport`; not exposed as a
  public campaign report download route under `ReportController`)

Frontend surfaces (items 442, 445):

- `ReportsPage` — campaign picker, CSV/PDF download, export history
- `ReportDownloadPanel` / `CampaignReportDownloadActions` — reusable download UI
- API client: `frontend/src/api/reports.ts`
- Download helpers: `frontend/src/features/reports/reportDownload.ts`

## KB Traceability

| KB / FR / rule | Report capability |
| --- | --- |
| Epic **E20** | Reports (CSV/PDF exports and dashboard reports) |
| **FR-109** | Users can export CSV reports |
| **FR-110** | Users can generate PDF reports |
| **COMP-010** | Executive-oriented content prefers aggregates (analytics source data) |
| Item **466** | Reports use aggregated data; metrics traceable to recipients and contact events |
| Item **435** | `ReportExport` entity / `report_exports` table |
| Item **436** | `ReportService` export orchestration |
| Item **437** / **455** | Campaign CSV export works |
| Item **438** / **456** | Campaign PDF export works |
| Item **439** | Store and list report export history |
| Item **458** | Unauthorized user cannot export restricted reports |
| Sprint 16 **663** | Report export is restricted to authorized roles — `ReportExportIsRestrictedToAuthorizedRolesTests` |
| Item **531** | Log report exports (`EXPORT_REPORT` on `report_exports`) |

## REST API Surface

Base path: `/api/reports`

| Method | Path | Description | Content / response |
| --- | --- | --- | --- |
| `GET` | `/api/reports/campaigns/{campaignId}/csv` | Campaign performance CSV (FR-109) | `text/csv` attachment |
| `GET` | `/api/reports/campaigns/{campaignId}/pdf` | Campaign performance PDF (FR-110) | `application/pdf` attachment |
| `GET` | `/api/reports/exports` | Export history (newest first) | `ApiResponse` list of `ReportExportView` |
| `GET` | `/api/reports/exports/{exportId}` | Single history row | `ApiResponse` of `ReportExportView` |

### Export history query parameters

`GET /api/reports/exports`:

| Parameter | Default | Behavior |
| --- | --- | --- |
| `mine` | `false` | When `true`, limit to the authenticated requester’s exports |
| `status` | (none) | Filter by `REQUESTED`, `COMPLETED`, or `FAILED` (ignored when `mine=true`) |

### HTTP download response

Campaign CSV/PDF endpoints return raw file bytes (not `ApiResponse` JSON):

- `Content-Type`: `text/csv; charset=UTF-8` or `application/pdf`
- `Content-Disposition`: `attachment` with sanitized campaign-based filename (e.g.
  `Spring-Life-Drive.csv`)
- `Content-Length`: byte length of the body

Unknown campaign or export ids yield **404**. Unauthenticated callers yield **401**. Wrong roles
yield **403** (item 458 / Sprint 16 critical item **663** —
`ReportExportIsRestrictedToAuthorizedRolesTests`; companion
`UnauthorizedUserCannotExportRestrictedReportsTests`). Frontend catalog:
`frontend/src/features/reports/reportExportIsRestrictedToAuthorizedRoles.ts` (aligned with
`REPORT_READ_ROLES` / `canViewReports`).

## Campaign CSV Content (FR-109)

`CampaignReportDocument.campaignCsv` produces UTF-8 CSV with:

1. Header row of identity and KPI columns
2. One data row for the requested campaign

Columns include:

- Identity: `campaignId`, `campaignName`, `objective`, `status`, `channel`, `startDate`, `endDate`,
  `ownerUserId`, `ownerFullName`
- Funnel / engagement: `audienceSize`, `eligibleCount`, `excludedCount`, `sentCount`,
  `openedCount`, `clickedCount`, `repliedCount`, `convertedCount`
- Rates: `openRate`, `clickRate`, `conversionRate`
- Financials: `estimatedCost`, `estimatedRevenue`, `estimatedRoi`
- `generatedAt`

When metrics are missing (draft / not launched), identity columns are still exported and KPI cells
are empty. Commas and quotes in text fields are CSV-escaped.

Service method: `ReportService.exportCampaignCsv` / alias `campaignCsv`.

## Campaign PDF Content (FR-110)

`CampaignReportDocument.campaignPdf` builds a minimal PDF 1.4 document (no external PDF library)
with the same narrative KPIs as CSV:

- Title: Bayer-Westphalian Campaign Report
- Campaign identity lines (name, id, objective, status, channel, schedule, owner)
- Audience, engagement, rates, cost/revenue/ROI when metrics exist
- Placeholder line `Metrics: (none recorded yet)` when metrics are null
- Generated-at timestamp

Service method: `ReportService.generateCampaignPdf` / alias `campaignPdf`.

Acceptance suite: `PdfExportWorksTests` (item **456**) — PDF 1.4 document content, service
completion with `application/pdf`, export history REQUESTED → COMPLETED, missing-campaign errors.
Companion HTTP coverage: `CampaignPdfReportEndpointTests` (item 438).

## Export History Lifecycle (Item 439)

Table: `report_exports` (`ReportExport` entity).

```text
REQUESTED ──► COMPLETED  (fileUrl set, completedAt set)
     │
     └──► FAILED     (on generation error; fileUrl cleared)
```

1. Validate campaign exists (`CampaignRepository.findById`).
2. Persist history row with status **REQUESTED** and report name
   (`Campaign CSV: {name}` or `Campaign PDF: {name}`).
3. Load `CampaignAnalyticsView` via `AnalyticsService`.
4. Generate document bytes and filename.
5. Mark history **COMPLETED** with `fileUrl` like
   `local://reports/{exportId}/{filename}`.
6. Return `ReportFile` (bytes + `ReportExportView`).

On failure after REQUESTED, status becomes **FAILED** for auditability; the exception is rethrown.

History list/detail endpoints expose only metadata (`ReportExportView`), not the binary file body
again. Re-download uses the campaign CSV/PDF endpoints.

## Sensitive Action Audit (Item 531)

In addition to `report_exports` history rows, every **successful** campaign CSV/PDF or audit-history
export writes an immutable `audit_logs` entry:

| Field | Value |
| --- | --- |
| `action` | `EXPORT_REPORT` |
| `entityType` | `report_exports` |
| `entityId` | export history UUID |
| `actorUserId` | requester when known |
| `newValue` | `id`, `reportName`, `exportType`, `status=COMPLETED`, optional `campaignId`, `fileUrl`, `requestedByUserId` |

Failed generation (FAILED history) does not emit `EXPORT_REPORT`. Validation / missing-campaign
errors before REQUESTED write neither history success audit nor `EXPORT_REPORT`.

## Authorization (Item 458)

Campaign report export and export history under `/api/reports/**` are restricted by path security
and method-level `@PreAuthorize`.

**Allowed roles** (matches `SecurityConfiguration` and `canViewReports()`):

- `ADMIN`
- `BI_ANALYST`
- `CAMPAIGN_MANAGER`
- `MARKETING_ANALYST`
- `EXECUTIVE_VIEWER`

**Not allowed** (examples): `PRODUCT_MANAGER`, `COMPLIANCE_OFFICER`, `CUSTOMER_SERVICE_AGENT`,
`SALES_AGENT`, `SYSTEM_AUDITOR`.

| Situation | HTTP result | Service export invoked? |
| --- | --- | --- |
| No token | 401 Unauthorized | No |
| Authenticated wrong role | 403 Forbidden | No |
| Allowed role, missing campaign | 404 Not Found | After auth, validation fails |

Audit history export (`ReportService.exportAuditReport` / `auditReport`) uses a **different** role
set (`ADMIN`, `COMPLIANCE_OFFICER`, `SYSTEM_AUDITOR`) and is not the campaign FR-109/110 download
surface on `ReportController`.

Frontend: `canViewReports` / `canExportReports` in `frontend/src/features/auth/permissions.ts`.
Route: `/reports` (`ReportsPage`).

## Frontend Screens

| Screen / component | Purpose |
| --- | --- |
| `ReportsPage` | Select campaign, download CSV/PDF, browse export history |
| `ReportDownloadPanel` | Shared download actions and status messaging |
| `CampaignReportDownloadActions` | Per-campaign CSV/PDF actions on campaigns list |
| `api/reports.ts` | `exportCampaignCsv`, `exportCampaignPdf`, list/get exports |
| `apiDownload` helpers in `client.ts` | Browser file download from attachment responses |

## Traceability to Analytics and Contact Events

```text
campaign_metrics (+ CampaignAnalyticsView)
        │
        ▼ ReportService
CampaignReportDocument (CSV / PDF bytes)
        │
        ▼ ReportExport history (REQUESTED → COMPLETED/FAILED)
        │
        ▼ ReportController attachment / history API
Frontend ReportsPage download
```

Metrics remain traceable to launch and contact-event updates (see [Analytics Module
Documentation](analytics-module.md) and [Communication Tracking](communication-tracking.md)).

### Item 466 — aggregated reports and metric traceability

| Rule | Implementation |
| --- | --- |
| Reports use **aggregated data** where possible (COMP-010) | CSV/PDF columns are campaign-level KPI aggregates from `CampaignMetricsView`; executive dashboard uses platform sums. Exports do **not** dump recipient lists or contact-event streams. |
| Metrics are **traceable** to campaign recipients | `eligible_count` / `excluded_count` / launch `sent_count` originate from `campaign_recipients` tallies at launch (`recordLaunchCounts`). |
| Metrics are **traceable** to contact events (BR-034) | `opened` / `clicked` / `replied` / `converted` (and incremental `sent`) update from recorded contact events via `CommunicationService`. |

Chain:

```text
campaign_recipients (ELIGIBLE/EXCLUDED) + contact_events (SENT/OPENED/…)
        → campaign_metrics
        → AnalyticsService (CampaignAnalyticsView / ExecutiveDashboardView)
        → ReportService / CampaignReportDocument (CSV/PDF aggregates)
```

Acceptance suite: `ReportsUseAggregatedDataAndMetricsAreTraceableTests` (item **466**).

## Related Documentation

- [Analytics Module Documentation](analytics-module.md) — KPI source for campaign exports
- [Campaign Launch Documentation](campaign-launch.md) — sent counts and launch metrics
- [Communication Tracking Module](communication-tracking.md) — contact events (BR-034)
- [Role-Based Access Documentation](../architecture/role-based-access.md) — report role matrix
- [KPI Definition Document](kpi-definitions.md) — formal KPI catalog (item 461)
- [BI Analyst User Guide](../user-guides/bi-analyst-guide.md) — export workflows for analysts (item 462)
- [Executive Viewer User Guide](../user-guides/executive-viewer-guide.md) — management export workflows (item 463)

## Implementation Evidence

| Area | Location |
| --- | --- |
| Package | `backend/.../report/` |
| Entity | `ReportExport.java` / table `report_exports` |
| Document builder | `CampaignReportDocument.java` |
| Service | `ReportService.java` |
| Controller | `ReportController.java` |
| Acceptance / endpoint tests | `CsvExportWorksTests` (item 455), `PdfExportWorksTests` (item 456), **`ReportExportIsRestrictedToAuthorizedRolesTests`** (item **663**), `UnauthorizedUserCannotExportRestrictedReportsTests` (item 458), `ReportsUseAggregatedDataAndMetricsAreTraceableTests` (item 466), `CampaignCsvReportEndpointTests`, `CampaignPdfReportEndpointTests`, `ReportExportHistoryEndpointTests` |
| Frontend API | `frontend/src/api/reports.ts` |
| Frontend page | `ReportsPage` |
| Module doc tests | `ReportExportDocumentationTests` (item 460) |
