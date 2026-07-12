# UI Consent Update

Consent recording, opt-out, and withdrawal through the React UI for the Bayer-Westphalian
Campaign Management Platform (KB **FR-018**, **BR-004**, Sprint 15 item **600**).

## Acceptance (item 600)

**Consent update works through UI** when:

1. Authorized roles open customer details and see the **Consent** panel.
2. Record consent requires a purpose (client-side) before `POST /api/consents`.
3. Successful record shows **Consent recorded.** and refreshes the consent list.
4. Marketing opt-out records withdrawn marketing consent (**Marketing opt-out recorded.**).
5. Withdraw marks an existing consent withdrawn (**Consent withdrawn.**).
6. Empty customers show a clear empty state for consent records.

## Journey

```text
Open customer details → Open Consent panel → Submit consent change → See updated consent
```

## Implementation map

| Layer | Path |
| --- | --- |
| Pure flow rules | `frontend/src/features/customers/consentUpdateFlow.ts` |
| Unit tests | `frontend/src/features/customers/consentUpdateFlow.test.ts` |
| Customer details | `frontend/src/pages/CustomerDetailsPage.tsx` (+ tests) |
| Integration | `frontend/src/test/integration/consentUpdate.integration.test.tsx` |
| Playwright UI | `frontend/tests/e2e/consent-update.spec.ts` |

## Running tests

```bash
cd frontend
npm test -- --run src/features/customers/consentUpdateFlow.test.ts
npm test -- --run src/pages/CustomerDetailsPage.test.tsx
npm test -- --run src/test/integration/consentUpdate.integration.test.tsx
npx playwright test tests/e2e/consent-update.spec.ts
```

Do **not** run these when a backlog item says “do not run any tests” unless execution is explicitly requested.

## Related backlog

| Item | Topic |
| --- | --- |
| **598** | Login flow works through UI |
| **599** | Customer creation works through UI |
| **600** | Consent update works through UI (this document) |
| **601** | Product creation works through UI — see [ui-product-creation.md](ui-product-creation.md) |
