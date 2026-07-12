# Campaign Manager User Guide

This guide describes the MVP workflows available to employees with the `CAMPAIGN_MANAGER` role.

## Scope

Campaign Managers plan campaigns, define audiences, select promoted products, draft compliant messages,
schedule outreach, submit campaigns for compliance review, and revise rejected campaigns.
They own campaign setup before launch, but compliance approval and final review remain controlled
by Compliance Officers or Admins.

Campaign Managers cannot approve or reject campaigns, cannot approve their own campaigns, manage
employee users, assign roles, or bypass consent and eligibility checks.

## Dashboard Workflow

Campaign Managers can use the dashboard to:

- View draft, submitted, approved, rejected, active, paused, completed, and archived campaigns.
- Identify campaigns requiring draft completion, compliance submission, revision, launch, pause,
  completion, or archive action.
- Review audience preview counts, eligibility warnings, and product or segment readiness.
- Navigate to Campaigns, Campaign Builder, Segmentation, Products, recipient preview, and reports.

## Campaign Creation And Draft Editing

Use **Campaign Builder** (`/campaign-builder`) for a guided five-step draft flow (item **592**):

1. **Basics** — name, objective, channel  
2. **Audience & product** — segment and promoted product  
3. **Message** — subject and body (optional AI copy with human approval)  
4. **Schedule** — start and end dates  
5. **Review & create** — confirm, create `DRAFT`, then submit for compliance  

Each step is validated before continue. The live summary sidebar tracks selections across steps.

Campaign Managers can create a draft campaign with:

- Campaign name and objective.
- Campaign channel such as `EMAIL`, `SMS`, or `PHONE`.
- Message subject and message body.
- Start date and end date.
- Segment and promoted products (required in the builder flow).

Draft and rejected campaigns can be edited. Submitted, approved, active, paused, completed, and archived campaigns cannot be edited
as drafts without returning through the proper workflow.

Primary APIs:

- `POST /api/campaigns`
- `PUT /api/campaigns/{id}`
- `PUT /api/campaigns/{id}/segment`
- `PUT /api/campaigns/{id}/products`
- `GET /api/campaigns/{id}`

## Audience And Product Selection

Campaign Managers can use Segmentation to create reusable segments and preview audience size before
selecting a segment for a campaign. Segment criteria support age group, location, customer type,
product ownership, payment history, behavior/status, product expiration, and AND/OR logic.

Campaign Managers can select promoted products so the campaign message and eligibility review are
tied to the correct product context.

Related documentation:

- [`Segmentation User Guide`](segmentation-user-guide.md)
- [`Campaign Lifecycle Documentation`](../modules/campaign-lifecycle.md)
- [`Compliance Review Documentation`](../modules/compliance-review.md)

## Submission And Compliance Review

Campaigns cannot be submitted until required fields are complete. Required submission fields include
campaign name, objective, channel, and the message/schedule fields required by the KB workflow.

Submission moves a campaign from `DRAFT` or `REJECTED` to `SUBMITTED` and creates a `SUBMIT` audit
log. Once submitted, the campaign waits for Compliance Officer review.

Compliance Officers may:

- Approve the campaign, moving it to `APPROVED`.
- Reject the campaign, moving it to `REJECTED` with a required `rejectionReason`.
- Add optional `complianceReviewNotes`.

Campaign Managers should use `rejectionReason` and `complianceReviewNotes` to revise rejected
campaigns before resubmitting.

Primary API:

- `POST /api/campaigns/{id}/submit`

## Recipient Preview Before Launch

Open **Recipient preview** on a campaign to:

- Read the audience snapshot (total matched vs eligible vs excluded).
- Inspect eligible and excluded rows; exclusion reasons show a human title and stable code.
- Confirm launch readiness (only **APPROVED** campaigns can launch).
- Launch with a confirmation that shows eligible/excluded counts (item **594**).

Prefer **eligible** counts over total matched audience for launch decisions.

## Launch And Lifecycle Operations

Approved campaigns can be launched. Campaign Managers cannot launch campaigns before Compliance
Officer approval (`BR-005`).

Allowed later lifecycle operations:

- Launch `APPROVED` campaigns to `ACTIVE`.
- Pause `ACTIVE` campaigns to `PAUSED`.
- Resume `PAUSED` campaigns to `ACTIVE`.
- Complete `ACTIVE` or `PAUSED` campaigns to `COMPLETED`.
- Archive `COMPLETED` or `REJECTED` campaigns to `ARCHIVED`.

Primary APIs:

- `POST /api/campaigns/{id}/launch`
- `POST /api/campaigns/{id}/pause`
- `POST /api/campaigns/{id}/complete`
- `POST /api/campaigns/{id}/archive`

## Access And Error Handling

Backend authorization is authoritative. Frontend role-based controls improve usability, but the
backend enforces Campaign Manager permissions and ownership rules.

Expected responses:

- Missing authentication returns an unauthorized response.
- Authenticated users without the correct role receive `403 Forbidden`.
- Missing required fields return validation errors.
- Invalid lifecycle operations return business-rule errors.
- Product Manager, BI Analyst, Marketing Analyst, and other non-owner/non-review roles cannot
  perform Campaign Manager workflow actions unless they also have an authorized role.

## Audit And Evidence

Campaign Manager actions are auditable:

- Draft creation writes `CREATE`.
- Draft edits, targeting changes, and lifecycle updates write `UPDATE`.
- Submission writes `SUBMIT`.
- Compliance approval and rejection write `APPROVE` or `REJECT` by the reviewer.

Audit logs preserve campaign status, owner, channel, message, schedule, product links, segment,
approval metadata, `rejectionReason`, and `complianceReviewNotes`.

## KB Traceability

This guide preserves the KB Campaign Manager expectations:

- Role description: plan campaigns, define target audiences, draft messages, and manage campaign
  workflow before and after compliance review.
- Allowed functions: create campaign drafts, edit drafts and rejected campaigns, select segments,
  select products, submit campaigns, launch approved campaigns, pause/resume, complete, and archive
  allowed campaigns.
- Screens: Dashboard, Campaigns, Campaign Builder, Segmentation, Products, Recipient Preview,
  Compliance Feedback, Analytics, and Reports.
- `FR-050` / `FR-057`: Campaign Manager can create and edit draft campaigns.
- `FR-052`: Campaign Manager can select promoted products.
- `FR-053`: Campaign Manager can select target segment.
- `FR-054` / `FR-055`: Campaign audience preview respects eligibility and exclusions.
- `FR-058`: Campaign Manager can submit campaign.
- `FR-060` / `BR-005`: Only approved campaigns can launch.
- `FR-061`: Campaign Manager can pause/resume active campaigns.
- `FR-062`: Campaign Manager can complete/archive allowed campaigns.
- `FR-077` / `FR-078` / `FR-079`: Campaign Manager can create reusable segments, combine criteria,
  and preview audience size.
