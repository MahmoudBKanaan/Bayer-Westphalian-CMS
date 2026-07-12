# UI Login Flow

Employee sign-in through the React UI for the Bayer-Westphalian Campaign Management Platform
(KB **FR-001**, **NFR-001**, authentication design, Sprint 15 item **598**).

## Acceptance (item 598)

**Login flow works through UI** when:

1. Unauthenticated users can open `/login` and see the employee sign-in form.
2. Protected routes redirect to `/login` with an auth-required notice and preserve the target path.
3. Client-side validation blocks invalid email / short passwords before `POST /api/auth/login`.
4. Valid credentials create a `sessionStorage` session and open the protected shell.
5. Failed credentials and rate-limited logins show safe messages (no stack traces).
6. Successful login after a deep-link redirect returns to the originally requested route.
7. Disabled (or locked) accounts cannot log in (Sprint 16 critical item **659** —
   `DisabledUserCannotLogInTests`; UI uses the same safe failure copy as invalid credentials —
   `frontend/src/features/auth/disabledUserCannotLogIn.ts`).

## Implementation map

| Layer | Path |
| --- | --- |
| Pure flow rules | `frontend/src/features/auth/loginFlow.ts` |
| Unit tests | `frontend/src/features/auth/loginFlow.test.ts` |
| Login page | `frontend/src/pages/LoginPage.tsx` (+ `LoginPage.test.tsx`) |
| Auth session | `frontend/src/auth/AuthProvider.tsx`, `sessionStorageStrategy.ts` |
| Route guard | `frontend/src/auth/ProtectedRoute.tsx` |
| Integration (router) | `frontend/src/test/integration/loginFlow.integration.test.tsx` |
| Playwright UI | `frontend/tests/e2e/login-flow.spec.ts` |

## Journey

```text
Open sign-in → Enter credentials → Submit sign-in → Land on protected UI
```

Default landing: **`/dashboard`**. Deep link: `state.from.pathname` from `ProtectedRoute`.

## Running tests

```bash
cd frontend
# Unit + page + integration (do not run when backlog says “do not run any tests”)
npm test -- --run src/features/auth/loginFlow.test.ts
npm test -- --run src/pages/LoginPage.test.tsx
npm test -- --run src/test/integration/loginFlow.integration.test.tsx

# Browser UI
npx playwright test tests/e2e/login-flow.spec.ts
```

## Related backlog

| Item | Topic |
| --- | --- |
| **597** | Playwright multi-step happy-path E2E |
| **598** | Login flow works through UI (this document) |
| **599** | Customer creation works through UI — see [ui-customer-creation.md](ui-customer-creation.md) |
| **600** | Consent update works through UI — see [ui-consent-update.md](ui-consent-update.md) |
| **601+** | Other UI workflow acceptance scenarios |
| **544** | Login lockout / rate limiting (backend + 429 UI message) |
