# Production Campaign Manager Create-Campaign Verification

**Sprint 18 item 751** requires an authorized Campaign Manager to create a campaign after deployment.

## Current Execution

Item 751 is **BLOCKED** because no production backend or HTTPS deployment is running. No Campaign
Manager or Compliance cleanup credential was requested, and no campaign was created. Admin activity,
direct SQL, mocked tests, or frontend form visibility do not prove this deployed role workflow.

## Safe Verification

Run after items 744 and 750 pass with approved Campaign Manager and Compliance Officer smoke accounts:

```powershell
$campaignManagerCredential = Get-Credential -Message "Approved Campaign Manager smoke account"
$complianceCredential = Get-Credential -Message "Approved Compliance Officer cleanup account"
.\scripts\test-production-campaign-manager-create-campaign.ps1 `
  -BaseUrl https://campaign.example.com `
  -CampaignManagerCredential $campaignManagerCredential `
  -ComplianceCleanupCredential $complianceCredential
```

The verifier creates a uniquely named targetless `EMAIL` draft with no segment, products, schedule,
or recipients. It requires HTTP 201, a valid UUID, `DRAFT` status, and ownership by the authenticated
`CAMPAIGN_MANAGER`, then reads the persisted draft. Campaigns have no delete endpoint, so cleanup uses
the controlled lifecycle: Manager submits, a separate human Compliance Officer rejects with explicit
smoke-cleanup reason, and Manager archives. The script never previews recipients, approves, launches,
pauses, completes, invokes providers, or sends communication.

If failure interrupts cleanup, the campaign remains non-approved; the script archives automatically
when already rejected and emits a clear manual cleanup warning otherwise. It prints no credentials,
tokens, account emails, campaign name/message, request payload, or response.

## Acceptance

Item 751 passes only when an active `CAMPAIGN_MANAGER`, not Admin, receives HTTP 201, the server
assigns a valid UUID and persists the owner plus `DRAFT` status, a separate details read succeeds,
and create audit evidence exists. The synthetic campaign must remain targetless and unscheduled.
Cleanup must preserve human separation: only Compliance rejects, and Campaign Manager archives; no
approval or launch is allowed.

Record sanitized outcomes, release SHA/image digests, environment, campaign UUID in controlled audit
evidence, lifecycle/audit IDs, cleanup status, operators, and approver.

Related documentation: [Campaign Manager Guide](../user-guides/campaign-manager-guide.md),
[Campaign Lifecycle](../modules/campaign-lifecycle.md),
[Campaign Audit Logging](../modules/campaign-audit-logging.md), and
[Production Smoke Checklist](production-smoke-test-checklist.md).
