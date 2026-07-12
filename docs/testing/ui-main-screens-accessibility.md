# UI Main Screens — Basic Accessibility Checks

Basic accessibility acceptance for primary business screens on the
Bayer-Westphalian Campaign Management Platform (KB **NFR-011**, Sprint 15 items
**588–590** / **609**).

## Acceptance (item 609)

**Main screens pass basic accessibility checks** when each catalogued screen:

1. Exposes a clear **page heading** (shell `h1` or login `h1`).
2. Exposes a **main** landmark (`main#main-content` in the app shell; login `main`).
3. Authenticated screens provide **Skip to content**, **Main navigation**, and **Breadcrumb**.
4. Primary content (forms, tables, key regions) has **accessible names**.
5. Core form controls are **labeled** (visible label or `aria-label`).
6. Global **focus-visible** outline styles exist for interactive controls (item **608**).
7. Shared UI color tokens meet **WCAG AA** contrast (4.5:1 text / 3:1 non-text focus).

This is a **basic** structural/label check set. Full automated axe runs and deeper audits
belong to Sprint 16 item **638**.

## Main screens catalog

| Screen | Path | Shell h1 |
| --- | --- | --- |
| Login | `/login` | Bayer-Westphalian Campaign Management |
| Dashboard | `/dashboard` | Dashboard |
| Customers | `/customers` | Customers |
| Products | `/products` | Products |
| Segments | `/segments` | Segments |
| Campaigns | `/campaigns` | Campaigns |
| Campaign builder | `/campaign-builder` | Builder |
| Compliance | `/compliance` | Compliance |
| Analytics | `/analytics` | Analytics |
| Reports | `/reports` | Reports |
| Audit | `/audit` | Audit |
| Users | `/users` | Users |

## Journey

```text
Page heading → Main landmark → Skip link → Main navigation → Breadcrumb → Primary content labels → Focus-visible styles → Color contrast tokens → Form control labels
```

## Implementation map

| Layer | Path |
| --- | --- |
| Screen catalog + check rules | `frontend/src/features/a11y/mainScreensAccessibility.ts` |
| Unit tests | `frontend/src/features/a11y/mainScreensAccessibility.test.ts` |
| Integration | `frontend/src/test/integration/mainScreensAccessibility.integration.test.tsx` |
| Playwright | `frontend/tests/e2e/main-screens-accessibility.spec.ts` |
| Related keyboard / focus | `frontend/src/features/a11y/keyboardNavigationFlow.ts` (item **608**) |
| Contrast / focus CSS | `frontend/src/app/styles.css` (+ `styles.test.ts`) |

## Running tests

```bash
cd frontend
npm test -- --run src/features/a11y/mainScreensAccessibility.test.ts
npm test -- --run src/test/integration/mainScreensAccessibility.integration.test.tsx
npx playwright test tests/e2e/main-screens-accessibility.spec.ts
```

Do **not** run these when a backlog item says “do not run any tests” unless execution is explicitly requested.

## Related backlog

| Item | Topic |
| --- | --- |
| **588** | Add labels for form controls |
| **589** | Check color contrast |
| **590** | Accessible table labels |
| **608** | Keyboard navigation works for core forms — see [ui-keyboard-navigation.md](ui-keyboard-navigation.md) |
| **609** | Main screens pass basic accessibility checks (this document) |
| **610** | UI style notes — see [ui-style-notes.md](../development/ui-style-notes.md) |
| **611** | Accessibility notes — see [accessibility-notes.md](../development/accessibility-notes.md) |
| **638** | Run accessibility checks (full suite execution) |
