# Production Analytics Dashboard Update Verification

**Sprint 18 item 755** requires analytics dashboards to update after underlying campaign state changes.

## Current Execution

Item 755 is **BLOCKED** because no production backend or HTTPS deployment is running. No credentials
were requested, no campaign lifecycle ran, and no dashboard was queried. Static UI rendering or mocked
analytics responses do not prove deployed aggregation updates.

## Safe Verification And Acceptance

Run the provider-disabled verifier with approved Campaign Manager and Compliance Officer accounts:

```powershell
.\scripts\test-production-analytics-dashboard-updates.ps1 `
  -BaseUrl https://campaign.example.com `
  -CampaignManagerCredential $campaignManagerCredential `
  -ComplianceCredential $complianceCredential `
  -ProviderSendingConfirmedDisabled
```

The verifier records dashboard/executive baselines, creates one targetless campaign, and requires
campaign total `+1`. After separate human approval it confirms zero recipient rows, launches, and
requires active campaigns `+1` while audience and messages sent remain unchanged. Campaign analytics
must show `ACTIVE` with zero audience/sent metrics. After completion, active returns to baseline and
executive completed campaigns increases by one; the campaign is then archived.

Any recipient row, provider-enabled uncertainty, unexpected sent/audience increase, stale status,
missing metrics, or wrong counter delta fails closed. Record only before/after aggregate values,
synthetic campaign UUID, release SHA/image digests, UTC time, cleanup state, and approvers; never
capture credentials, tokens, raw customers, recipient rows, or message content.

Related documentation: [Analytics Module](../modules/analytics-module.md),
[Launch Verification](campaign-manager-launch-approved-verification.md),
[Operational Monitoring](operational-monitoring-notes.md), and
[Production Smoke Checklist](production-smoke-test-checklist.md).
