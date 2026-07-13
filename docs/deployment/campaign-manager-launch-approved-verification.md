# Production Campaign Manager Launch-Approved Verification

**Sprint 18 item 753** requires a Campaign Manager to launch a human-approved campaign.

## Current Execution

Item 753 is **BLOCKED** because no production backend or HTTPS deployment is running. No Manager or
Compliance credential was requested, and no campaign was launched. Status manipulation, direct SQL,
mocked tests, or launching an unverified campaign cannot satisfy this production acceptance check.

## Safety-Critical Verification

This verifier must run only with explicit release/smoke approval and real provider sending disabled:

```powershell
$campaignManagerCredential = Get-Credential -Message "Approved Campaign Manager smoke account"
$complianceCredential = Get-Credential -Message "Approved Compliance Officer smoke account"
.\scripts\test-production-campaign-manager-launch-approved.ps1 `
  -BaseUrl https://campaign.example.com `
  -CampaignManagerCredential $campaignManagerCredential `
  -ComplianceCredential $complianceCredential `
  -ProviderSendingConfirmedDisabled
```

The script creates a new targetless campaign with no segment, products, schedule, or recipient rows.
The confirmation switch is mandatory and must be backed by approved deployment evidence showing real
email/SMS sending is disabled; the application does not expose provider secrets/configuration publicly.
Campaign Manager submits it and a separate human Compliance Officer approves it. Immediately before
launch, both eligible and excluded recipient APIs must return zero rows; any row blocks launch. The
Campaign Manager then launches, requiring `ACTIVE`, verifies recipient count remains zero, and
requires an immutable `LAUNCH` audit event by that Manager. Finally, the Manager completes and archives
the synthetic campaign. If failure occurs while active, `finally` retries complete/archive cleanup.

Launch implementation creates SENT contact history for stored eligible recipients. Zero recipient
rows are therefore a mandatory fail-closed precondition, not merely test data preference. Do not
adapt this verifier to a real segment, customer, provider destination, or previously approved campaign.

## Acceptance

Item 753 passes only when item 752-style human approval occurs first, the approved campaign has zero
recipient rows, the owning `CAMPAIGN_MANAGER` changes status to `ACTIVE`, no recipients/contact events
are generated, the `LAUNCH` audit event names that Manager, and lifecycle cleanup reaches `ARCHIVED`.
Provider mode and sanitized logs must also confirm no delivery attempt.

Record UTC time, release SHA/image digests, environment, synthetic campaign UUID, approval/launch
audit IDs, zero-recipient evidence, provider-disabled evidence, cleanup status, operators, and human
approver. Never capture credentials, JWTs, messages, or full audit payloads.

Related documentation: [Compliance Approval Verification](compliance-approve-campaign-verification.md),
[Campaign Launch](../modules/campaign-launch.md), [Campaign Audit Logging](../modules/campaign-audit-logging.md),
and [Production Smoke Checklist](production-smoke-test-checklist.md).
