# UI Product Creation

Insurance and investment product creation through the React UI for the Bayer-Westphalian
Campaign Management Platform (KB product management, Sprint 15 item **601**).

## Acceptance (item 601)

**Product creation works through UI** when:

1. Authorized roles (`ADMIN`, `PRODUCT_MANAGER`) open `/products` and see **Create product**.
2. Read-only product roles (e.g. campaign manager) can list/view but not create.
3. The create form validates name (required), price, and duration client-side.
4. Valid submit calls `POST /api/products` and shows **Product created.**
5. The new product appears in the product catalog table.

## Journey

```text
Open Products → Fill create form → Submit create → See created product
```

## Implementation map

| Layer | Path |
| --- | --- |
| Pure flow rules | `frontend/src/features/products/productCreationFlow.ts` |
| Unit tests | `frontend/src/features/products/productCreationFlow.test.ts` |
| Products page | `frontend/src/pages/ProductsPage.tsx` (+ `ProductsPage.test.tsx`) |
| Integration | `frontend/src/test/integration/productCreation.integration.test.tsx` |
| Playwright UI | `frontend/tests/e2e/product-creation.spec.ts` |

## Running tests

```bash
cd frontend
npm test -- --run src/features/products/productCreationFlow.test.ts
npm test -- --run src/pages/ProductsPage.test.tsx
npm test -- --run src/test/integration/productCreation.integration.test.tsx
npx playwright test tests/e2e/product-creation.spec.ts
```

Do **not** run these when a backlog item says “do not run any tests” unless execution is explicitly requested.

## Related backlog

| Item | Topic |
| --- | --- |
| **599** | Customer creation works through UI |
| **600** | Consent update works through UI |
| **601** | Product creation works through UI (this document) |
| **602** | Segment creation works through UI — see [ui-segment-creation.md](ui-segment-creation.md) |
