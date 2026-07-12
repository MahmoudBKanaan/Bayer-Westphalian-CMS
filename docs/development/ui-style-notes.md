# UI Style Notes

Visual and interaction style guide for the Bayer-Westphalian Campaign Management Platform
frontend (KB **NFR-005** Usability, **NFR-011** Accessibility, Sprint 15 item **610**).

These notes describe the **implemented** professional UI language in `frontend/src/app/styles.css`
and shared components under `frontend/src/components/`. They are the reference for consistent
screens, demos, screenshots (item **613**), and university report evidence.

Related:

- [Accessibility Notes](accessibility-notes.md) (item **611**)
- [Frontend Testing Notes](../testing/frontend-testing-notes.md) (item **612**)
- [Main screens accessibility](../testing/ui-main-screens-accessibility.md) (item **609**)
- [Keyboard navigation](../testing/ui-keyboard-navigation.md) (item **608**)
- [Role-based menu](../testing/ui-role-based-menu.md) (item **607**)
- [Developer setup](developer-setup.md)

## Goals

| Goal | Style implication |
| --- | --- |
| Internal business app | Calm enterprise chrome; no consumer-marketing visuals |
| Role-aware productivity | Dense but scannable lists, clear primary actions |
| Compliance visibility | Status badges, alerts, and confirmation dialogs for sensitive actions |
| Demo-ready professionalism | Consistent spacing, type, and focus rings across all main screens |
| Accessibility baseline | Visible focus, labeled forms, WCAG AA text contrast on shared tokens |

## Implementation map

| Concern | Location |
| --- | --- |
| Global stylesheet | `frontend/src/app/styles.css` |
| Style token catalog (code) | `frontend/src/features/ui/uiStyleNotes.ts` |
| Application shell | `frontend/src/components/AppLayout.tsx` |
| Shared badges / states | `StatusBadge`, `EmptyState`, `ErrorState`, `SuccessNotification`, `FormValidationMessage`, `ConfirmationDialog`, `MetricCard` |
| Charts palette | `frontend/src/components/charts/` (`CHART_COLORS`) |
| Unit lock | `frontend/src/features/ui/uiStyleNotes.test.ts` |
| CSS regression lock | `frontend/src/app/styles.test.ts` |

## Typography

| Token | Value | Usage |
| --- | --- | --- |
| Font family | `Roboto, Arial, sans-serif` | Global body and controls |
| Font weight (default) | `400` | Body and most UI chrome |
| Line height | `1.5` | Readable paragraphs and form help |
| Page title (`h1` in shell) | Large, top bar | Route title from breadcrumbs |
| Section title (`h2`) | Panel headings | In-page structure |
| Eyebrow | Small uppercase/secondary | Product context under shell title |
| Table header | `0.78rem`, uppercase, `#64748b` | Column labels |

Prefer plain language labels; avoid decorative display fonts.

## Color system

### Surfaces

| Role | Hex | Notes |
| --- | --- | --- |
| Page background | `#f4f7fb` | Soft cool gray-blue canvas |
| Panel / card | white with light border/shadow | `.panel`, `.metric-card`, `.login-panel` |
| Table header row | `#f8fafc` | Sticky headers on dense tables |
| Empty dashed state | `#f8fafc` / border `#cbd5e1` | Empty guidance boxes |

### Text

| Role | Hex | Notes |
| --- | --- | --- |
| Primary text | `#1f2937` | Body default (`:root`) |
| Strong emphasis | `#0f172a` | Campaign names, KPI values |
| Secondary / hint | `#475569`, `#64748b` | Leads, table headers, muted copy |

### Actions

| Role | Background | Text | Class |
| --- | --- | --- | --- |
| Primary button | `#1d4ed8` → hover `#1e40af` | `#ffffff` | default `button` |
| Secondary button | `#e0f2fe` → hover `#bae6fd` | `#075985` | `.secondary-button` |
| Danger button | `#b91c1c` → hover `#991b1b` | white | `.danger-button` |
| Disabled button | `#64748b` | white | `button:disabled` |
| Focus ring | outline `#2563eb` (3px) | — | `:focus-visible` rules |

Primary blue must remain the **only** default action color so approve/submit controls read as intentional.

### Status badge tones

Shared `.status-badge` and domain badges (campaign, customer, consent, reminder, audit):

| Tone | Text | Background | Typical meaning |
| --- | --- | --- | --- |
| Success | `#166534` | `#dcfce7` | Approved, active, paid, completed |
| Warning | `#92400e` | `#fef3c7` | Submitted, pending, yellow reminder |
| Danger | `#991b1b` | `#fee2e2` | Rejected, failed, do-not-contact, red reminder |
| Info | `#1e40af` | `#dbeafe` | Draft, informational states |

Contrast for these pairs is locked at **WCAG AA 4.5:1** in `styles.test.ts` and item **609** color checks.

## Spacing and radius

| Token | Typical value | Usage |
| --- | --- | --- |
| Control padding | `0.75rem 1rem` | Buttons, compact inputs |
| Panel padding | ~`1rem`–`1.25rem` | Cards and form panels |
| Table cell padding | `0.85rem` | Worklists |
| Border radius | `6px`–`8px` | Buttons, panels, empty states |
| Page stack gap | CSS grid/gap on `.page-stack` | Vertical rhythm between panels |
| Button row gap | `.button-row` | Primary + secondary actions |

Avoid one-off magic margins inside page JSX when a shared class already exists.

## Application shell

| Element | Style notes |
| --- | --- |
| `.app-shell` | Desktop: `280px` sticky sidebar + fluid main |
| Sidebar | Brand mark **BW**, sectioned nav (`Workspace`, `Campaign Operations`, …) |
| Nav link | Quiet default; active/hover highlighted |
| Top bar | Breadcrumb + eyebrow product name + `h1` page title + health pill + user menu |
| Skip link | Off-screen until focused; jumps to `#main-content` |
| Loading indicator | Top-bar status when queries/mutations are in flight |

### Responsive breakpoints (item 585–586)

| Breakpoint | Layout behavior |
| --- | --- |
| ≥ `1281px` | Full desktop shell, sticky sidebar |
| `961px`–`1280px` | Tablet shell (`240px` sidebar) |
| ≤ `960px` | Collapsed multi-column content; denser grids |
| ≤ `640px` | Mobile minimum usability; touch targets ≥ `44px` height |

Internal use only: mobile is **usable**, not a consumer-app redesign.

## Forms

| Pattern | Style / behavior |
| --- | --- |
| `.form-grid` | Vertical labeled fields |
| Labels | Visible text wrapping control or explicit `aria-label` |
| Validation | `.form-error` / field error text; `role="alert"` for blocking messages |
| Success | `.form-success` or `SuccessNotification` (`role="status"`) |
| Required fields | Native `required` + client validation messages from feature flows |
| Dense multi-step | Campaign builder stepper + panel sections |

Core form keyboard order is documented under item **608**.

## Tables and worklists

| Pattern | Style notes |
| --- | --- |
| Full-width tables | Collapsed borders, light row separators |
| Header cells | Uppercase, muted slate, optional sticky |
| Numeric columns | Right-aligned, tabular nums on dashboards |
| Selected row | Highlight class (`.selected-row` / `.selected-table-row`) |
| Captions | Prefer `sr-only` captions or `aria-label` / `aria-labelledby` |
| Empty lists | Message + short guidance (not a blank white void) |

## Feedback components

| Component | Role | Style intent |
| --- | --- | --- |
| `EmptyState` | `status` | Calm empty guidance, optional compact mode for nav |
| `ErrorState` / `.form-error` | `alert` | Clear failure without stack traces |
| `SuccessNotification` | `status` | Short confirmation after create/update |
| `FormValidationMessage` | field error | Inline, associated via `id` / `aria-describedby` where used |
| `ConfirmationDialog` | modal dialog | Focus trap, Escape cancel, primary vs danger confirm |

Sensitive actions (approve, launch, disable user, role change) **must** use confirmation styling, not a silent click.

## Status and domain badges

| Component | Domain |
| --- | --- |
| `StatusBadge` | Generic tokenized status text |
| `CampaignStatusBadge` | DRAFT → ARCHIVED lifecycle |
| `CustomerStatusBadge` | ACTIVE, INTERESTED, CONVERTED, … |
| `ConsentStatusBadge` | GIVEN, WITHDRAWN, REQUIRED, … |
| `ReminderLevelBadge` | GREEN / YELLOW / RED |
| `AuditActionBadge` | CREATE, APPROVE, LAUNCH, … |

Badges are **read-only** visual signals; they do not replace table columns of full values.

## Dashboard and analytics

| Element | Style notes |
| --- | --- |
| KPI metric cards | Top accent stripe by group (inventory / engagement / financial) |
| Metric grids | Responsive 1–4 columns |
| Chart panels | Recharts inside labeled `role="img"` frames |
| Chart colors | Shared `CHART_COLORS` for sent, open, click, conversion series |
| Section hints | Short secondary paragraphs under each chart title |

Prefer scannable KPI groups (item **591**) over a single wall of numbers.

## Campaign builder and compliance clarity

| Screen | Style emphasis |
| --- | --- |
| Campaign builder | Stepper nav, step panels, live summary sidebar |
| Compliance review | Queue table, checklist, separated approve vs reject decision panels |
| Recipient preview | Tabbed eligible/excluded, exclusion summary panel |

These screens prioritize **workflow clarity** over decorative chrome (items **592–594**).

## Login

| Element | Style notes |
| --- | --- |
| Split layout | Brand hero + sign-in panel |
| Brand mark | **BW** square mark |
| Form | Email / password, primary Sign in button |
| Errors | Inline field errors + root `role="alert"` |

Login is outside the app shell; it still uses the same type, colors, and focus rules.

## Do / don’t

| Do | Don’t |
| --- | --- |
| Reuse `.panel`, `.button-row`, badge components | Invent one-off card chrome per page |
| Use primary blue only for the main action | Use danger red for ordinary saves |
| Keep table headers muted and uppercase | Mix random font sizes in worklists |
| Show empty and error states with guidance | Leave spinners forever or blank failures |
| Keep focus-visible outlines | Remove outlines for “cleaner” demos |
| Align copy with feature flow constants | Hard-code divergent success strings |

## Evidence for reports

Recommended screenshots for item **613** / Sprint 15 evidence are catalogued in
[Core Workflow Screenshots](../testing/core-workflow-screenshots.md) (files under
`docs/evidence/core-workflows/`). At minimum capture:

1. Application shell (sidebar + dashboard)
2. Role-filtered navigation (e.g. BI vs Admin)
3. Form validation state
4. Confirmation dialog
5. Status badges on a worklist
6. Responsive tablet or narrow width
7. Focus ring on a primary control

## Acceptance (item 610)

**UI style notes** are complete when:

1. This document exists under `docs/development/ui-style-notes.md`.
2. It covers typography, color, shell, forms, tables, badges, feedback, responsive breakpoints, and do/don’t rules.
3. Token catalog in `uiStyleNotes.ts` matches documented hex values and breakpoints.
4. Unit tests assert documentation presence, index links, and token consistency with `styles.css`.
5. Notes reference Sprint 15 professionalization goals (items **569–594**, **608–609**).
