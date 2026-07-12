# System Auditor User Guide

This guide describes the workflows available to employees with the `SYSTEM_AUDITOR` role
(KB item **562**).

## Scope

System Auditors review audit logs, consent history, campaign approval history, user activity
history, and audit-report exports. Their work is read-only: they verify evidence, trace sensitive
actions, and export audit history when required, but they do not create or modify business records.

System Auditors can:

- View the Audit Log screen.
- Review system actions and sensitive changes.
- Review consent and opt-out changes (critical item **658**: every successful consent
  record/withdraw writes an immutable `consent_records` audit row — CREATE /
  WITHDRAW_CONSENT / marketing OPT_OUT).
- Review campaign approval and rejection history.
- Review sensitive user activity history.
- Export audit reports.

System Auditors cannot:

- Create, edit, or disable users.
- Assign roles or reset passwords.
- Create, approve, reject, launch, or edit campaigns.
- Record consent, opt-outs, or do-not-contact changes.
- Edit or delete audit log entries.
- Export campaign performance reports unless another authorized report role is also assigned.

## Audit Log Workflow

Open **Audit** in the application shell (`/audit`) to review immutable sensitive-action history.
The screen is backed by:

- `GET /api/audit-logs`
- `GET /api/audit-logs/entity-history`

The audit log list shows newest events first with action, entity type, entity id, actor, recorded
time, IP address, and value summary. Selecting a row opens the details panel with previous and new
JSON values. When the selected row has an entity type and entity id, the page loads entity history
for that record.

## Audit Filters

Use filters to narrow the evidence set:

| Filter | Purpose |
| --- | --- |
| `actorUserId` | Find activity by one user |
| `action` | Find actions such as `CREATE`, `APPROVE`, `REJECT`, `LAUNCH`, `WITHDRAW_CONSENT`, or `EXPORT_REPORT` |
| `entityType` | Focus on records such as `users`, `campaigns`, `consent_records`, or `report_exports` |
| `entityId` | Review history for one affected record |
| `createdFrom` / `createdTo` | Limit by recorded date range |

Use **Apply filters** to run the query and **Reset** to return to the recent unfiltered list.

## Consent And Opt-Out History

System Auditors use audit filters and entity history to verify:

- Consent creation and withdrawal.
- Opt-out recording.
- Do-not-contact changes.
- Guardian consent updates when applicable.
- Actor, timestamp, entity id, and previous/new value evidence.

System Auditors do not correct consent records themselves. Corrections must be performed by an
authorized Compliance Officer or Customer Service Agent and will create their own audit entries.

## Campaign Approval History

System Auditors review campaign lifecycle evidence:

- `SUBMIT` actions from Campaign Managers.
- `APPROVE` and `REJECT` actions from Compliance Officers.
- Approval timestamps, reviewer ids, rejection reasons, and compliance review notes.
- `LAUNCH` actions for approved campaigns.

This evidence supports the KB rule that campaigns cannot launch before compliance approval.

## User Activity History

System Auditors review sensitive user administration events:

- User creation.
- Role assignment or role changes.
- User disable actions.

The user activity history is read-only. System Auditors cannot manage employee accounts or roles.

## Audit Report Export

System Auditors may export audit history through the audit-report surface, which is separate from
campaign performance report export. It is separate from campaign performance report export. Audit export is restricted to audit roles and records an
`EXPORT_REPORT` audit entry for accountability.

Relevant backend capability:

- `ReportService.exportAuditReport`

## Access And Error Handling

Backend authorization is authoritative. The `SYSTEM_AUDITOR` role may view audit logs and audit
exports, but normal business mutation endpoints remain forbidden.

Expected responses:

- Missing authentication returns an unauthorized response.
- Authenticated users without an audit role receive `403 Forbidden` for audit logs.
- Attempts to mutate audit logs are not supported by the API.
- Attempts to use non-audit business mutation endpoints are rejected unless another authorized role
  is also assigned.

## KB Traceability

This guide preserves the KB System Auditor expectations:

- Role description: reviews audit logs, consent history, approval history, and sensitive actions.
- Allowed functions: view audit logs, consent history, campaign approval history, user activity
  history, and export audit reports.
- Screens: Audit Log, Consent History, Campaign Approval History, User Activity History, Reports.
- Entity access: `audit_logs` is `READ/EXPORT`; other modules are audit review only.
- Audit log entries remain immutable and read-only.
