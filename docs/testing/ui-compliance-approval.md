# UI Compliance Approval

Compliance approval of submitted campaigns through the React UI for the Bayer-Westphalian
Campaign Management Platform (KB **FR-059**, **BR-005**, **COMP-006**, Sprint 15 item **604**).

## Acceptance (item 604)

**Compliance approval works through UI** when:

1. Authorized roles (`ADMIN`, `COMPLIANCE_OFFICER`) open `/compliance`.
2. The queue lists only **SUBMITTED** campaigns.
3. Reviewers can inspect checklist evidence, message, and recipient summary.
4. **Approve campaign** opens a confirmation dialog (optional review notes).
5. Confirm calls `POST /api/campaigns/{id}/approve` and shows **Campaign approved.**
6. Unauthorized roles cannot complete review actions.
7. Reject still requires a formal reason (related validation; full reject path also covered in page tests).

## Journey

```text
Open Compliance review → Select submitted campaign → Confirm approval → See approved result
```

## Implementation map

| Layer | Path |
| --- | --- |
| UI acceptance contract | `frontend/src/features/campaigns/complianceApprovalFlow.ts` |
| Checklist / decision copy | `frontend/src/features/campaigns/complianceReviewClarity.ts` |
| Unit tests | `frontend/src/features/campaigns/complianceApprovalFlow.test.ts` |
| Compliance page | `frontend/src/pages/CompliancePage.tsx` (+ tests) |
| Integration | `frontend/src/test/integration/complianceApproval.integration.test.tsx` |
| Playwright UI | `frontend/tests/e2e/compliance-approval.spec.ts` |

## Running tests

```bash
cd frontend
npm test -- --run src/features/campaigns/complianceApprovalFlow.test.ts
npm test -- --run src/pages/CompliancePage.test.tsx
npm test -- --run src/test/integration/complianceApproval.integration.test.tsx
npx playwright test tests/e2e/compliance-approval.spec.ts
```

Do **not** run these when a backlog item says “do not run any tests” unless execution is explicitly requested.

## Related backlog

| Item | Topic |
| --- | --- |
| **593** | Improve compliance review clarity |
| **603** | Campaign creation works through UI |
| **604** | Compliance approval works through UI (this document) |
| **605** | Campaign launch works through UI — see [ui-campaign-launch.md](ui-campaign-launch.md) |
