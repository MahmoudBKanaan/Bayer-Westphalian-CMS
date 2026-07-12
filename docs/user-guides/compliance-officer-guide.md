# Compliance Officer User Guide

This guide describes the MVP workflows available to employees with the
`COMPLIANCE_OFFICER` role.

## Scope

Compliance Officers review consent, opt-outs, guardian consent, campaign eligibility, campaign
approval, audit logs, and compliance reports. They are responsible for confirming that marketing
contacts respect customer preferences and KB eligibility rules before a campaign can launch.

Compliance Officers can review customer profiles, consent history, recipient preview outcomes,
submitted campaigns, approved campaigns, rejected campaigns, and sensitive audit evidence.

Compliance Officers cannot manage employee users, assign roles, reset passwords, or launch
campaigns unless they also have an additional authorized role.

## Dashboard Workflow

Compliance Officers can use the dashboard to:

- View consent alerts and pending approvals.
- Identify campaigns waiting for compliance review.
- Prioritize records with opt-outs, withdrawn consent, missing guardian consent, or invalid
  eligibility.
- Prioritize invalid eligibility records before campaign approval.
- Navigate to compliance review, recipient preview, audit log, and reports.

## Consent Management

Compliance Officers can use consent management and customer details to:

- Review current consent status and consent history.
- Review opt-outs, withdrawn consent, rejected consent, and expired consent.
- Review guardian consent for beneficiaries.
- Review and update customer `doNotContact` status when required.
- Confirm consent evidence such as purpose, source, granted date, withdrawn date, expiration date,
  evidence URL, and recorder information.
- Record or withdraw consent when correcting compliance records.

Marketing must remain blocked when valid consent is missing, when consent is withdrawn or
rejected, when the customer opted out, or when `doNotContact = true`.

## Eligibility And Recipient Preview

Compliance Officers can use recipient preview to:

- Review eligible and excluded recipients before campaign launch.
- Confirm exclusion reason codes and readable explanations.
- Verify that `DO_NOT_CONTACT`, `MARKETING_OPT_OUT`, `INVALID_CONSENT`,
  `DUPLICATE_CAMPAIGN_RECIPIENT`, and `MONTHLY_CONTACT_LIMIT` are handled.
- Confirm minor beneficiaries requiring guardian consent are excluded until valid guardian consent
  exists.
- Confirm exclusion reasons are ready to be stored on campaign recipient records.

Recipient preview must show both eligible and excluded counts so compliance review can confirm the
campaign audience before approval.

## Campaign Compliance Review

Compliance Officers can use compliance review to:

- Review submitted campaigns before launch.
- Check campaign name, objective, target segment, product, message, schedule, and owner.
- Confirm recipient preview and eligibility results.
- Approve campaigns that satisfy consent, opt-out, eligibility, and audit requirements.
- Reject campaigns that fail compliance checks with a required formal rejection reason
  (`rejectionReason` on `POST /api/campaigns/{id}/reject`; stored as `campaigns.rejection_reason`).
- Request changes and add review notes when campaign details or recipient evidence are incomplete
  (`complianceReviewNotes` on approve/reject, or `PUT /api/campaigns/{id}/compliance-review-notes`).

Campaigns cannot launch before Compliance Officer approval. Unauthorized roles cannot approve
compliance-controlled campaigns.

## Campaign Review Steps

For each submitted campaign, Compliance Officers should:

1. Open the Compliance Review page (`/compliance`) — the queue already filters to `SUBMITTED` campaigns.
2. Use the on-page checklist (message, audience, eligibility, products, schedule, decision record).
3. Select a campaign, review structured details, and open **recipient preview** when eligibility evidence is needed.
4. Confirm the campaign owner is not the reviewer before making an approval or rejection decision.
5. Approve (optional notes) or reject (required formal reason) and confirm the decision dialog.
3. Check the campaign objective, channel, message subject, message body, selected products,
   selected segment, start date, and end date.
4. Review recipient preview totals, eligible recipients, excluded recipients, and exclusion
   reasons before approval.
5. Confirm consent, opt-out, do-not-contact, guardian consent, duplicate-recipient, and monthly
   contact-limit evidence.
6. Approve the campaign only when the campaign is compliant and ready to launch.
7. Reject the campaign when compliance evidence is incomplete or messaging is not acceptable; a
   non-blank `rejectionReason` is required.
8. Add `complianceReviewNotes` when the Campaign Manager needs actionable revision guidance.
9. Verify that approval or rejection appears in the audit log as `APPROVE` or `REJECT` for
   `entityType=campaigns`.

Relevant endpoints:

- `POST /api/campaigns/{id}/approve`
- `POST /api/campaigns/{id}/reject`
- `PUT /api/campaigns/{id}/compliance-review-notes`
- `GET /api/audit-logs`

## Audit Log And Reports

Compliance Officers open **Audit** in the application shell (`/audit`, items 532–533) for a
read-only sensitive-action history. The screen lists newest events first, shows
action/entity/actor/time/IP summaries, and a **Selected entry** panel with previous/new JSON
values. Use **Filters** to narrow by actor, action, entity type/id, and recorded date range
(Apply / Reset). Selecting a row with an entity id also loads **Entity history** for that
record. Entries cannot be edited or deleted (COMP-008).

Compliance Officers can use the audit log and reports to:

- Review consent changes, consent withdrawals, opt-outs, and guardian consent updates.
- Review campaign creation history (`action=CREATE`, `entityType=campaigns`) as well as later
  lifecycle updates.
- Review campaign approval history (`action=APPROVE`, `entityType=campaigns`) including approver and
  status transition evidence.
- Review campaign rejection history.
- Review sensitive customer and campaign actions.
- Use compliance reports to support auditability and university report evidence.

Related API: `GET /api/audit-logs`, `GET /api/audit-logs/entity-history`.

Consent changes, approvals, rejections, and other sensitive compliance actions must be audit-log
ready.
Compliance actions must remain audit-log ready for review.

## Access And Error Handling

Backend authorization is authoritative. Frontend role-based controls improve usability, but the
backend must still enforce the `COMPLIANCE_OFFICER` permissions for every protected workflow.

Expected responses:

- Missing authentication returns an unauthorized response.
- Authenticated users without the correct role receive `403 Forbidden`.
- Validation failures return backend validation errors.
- Campaign approval failures must explain the compliance issue through review notes or eligibility
  reasons.

## KB Traceability

This guide preserves the KB Compliance Officer expectations:

- Role description: review consent, opt-outs, eligibility, campaign approval, and audit logs.
- Allowed functions: review consent, opt-outs, guardian consent, eligibility, approve or reject
  campaigns, view audit logs, and view compliance reports.
- Screens: Dashboard, Consent Management, Customer Details, Compliance Review, Recipient Preview,
  Campaigns, Audit Log, and Reports.
- Compliance review steps: verify campaign details, preview eligibility, consent evidence,
  owner/reviewer separation, approval/rejection decision, reviewer notes, and audit evidence.
- `FR-059`: Compliance Officer can approve or reject campaigns.
- `BR-005`: Campaigns cannot launch before Compliance Officer approval.
- `COMP-006`: Campaigns require compliance approval.
- `TC-011` / Sprint 16 item **655**: Compliance Officer can approve or reject campaigns
  (`ComplianceOfficerCanApproveRejectCampaignsTests`).
