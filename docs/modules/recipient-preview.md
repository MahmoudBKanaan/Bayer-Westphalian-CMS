# Recipient Preview Documentation

Recipient preview is the campaign-scoped audience review step used before launch. It shows who is
eligible to receive a campaign, who is excluded, why each exclusion happened, and whether the
campaign can be launched.

## KB Traceability

| KB / FR | Recipient preview capability |
| --- | --- |
| `FR-054` | Preview eligible recipients |
| `FR-055` | Exclude opt-outs and invalid consent |
| `BR-001` | Customer with `do_not_contact` is excluded |
| `BR-002` | Customer with marketing opt-out is excluded |
| `BR-003` | Minor beneficiary without guardian consent is excluded |
| `BR-006` | Exclusion reasons are visible |
| `BR-007` | Excluded contacts and exclusion reasons are recorded |
| `BR-010` | Same customer cannot receive the same campaign twice (`DUPLICATE_CAMPAIGN_RECIPIENT`) |
| `BR-011` | Monthly contact limit is enforced |
| `BR-014` | Converted customers are excluded |
| Item **594** | Recipient preview UI clarity (guide, snapshot, launch readiness, readable reasons) |
| Sprint 16 **651** | Critical test: same customer cannot be duplicated in same campaign |

## Backend Workflow

1. Campaign Manager or Compliance Officer opens recipient preview for a campaign.
2. The backend evaluates campaign segment candidates with `EligibilityService`.
3. Each matching customer becomes either `ELIGIBLE` or `EXCLUDED`.
4. Excluded rows include a stable `exclusionReason` and readable `eligibilityExplanation`.
5. Generated rows are stored in `campaign_recipients` so compliance review and launch use the same
   snapshot.
6. Duplicate rows are prevented by the campaign/customer unique constraint
   (`campaign_recipients_campaign_customer_unique`) and by eligibility
   (`DUPLICATE_CAMPAIGN_RECIPIENT` / **BR-010** / critical item **651**).

### Critical test evidence (item 651)

| Layer | Location |
| --- | --- |
| Backend | `SameCustomerCannotBeDuplicatedInSameCampaignTests` |
| Frontend catalog | `frontend/src/features/campaigns/sameCustomerCannotBeDuplicatedInSameCampaign.ts` |
| Eligibility architecture | [eligibility-rules.md](../architecture/eligibility-rules.md) |

## API Contract

| Endpoint | Purpose |
| --- | --- |
| `GET /api/campaigns/{id}/recipients/preview` | Generate or refresh the campaign recipient snapshot |
| `GET /api/campaigns/{id}/recipients/eligible` | List only eligible recipients |
| `GET /api/campaigns/{id}/recipients/excluded` | List only excluded recipients with reasons |
| `GET /api/campaigns/{id}/recipients/summary` | Return `eligible`, `excluded`, `sent`, and `failed` counts |

Preview responses expose `customerId`, customer name fields, `eligibilityStatus`,
`exclusionReason`, and `eligibilityExplanation`.

## UI Behavior

The Recipient Preview screen (`CampaignRecipientPreviewPage`, item **594**) shows:

- Plain-language page lead and gate note (preview does not send; launch needs APPROVED + manage role).
- **How to read this preview** guide for Audience / Eligible / Excluded tabs.
- Campaign context cards (status, segment, channel).
- **Audience snapshot** metrics (total matched, eligible, excluded, sent) plus eligibility rate hint
  that eligible counts — not total matched — drive launch decisions.
- **Launch readiness** status explaining why launch is enabled or blocked.
- An **Eligible tab** (Eligible recipients) for recipients that can be contacted.
- An **Excluded tab** (Excluded recipients) for blocked recipients with reason codes.
- Tabs with optional counts: Audience preview, Eligible recipients, Excluded recipients.
- Eligible table for contactable rows (status may become Sent after launch).
- Excluded table with human reason titles **and** stable reason codes (BR-006).
- An **exclusion reason summary panel** for compliance review.
- A **launch button** only for campaign managers/admins when status is APPROVED.
- A **launch confirmation dialog** with eligible/excluded counts before contacting recipients.
- A **launch result** state after the campaign becomes active.

## Launch Boundary

Recipient preview does not send campaign messages. Launch is separate and is allowed only for an
`APPROVED` campaign by a role that can manage campaigns. Launch creates `contact_events`, marks
eligible recipients as sent, updates campaign metrics, and writes a launch audit log.

## Authorization

Campaign recipient preview is readable by campaign read roles, including Campaign Manager and
Compliance Officer. Recipient generation and launch remain protected by campaign management rules.
Product Manager can read campaign information but cannot launch campaigns.
