# Playwright Happy-Path E2E

Browser end-to-end coverage for the Bayer-Westphalian Campaign Management Platform
(KB **Testing Plan** / **NFR-010**, Sprint 15 item **597**).

## KB journey

```text
Login → create customer → consent → campaign → approval → launch
```

Implemented as a single Playwright scenario with named steps, plus unit tests that lock the
step order and the deterministic REST mock.

## Layout

| Area | Path | Role |
| --- | --- | --- |
| Step contract | `frontend/src/features/e2e/happyPathFlow.ts` | Ordered steps, admin credentials, fixture ids |
| REST mock | `frontend/src/features/e2e/happyPathApiMock.ts` | Pure request → response state machine |
| Unit tests | `frontend/src/features/e2e/*.test.ts` | Vitest (no browser) |
| Playwright helpers | `frontend/tests/e2e/helpers/` | `page.route` install + UI actions |
| Happy-path spec | `frontend/tests/e2e/happy-path.spec.ts` | Full KB journey |
| Dashboard smoke | `frontend/tests/e2e/dashboard.spec.ts` | Login → dashboard shell |

## Why API mock?

Item **597** verifies the **UI happy path** through a real browser. The mock keeps the suite:

- independent of a running Spring Boot / Postgres stack during frontend CI
- multi-role capable under a single **ADMIN** session (admin may create customers, manage campaigns, approve, and launch)
- deterministic for screenshots and later evidence capture (items **644–645**)

Live backend UI acceptance is covered by later backlog items (**598+** workflow scenarios and **637** run E2E).

## Running

```bash
cd frontend
# Unit contract + mock (part of npm test / Vitest)
npm test -- --run src/features/e2e

# Browser E2E (starts Vite via playwright.config.ts webServer)
npm run test:e2e
# or only the happy path:
npx playwright test tests/e2e/happy-path.spec.ts
```

Do **not** run these as part of backlog implementation items that say “do not run any tests”
unless the item explicitly requests execution (e.g. item **637**).

## Conventions

1. Keep the KB step order in `HAPPY_PATH_STEPS`; update unit tests when the journey changes.
2. Prefer accessible queries (`getByRole`, `getByLabel`) in Playwright helpers.
3. Extend `handleHappyPathApiRequest` when new pages in the journey call additional REST paths.
4. Admin credentials match seeded demo accounts (`admin@bayer-westphalian.test` / shared strong password).

## Related backlog

| Item | Topic |
| --- | --- |
| **595** | Frontend component tests |
| **596** | Frontend integration tests |
| **597** | Playwright happy-path E2E (this document) |
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
| **613** | Core workflow screenshots — see [core-workflow-screenshots.md](core-workflow-screenshots.md) |
| **616** | Business-user production gate — see [sprint-15-production-gate.md](../agile/sprint-15-production-gate.md) |
| **610+** | Sprint 15 documentation / remaining UX acceptance |
| **637** | Run E2E happy-path tests |
