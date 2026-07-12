# UI Segment Creation

Reusable audience segment creation through the React UI for the Bayer-Westphalian Campaign
Management Platform (KB **FR-077**, item **201**, Sprint 15 item **602**).

## Acceptance (item 602)

**Segment creation works through UI** when:

1. Authorized roles (`ADMIN`, `CAMPAIGN_MANAGER`) open `/segments` and see **Create segment**.
2. Read-only roles (e.g. BI Analyst) can list/preview insights but not create.
3. The create form validates name (required) and partially filled criteria rows.
4. Valid submit calls `POST /api/segments` and shows **Segment created.**
5. The new segment appears in the saved segments table for campaign reuse.

## Journey

```text
Open Segments → Fill create form → Submit create → See created segment
```

## Implementation map

| Layer | Path |
| --- | --- |
| Pure flow rules | `frontend/src/features/segments/segmentCreationFlow.ts` |
| Unit tests | `frontend/src/features/segments/segmentCreationFlow.test.ts` |
| Segments page | `frontend/src/pages/SegmentsPage.tsx` (+ `SegmentsPage.test.tsx`) |
| Integration | `frontend/src/test/integration/segmentCreation.integration.test.tsx` |
| Playwright UI | `frontend/tests/e2e/segment-creation.spec.ts` |

## Running tests

```bash
cd frontend
npm test -- --run src/features/segments/segmentCreationFlow.test.ts
npm test -- --run src/pages/SegmentsPage.test.tsx
npm test -- --run src/test/integration/segmentCreation.integration.test.tsx
npx playwright test tests/e2e/segment-creation.spec.ts
```

Do **not** run these when a backlog item says “do not run any tests” unless execution is explicitly requested.

## Related backlog

| Item | Topic |
| --- | --- |
| **601** | Product creation works through UI |
| **602** | Segment creation works through UI (this document) |
| **603** | Campaign creation works through UI — see [ui-campaign-creation.md](ui-campaign-creation.md) |
