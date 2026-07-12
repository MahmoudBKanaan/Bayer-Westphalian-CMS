# Compliance Review Documentation

Compliance review is the gate between campaign submission and launch. It confirms that a submitted
campaign has acceptable messaging, targeting, consent evidence, recipient eligibility, and audit
traceability before the campaign can become active.

## KB Traceability

| KB / FR | Compliance review capability |
| --- | --- |
| `FR-054` | Preview eligible recipients |
| `FR-055` | Exclude opt-outs and invalid consent |
| `FR-058` | Submitted campaigns enter compliance review |
| `FR-059` | Compliance Officer can approve or reject campaigns |
| `BR-005` | Campaigns cannot launch before Compliance Officer approval |
| `COMP-006` | Campaigns require compliance approval |
| `TC-011` | Compliance Officer can approve or reject campaigns |
| Sprint 16 **655** | Critical test: *Compliance Officer can approve/reject campaigns* |
| Item **593** | Compliance Review UI clarity (checklist, decision outcomes, confirmations) |

### Critical test evidence (item 655)

| Layer | Location |
| --- | --- |
| Backend critical suite | `ComplianceOfficerCanApproveRejectCampaignsTests` |
| Related HTTP security | `ProtectedEndpointSecurityTests#unauthorizedRoleCannotApproveComplianceCampaign` |
| Method security | `CampaignApprovalAccess` / `@authz.canApproveCampaigns()` on service approve/reject |
| Controller | `@PreAuthorize("@authz.canReviewCampaigns()")` on approve/reject endpoints |
| HTTP filter | `POST .../approve|reject` → `SecurityConfiguration.COMPLIANCE_ROLES` (Admin + Compliance) |
| Frontend catalog | `frontend/src/features/campaigns/complianceOfficerCanApproveRejectCampaigns.ts` |

`COMPLIANCE_OFFICER` (and `ADMIN`) may approve and reject submitted campaigns. `CAMPAIGN_MANAGER`
and other non-review roles receive **403** on approve/reject. Self-approval by campaign owner is
blocked separately by domain rules (item 250).

## Review Inputs

Compliance Officers review:

- Campaign name, objective, owner, channel, message subject, message body, schedule, segment, and
  promoted products.
- Recipient preview totals, eligible recipient count, excluded recipient count, and exclusion reasons.
- Consent, opt-out, do-not-contact, guardian consent, duplicate-recipient, and contact-frequency
  evidence.
- Existing audit history for campaign creation, submission, approval, rejection, and later
  lifecycle updates.

## Allowed Review Decisions

| Decision | From status | To status | Endpoint | Required data |
| --- | --- | --- | --- | --- |
| Approve | `SUBMITTED` | `APPROVED` | `POST /api/campaigns/{id}/approve` | optional `complianceReviewNotes` |
| Reject | `SUBMITTED` | `REJECTED` | `POST /api/campaigns/{id}/reject` | required `rejectionReason`; optional `complianceReviewNotes` |
| Record notes | `SUBMITTED`, `APPROVED`, or `REJECTED` | unchanged | `PUT /api/campaigns/{id}/compliance-review-notes` | optional `complianceReviewNotes` |

All other review decisions are rejected by backend business rules before persistence.

## Approval Rules

Approval is available only to `COMPLIANCE_OFFICER` and `ADMIN` users for submitted campaigns.
Approval captures `approvedByUserId`, `approvedByFullName`, `approvedAt`, and optional
`complianceReviewNotes`.

The campaign owner cannot approve or reject their own campaign. Non-review roles such as
`CAMPAIGN_MANAGER`, `PRODUCT_MANAGER`, `BI_ANALYST`, `MARKETING_ANALYST`, and
`EXECUTIVE_VIEWER` cannot approve or reject campaigns.

## Rejection Rules

Rejection requires a non-blank `rejectionReason`. The rejection reason is stored as
`campaigns.rejection_reason` and returned as `rejectionReason` in campaign API responses.

Optional `complianceReviewNotes` can explain requested changes, missing evidence, or follow-up
steps. When a rejected campaign is edited and resubmitted, the previous rejection reason, review
notes, approval user, and approval timestamp are cleared.

## Audit Evidence

Compliance review actions create audit evidence:

- `SUBMIT` records entry into review from `DRAFT` or `REJECTED`.
- `APPROVE` records the `SUBMITTED` to `APPROVED` decision with approver identity.
- `REJECT` records the `SUBMITTED` to `REJECTED` decision with the rejection reason.
- `UPDATE` records changes to compliance review notes when notes are updated without changing
  status.

Audit payloads include old and new campaign state, status, actor, approval metadata,
`rejectionReason`, and `complianceReviewNotes`.

## UI Evidence

The Compliance Review page (`CompliancePage`, item **593**) presents:

- A plain-language gate note that campaigns cannot launch without approval (**BR-005**).
- A six-point review checklist (message, audience, eligibility, products, schedule, decision record).
- A SUBMITTED-only queue with keyboard-selectable rows and explicit Review/Selected actions.
- Structured detail blocks for overview, message content, audience/products, schedule, and
  recipient eligibility snapshot, plus a link to full recipient preview.
- Separated **Approve** and **Reject** decision cards with outcome text, field hints, and confirmation
  dialogs before the API call.
- Reject **requires a rejection reason** (formal `rejectionReason`) before the decision is submitted.

The Campaigns page and Campaign Builder page surface campaign status, rejection reasons, and review
notes so Campaign Managers can revise rejected campaigns before resubmission.

## Implementation Evidence

- `CampaignController` exposes approve, reject, and compliance-review-notes endpoints.
- `CampaignService` enforces role, ownership, status, validation, persistence, and audit rules.
- `Campaign` enforces status transition and review-field invariants.
- `CompliancePage` implements the Compliance Review UI workflow.
- `CampaignComplianceReviewNotesTests`, `CampaignCanBeApprovedTests`,
  `CampaignCanBeRejectedTests`, and `CampaignServiceIntegrationTests` cover the backend workflow.
- `CompliancePage.test.tsx` covers the frontend approval, rejection, validation, and unauthorized
  review states.
