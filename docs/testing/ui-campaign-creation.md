# UI Campaign Creation

Campaign draft creation through the multi-step Campaign Builder UI for the Bayer-Westphalian
Campaign Management Platform (KB **FR-050**, **FR-057**, Sprint 15 item **603**).

## Acceptance (item 603)

**Campaign creation works through UI** when:

1. Authorized roles (`ADMIN`, `CAMPAIGN_MANAGER`) open `/campaign-builder`.
2. Unauthorized roles cannot create drafts.
3. The builder enforces per-step validation (basics → audience/product → message → schedule → review).
4. **Create draft** calls `POST /api/campaigns` (and product selection) and sets status **DRAFT**.
5. Success shows **Campaign draft created.** with a Draft status badge.

Optional follow-on (still on the builder): **Submit for review** moves the draft to compliance
(covered more fully by item **604**).

## Journey

```text
Open Campaign Builder → Complete builder steps → Create draft → See created draft
```

## Implementation map

| Layer | Path |
| --- | --- |
| UI acceptance contract | `frontend/src/features/campaigns/campaignCreationFlow.ts` |
| Builder steps / step validation | `frontend/src/features/campaigns/campaignBuilderFlow.ts` |
| Full form validation | `frontend/src/features/campaigns/campaignFormValidation.ts` |
| Unit tests | `frontend/src/features/campaigns/campaignCreationFlow.test.ts` |
| Builder page | `frontend/src/pages/CampaignBuilderPage.tsx` (+ tests) |
| Integration | `frontend/src/test/integration/campaignCreation.integration.test.tsx` |
| Playwright UI | `frontend/tests/e2e/campaign-creation.spec.ts` |

## Running tests

```bash
cd frontend
npm test -- --run src/features/campaigns/campaignCreationFlow.test.ts
npm test -- --run src/pages/CampaignBuilderPage.test.tsx
npm test -- --run src/test/integration/campaignCreation.integration.test.tsx
npx playwright test tests/e2e/campaign-creation.spec.ts
```

Do **not** run these when a backlog item says “do not run any tests” unless execution is explicitly requested.

## Related backlog

| Item | Topic |
| --- | --- |
| **592** | Improve campaign builder flow |
| **602** | Segment creation works through UI |
| **603** | Campaign creation works through UI (this document) |
| **604** | Compliance approval works through UI — see [ui-compliance-approval.md](ui-compliance-approval.md) |
