# Production Customer Service Agent Create-Customer Verification

**Sprint 18 item 747** requires an authorized Customer Service Agent to create a customer after
deployment.

## Current Execution

Item 747 is **BLOCKED** because no production backend or HTTPS deployment is running. No Customer
Service Agent or Admin cleanup credential was requested, and no customer was created. A development
database insert, Admin-created customer, or mocked request does not prove this role-specific workflow.

## Safe Verification

Run after items 744 and 745 pass, using separate approved role credentials:

```powershell
$agentCredential = Get-Credential -Message "Approved Customer Service Agent smoke account"
$adminCredential = Get-Credential -Message "Approved Admin cleanup account"
.\scripts\test-production-agent-create-customer.ps1 `
  -BaseUrl https://campaign.example.com `
  -AgentCredential $agentCredential `
  -AdminCleanupCredential $adminCredential
$agentCredential = $null
$adminCredential = $null
```

The Agent creates a unique `@example.invalid` `PROSPECT` through `POST /api/customers`. The synthetic
record starts `INACTIVE` with `doNotContact=true`, so it cannot enter communication workflows. The
verifier requires HTTP 201, a valid UUID, exact safety fields, and a separate Agent-authenticated
read. Because Customer Service Agent correctly lacks delete permission, a separately authenticated
Admin performs audited soft-delete cleanup in `finally`.

The script prints no account email, credential, token, synthetic customer identity, or API payload.
Record only UTC time, release SHA/image digests, environment, operator roles, sanitized pass output,
customer UUID when needed in access-controlled audit evidence, cleanup result, request IDs, and
approver. Never capture either credential prompt.

## Acceptance

Item 747 passes only when the same deployed release proves:

1. An active `CUSTOMER_SERVICE_AGENT`, not Admin, receives HTTP 201 from `POST /api/customers`.
2. The server assigns a valid UUID and persists the synthetic non-contactable prospect.
3. The Agent can read the created customer through the authorized API.
4. Creation appears in the expected audit/operational evidence.
5. Customer Service Agent still cannot use Admin-only delete behavior.
6. An authorized Admin soft-deletes the synthetic customer and cleanup is auditable.

Related documentation: [Admin Login Verification](admin-login-verification.md),
[Admin Create-User Verification](admin-create-user-verification.md),
[Customer Service Agent Guide](../user-guides/customer-service-agent-guide.md), and
[Production Smoke Checklist](production-smoke-test-checklist.md).
