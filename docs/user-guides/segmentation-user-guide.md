# Segmentation User Guide

This guide describes MVP Segmentation screen workflows for employees who build or analyze
audiences: primarily **Campaign Manager** and **BI Analyst**, with **Admin** having full access.
It is the end-user companion to the technical
[`Segmentation Module Documentation`](../modules/segmentation-module.md).

## Scope

Segmentation lets authorized staff:

- Define **reusable audience segments** from customer attributes and related product, payment, and
  consent data (FR-070–077).
- Combine multiple rules with **AND** or **OR** logic (FR-078).
- **Preview** audience size, eligible vs excluded counts, and exclusion reasons before campaigns
  use the segment (FR-079, FR-054, FR-055).

Segmentation does **not** launch campaigns, approve compliance reviews, or send messages. A
preview is not a final contact list: eligibility is always applied on preview, and campaign launch
will re-check eligibility later.

### Who can do what

| Role | View segments | Preview audience | Create / edit / delete segments |
| --- | --- | --- | --- |
| `CAMPAIGN_MANAGER` | Yes | Yes | Yes |
| `ADMIN` | Yes | Yes | Yes |
| `BI_ANALYST` | Yes | Yes | No (unless also CM or Admin) |
| `COMPLIANCE_OFFICER` | Yes (read) | No | No |
| Other MVP roles | No (typical) | No | No |

**BI Analyst cannot edit segments unless allowed** (dual-role with Campaign Manager or Admin).
**Campaign Manager can create reusable segments** for later campaign selection.

## Open Segmentation

1. Sign in with an authorized account.
2. Open **Segmentation** from the main navigation (menu label: Segments / Segmentation).
3. You should see:
   - **Saved segments** table (search and visibility filters).
   - **Create segment** form (Campaign Manager / Admin only).
   - **Edit segment** form when a segment is selected (manage roles only).
   - **Segment details** for the selected definition.
   - **Preview results** and **exclusion reason** panels after a preview run.
   - **Segmentation insights** panel for BI Analyst read-only analysis.

Page tagline intent: reusable audience criteria with eligibility-aware preview.

## Campaign Manager Workflows

Campaign Managers use segmentation to define who a campaign should target, then verify how many of
those people are currently contactable.

### List and find saved segments

- Browse **Saved segments**.
- Search by name/description term.
- Filter by visibility: `PRIVATE`, `TEAM`, `GLOBAL`, or all.
- Select a row to load details into the edit/details panels.

Visibility meaning:

| Visibility | Intent |
| --- | --- |
| `PRIVATE` | Visible mainly to the owner (and Admin) |
| `TEAM` | Shared with team users who can read segments |
| `GLOBAL` | Shared organization-wide for reuse |

### Create a reusable segment

1. In **Create segment**, enter a clear **Name** (required).
2. Optionally add a **Description** of the business purpose.
3. Choose **Visibility** (`PRIVATE`, `TEAM`, or `GLOBAL`).
4. Build **criteria** with the criteria builder (see below).
5. Optionally **Preview** the draft before saving.
6. Click **Create segment** to save.

You should see confirmation that the segment was created. The definition is owned by your user
account and can be selected later when building campaigns.

### Edit or delete a segment

1. Select a saved segment available to the Campaign Manager account.
2. Update name, description, visibility, or criteria in **Edit segment**.
3. **Save changes** to replace the stored definition (criteria updates are audited).
4. **Delete segment** only when the definition is obsolete and not needed for active planning.

### Preview audience

1. Build criteria on create/edit **or** load a saved segment’s rules into preview.
2. Run **Preview**.
3. Review:
   - **Total audience count** — people matching criteria only.
   - **Eligible count** — still contactable after eligibility rules.
   - **Excluded count** — matched but blocked (opt-out, do-not-contact, invalid consent, etc.).
   - **Matching customers** list — eligible people only.
   - **Exclusion reason summary** — why others were excluded.

Always treat **eligible** numbers as the contactable preview. Do not use total criteria matches as
permission to market. **Production gate (item 208):** segmentation must never return a final
campaign audience without eligibility checks — preview always applies eligibility, and campaign
launch must re-check it later.

### Recommended Campaign Manager practice

1. Start with product, location, or expiration business intent (for example life policies expiring
   in 6 months in Munich).
2. Add criteria using AND by default; use OR only for explicit unions (for example Munich **or**
   Berlin).
3. Preview early; adjust criteria if eligible count is too small or exclusion rates are unexpected.
4. Save as `TEAM` or `GLOBAL` when colleagues should reuse the definition.
5. In campaign builder (later sprint), select the saved segment and re-check recipient preview
   before submit/approval.

## BI Analyst Workflows

BI Analysts use segmentation for **insight**, not definition ownership (unless dual-roled).

### Read-only access

- Open **Segmentation** and review saved segments.
- Inspect criteria patterns, visibility mix, and audience outcomes.
- Use **Segmentation insights** for analytical summaries.
- Run **Preview** on draft or listed criteria to study eligible vs excluded counts and exclusion
  reason breakdowns.

### What BI Analysts cannot do alone

- Create, edit, or delete reusable segments.
- See create/edit forms or save actions (UI hides them; backend also rejects mutations).

If your account also has `CAMPAIGN_MANAGER` or `ADMIN`, create/edit becomes available through that
role (item 200 “unless allowed”).

### Analytical tips

- Compare `totalAudienceCount` vs `eligibleCount` to estimate compliance attrition.
- Use exclusion reason codes (`DO_NOT_CONTACT`, `MARKETING_OPT_OUT`, `INVALID_CONSENT`,
  `MONTHLY_CONTACT_LIMIT`, …) to brief Campaign Managers on data or consent gaps.
- Prefer saved `TEAM` / `GLOBAL` segments as stable definitions for reporting periods.

## Criteria Builder (All Authorized Creators)

Use **Add criterion** for each filter rule.

| Control | Meaning |
| --- | --- |
| Field | What to filter (age, city, product type, payment status, expiration, …) |
| Operator | Equals, not equals, contains, in list, before/after/between where supported |
| Value | Comparison value (see field hints in the UI) |
| Join | For rules after the first: **AND** (default) or **OR** |
| Logical group | Optional label for organization only |

Common field groups:

- Demographics: age group  
- Location: city, country, address line  
- Customer type: customer / prospect / beneficiary  
- Product ownership: product type, product id, ownership status  
- Payment history: payment status (`DUE`, `PAID`, `OVERDUE`, `DEFAULT_RISK`), days overdue  
- Behavior: customer status, interest, source, do-not-contact  
- Consent-oriented: consent status/type, valid marketing consent, opt-out, guardian consent  
- Product expiration: expiring within 3 / 6 / 12 months, expiration date, is expiring  

Detailed values, aliases, and recipes:
[`Segment Criteria Guide`](../modules/segment-criteria-guide.md).

## Understanding Preview Results

| Result | Meaning |
| --- | --- |
| Total audience | Criteria matches (size of the filter result) |
| Eligible | Pass eligibility (contactable under current rules) |
| Excluded | Fail eligibility |
| Invariant | Eligible + Excluded = Total |

Eligibility always runs on preview. Criteria filters alone never authorize marketing. See
[`Audience Preview Logic Documentation`](../modules/audience-preview-logic.md).

## Access And Error Handling

Backend authorization is authoritative. Frontend menus and buttons improve usability but do not
replace server checks.

Expected outcomes:

- Not signed in → unauthorized.
- Role cannot preview → `403 Forbidden` on preview API.
- Role cannot create/edit → create/edit UI hidden; API returns forbidden if called.
- Invalid criteria or blank required name → validation errors from the backend.
- Private segments owned by others may be hidden or forbidden for readers without management access.

## Audit Expectations

Creating, updating, and deleting saved segments are auditable sensitive actions (entity type
`segments`). Campaign Managers and Admins who mutate definitions produce audit evidence for later
compliance or system audit review.

## Related Documentation

- [`Segmentation Module Documentation`](../modules/segmentation-module.md)
- [`Segment Criteria Guide`](../modules/segment-criteria-guide.md)
- [`Audience Preview Logic Documentation`](../modules/audience-preview-logic.md)
- [`Eligibility Rules Documentation`](../architecture/eligibility-rules.md)
- [`Role-Based Access Documentation`](../architecture/role-based-access.md)

## KB Traceability

This guide preserves KB Segmentation user expectations:

- Role descriptions: Campaign Manager defines segments; BI Analyst views segmentation insights and
  audience counts; Admin has full segmentation access.
- Allowed functions: Campaign Manager create/edit/preview/save segments; BI Analyst view analytics
  and segmentation insights (draft create only if explicitly allowed via dual role).
- Screens: Segmentation (criteria builder, preview, save).
- `FR-070`–`FR-076`: filter dimensions available in the builder.
- `FR-077`: save reusable segments.
- `FR-078`: AND/OR criteria.
- `FR-079`: preview audience size.
- `FR-054` / `FR-055`: eligible preview and exclusion of opt-outs / invalid consent.
- Item **200**: BI Analyst cannot edit unless allowed.
- Item **201**: Campaign Manager can create reusable segment.
