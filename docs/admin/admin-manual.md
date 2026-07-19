# Administrator Manual

This manual is the final administrator-documentation deliverable for KB items **777** and **856**.

**Item 777** is the administrator manual for the Bayer-Westphalian Campaign Management Platform.
It covers in-application administration by employees with the `ADMIN` role. Host, database,
deployment, backup, provider, and incident operations are separate privileged responsibilities and
follow the linked operations runbooks.

## Administrative principles

- Use a named Admin account only for administrative work; use a lower-privilege account for normal
  business work when organizational policy requires separation.
- Apply least privilege. Assign only roles needed for an approved job function and review access
  when responsibilities change.
- Never share temporary passwords, sessions, JWTs, secrets, exports, or customer data through
  unapproved channels.
- Confirm the employee identity and authorization request before creating an account, assigning a
  role, resetting a password, or changing status.
- Sensitive actions must be auditable. Stop and escalate if an expected audit record is missing.
- Frontend visibility is not the security boundary; backend authorization must deny non-Admins.

## Admin access and navigation

Sign in through the approved HTTPS URL and confirm the user menu includes `ADMIN`. The sidebar adds
**Users** and **Settings**. Admin can also access **Audit** and authorized business screens.

Direct `/users`, `/settings`, and related APIs are Admin-only. A non-Admin must receive a restricted
route redirect or `403 Forbidden`; unauthenticated access must receive `401 Unauthorized`.

## Account lifecycle

There is no public signup and no permanent employee-user deletion in the MVP. Admin creates,
updates, disables, locks/unlocks through supported status editing, assigns roles, and resets
passwords. Preserve account and audit history when employment or responsibilities change.

### Create an employee account

1. Obtain an approved access request containing employee identity, unique work email, required
   role(s), manager/owner, and effective/expiry date where applicable.
2. Search the Users table to prevent duplicate identities or email addresses.
3. Enter full name, work email, and a policy-compliant temporary password.
4. Submit and verify the success notification and generated user UUID.
5. Assign approved roles separately and verify the displayed role list.
6. Communicate the temporary password through an approved secure channel, never email/chat/ticket
   plaintext unless organizational policy explicitly provides a protected mechanism.
7. Confirm the `CREATE` audit entry identifies the Admin actor and contains no password/hash.

The backend stores BCrypt hashes and never returns password material. Synthetic `.test` accounts
are development data and must not be enabled as production employees.

### Edit identity or account status

Select the employee, review their UUID/email/current roles, update the full name or supported status,
and save. Status meanings:

| Status | Effect |
| --- | --- |
| `ACTIVE` | Login is allowed when credentials and other controls pass |
| `LOCKED` | Login is blocked pending approved investigation/unlock |
| `DISABLED` | Login is blocked; normal offboarding/removal state |

Do not reactivate a locked/disabled account without validating the reason and approval. Verify the
saved status and audit evidence. Existing sessions should be handled according to the configured
session/token policy and incident process; status changes are not a reason to assume every issued
token has disappeared without verification.

### Disable an account

1. Confirm the correct employee UUID/email and approved offboarding/security request.
2. Select **Disable user** and review the confirmation dialog.
3. Confirm, then verify status `DISABLED` and inability to log in.
4. Verify one `DISABLE_USER` audit entry with before/after status and Admin actor.
5. Transfer business ownership/tasks through approved workflows; never reuse the disabled identity.

Disabling an already-disabled account is an idempotent no-op and should not create duplicate audit
evidence. Do not delete historical audit records.

## Role administration

The system roles are `ADMIN`, `CAMPAIGN_MANAGER`, `BI_ANALYST`, `PRODUCT_MANAGER`,
`COMPLIANCE_OFFICER`, `CUSTOMER_SERVICE_AGENT`, `SALES_AGENT`, `MARKETING_ANALYST`,
`EXECUTIVE_VIEWER`, and `SYSTEM_AUDITOR`.

Before assigning a role:

1. Verify the access request and separation-of-duty implications.
2. Select the employee and review all current roles.
3. Choose only a role not already assigned; duplicate assignment is a no-op.
4. Confirm the role-assignment dialog and verify the new role list.
5. Verify an `ASSIGN_ROLE` audit entry contains previous/new role sets and the Admin actor.
6. Ask the user to sign out/in so their new token/session reflects current roles.

High-risk combinations require explicit review. In particular, campaign creation and compliance
approval should remain separated; dual-role access must not be used to approve one's own work.
`ADMIN` is not a convenience role for troubleshooting ordinary authorization errors.

If role removal is required but the current UI/API does not expose an approved removal action, do
not edit database tables. Follow the change/access process and use only an implemented, tested,
audited workflow.

## Password reset and lockout

Reset passwords only after approved identity verification:

1. Select the exact employee and enter a new policy-compliant temporary password.
2. Confirm **Reset password**.
3. Communicate it once through the approved secure channel.
4. Require the employee to sign in and follow the organization's password-change/session policy.
5. Review account and audit/security evidence for unexpected activity when compromise is suspected.

Never ask for the user's old password, expose the new value in notes/screenshots, or reset a
password merely to bypass lockout. Repeated failures trigger generic rate-limit/lockout behavior to
avoid account discovery. Suspected compromise invokes incident response, session containment, and
credential rotation as approved.

## System settings

Open **Settings** to manage business safeguards:

| Setting | Allowed range | Operational effect |
| --- | --- | --- |
| Monthly marketing contact limit | 1-100 | Next eligibility and duplicate-risk evaluation |
| Send retry limit | 1-20 | Next outbound retry workflow; hard ceiling remains 20 |
| Uninterested exclusion period | 1-3650 days | Next eligibility evaluation of uninterested status |

Before saving, document the business/compliance reason, previous/new value, approver, and effective
time. Assess active campaigns, scheduler/provider impact, and rollback value. Use **Reset form** only
to restore the last loaded server values; it does not restore historical configuration.

Changes apply at runtime to their consumers. Do not weaken contact, retry, or uninterested safeguards
to make a campaign preview or provider retry pass. Verify the saved values and expected behavior with
synthetic data. Report missing audit/change evidence according to policy.

## Audit review

Open **Audit** for read-only sensitive-action history. Filter by actor UUID, action, entity type/id,
and UTC date range. Select an entry to review previous/new values and entity history.

Regular Admin review includes:

- user creation, status changes, role assignments, and password/security administration;
- consent/opt-out/do-not-contact changes;
- product and campaign creation/lifecycle actions;
- campaign submit, human approve/reject, and launch sequence;
- report exports and other sensitive actions.

The application exposes no normal audit update/delete workflow. Do not manipulate `audit_logs`
directly. Compliance Officer and System Auditor also have authorized read access; normal business
roles must be denied.

## First production administrator

The one-time bootstrap is a deployment operation, not an in-app shortcut:

1. An authorized operator enables `ADMIN_BOOTSTRAP_ENABLED=true` for the first production startup.
2. Email/full name are configured and the password is injected from the secret manager.
3. Startup creates the Admin with BCrypt hashing and system-attributed immutable audit records.
4. The operator verifies login, sets bootstrap false, removes the bootstrap password, and restarts.

Bootstrap is idempotent and must not reset/reactivate an existing account. Weak, blank, or `.test`
identity configuration fails safely. Follow [Admin Bootstrap](../operations/admin-bootstrap.md) and
the [Production Deployment Guide](../deployment/production-deployment-guide.md).

## What Admin does not do in the application

The Admin UI is not a host/secret/database console. Do not use it or direct APIs to:

- alter database tables, Flyway history, or audit rows;
- view or rotate JWT/database/provider/TLS secrets;
- run backups/restores or delete Docker volumes;
- enable real email/SMS without provider-policy approval;
- approve a campaign on behalf of the responsible human reviewer merely because Admin is broad;
- bypass consent, do-not-contact, `EligibilityService`, contact limits, or confirmation controls.

Use the [Operations Guide](../operations/operations-guide.md),
[Backup Guide](../deployment/backup-guide.md), [Restore Guide](../deployment/restore-guide.md),
[Security Checklist](../deployment/production-security-checklist.md), and
[Incident Notes](../deployment/incident-response-notes.md).

## Routine administration checklist

### Daily or event-driven

- Review approved account requests, lockouts, disabled accounts, and high-risk access anomalies.
- Confirm critical administrative actions have expected audit entries without secret material.
- Escalate unauthorized access, missing audit evidence, or suspected compromise immediately.

### Monthly

- Review active Admin and privileged-role assignments against current job responsibilities.
- Review dormant/temporary accounts, last login, lock/disable state, and expired approvals.
- Review system-setting values and change records against approved policy.
- Confirm audit access remains restricted and exports follow retention policy.

## Errors and escalation

| Condition | Admin response |
| --- | --- |
| Duplicate email/role | Recheck identity/current roles; do not create workaround duplicates |
| Validation error | Correct input; do not use direct database/API manipulation |
| `401` | Reauthenticate and investigate session policy if repeated |
| `403` for expected Admin action | Confirm current token role and backend policy; do not bypass |
| User cannot login | Check status/lockout and safe logs; never disclose whether unknown accounts exist publicly |
| Expected audit event missing | Stop affected sensitive workflow and escalate |
| Suspected secret/account exposure | Invoke incident response; preserve safe evidence and rotate/contain as approved |

Record UTC time, affected user/entity UUID, action, safe error code/request ID, and expected versus
observed result. Never include passwords, hashes, JWTs, environment values, customer payloads, or
unrestricted logs.

## Related documentation

- [Employee User Manual](../user-guides/user-manual.md)
- [Admin User-Management Guide](user-management-guide.md)
- [System Settings](../modules/system-settings.md)
- [Audit Logging](../modules/audit-logging.md)
- [Role-Based Access](../architecture/role-based-access.md)

Automated documentation evidence: `AdministratorManualDocumentationTests`.
