# Production Contact-Event Recording Verification

**Sprint 18 item 754** requires a contact event to be recorded and visible in customer history.

## Current Execution

Item 754 is **BLOCKED** because no production backend or HTTPS deployment is running. No Agent or
Admin cleanup credential was requested, and no customer/contact event was created. Database inserts,
mocked tests, provider logs, or a UI-only timeline do not prove the deployed write/read workflow.

## Safe Verification

Run after items 744 and 747 pass:

```powershell
$agentCredential = Get-Credential -Message "Approved Customer Service Agent smoke account"
$adminCredential = Get-Credential -Message "Approved Admin cleanup account"
.\scripts\test-production-record-contact-event.ps1 `
  -BaseUrl https://campaign.example.com `
  -AgentCredential $agentCredential `
  -AdminCleanupCredential $adminCredential
```

Customer Service Agent creates an inactive, do-not-contact synthetic prospect and records a
provider-free `PHONE`/`NOTE` event with no campaign. The verifier requires HTTP 201, a valid event
UUID, matching customer and Agent creator, expected safe type/channel, and exactly one matching event
from the filtered customer timeline. Admin then soft-deletes the synthetic customer. The immutable
event is retained for contact-history evidence; there is no event-delete endpoint.

This test does not call an email/SMS provider, create a SENT event, or claim that communication
occurred. The script prints no credential, token, account/customer identity, notes, payload, or
response. Record event/customer UUID references only in controlled evidence, plus UTC time, release
SHA/image digests, request IDs, cleanup state, operators, and approver.

## Acceptance

Item 754 passes only when an authorized contact-write role receives HTTP 201, the server assigns a
valid event UUID and creator, the event is returned once by the customer timeline, and the synthetic
customer is soft-deleted without deleting immutable history. A provider delivery record is not
required for this manual `NOTE` acceptance check.

Related documentation: [Agent Create-Customer Verification](agent-create-customer-verification.md),
[Campaign Lifecycle](../modules/campaign-lifecycle.md),
[Production Logging](production-logging.md), and
[Production Smoke Checklist](production-smoke-test-checklist.md).
