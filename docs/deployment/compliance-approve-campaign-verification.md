# Production Compliance Officer Approve-Campaign Verification

**Sprint 18 item 752** requires an authorized Compliance Officer to approve a submitted campaign.

## Current Execution

Item 752 is **BLOCKED** because no production backend or HTTPS deployment is running. No Campaign
Manager or Compliance Officer credential was requested, and no campaign was approved. Admin
approval, direct SQL, mocked tests, or frontend controls do not prove this role-specific human gate.

## Safe Verification

Run after items 744 and 751 pass with separate approved accounts:

```powershell
$campaignManagerCredential = Get-Credential -Message "Approved Campaign Manager smoke account"
$complianceCredential = Get-Credential -Message "Approved Compliance Officer smoke account"
.\scripts\test-production-compliance-approve-campaign.ps1 `
  -BaseUrl https://campaign.example.com `
  -CampaignManagerCredential $campaignManagerCredential `
  -ComplianceCredential $complianceCredential
```

Campaign Manager creates and submits a uniquely named targetless, unscheduled synthetic campaign.
A separately authenticated `COMPLIANCE_OFFICER` approves it with explicit review notes. The verifier
requires persisted `APPROVED` status, approval timestamp, `approvedByUserId` matching the human
Compliance Officer, a successful details read, and an immutable entity-history `APPROVE` audit event
with the same actor.

There is no safe API transition from `APPROVED` back to rejected/archived without launch. Therefore
the inert approved campaign is intentionally retained and its synthetic UUID is printed for controlled
evidence or a separately approved follow-on lifecycle test. It has no segment, products, schedule,
or recipients and must never be launched casually. The verifier never previews, launches, or sends.

## Acceptance

Item 752 passes only when a real human using the `COMPLIANCE_OFFICER` role, not Admin or AI, approves
the submitted synthetic campaign; persisted approver identity, timestamp, notes, and `APPROVED` state
match; and the immutable `APPROVE` audit event names the same actor. Human release/smoke approval is
still required before any follow-on launch.

Record the synthetic campaign UUID, UTC time, release SHA/image digests, environment, request/audit
IDs, sanitized outcomes, Manager/Compliance operators, and approver in controlled evidence. Never
capture credentials, JWTs, campaign message bodies, or full audit payloads.

Related documentation: [Campaign Lifecycle](../modules/campaign-lifecycle.md),
[Campaign Audit Logging](../modules/campaign-audit-logging.md),
[Campaign Manager Create-Campaign Verification](campaign-manager-create-campaign-verification.md),
and [Production Smoke Checklist](production-smoke-test-checklist.md).
