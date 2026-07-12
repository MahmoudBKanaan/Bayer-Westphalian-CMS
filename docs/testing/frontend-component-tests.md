# Frontend Component Tests

Shared UI component coverage for the Bayer-Westphalian Campaign Management Platform
(KB **NFR-010**, Sprint 15 item **595**).

Component tests live next to sources under `frontend/src/components/**` and use **Vitest** +
**React Testing Library**. They focus on accessible rendering, empty/loading/error states, and
domain badge labels — not full page workflows (those are page/integration/E2E tests).

## Scope (item 595)

| Area | Components | Primary test modules |
| --- | --- | --- |
| Shell / navigation | `AppLayout` | `AppLayout.test.tsx` |
| Feedback states | `EmptyState`, `ErrorState`, `SuccessNotification`, `FormValidationMessage` | matching `*.test.tsx` |
| Confirmations | `ConfirmationDialog` | `ConfirmationDialog.test.tsx` |
| Domain badges | `CampaignStatusBadge`, `CustomerStatusBadge`, `ConsentStatusBadge`, `ReminderLevelBadge`, `AuditActionBadge`, `StatusBadge` | matching `*.test.tsx` |
| Metrics / AI | `MetricCard`, `AiExplanationDisplay`, `AiRecommendationSections` | matching `*.test.tsx` |
| Segmentation UI | `SegmentCriteriaBuilder`, `SegmentPreviewResults`, `SegmentInsightPanel`, `ExclusionReasonSummaryPanel`, `ProductSearchFilters` | matching `*.test.tsx` |
| Reports | `ReportDownloadPanel` | `ReportDownloadPanel.test.tsx` |
| Charts (item 444) | `ChartFrame`, bar/pie/line charts, `chartTheme` | `charts/charts.test.tsx`, `charts/chartTheme.test.ts` |
| Inventory smoke suite | badges, feedback, metrics, charts | `frontendComponentInventory.test.tsx` |

## Running tests

```bash
cd frontend
npm test -- --run src/components
```

Do **not** run the suite as part of backlog implementation items that say “do not run any tests”
unless the backlog item explicitly requests execution (e.g. item 635).

## Conventions

1. Co-locate `ComponentName.test.tsx` with `ComponentName.tsx`.
2. Prefer accessible queries (`getByRole`, `getByLabelText`) over CSS class sniffing when possible.
3. Cover loading, empty, and error surfaces for data-bound widgets (charts, panels).
4. Domain badges should cover all enum values or documented status sets.
5. Keep page-level workflows in `src/pages/*.test.tsx`; keep reusable UI here.

## Related backlog

| Item | Topic |
| --- | --- |
| **595** | Add frontend component tests (this document) |
| **596** | Frontend integration tests — see [frontend-integration-tests.md](frontend-integration-tests.md) |
| **597** | Playwright happy-path E2E — see [playwright-e2e.md](playwright-e2e.md) |
| **612** | Frontend testing notes — see [frontend-testing-notes.md](frontend-testing-notes.md) |
| **635** | Run frontend component tests |
