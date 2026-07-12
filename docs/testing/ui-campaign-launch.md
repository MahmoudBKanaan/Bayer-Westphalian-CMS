# UI Campaign Launch

Launching approved campaigns through the Recipient Preview UI for the Bayer-Westphalian
Campaign Management Platform (KB **FR-054–055**, **BR-005**, Sprint 15 item **605**).

## Acceptance (item 605)

**Campaign launch works through UI** when:

1. Authorized roles (`ADMIN`, `CAMPAIGN_MANAGER`) open `/campaigns/:id/recipients/preview`.
2. Launch readiness reflects status; only **APPROVED** campaigns enable **Launch campaign**.
3. Launch opens a confirmation dialog with eligible/excluded counts.
4. Confirm calls `POST /api/campaigns/{id}/launch` and moves the campaign to **ACTIVE**.
5. Success shows **Campaign launched.** and the **Launch result** panel.
6. Non-managers do not get a launch control; non-approved status keeps launch disabled.

## Journey

```text
Open recipient preview → Confirm launch readiness → Confirm launch dialog → See launch result
```

## Implementation map

| Layer | Path |
| --- | --- |
| UI acceptance contract | `frontend/src/features/campaigns/campaignLaunchFlow.ts` |
| Readiness helpers | `frontend/src/features/campaigns/recipientPreviewClarity.ts` |
| Unit tests | `frontend/src/features/campaigns/campaignLaunchFlow.test.ts` |
| Recipient preview page | `frontend/src/pages/CampaignRecipientPreviewPage.tsx` (+ tests) |
| Integration | `frontend/src/test/integration/campaignLaunch.integration.test.tsx` |
| Playwright UI | `frontend/tests/e2e/campaign-launch.spec.ts` |

## Running tests

```bash
cd frontend
npm test -- --run src/features/campaigns/campaignLaunchFlow.test.ts
npm test -- --run src/pages/CampaignRecipientPreviewPage.test.tsx
npm test -- --run src/test/integration/campaignLaunch.integration.test.tsx
npx playwright test tests/e2e/campaign-launch.spec.ts
```

Do **not** run these when a backlog item says “do not run any tests” unless execution is explicitly requested.

## Related backlog

| Item | Topic |
| --- | --- |
| **594** | Improve recipient preview clarity |
| **604** | Compliance approval works through UI |
| **605** | Campaign launch works through UI (this document) |
| **606** | Dashboard loads analytics — see [ui-dashboard-analytics.md](ui-dashboard-analytics.md) |
