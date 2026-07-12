# Frontend Testing Notes

How the Bayer-Westphalian Campaign Management Platform frontend is tested
(KB **NFR-010** Testability, Testing Plan, Sprint 15 item **612**).

This is the **index and convention guide** for frontend quality. Detailed suites live in linked
docs; this note explains layers, tools, commands, conventions, and when **not** to run tests.

Related:

- [Functional Requirements Test Map](functional-requirements-test-map.md) (item **620**)
- [Business Rules Test Map](business-rules-test-map.md) (item **621**)
- [Non-Functional Requirements Test Map](non-functional-requirements-test-map.md) (item **622**)
- [Performance Smoke Checks](performance-smoke.md) (item **639** / NFR-003)
- [Frontend Component Tests](frontend-component-tests.md) (item **595**)
- [Frontend Integration Tests](frontend-integration-tests.md) (item **596**)
- [Playwright Happy-Path E2E](playwright-e2e.md) (item **597**)
- [Accessibility Notes](../development/accessibility-notes.md) (item **611**)
- [UI Style Notes](../development/ui-style-notes.md) (item **610**)

## Goals

| Goal | How frontend tests support it |
| --- | --- |
| Business users complete workflows without developer help | UI flow contracts + integration + E2E happy path |
| Role-aware and compliant UX | Role menu, form gates, confirmation dialogs |
| Fast feedback while coding | Vitest unit/component tests |
| Realistic shell + routing | Integration with `renderApp` |
| Browser-level journey evidence | Playwright with deterministic API mock |
| University / sprint evidence | Named items **595–612**, screenshots **613** |

## Test pyramid

```text
                    ┌─────────────────────┐
                    │  Playwright E2E     │  real browser, mocked REST
                    │  tests/e2e/**       │  items 597, 598–609 smoke
                    └──────────┬──────────┘
                               │
              ┌────────────────┴────────────────┐
              │  Integration (Vitest + RTL)     │
              │  src/test/integration/**        │  item 596 + flow suites
              │  real router + Auth + Query     │
              └────────────────┬────────────────┘
                               │
     ┌─────────────────────────┼─────────────────────────┐
     │  Page / component tests │  Pure feature contracts │
     │  src/pages, components  │  src/features/**/*.ts   │
     │  item 595 + page tests  │  flows, a11y, style     │
     └─────────────────────────┴─────────────────────────┘
```

| Layer | Tooling | What it proves |
| --- | --- | --- |
| Pure feature contracts | Vitest | Step order, validation, role allow-lists, a11y catalogs |
| Component tests | Vitest + RTL | Shared UI primitives, badges, dialogs, charts |
| Page tests | Vitest + RTL | Single-page behavior with local mocks |
| Integration | Vitest + RTL + `renderApp` | Route tree, shell, session, mocked `fetch` |
| E2E | Playwright | Browser journey with `happyPathApiMock` |

**Out of scope for frontend suite:** Spring Boot / Testcontainers (backend suite); full production
axe gate is Sprint 16 item **638**.

## Tools and scripts

| Concern | Choice |
| --- | --- |
| Unit / component / integration runner | **Vitest** (`npm test` / `vitest run`) |
| Component queries / events | **React Testing Library** + **user-event** |
| DOM environment | **jsdom** |
| Browser E2E | **Playwright** (`npm run test:e2e`) |
| Typecheck + bundle | `npm run build` (`tsc -b && vite build`) |
| Lint / format | ESLint, Prettier |
| Full local frontend gate | `npm run verify` |

### Commands (from `frontend/`)

```bash
npm test
npm test -- --run src/features
npm test -- --run src/test/integration
npm test -- --run src/components
npm run test:e2e
npx playwright test tests/e2e/happy-path.spec.ts
npm run verify
```

### Do not run tests when the backlog says so

Many Sprint 15 implementation items end with *“Add the necessary unit or integration tests but
do not run any tests.”* For those items:

1. **Write** tests and docs.
2. **Do not** execute `npm test`, Playwright, or `verify` unless the user later requests a suite
   run (e.g. items **617**, **635–637**).

## Directory map

| Path | Role |
| --- | --- |
| `src/features/**/*.ts` | Pure flow contracts + `.test.ts` (login, create, a11y, e2e mock) |
| `src/features/**/*.test.ts` | Unit locks for contracts |
| `src/components/**/*.test.tsx` | Shared component tests (item **595**) |
| `src/pages/**/*.test.tsx` | Page-level unit/integration-style tests |
| `src/api/**/*.test.ts` | API client unit tests |
| `src/test/integration/` | Cross-route integration harness + suites |
| `src/test/integration/renderApp.tsx` | Session seed, `renderApp`, `createFetchRouter` |
| `src/test/setup.ts` | Vitest / jest-dom setup |
| `tests/e2e/**/*.spec.ts` | Playwright specs |
| `tests/e2e/helpers/` | API mock install + UI actions |
| `playwright.config.ts` | Browser projects + Vite webServer |
| `docs/testing/*` | Human-readable suite documentation |

## Feature flow pattern (items 598–609)

UI acceptance stories use a consistent shape:

1. **`*Flow.ts`** — pure constants, validation, step IDs, fixtures (no React).
2. **`*Flow.test.ts`** — Vitest locks for steps and rules.
3. **Page wiring** — pages import constants/labels from the flow module.
4. **`*.integration.test.tsx`** — `renderApp` + mocked REST for the real route.
5. **`tests/e2e/*.spec.ts`** — Playwright happy-path or focused UI scenario.
6. **`docs/testing/ui-*.md`** — acceptance narrative and run commands.

Examples:

| Item | Flow module | Integration | Playwright |
| --- | --- | --- | --- |
| **598** | `features/auth/loginFlow.ts` | `loginFlow.integration.test.tsx` | `login-flow.spec.ts` |
| **599** | `features/customers/customerCreationFlow.ts` | `customerCreation.integration.test.tsx` | `customer-creation.spec.ts` |
| **606** | `features/dashboard/dashboardAnalyticsFlow.ts` | `dashboardAnalytics.integration.test.tsx` | `dashboard-analytics.spec.ts` |
| **607** | `features/auth/roleBasedMenu.ts` | `roleBasedMenu.integration.test.tsx` | `role-based-menu.spec.ts` |
| **608** | `features/a11y/keyboardNavigationFlow.ts` | `keyboardNavigation.integration.test.tsx` | `keyboard-navigation.spec.ts` |
| **609** | `features/a11y/mainScreensAccessibility.ts` | `mainScreensAccessibility.integration.test.tsx` | `main-screens-accessibility.spec.ts` |

Happy-path multi-step journey: `features/e2e/happyPathFlow.ts` + `happyPathApiMock.ts` +
`tests/e2e/happy-path.spec.ts` (item **597**).

## Integration harness conventions

Use `renderApp` from `src/test/integration/renderApp.tsx`:

1. Seed JWT roles via `createAccessToken` / `seedAuthenticatedSession`.
2. Stub `fetch` with `createFetchRouter` returning `jsonOk` / `jsonError`.
3. Prefer **accessible queries** (`getByRole`, `getByLabelText`) over class names.
4. Assert shell landmarks (main nav, headings) where the shell is in scope.
5. Keep deep single-page interactions in `src/pages/*.test.tsx` when they do not need routing.

Health probes must return **actuator-style** JSON (not only `ApiResponse` envelopes).

## Playwright conventions

1. Install the shared mock with `installHappyPathApiMock` (or an equivalent route handler).
2. Prefer `getByRole` / `getByLabel` for interactions.
3. Admin demo credentials come from `HAPPY_PATH_ADMIN` in `happyPathFlow.ts`.
4. Keep the mock pure so Vitest can cover routing without a browser.
5. Do not require a live Spring Boot process for default E2E CI.

## Documentation-as-test (items 610–613)

Some Sprint 15 docs are locked by Vitest reading markdown from the repo:

| Item | Catalog | Doc path |
| --- | --- | --- |
| **610** | `features/ui/uiStyleNotes.ts` | `docs/development/ui-style-notes.md` |
| **611** | `features/a11y/accessibilityNotes.ts` | `docs/development/accessibility-notes.md` |
| **612** | `features/testing/frontendTestingNotes.ts` | `docs/testing/frontend-testing-notes.md` |
| **613** | `features/testing/coreWorkflowScreenshots.ts` | `docs/testing/core-workflow-screenshots.md` |

These tests assert presence, required headings, and links from `docs/README.md`.
Screenshot binary files for **613** live under `docs/evidence/core-workflows/`.

## UI workflow documentation index

| Doc | Item |
| --- | --- |
| [ui-login-flow.md](ui-login-flow.md) | **598** |
| [ui-customer-creation.md](ui-customer-creation.md) | **599** |
| [ui-consent-update.md](ui-consent-update.md) | **600** |
| [ui-product-creation.md](ui-product-creation.md) | **601** |
| [ui-segment-creation.md](ui-segment-creation.md) | **602** |
| [ui-campaign-creation.md](ui-campaign-creation.md) | **603** |
| [ui-compliance-approval.md](ui-compliance-approval.md) | **604** |
| [ui-campaign-launch.md](ui-campaign-launch.md) | **605** |
| [ui-dashboard-analytics.md](ui-dashboard-analytics.md) | **606** |
| [ui-role-based-menu.md](ui-role-based-menu.md) | **607** |
| [ui-keyboard-navigation.md](ui-keyboard-navigation.md) | **608** |
| [ui-main-screens-accessibility.md](ui-main-screens-accessibility.md) | **609** |

## Quality gate timeline

Related run anchors: item **608**, item **609**, item **617**, item **635**, and item **637**.

| Item | Action |
| --- | --- |
| **595–612** | Write frontend tests and notes (often without running) |
| **613** | Core workflow screenshots — see [core-workflow-screenshots.md](core-workflow-screenshots.md) |
| **616** | Business-user production gate — see [sprint-15-production-gate.md](../agile/sprint-15-production-gate.md) |
| **617** | Run full local suite when requested (fix failures) |
| **635** | Run frontend component tests |
| **636** | Run frontend integration tests |
| **637** | Run E2E happy-path tests |
| **638** | Run accessibility checks (broader than basic **609**) |

## Conventions checklist

1. New UI acceptance item → add flow module + unit + integration (+ Playwright if UI journey).
2. Prefer constants from flow modules in pages and tests (single source of truth for labels).
3. Accessible queries over CSS selectors.
4. Mock network at `fetch` (integration) or Playwright `route` (E2E); do not hit random ports.
5. Document each suite under `docs/testing/` and link from `docs/README.md`.
6. Respect **do not run any tests** wording on backlog items.
7. Backend remains system of record; frontend tests do not replace Java security tests.

## Do / donâ€™t

| Do | Don’t |
| --- | --- |
| Co-locate component tests with sources | Dump all tests in a single mega-file |
| Use `renderApp` for multi-route flows | Re-implement auth session ad hoc every time |
| Keep happy-path mock deterministic | Depend on a developer’s local DB seed for E2E |
| Assert roles and validation messages | Only screenshot without assertions |
| Update docs when adding a suite | Leave unlinked orphan test files |

## Acceptance (item 612)

Readable heading anchors: ## Feature flow pattern (items 598–609); ## Documentation-as-test (items 610–613); ## Do and don't.
Readable heading anchor: ## Do / don’t.

Catalog anchors for `frontendTestingNotes.ts`: Frontend Testing Notes; item **612**; NFR-010; ## Goals; ## Test pyramid; ## Tools and scripts; ## Directory map; ## Feature flow pattern (items 598â€“609); ## Integration harness conventions; ## Playwright conventions; ## Documentation-as-test (items 610â€“613); ## UI workflow documentation index; ## Quality gate timeline; ## Conventions checklist; ## Do / donâ€™t; ## Acceptance (item 612); Vitest; Playwright; React Testing Library; renderApp; happyPathApiMock; npm test; npm run test:e2e; npm run verify; do not run any tests; item **595**; item **596**; item **597**; item **608**; item **609**; item **617**; item **635**; item **637**; src/test/integration; tests/e2e; frontendTestingNotes.ts.

**Frontend testing notes** are complete when:

1. This document exists at `docs/testing/frontend-testing-notes.md`.
2. It describes the pyramid, tools, directories, flow pattern, harness/Playwright conventions, doc locks, UI doc index, and quality-gate timeline.
3. Code catalog `frontendTestingNotes.ts` lists sections, scripts, and related docs.
4. Unit tests assert documentation presence, required snippets, index link, and that key related docs exist.
5. Notes reference items **595–611** and later run gates **617**, **635–638**.
