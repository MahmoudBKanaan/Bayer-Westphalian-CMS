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
- Reject campaigns that fail compliance checks.
- Request changes and add review notes when campaign details or recipient evidence are incomplete.

Campaigns cannot launch before Compliance Officer approval. Unauthorized roles cannot approve
compliance-controlled campaigns.

## Audit Log And Reports

Compliance Officers can use the audit log and reports to:

- Review consent changes, consent withdrawals, opt-outs, and guardian consent updates.
- Review campaign approval and rejection history.
- Review sensitive customer and campaign actions.
- Use compliance reports to support auditability and university report evidence.

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
- `FR-059`: Compliance Officer can approve or reject campaigns.
- `BR-005`: Campaigns cannot launch before Compliance Officer approval.
- `COMP-006`: Campaigns require compliance approval.
- `TC-011`: Compliance Officer can approve or reject campaigns.
