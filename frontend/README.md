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
npm run verify
```

## Formatting and Linting

ESLint is configured in `eslint.config.js` for React, TypeScript, React Hooks, and Vite React Fast Refresh rules.

Prettier is configured in `.prettierrc.json` for frontend formatting. `eslint-config-prettier` is included so ESLint focuses on code-quality rules while Prettier owns formatting.

Generated folders such as `dist/`, `coverage/`, `playwright-report/`, `test-results/`, and `node_modules/` are ignored by the lint baseline.

Use `npm run verify` before opening a pull request. It runs linting, formatting checks, unit tests, and the production build.

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
