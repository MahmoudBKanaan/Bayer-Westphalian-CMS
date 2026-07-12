# Frontend

React, TypeScript, and Vite client for the Bayer-Westphalian Campaign Management Platform.

## Commands

```bash
npm install
npm run dev
npm run build
npm run lint
npm run lint:fix
npm run format
npm run format:check
npm run test
npm run test:e2e
npm run verify
```

Playwright happy-path E2E (item **597**): `npm run test:e2e` — see `tests/e2e/` and
`docs/testing/playwright-e2e.md`. Unit contracts for the E2E journey live under
`src/features/e2e/` and run with `npm test`.

## Formatting and Linting

ESLint is configured in `eslint.config.js` for React, TypeScript, React Hooks, and Vite React Fast Refresh rules.

Prettier is configured in `.prettierrc.json` for frontend formatting. `eslint-config-prettier` is included so ESLint focuses on code-quality rules while Prettier owns formatting.

Generated folders such as `dist/`, `coverage/`, `playwright-report/`, `test-results/`, and `node_modules/` are ignored by the lint baseline.

Use `npm run verify` before opening a pull request. It runs linting, formatting checks, unit tests, and the production build.

## UI style notes

Professional visual language (shell, colors, badges, forms, responsive breakpoints) is documented in
`docs/development/ui-style-notes.md` (Sprint 15 item **610**). Token catalog and documentation locks
live under `src/features/ui/uiStyleNotes.ts`.

## Accessibility notes

Landmarks, keyboard, labels, contrast, live regions, and a11y testing map are documented in
`docs/development/accessibility-notes.md` (Sprint 15 item **611**). Catalog locks live under
`src/features/a11y/accessibilityNotes.ts` (with **608** / **609** implementation modules).

## Frontend testing notes

Test pyramid, Vitest/Playwright conventions, flow-module pattern, and suite index are documented in
`docs/testing/frontend-testing-notes.md` (Sprint 15 item **612**). Catalog locks live under
`src/features/testing/frontendTestingNotes.ts`.

## Core workflow screenshots

Report/demo screenshot inventory (happy path + shell UX) is documented in
`docs/testing/core-workflow-screenshots.md` (Sprint 15 item **613**). Place PNG captures under
`docs/evidence/core-workflows/`. Catalog locks live under
`src/features/testing/coreWorkflowScreenshots.ts`.

## Sprint 15 production gate

Business users complete core workflows without developer help (item **616**): see
`docs/agile/sprint-15-production-gate.md`. Catalog locks live under
`src/features/readiness/businessUserWorkflowGate.ts`.

## Structure

```text
src/
+-- app/
+-- api/
+-- components/
+-- features/
+-- pages/
+-- types/
+-- utils/
```

## TypeScript Path Alias

Use `@/*` for imports from `src/*`.

Examples:

```ts
import { AppLayout } from "@/components/AppLayout";
import type { Campaign } from "@/types/domain";
```

The alias is configured in both `tsconfig.app.json` and `vite.config.ts` so TypeScript and Vite resolve imports consistently.

The frontend owns UI, routing, forms, dashboards, and client-side validation only. The backend remains the system of record for authorization, business rules, campaign eligibility, consent, audit, and data persistence.
