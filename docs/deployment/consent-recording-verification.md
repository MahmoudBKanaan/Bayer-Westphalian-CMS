# Production Consent Recording Verification

**Sprint 18 item 748** requires consent to be recorded through the authorized deployed workflow.

## Current Execution

Item 748 is **BLOCKED** because no production backend or HTTPS deployment is running. No role
credential was requested, and no customer or consent record was created. Direct SQL, mocked service
tests, or UI visibility alone do not prove deployed consent recording.

## Safe Verification

Run after items 744, 745, and 747 pass for the same release:

```powershell
$agentCredential = Get-Credential -Message "Approved Customer Service Agent smoke account"
$adminCredential = Get-Credential -Message "Approved Admin cleanup account"
.\scripts\test-production-record-consent.ps1 `
  -BaseUrl https://campaign.example.com `
  -AgentCredential $agentCredential `
  -AdminCleanupCredential $adminCredential
```

The verifier creates an `INACTIVE`, do-not-contact `@example.invalid` prospect, then uses the
Customer Service Agent consent-write permission to record synthetic `MARKETING_EMAIL` consent. It
requires HTTP 201, a valid consent UUID, `GIVEN`/valid state, matching customer and type, and read-back
from `/api/consents/status`. It immediately withdraws the consent and requires `WITHDRAWN`, a
withdrawal timestamp, and invalid state. Admin then soft-deletes the synthetic customer. Both cleanup
steps are retried in `finally` after failure.

No consent evidence file is uploaded in this check; evidence storage has separate readiness and
workflow verification. The script prints no credential, token, account email, customer identity,
consent payload, or response. Record sanitized outcomes, UUID references only in access-controlled
audit evidence, request IDs, release SHA/image digests, cleanup state, operator, and approver.

## Acceptance

Item 748 passes only when an authorized consent-write role records consent through `POST /api/consents`,
the server assigns a valid UUID and persists current status, immutable consent-creation audit evidence
exists, withdrawal succeeds and is audited, and the synthetic customer is soft-deleted. The
do-not-contact customer must remain ineligible for communication regardless of recorded consent.

Related documentation: [Agent Create-Customer Verification](agent-create-customer-verification.md),
[Consent Evidence Storage](consent-evidence-storage.md), [Audit Logging](../modules/audit-logging.md),
and [Production Smoke Checklist](production-smoke-test-checklist.md).
