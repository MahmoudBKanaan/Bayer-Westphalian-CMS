# UI Customer Creation

Customer/prospect creation through the React UI for the Bayer-Westphalian Campaign Management
Platform (KB **FR-011**, **NFR-005**, Sprint 15 item **599**).

## Acceptance (item 599)

**Customer creation works through UI** when:

1. Authorized roles (`ADMIN`, `CUSTOMER_SERVICE_AGENT`) open `/customers` and see **Create customer**.
2. Read-only customer roles (e.g. campaign manager) can list/view but not create.
3. The create form validates required name fields and optional email/phone formats client-side.
4. Valid submit calls `POST /api/customers` and shows **Customer created.**
5. The new customer appears in the customer list (and can be opened for details/consent later).

## Journey

```text
Open Customers → Fill create form → Submit create → See created customer
```

## Implementation map

| Layer | Path |
| --- | --- |
| Pure flow rules | `frontend/src/features/customers/customerCreationFlow.ts` |
| Unit tests | `frontend/src/features/customers/customerCreationFlow.test.ts` |
| Customers page | `frontend/src/pages/CustomersPage.tsx` (+ `CustomersPage.test.tsx`) |
| Integration | `frontend/src/test/integration/customerCreation.integration.test.tsx` |
| Playwright UI | `frontend/tests/e2e/customer-creation.spec.ts` |

## Running tests

```bash
cd frontend
npm test -- --run src/features/customers/customerCreationFlow.test.ts
npm test -- --run src/pages/CustomersPage.test.tsx
npm test -- --run src/test/integration/customerCreation.integration.test.tsx
npx playwright test tests/e2e/customer-creation.spec.ts
```

Do **not** run these when a backlog item says “do not run any tests” unless execution is explicitly requested.

## Related backlog

| Item | Topic |
| --- | --- |
| **598** | Login flow works through UI |
| **599** | Customer creation works through UI (this document) |
| **600** | Consent update works through UI — see [ui-consent-update.md](ui-consent-update.md) |
| **601** | Product creation works through UI — see [ui-product-creation.md](ui-product-creation.md) |
| **597** | Playwright multi-step happy-path E2E |
