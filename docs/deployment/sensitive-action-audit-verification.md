# Production Sensitive-Action Audit Verification

**Sprint 18 item 756** requires sensitive actions to create immutable audit records.

## Current Execution

Item 756 is **BLOCKED** because no production backend or HTTPS deployment is running. No Admin
credential was requested, no sensitive action ran, and no audit history was queried. Existing seed
rows, logs, direct SQL, or mocked tests do not prove deployed transactional audit behavior.

## Verification And Acceptance

Run with the approved Admin smoke credential after item 745 passes:

```powershell
.\scripts\test-production-sensitive-action-audit.ps1 `
  -BaseUrl https://campaign.example.com `
  -AdminCredential $adminCredential
```

The verifier creates a uniquely named synthetic employee through the Admin API and requires exactly
one entity-history `CREATE` event whose actor, action, entity type, and entity UUID match. It serializes
only that audit response in memory and fails if any password field/name or generated password appears.
It then disables the synthetic account and requires exactly one `DISABLE_USER` event by the same
Admin. Failure cleanup attempts disable; audit rows and the disabled employee remain retained.

Item 756 passes only when both domain changes and audit records persist, the actor is the authenticated
human Admin, event cardinality is exact, password material is absent, and normal application behavior
offers no audit edit/delete path. Operational logs alone do not replace `AuditLog`.

Record audit/user UUID references, UTC time, release SHA/image digests, environment, request IDs,
sanitized outcomes, cleanup state, operator, and approver in controlled evidence. Never capture the
credential prompt, JWT, password, complete audit payload, or unrelated audit history.

Related documentation: [Audit Logging](../modules/audit-logging.md),
[Admin Create-User Verification](admin-create-user-verification.md),
[System Auditor Guide](../user-guides/system-auditor-guide.md), and
[Production Security Checklist](production-security-checklist.md).
