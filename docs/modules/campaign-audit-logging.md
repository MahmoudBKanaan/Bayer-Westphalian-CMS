# Campaign Audit Logging Documentation

The campaign domain records audit entries for sensitive campaign lifecycle mutations. Audit logs
are persisted through `AuditService` inside the same database transaction as the campaign change so
compliance officers and system auditors can review what changed, when it changed, and which actor
performed the action.

## Package Boundary

Primary backend packages:

```text
com.bayerwestphalian.campaign.campaign
com.bayerwestphalian.campaign.audit
```

Campaign services that emit audit logs:

- `CampaignService`: campaign create, draft update, product/segment selection, submit, approve,
  reject, launch, pause, complete, and archive actions.

Audit persistence boundary:

- `AuditService`: creates immutable audit log entries (`logCreate`, `logUpdate`, and workflow
  helpers such as `logSubmission`, `logApproval`, `logRejection`).
- `AuditLog`: JPA entity mapped to the `audit_logs` table.
- `AuditLogRepository`: stores and lists audit history.
- `AuditController`: exposes `GET /api/audit-logs` for authorized audit review.

## Audited Entity Types

| Entity type | Service | Audited actions (current) |
| --- | --- | --- |
| `campaigns` | `CampaignService` | `CREATE` on draft creation (item 233); `SUBMIT` on submission (item 528 / FR-058); `APPROVE` / `REJECT` on compliance decision (item 529 / FR-059); `LAUNCH` on launch (item 530 / FR-060); `UPDATE` on draft edits and other lifecycle transitions |

## Campaign Creation Audit (Item 233)

When a Campaign Manager or Admin creates a draft campaign via `POST /api/campaigns` /
`CampaignService.createCampaign`, the service writes:

| Field | Value |
| --- | --- |
| `action` | `CREATE` |
| `entityType` | `campaigns` |
| `entityId` | new campaign UUID |
| `actorUserId` | authenticated user who created the draft |
| `oldValue` | `null` |
| `newValue` | campaign audit payload (see below) |

Failed validation before persist does not write an audit row.

## Campaign Submission Audit (Item 528)

When a Campaign Manager or Admin submits a campaign for compliance review via
`POST /api/campaigns/{id}/submit` / `CampaignService.submitCampaign`, the service writes:

| Field | Value |
| --- | --- |
| `action` | `SUBMIT` |
| `entityType` | `campaigns` |
| `entityId` | campaign UUID |
| `actorUserId` | authenticated campaign owner/manager (or Admin) who submitted |
| `oldValue` | campaign payload before submit (typically `status=DRAFT`, or `status=REJECTED` on resubmit) |
| `newValue` | campaign payload after submit (`status=SUBMITTED`; rejection fields cleared on resubmit) |

Validation failures (incomplete form fields), forbidden non-owner access, missing campaigns, or
invalid lifecycle status (for example submitting an already `APPROVED` campaign) do not write a
submission audit row. The audit entry is persisted through `AuditService.logSubmission` in the same
transaction as the status change.

## Campaign Approval Audit (Item 529)

When a Compliance Officer or Admin approves a submitted campaign via
`POST /api/campaigns/{id}/approve` / `CampaignService.approveCampaign`, the service writes:

| Field | Value |
| --- | --- |
| `action` | `APPROVE` |
| `entityType` | `campaigns` |
| `entityId` | campaign UUID |
| `actorUserId` | authenticated compliance/admin approver |
| `oldValue` | campaign payload before approval (typically `status=SUBMITTED`) |
| `newValue` | campaign payload after approval (`status=APPROVED`, `approvedByUserId`, `approvedAt`, optional `complianceReviewNotes`) |

Failed authorization (for example owner self-approve) or invalid lifecycle status does not write an
approval audit row. The audit entry is persisted through `AuditService.logApproval` in the same
transaction as the status change.

## Campaign Rejection Audit (Item 529)

When a Compliance Officer or Admin rejects a submitted campaign via
`POST /api/campaigns/{id}/reject` / `CampaignService.rejectCampaign`, the service writes:

| Field | Value |
| --- | --- |
| `action` | `REJECT` |
| `entityType` | `campaigns` |
| `entityId` | campaign UUID |
| `actorUserId` | authenticated compliance/admin rejector |
| `oldValue` | campaign payload before rejection (typically `status=SUBMITTED`) |
| `newValue` | campaign payload after rejection (`status=REJECTED`, required `rejectionReason`, optional `complianceReviewNotes`) |

Rejection reason is required. Owner self-reject, blank reason, missing campaigns, or invalid
lifecycle status do not write a rejection audit row. The audit entry is persisted through
`AuditService.logRejection` in the same transaction as the status change.

## Campaign Launch Audit (Item 530)

When a Campaign Manager or Admin launches an approved campaign via
`POST /api/campaigns/{id}/launch` / `CampaignService.launchCampaign`, the service writes:

| Field | Value |
| --- | --- |
| `action` | `LAUNCH` |
| `entityType` | `campaigns` |
| `entityId` | campaign UUID |
| `actorUserId` | authenticated campaign owner/manager (or Admin) who launched |
| `oldValue` | campaign payload before launch (`status=APPROVED`, approver fields present) |
| `newValue` | campaign payload after launch (`status=ACTIVE`) |

Only `APPROVED` campaigns may be launched (BR-005 / FR-060). Only APPROVED campaigns may be launched for audit evidence. Draft, submitted, rejected, or already
active campaigns, forbidden non-owner access, and missing campaigns do not write a launch audit row.
The audit entry is persisted through `AuditService.logLaunch` in the same transaction as the status
change (after contact-event creation and metrics refresh for eligible recipients).

## Audit Payload Fields

Campaign audit payloads include:

- `id`, `name`, `objective`, `status`
- `ownerUserId`, `segmentId`, `channel`
- `messageSubject`, `messageBody`, `startDate`, `endDate`
- `approvedByUserId`, `approvedAt`
- `rejectionReason`, `complianceReviewNotes`
- `productIds` (promoted products linked through `campaign_products`)

Creation payloads typically have `status=DRAFT`, null approval/rejection fields, and optional
segment/product selections provided at create time.

Approval payloads include the transition from `SUBMITTED` to `APPROVED`, the approver id, approval
timestamp, and any optional compliance review notes captured at approve time.

Rejection payloads include the transition from `SUBMITTED` to `REJECTED`, the formal rejection
reason, and optional compliance review notes.

Submission payloads include the transition from `DRAFT` (or `REJECTED`) to `SUBMITTED`, campaign
identity fields, channel, schedule, and promoted `productIds` when present.

Launch payloads include the transition from `APPROVED` to `ACTIVE`, campaign identity, channel,
approver identity retained from compliance review, and promoted `productIds` when present.

## Authorization And Review

Spring Security and method-level authorization protect both campaign mutations and audit review.

- Campaign create and draft management: `ADMIN`, `CAMPAIGN_MANAGER` (and method-level
  `CampaignWriteAccess` / ownership rules).
- Campaign approve/reject: `ADMIN`, `COMPLIANCE_OFFICER`.
- Audit log review: `ADMIN`, `COMPLIANCE_OFFICER`, `SYSTEM_AUDITOR`.

Frontend role checks may hide campaign-management controls, but audit logging is enforced in the
backend service layer and must not depend on UI-only checks.

## Audit API

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/audit-logs` | List persisted audit entries for authorized audit review. |

Audit list responses include `action`, `entityType`, `entityId`, `oldValue`, `newValue`,
`actorUserId`, and `createdAt`.

## Evidence

The campaign domain must preserve KB evidence that:

- Campaign creation actions create audit logs.
- Campaign creation audit logs use entity type `campaigns` and action `CREATE`.
- Campaign creation audit payloads capture name, objective, status, owner, channel, and related
  draft fields for later compliance review.
- Campaign submission actions create audit logs.
- Campaign submission audit logs use entity type `campaigns` and action `SUBMIT`.
- Campaign submission audit payloads capture the DRAFT to SUBMITTED (or REJECTED to SUBMITTED)
  transition and the submitting actor.
- Campaign approval actions create audit logs.
- Campaign approval audit logs use entity type `campaigns` and action `APPROVE`.
- Campaign approval audit payloads capture the SUBMITTED to APPROVED transition, approver identity,
  and optional compliance review notes.
- Campaign rejection actions create audit logs.
- Campaign rejection audit logs use entity type `campaigns` and action `REJECT`.
- Campaign rejection audit payloads capture the SUBMITTED to REJECTED transition, rejection reason,
  and optional compliance review notes.
- Campaign launch actions create audit logs.
- Campaign launch audit logs use entity type `campaigns` and action `LAUNCH`.
- Campaign launch audit payloads capture the APPROVED to ACTIVE transition and the launching actor.
- Campaign creation, submission, approval, rejection, and launch audit logs remain reviewable
  through the audit log API.
- Compliance officers and system auditors can inspect campaign-related sensitive actions without
  modifying campaign data.
