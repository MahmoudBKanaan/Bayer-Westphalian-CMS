# Production Admin Create-User Verification

**Sprint 18 item 746** requires an authorized Admin to create an employee user after deployment.

## Current Execution

Item 746 is **BLOCKED** because no production backend or HTTPS deployment is running. The production
login and users APIs are unreachable, so no Admin credential was requested and no synthetic user was
created. Development database access or direct SQL would not prove the authorized workflow.

## Safe Verification

Run after items 744 and 745 pass for the same release and environment:

```powershell
$adminCredential = Get-Credential -Message "Approved production smoke administrator"
.\scripts\test-production-admin-create-user.ps1 `
  -BaseUrl https://campaign.example.com `
  -AdminCredential $adminCredential
$adminCredential = $null
```

The verifier authenticates an active ADMIN, generates a unique `@example.invalid` user and random
password in memory, requires `POST /api/users` to return HTTP 201 and a valid UUID, reads the same
record through `GET /api/users/{id}`, and disables it through the audited
`PATCH /api/users/{id}/disable` workflow. Failure cleanup also attempts disable, because employee
accounts are retained and disabled rather than hard-deleted.

The script prints no administrator/synthetic email, password, token, or API response. Record only
UTC time, release SHA/image digests, environment, operator, sanitized pass categories, created-user
UUID in the access-controlled evidence record when required for audit lookup, cleanup status, request
IDs, and approver. Never capture the credential prompt.

## Acceptance

Item 746 passes only when:

1. Items 744 and 745 passed for the same deployed release.
2. An active ADMIN receives HTTP 201 from the server-authorized create endpoint.
3. The response contains a valid UUID, expected synthetic identity, `ACTIVE` status, and no password.
4. A separate authenticated read returns the same user.
5. The user-creation audit event can be reviewed under the approved smoke/audit process.
6. The synthetic account is `DISABLED` after verification and its disable action is auditable.

A mocked test, direct database insert, frontend button visibility, or leftover seeded account does not
prove this acceptance criterion.

Related documentation: [Admin Login Verification](admin-login-verification.md),
[User-Management Guide](../admin/user-management-guide.md),
[Audit Logging](../modules/audit-logging.md), and
[Production Smoke Checklist](production-smoke-test-checklist.md).
