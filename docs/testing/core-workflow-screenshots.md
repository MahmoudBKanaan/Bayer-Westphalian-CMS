# Core Workflow Screenshots

Screenshot evidence inventory for the Bayer-Westphalian Campaign Management Platform
(KB Testing Plan / NFR-010 / Sprint 15 item **613**, report appendix).

This document defines **which** core UI workflows must be photographed, **where** files live,
and **how** to capture them. Binary images may be added later; the catalog and capture notes
are the acceptance deliverable for item **613**.

Related acceptance anchors include item **598** and item **607**.

Related:

- [Sprint 15 Production Gate](../agile/sprint-15-production-gate.md) (item **616**)
- [Frontend Testing Notes](frontend-testing-notes.md) (item **612**)
- [Playwright Happy-Path E2E](playwright-e2e.md) (item **597**)
- [UI Style Notes](../development/ui-style-notes.md) (item **610**)
- [Accessibility Notes](../development/accessibility-notes.md) (item **611**)
- Evidence folder: [`docs/evidence/core-workflows/`](../evidence/core-workflows/)

## Goals

| Goal | Outcome |
| --- | --- |
| University / sprint evidence | Stable appendix paths for Project Report |
| Demo narrative | Visual story of Login → … → Launch + shell UX |
| Traceability | Each shot maps to a backlog item and route |
| Repeatable capture | Capture procedure works with demo accounts or E2E mock |

## KB happy-path journey

```text
Login → create customer → consent → campaign → approval → launch
```

Canonical text anchor: Login → create customer → consent → campaign → approval → launch.
Catalog text anchor: Login â†’ create customer â†’ consent â†’ campaign â†’ approval â†’ launch.

Plus professionalization evidence from Sprint 15 (shell, roles, validation, a11y cues).

## Evidence folder

| Item | Path |
| --- | --- |
| Binary captures | `docs/evidence/core-workflows/*.png` |
| Folder guide | `docs/evidence/core-workflows/README.md` |
| Code catalog | `frontend/src/features/testing/coreWorkflowScreenshots.ts` |
| Unit lock | `frontend/src/features/testing/coreWorkflowScreenshots.test.ts` |

Prefer **PNG**. Keep the catalog file name exactly (including zero-padded index prefix).

## Screenshot catalog

### A. Core marketing workflow (KB E2E)

| # | ID | File name | Route / surface | What to show | Backlog |
| --- | --- | --- | --- | --- | --- |
| 01 | `login` | `01-login-sign-in.png` | `/login` | Employee sign-in form | **598** |
| 02 | `dashboard` | `02-dashboard-analytics.png` | `/dashboard` | KPI cards / analytics shell | **606** |
| 03 | `customer-create` | `03-customer-create.png` | `/customers` | Create customer form + list | **599** |
| 04 | `consent-update` | `04-consent-update.png` | `/customers/:id` | Record consent panel | **600** |
| 05 | `product-create` | `05-product-create.png` | `/products` | Create product form | **601** |
| 06 | `segment-create` | `06-segment-create.png` | `/segments` | Create segment + criteria | **602** |
| 07 | `campaign-builder` | `07-campaign-builder.png` | `/campaign-builder` | Multi-step builder | **603** |
| 08 | `compliance-approval` | `08-compliance-approval.png` | `/compliance` | Submitted queue + approve | **604** |
| 09 | `campaign-launch` | `09-campaign-launch.png` | recipients preview | Eligible list + launch | **605** |

### B. Shell, roles, and professionalization

| # | ID | File name | Route / surface | What to show | Backlog |
| --- | --- | --- | --- | --- | --- |
| 10 | `app-shell-layout` | `10-app-shell-layout.png` | `/dashboard` | Sidebar + top bar + main | **569–573** |
| 11 | `role-based-menu` | `11-role-based-menu.png` | any shell | Menu for non-admin role | **607** |
| 12 | `form-validation` | `12-form-validation.png` | login or create | Field errors visible | **578** |
| 13 | `confirmation-dialog` | `13-confirmation-dialog.png` | compliance / launch | Confirm dialog open | **579** |
| 14 | `status-badges` | `14-status-badges.png` | campaigns worklist | Status badge column | **580** |
| 15 | `responsive-tablet` | `15-responsive-tablet.png` | shell ~1000px | Tablet layout | **585–586** |
| 16 | `keyboard-focus` | `16-keyboard-focus.png` | any form | Focus-visible ring | **608** |
| 17 | `skip-link-or-landmarks` | `17-skip-link-or-landmarks.png` | shell | Skip link focused or landmarks noted | **609** |
| 18 | `playwright-evidence` | `18-playwright-evidence.png` | CI or local run | Playwright report / green run | **597** |

## Capture procedure

### Option 1 — Live demo stack

1. Start backend + frontend with seeded demo users.
2. Sign in as Admin (`admin@bayer-westphalian.test` / shared strong password).
3. Walk the happy path; capture each catalog row after the success signal appears.
4. For **role-based menu**, sign in as BI Analyst or Campaign Manager and capture the sidebar.
5. For **responsive**, resize browser to ~1000px width (tablet band).
6. For **keyboard focus**, Tab to a primary button so the blue focus ring is visible.

### Option 2 — Playwright UI with API mock

1. Run a focused Playwright spec with headed browser or `page.screenshot` (optional later).
2. Use the happy-path mock so captures stay deterministic without Postgres.
3. Prefer stable routes listed in `happyPathFlow.ts` and UI workflow docs.

### Option 3 — Deferred binary files

Item **613** is satisfied by this inventory + empty evidence folder contract. Binary files can be
filled before final report packaging (items **644–645** also capture test/E2E evidence).

## Caption templates (report appendix)

Use short captions such as:

- **Figure 613-01.** Employee sign-in screen for internal Bayer-Westphalian access.
- **Figure 613-02.** Dashboard analytics KPIs after successful login.
- **Figure 613-08.** Compliance Officer approval queue for submitted campaigns.
- **Figure 613-11.** Role-filtered main navigation for a non-admin persona.

## Manifest helpers

The catalog exports machine-readable entries:

- `id`, `fileName`, `route`, `title`, `description`, `relatedBacklogItems`
- `coreWorkflowScreenshotFileNames()` for expected files under `docs/evidence/core-workflows/`
- `formatCoreWorkflowScreenshotJourney()` for the ordered narrative string

## Relationship to tests

| Layer | Role for screenshots |
| --- | --- |
| Unit catalog tests | Lock inventory and docs (this item) |
| Playwright | Optional future `page.screenshot` into evidence folder |
| Manual capture | Primary path for polished report figures |

Do **not** run Playwright solely for item **613** when the backlog says “do not run any tests.”

## Acceptance (item 613)

Catalog acceptance anchor: Binary PNG files may still be pending.

Catalog anchors for `coreWorkflowScreenshots.ts`: Core Workflow Screenshots; item **613**; NFR-010; ## Goals; ## KB happy-path journey; ## Evidence folder; ## Screenshot catalog; ## Capture procedure; ## Caption templates (report appendix); ## Manifest helpers; ## Relationship to tests; ## Relationship to automated tests; ## Acceptance (item 613); docs/evidence/core-workflows; 01-login-sign-in.png; 09-campaign-launch.png; 10-app-shell-layout.png; 18-playwright-evidence.png; Login â†’ create customer â†’ consent â†’ campaign â†’ approval â†’ launch; coreWorkflowScreenshots.ts; do not run any tests; item **597**; item **598**; item **607**; Binary PNG files may still be pending.

**Core workflow screenshots** are complete when:

1. This document exists at `docs/testing/core-workflow-screenshots.md`.
2. The evidence directory `docs/evidence/core-workflows/` exists with a README naming guide.
3. The catalog lists all core workflow and professionalization shots with stable file names.
4. Unit tests assert documentation presence, catalog integrity, ordered journey, and evidence folder.
5. `docs/README.md` links this document.
6. Binary PNG files may still be pending; the inventory and capture procedure are mandatory.
