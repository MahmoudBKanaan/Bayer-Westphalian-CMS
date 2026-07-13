# Production Campaign Manager Create-Segment Verification

**Sprint 18 item 750** requires an authorized Campaign Manager to create a segment after deployment.

## Current Execution

Item 750 is **BLOCKED** because no production backend or HTTPS deployment is running. No Campaign
Manager credential was requested, and no segment was created. Admin activity, direct SQL, mocked
tests, or a visible create panel do not prove the role-specific deployed workflow.

## Safe Verification

Run after item 744 passes with a dedicated approved Campaign Manager smoke account:

```powershell
$campaignManagerCredential = Get-Credential -Message "Approved Campaign Manager smoke account"
.\scripts\test-production-campaign-manager-create-segment.ps1 `
  -BaseUrl https://campaign.example.com `
  -CampaignManagerCredential $campaignManagerCredential
$campaignManagerCredential = $null
```

The verifier authenticates an active `CAMPAIGN_MANAGER` and creates a uniquely named `PRIVATE`
segment with no criteria. It does not preview or enumerate customers. It requires HTTP 201, a valid
automatically generated segment UUID, matching authenticated owner ID, private visibility, and an
empty criteria list. It reads the segment from `GET /api/segments/{id}`, then deletes it using the
same Campaign Manager role. Failure cleanup retries deletion in `finally`.

The script prints no credential, token, account email, segment name, request body, response, or
customer data. Record sanitized outcomes, release SHA/image digests, environment, segment UUID only
in access-controlled evidence, request IDs, deletion result, operator, and approver.

## Acceptance

Item 750 passes only when an active `CAMPAIGN_MANAGER`, not Admin, receives HTTP 201, the server
assigns a valid-format UUID automatically, the details read returns the same ID/name/owner and makes
the UUID available to the segment UI/API contract, and the same role deletes the synthetic segment.
No customer preview is needed to prove creation. Creation/deletion operational or audit evidence must
be retained where the application records it.

Related documentation: [Campaign Manager Guide](../user-guides/campaign-manager-guide.md),
[Segment Module](../modules/segmentation-module.md), [Production Smoke Checklist](production-smoke-test-checklist.md),
and [Incident Response](incident-response-notes.md).
