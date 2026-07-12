# Frontend Integration Tests

Multi-module frontend tests for the Bayer-Westphalian Campaign Management Platform
(KB **NFR-010**, Sprint 15 item **596**).

These tests mount the **real route tree** (`routes` from `app/router.tsx`) with **AuthProvider**
and **React Query**, stub network with `fetch`, and exercise shell + page composition. They sit
between co-located component/page unit tests (item **595**) and Playwright E2E (item **597**).

## Scope (item 596)

| Suite | File | Covers |
| --- | --- | --- |
| Harness | `src/test/integration/renderApp.tsx` | Session seed, `renderApp`, fetch router helpers |
| Auth / routing | `src/test/integration/authRouting.integration.test.tsx` | Unauthenticated redirect, login → dashboard, root redirect, 404, nav between protected routes |
| Role navigation | `src/test/integration/roleNavigation.integration.test.tsx` | Menu visibility by role; admin settings; executive/analytics (item **596**) |
| Role-based menu (item 607) | `src/test/integration/roleBasedMenu.integration.test.tsx` | Full persona matrix, admin-only hide, multi-role, empty roles |
| Workflows | `src/test/integration/workflowRoutes.integration.test.tsx` | Campaigns list, builder, compliance queue, recipient preview, audit |
| Login UI (item 598) | `src/test/integration/loginFlow.integration.test.tsx` | Sign-in form, redirect, success, deep-link return, validation, 401/429 |
| Customer create UI (item 599) | `src/test/integration/customerCreation.integration.test.tsx` | Create panel, POST success, list refresh, validation, role gate |
| Consent update UI (item 600) | `src/test/integration/consentUpdate.integration.test.tsx` | Record/opt-out/withdraw, purpose validation, success notices |
| Product create UI (item 601) | `src/test/integration/productCreation.integration.test.tsx` | Create panel, POST success, catalog refresh, validation, role gate |
| Segment create UI (item 602) | `src/test/integration/segmentCreation.integration.test.tsx` | Create panel, POST success, list refresh, validation, BI role gate |
| Campaign create UI (item 603) | `src/test/integration/campaignCreation.integration.test.tsx` | Builder steps, draft POST, product select, validation, role gate |
| Compliance approval UI (item 604) | `src/test/integration/complianceApproval.integration.test.tsx` | SUBMITTED queue, confirm approve, notes, role gate, reject reason |
| Campaign launch UI (item 605) | `src/test/integration/campaignLaunch.integration.test.tsx` | Recipient preview, confirm launch, ACTIVE result, role/status gates |
| Dashboard analytics UI (item 606) | `src/test/integration/dashboardAnalytics.integration.test.tsx` | KPI load, charts/table headings, auth gate, API error surface |
| Keyboard navigation (item 608) | `src/test/integration/keyboardNavigation.integration.test.tsx` | Tab/Shift+Tab/Enter on login + create forms; skip link |
| Main screens a11y (item 609) | `src/test/integration/mainScreensAccessibility.integration.test.tsx` | Landmarks, headings, labeled primary content on main routes |
| Legacy router suite | `src/app/router.integration.test.tsx` | Earlier routing / campaigns / customer details coverage |

## What integration means here

- **In scope:** Router + auth session + layout shell + page + mocked REST client  
- **Out of scope:** Real backend, browser E2E, pure component isolation  

## Running tests

```bash
cd frontend
npm test -- --run src/test/integration
npm test -- --run src/app/router.integration.test.tsx
```

Do **not** run as part of backlog items that say “do not run any tests” unless the item
explicitly requests execution.

## Conventions

1. Prefer `renderApp({ path, roles })` over ad-hoc `MemoryRouter` for new integration cases.
2. Stub `fetch` with `createFetchRouter` and return `jsonOk(data)` matching API wrappers.
3. Seed JWT roles via `createAccessToken` so permission checks match the shell menu.
4. Assert accessible headings/nav labels, not CSS class names.
5. Keep single-page deep interactions in `src/pages/*.test.tsx`; keep cross-route flows here.

## Related backlog

| Item | Topic |
| --- | --- |
| **595** | Frontend component tests |
| **596** | Frontend integration tests (this document) |
| **597** | Playwright happy-path E2E — see [playwright-e2e.md](playwright-e2e.md) |
| **598** | Login flow works through UI — see [ui-login-flow.md](ui-login-flow.md) |
| **599** | Customer creation works through UI — see [ui-customer-creation.md](ui-customer-creation.md) |
| **600** | Consent update works through UI — see [ui-consent-update.md](ui-consent-update.md) |
| **601** | Product creation works through UI — see [ui-product-creation.md](ui-product-creation.md) |
| **602** | Segment creation works through UI — see [ui-segment-creation.md](ui-segment-creation.md) |
| **603** | Campaign creation works through UI — see [ui-campaign-creation.md](ui-campaign-creation.md) |
| **604** | Compliance approval works through UI — see [ui-compliance-approval.md](ui-compliance-approval.md) |
| **605** | Campaign launch works through UI — see [ui-campaign-launch.md](ui-campaign-launch.md) |
| **606** | Dashboard loads analytics — see [ui-dashboard-analytics.md](ui-dashboard-analytics.md) |
| **607** | Role-based menu hides unauthorized features — see [ui-role-based-menu.md](ui-role-based-menu.md) |
| **608** | Keyboard navigation works for core forms — see [ui-keyboard-navigation.md](ui-keyboard-navigation.md) |
| **609** | Main screens pass basic accessibility checks — see [ui-main-screens-accessibility.md](ui-main-screens-accessibility.md) |
| **612** | Frontend testing notes — see [frontend-testing-notes.md](frontend-testing-notes.md) |
| **610+** | Sprint 15 documentation / remaining UX acceptance |
