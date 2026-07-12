# Accessibility Notes

Accessibility guidance for the Bayer-Westphalian Campaign Management Platform
(KB **NFR-011** Accessibility, **NFR-005** Usability, **SEC-003** frontend checks are not
sufficient alone, Sprint 15 items **587–590**, **608–609**, **611**).

These notes describe **what the product implements**, how to verify it, and what remains for
later full automated audits (Sprint 16 item **638**). This is item **611** delivery evidence.
They complement:

- [UI Style Notes](ui-style-notes.md) (item **610**) — visual tokens and contrast colors
- [Frontend Testing Notes](../testing/frontend-testing-notes.md) (item **612**) — pyramid and suite map
- [Main screens basic accessibility](../testing/ui-main-screens-accessibility.md) (item **609**)
- [Keyboard navigation for core forms](../testing/ui-keyboard-navigation.md) (item **608**)
- [Role-based menu](../testing/ui-role-based-menu.md) (item **607**)
- [Role-based access](../architecture/role-based-access.md) — backend remains authoritative

## Goals

| Goal | Product expectation |
| --- | --- |
| Operable without a mouse | Tab / Shift+Tab / Enter / Escape on shell and core forms |
| Perceivable structure | Landmarks, page headings, labeled tables and forms |
| Understandable feedback | `role="alert"` / `role="status"` for errors and success |
| Inclusive contrast | Shared UI pairs meet WCAG AA (4.5:1 text, 3:1 non-text focus) |
| Least privilege UX | Role-filtered menus hide unauthorized features (backend still enforces) |

Accessibility is a **baseline for internal business users**, not a claim of full WCAG 2.2 AAA
conformance for every third-party chart interaction.

## Scope

### In scope (MVP / Sprint 15)

- Application shell landmarks (main, navigation, breadcrumb, skip link)
- Form labels and validation messaging
- Keyboard navigation for core forms (login, create customer/product/segment, consent, campaign builder)
- Focus-visible outlines on interactive controls
- Accessible names for primary tables and key regions
- Status badges with readable text (not color-only)
- Confirmation dialogs with keyboard dismissal
- Basic automated checks via Vitest + Playwright smoke suites

### Out of scope / deferred

- Full axe-core or WAVE production audit matrix (item **638**)
- Screen-reader scripted UAT for every role persona
- Custom high-contrast theme beyond shared tokens
- Mobile app native accessibility APIs (web responsive only)

## Principles

1. **Backend authorization is authoritative** (`SEC-002`, `SEC-003`). Hiding a menu item never
   replaces API enforcement.
2. **Visible labels first**; `aria-label` when layout requires compact controls.
3. **Document order = focus order**; avoid `tabindex > 0`.
4. **Never remove focus outlines** for visual polish.
5. **Status is not color alone** — badge text and table columns carry the meaning.
6. **Modals trap focus** and restore focus on close (`ConfirmationDialog`).
7. **Live regions** announce async failures (`alert`) and successes (`status`) without stealing focus unnecessarily.

## Landmarks and page structure

| Landmark / element | Implementation | Notes |
| --- | --- | --- |
| Skip link | `Skip to content` → `#main-content` | Off-screen until focused |
| Main | `main#main-content` (`tabIndex={-1}`) | Programmatic skip target |
| Main navigation | `aria-label="Main navigation"` | Role-filtered links |
| Breadcrumb | `aria-label="Breadcrumb"` | Current page via `aria-current` |
| Page title | Shell `h1` from route | One primary heading per view |
| In-page sections | `h2` + `aria-labelledby` panels | Dashboard, compliance, worklists |
| Login | Own `main` landmark | Outside app shell |

Main screens catalog and integration checks: item **609**.

## Keyboard support

| Interaction | Expected behavior |
| --- | --- |
| Tab | Moves to next focusable control in document order |
| Shift+Tab | Reverse order without positive-tabindex traps |
| Enter | Submits the focused form (or activates the focused button/link) |
| Escape | Closes user menu and confirmation dialogs; returns focus |
| Arrow keys | User menu open; recipient preview tabs where implemented |

Core form Tab contracts: `frontend/src/features/a11y/keyboardNavigationFlow.ts` (item **608**).

Shell extras: skip link, user menu keyboard handlers in `AppLayout.tsx`.

## Form labels and validation

| Rule | Practice |
| --- | --- |
| Every input has a name | Wrapping `<label>` text or explicit `aria-label` |
| Invalid fields | `aria-invalid` when validation fails |
| Error text | Adjacent field error or `FormValidationMessage` with stable `id` |
| Blocking form errors | Prefer `role="alert"` for root/server errors |
| Success after save | `role="status"` / success notification |
| Required fields | Client validation messages + native `required` where used |

Core forms: login, customer create, consent record, product create, segment create, campaign builder.

## Tables and lists

| Rule | Practice |
| --- | --- |
| Accessible name | `aria-label`, `aria-labelledby`, or `sr-only` caption |
| Column headers | `<th>` with text (uppercase visual style does not remove text) |
| Row selection | Keyboard-capable where rows are interactive (`tabIndex={0}`, key handlers) |
| Empty state | Visible guidance text (`EmptyState` / table-state copy), not a blank region |
| Dense worklists | Keep action links/buttons with visible text or `aria-label` |

## Focus and contrast

### Focus indicators

| Selector family | Style |
| --- | --- |
| `a`, `button`, `input`, `select`, `textarea` | `outline: 3px solid #2563eb` on `:focus-visible` |
| `[role="tab"]`, `[role="menuitem"]` | Same outline |
| Skip link | Outline + slide into view on `:focus` |

### Contrast targets

| Criterion | Ratio |
| --- | --- |
| Normal text (shared UI pairs) | ≥ **4.5:1** (WCAG AA) |
| Focus ring vs light surface | ≥ **3:1** (non-text) |

Token pairs and verification: `styles.test.ts`, `mainScreensAccessibility.ts` color helpers, [UI Style Notes](ui-style-notes.md).

## Live regions and dialogs

| Pattern | Role | Example |
| --- | --- | --- |
| Error banner | `alert` | Unauthorized analytics, load failure, login failure |
| Success toast / notice | `status` | Customer created, campaign approved |
| Loading | `status` / busy | Top-bar loading indicator; `aria-busy` on main when pending |
| Empty | `status` (often) | EmptyState compact/full |
| Confirm sensitive action | dialog | Approve campaign, launch, disable user |

`ConfirmationDialog` supports Escape cancellation and keeps keyboard focus inside the dialog.

## Role-aware navigation

| Concern | Accessibility impact |
| --- | --- |
| Hidden menu items | Reduce clutter; do not present dead ends |
| Empty roles | Empty navigation state with explanatory copy |
| Multi-role union | Predictable ordered menu |
| Deep links | Protected routes redirect to login with notice |

Menu allow-lists: `roleBasedMenu.ts` (item **607**). Authorization failures still surface as alerts when APIs return 403.

## Charts and non-text content

| Element | Approach |
| --- | --- |
| Recharts frames | `role="img"` + descriptive `aria-label` via `ChartFrame` |
| KPI cards | Text labels and values, not color alone |
| Loading / empty charts | Explicit loading and empty messages |

Charts are **supplementary**; critical KPIs also appear as text metrics or tables.

## Testing map

| Layer | Path | Item |
| --- | --- | --- |
| Keyboard contract | `src/features/a11y/keyboardNavigationFlow.ts` | **608** |
| Main screen checks | `src/features/a11y/mainScreensAccessibility.ts` | **609** |
| Notes catalog (this item) | `src/features/a11y/accessibilityNotes.ts` | **611** |
| CSS focus + contrast | `src/app/styles.test.ts` | **589**, **608** |
| Integration keyboard | `src/test/integration/keyboardNavigation.integration.test.tsx` | **608** |
| Integration main screens | `src/test/integration/mainScreensAccessibility.integration.test.tsx` | **609** |
| Playwright keyboard | `tests/e2e/keyboard-navigation.spec.ts` | **608** |
| Playwright main screens | `tests/e2e/main-screens-accessibility.spec.ts` | **609** |
| Component inventory | badges, dialogs, empty/error/success | **595** |

### Running related tests

```bash
cd frontend
npm test -- --run src/features/a11y/accessibilityNotes.test.ts
npm test -- --run src/features/a11y/mainScreensAccessibility.test.ts
npm test -- --run src/features/a11y/keyboardNavigationFlow.test.ts
npm test -- --run src/app/styles.test.ts
npx playwright test tests/e2e/keyboard-navigation.spec.ts tests/e2e/main-screens-accessibility.spec.ts
```

Do **not** run these when a backlog item says “do not run any tests” unless execution is explicitly requested.

## Known limitations

1. Full **axe** / CI accessibility gate is deferred to item **638**.
2. Complex multi-step builder and criteria builders may contain dense controls; prefer sequential Tab and labeled fields.
3. Third-party chart SVG internals are not fully keyboard-operable; rely on surrounding text and tables.
4. Mobile layout meets **minimum internal usability**; touch targets aim for ≥ 44px height.
5. Screen-reader phrasing of dynamic tables is best-effort via captions/`aria-label`; no custom virtual grid.

## Do / donâ€™t

| Do | Don’t |
| --- | --- |
| Associate every control with a label | Use placeholder-only “labels” |
| Keep one `h1` per shell page | Skip heading levels for visual layout alone |
| Use `role="alert"` for blocking errors | Show failures only as toast color changes |
| Test Tab order after form redesigns | Introduce `tabindex="1"` “quick fixes” |
| Document new main screens in item **609** catalog | Ship unlabeled tables in worklists |
| Keep focus rings | `outline: none` without replacement |

## Evidence for reports

Anchor: ## Do and don't
Anchor: ## Do / don’t

Catalog anchors for `accessibilityNotes.ts`: Accessibility Notes; item **611**; NFR-011; NFR-005; SEC-003; ## Goals; ## Scope; ## Principles; ## Landmarks and page structure; ## Keyboard support; ## Form labels and validation; ## Tables and lists; ## Focus and contrast; ## Live regions and dialogs; ## Role-aware navigation; ## Charts and non-text content; ## Testing map; ## Known limitations; ## Do / donâ€™t; ## Evidence for reports; ## Acceptance (item 611); Skip to content; main-content; Main navigation; Breadcrumb; WCAG AA; 4.5:1; 3:1; tabindex; role="alert"; role="status"; ConfirmationDialog; keyboardNavigationFlow; mainScreensAccessibility; item **608**; item **609**; item **638**; Backend authorization is authoritative.

Suggested screenshots / captures for Sprint 15 evidence and item **613** are listed in
[Core Workflow Screenshots](../testing/core-workflow-screenshots.md). Accessibility-focused
shots include:

1. Skip link visible on keyboard focus
2. Focus ring on primary button and text field
3. Form validation errors with labeled fields
4. Confirmation dialog open (keyboard focus inside)
5. Status badges with readable text in a worklist
6. Main navigation landmark + breadcrumb
7. Empty state with guidance text

## Acceptance (item 611)

**Accessibility notes** are complete when:

1. This document exists at `docs/development/accessibility-notes.md`.
2. It covers goals, scope, principles, landmarks, keyboard, forms, tables, focus/contrast, live regions, role-aware nav, charts, testing map, limitations, and do/don’t.
3. Code catalog `accessibilityNotes.ts` lists required sections and related backlog items.
4. Unit tests assert documentation presence, index links, and alignment with a11y modules / CSS focus tokens.
5. Notes cross-link items **608**, **609**, **610**, and future **638**.
