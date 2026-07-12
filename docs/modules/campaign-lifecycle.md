# Campaign Lifecycle Documentation

The campaign lifecycle is a controlled workflow for creating, reviewing, launching, pausing,
completing, and archiving campaigns. The backend enforces these rules in the `Campaign` aggregate
and `CampaignService`; frontend controls only guide users and do not replace backend validation.

## KB Traceability

| KB / FR | Lifecycle capability |
| --- | --- |
| `FR-050` / `FR-057` | Campaign Manager or Admin creates and edits draft campaigns |
| Item **592** | Campaign Builder multi-step flow (basics → audience/product → message → schedule → review) |
| `FR-058` | Campaign Manager submits a campaign for compliance review |
| `FR-059` | Compliance Officer approves or rejects submitted campaigns |
| `FR-060` / `BR-005` | Approved campaigns can be launched |
| `FR-061` | Active campaigns can be paused and resumed |
| `FR-062` | Completed or rejected campaigns can be archived |

## Statuses

Campaigns use the KB status set:

- `DRAFT`
- `SUBMITTED`
- `APPROVED`
- `REJECTED`
- `ACTIVE`
- `PAUSED`
- `COMPLETED`
- `ARCHIVED`

## Allowed Transitions

| From | Action | To | Primary endpoint |
| --- | --- | --- | --- |
| `DRAFT` | submit | `SUBMITTED` | `POST /api/campaigns/{id}/submit` |
| `REJECTED` | resubmit | `SUBMITTED` | `POST /api/campaigns/{id}/submit` |
| `SUBMITTED` | approve | `APPROVED` | `POST /api/campaigns/{id}/approve` |
| `SUBMITTED` | reject with reason | `REJECTED` | `POST /api/campaigns/{id}/reject` |
| `APPROVED` | launch | `ACTIVE` | `POST /api/campaigns/{id}/launch` |
| `ACTIVE` | pause | `PAUSED` | `POST /api/campaigns/{id}/pause` |
| `PAUSED` | resume | `ACTIVE` | `POST /api/campaigns/{id}/launch` |
| `ACTIVE` | complete | `COMPLETED` | `POST /api/campaigns/{id}/complete` |
| `PAUSED` | complete | `COMPLETED` | `POST /api/campaigns/{id}/complete` |
| `COMPLETED` | archive | `ARCHIVED` | `POST /api/campaigns/{id}/archive` |
| `REJECTED` | archive | `ARCHIVED` | `POST /api/campaigns/{id}/archive` |

All other lifecycle transitions are rejected with a business-rule error before persistence.

## Creation And Approval Activity Diagram

```mermaid
flowchart TD
    start([Start])
    create["Campaign Manager creates campaign draft"]
    validate{"Required campaign fields valid?"}
    validationError["Return validation errors"]
    saveDraft["Persist campaign as DRAFT"]
    createAudit["Write CREATE audit log"]
    editDraft["Campaign Manager edits draft details, products, segment, message, and schedule"]
    submit{"Submit for compliance review?"}
    submissionValidation{"Submission fields complete?"}
    submitError["Return submission validation errors"]
    submitted["Set status to SUBMITTED"]
    submitAudit["Write SUBMIT audit log"]
    review["Compliance Officer reviews campaign, recipient eligibility, consent, and audit evidence"]
    decision{"Compliance decision"}
    rejectReason{"Rejection reason provided?"}
    rejectError["Return rejection validation error"]
    rejected["Set status to REJECTED with rejectionReason and optional complianceReviewNotes"]
    rejectAudit["Write REJECT audit log"]
    revise["Campaign Manager revises rejected campaign"]
    approve["Set status to APPROVED with approvedByUserId, approvedAt, and optional complianceReviewNotes"]
    approveAudit["Write APPROVE audit log"]
    launchGate{"Campaign approved?"}
    launchBlocked["Block launch until approval"]
    ready["Campaign ready for launch"]
    end([End])

    start --> create --> validate
    validate -- No --> validationError --> editDraft
    validate -- Yes --> saveDraft --> createAudit --> editDraft
    editDraft --> submit
    submit -- No --> editDraft
    submit -- Yes --> submissionValidation
    submissionValidation -- No --> submitError --> editDraft
    submissionValidation -- Yes --> submitted --> submitAudit --> review --> decision
    decision -- Reject --> rejectReason
    rejectReason -- No --> rejectError --> review
    rejectReason -- Yes --> rejected --> rejectAudit --> revise --> editDraft
    decision -- Approve --> approve --> approveAudit --> launchGate
    launchGate -- No --> launchBlocked --> review
    launchGate -- Yes --> ready --> end
```

## Roles And Ownership

- `CAMPAIGN_MANAGER` and `ADMIN` can create draft campaigns, edit draft or rejected campaigns,
  submit campaigns, and operate launch/pause/complete/archive actions allowed by status.
- `COMPLIANCE_OFFICER` and `ADMIN` can approve or reject submitted campaigns.
- A campaign owner cannot approve or reject their own campaign.
- `PRODUCT_MANAGER` and other non-review roles cannot approve or reject campaigns.

## Review Data

Approval captures the approver and approval timestamp. Optional `complianceReviewNotes` may be
saved during approval.

Rejection requires a non-blank `rejectionReason`. Optional `complianceReviewNotes` may also be
saved. When a rejected campaign is edited and resubmitted, prior rejection reason, review notes,
and approval metadata are cleared.

## Audit Logging

Lifecycle mutations create audit evidence:

- `SUBMIT` for draft or rejected campaign submission.
- `APPROVE` for compliance approval.
- `REJECT` for compliance rejection.
- `UPDATE` for launch, pause, complete, archive, draft edits, and targeting changes.

Audit payloads include old and new campaign state so reviewers can verify the status transition,
actor, timestamp, rejection reason, review notes, and approval metadata.

## Implementation Evidence

- `Campaign` enforces the status transition rules.
- `CampaignService` applies authorization, ownership checks, validation, persistence, and audit
  logging.
- `CampaignController` exposes lifecycle REST endpoints under `/api/campaigns`.
- `CampaignTests` covers allowed and blocked domain transitions.
- `CampaignServiceIntegrationTests` covers persisted lifecycle transitions and audit evidence.
