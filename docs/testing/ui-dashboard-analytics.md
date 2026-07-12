# UI Dashboard Analytics Load

Loading platform analytics on the operational Dashboard for the Bayer-Westphalian Campaign
Management Platform (KB **FR-100–FR-108**, item **440**, Sprint 15 item **606**).

## Acceptance (item 606)

**Dashboard loads analytics** when:

1. Authorized analytics roles open `/dashboard` (Admin, BI Analyst, Campaign Manager, Marketing Analyst, Executive Viewer).
2. The UI requests `GET /api/analytics/dashboard` with the session bearer token.
3. KPI groups render inventory/delivery, engagement rates, and financial outcomes.
4. Campaign performance / engagement / financial chart sections render.
5. Recent campaign metrics table shows stored campaign rows.
6. Unauthorized roles see a clear denial message and do not call the analytics endpoint.

## Journey

```text
Open Dashboard → Request analytics → Render KPI groups → Render charts and metrics table
```

## Implementation map

| Layer | Path |
| --- | --- |
| UI acceptance contract | `frontend/src/features/dashboard/dashboardAnalyticsFlow.ts` |
| KPI grouping / lead copy | `frontend/src/features/dashboard/dashboardReadability.ts` |
| Unit tests | `frontend/src/features/dashboard/dashboardAnalyticsFlow.test.ts` |
| Dashboard page | `frontend/src/pages/DashboardPage.tsx` (+ tests) |
| Integration | `frontend/src/test/integration/dashboardAnalytics.integration.test.tsx` |
| Playwright UI | `frontend/tests/e2e/dashboard-analytics.spec.ts` |

## Running tests

```bash
cd frontend
npm test -- --run src/features/dashboard/dashboardAnalyticsFlow.test.ts
npm test -- --run src/pages/DashboardPage.test.tsx
npm test -- --run src/test/integration/dashboardAnalytics.integration.test.tsx
npx playwright test tests/e2e/dashboard-analytics.spec.ts
```

Do **not** run these when a backlog item says “do not run any tests” unless execution is explicitly requested.

## Related backlog

| Item | Topic |
| --- | --- |
| **591** | Improve dashboard readability |
| **605** | Campaign launch works through UI |
| **606** | Dashboard loads analytics (this document) |
| **607** | Role-based menu hides unauthorized features — see [ui-role-based-menu.md](ui-role-based-menu.md) |
| **608** | Keyboard navigation works for core forms — see [ui-keyboard-navigation.md](ui-keyboard-navigation.md) |
