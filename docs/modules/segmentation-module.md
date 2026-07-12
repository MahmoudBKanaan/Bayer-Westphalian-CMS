# Segmentation Module Documentation

The segmentation module owns reusable, eligibility-aware audience definitions for the MVP. It is
the backend source of truth for saved segments, segment filter criteria, AND/OR combination logic,
audience matching, and segment preview counts (total, eligible, excluded) with exclusion-reason
summaries. Segmentation supports campaign targeting without replacing the campaign lifecycle or
final recipient generation.

KB functional requirements covered by this module:

| ID | Requirement |
| --- | --- |
| FR-070 | Segment by age group |
| FR-071 | Segment by location |
| FR-072 | Segment by customer/prospect type |
| FR-073 | Segment by product ownership |
| FR-074 | Segment by payment history |
| FR-075 | Segment by behavior/interests (status and related behavior fields) |
| FR-076 | Segment by product expiration |
| FR-077 | Save reusable segments |
| FR-078 | Combine criteria with AND/OR logic |
| FR-079 | Preview audience size |

Related campaign and compliance requirements applied during segment preview:

| ID | Requirement |
| --- | --- |
| FR-054 | System previews eligible recipients |
| FR-055 | System excludes opt-outs and invalid consent |
| BR-001 | `do_not_contact = true` must never be included |
| BR-002 | Marketing opt-outs are excluded |
| BR-003 | Guardian consent required for applicable minors |
| BR-006 | Exclusion reasons are shown for eligibility decisions |

## Package Boundary

Primary backend package:

```text
com.bayerwestphalian.campaign.segment
```

Core segmentation components:

- `Segment`: JPA entity mapped to the `segments` table (name, description, owner, visibility,
  criteria collection).
- `SegmentCriteria`: JPA entity mapped to the `segment_criteria` table (field, operator, value,
  logical group, join operator).
- `SegmentRepository`: owner, visibility, and global segment lookup.
- `SegmentCriteriaRepository`: criteria persistence for a segment.
- `SegmentService`: backend-owned validation, authorization, create/update/delete, search, criteria
  matching, eligibility-aware preview, exclusion-reason aggregation, and audit calls.
- `SegmentController`: REST API boundary under `/api/segments`.
- `SegmentVisibility`, `SegmentOperator`, `SegmentJoinOperator`, request, command, search criteria,
  search request, criteria view, segment view, and preview view DTOs.

Filter and logic support classes:

- `SegmentAgeGroupSupport` (FR-070)
- `SegmentLocationSupport` (FR-071)
- `SegmentCustomerTypeSupport` (FR-072)
- `SegmentProductOwnershipSupport` (FR-073)
- `SegmentPaymentHistorySupport` (FR-074)
- `SegmentBehaviorStatusSupport` (FR-075)
- `SegmentConsentStatusSupport` (consent-oriented criteria used with audience filters)
- `SegmentProductExpirationSupport` (FR-076)
- `SegmentCriteriaLogicSupport` (FR-078 AND/OR evaluation, default AND when join is null)
- `SegmentExclusionReasonSummary` / `SegmentExclusionReasonSummarySupport` (preview reason rollup)

The segmentation module depends on:

- Customer module for active profile matching and location/demographics/type/behavior fields.
- Product ownership and payment records for ownership, expiration, and payment-history filters.
- Consent records for consent-status criteria.
- `EligibilityService` (`com.bayerwestphalian.campaign.campaign`) for every segment preview
  eligibility decision.
- `AuditService` for create/update/delete of saved segment definitions.
- User module for segment ownership (`owner_user_id`).

Campaign entity selection of a saved segment is a later campaign-lifecycle concern; this module
exposes reusable segment definitions and preview only.

## REST API

Segment endpoints return the shared `ApiResponse` wrapper.

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/segments` | List and search saved segments (optional `term`, `ownerUserId`, `visibility`). |
| `GET` | `/api/segments/{id}` | Load segment details including criteria. |
| `POST` | `/api/segments` | Create a reusable segment with optional criteria. |
| `PUT` | `/api/segments/{id}` | Update segment metadata and optionally replace criteria. |
| `DELETE` | `/api/segments/{id}` | Delete a saved segment definition. |
| `POST` | `/api/segments/preview` | Preview audience matches with eligibility applied. |

Preview response (`SegmentPreviewView`) includes:

- `totalAudienceCount` — criteria matches before contactability is treated as final.
- `eligibleCount` — matches that pass `EligibilityService` for segment preview.
- `excludedCount` — matches excluded by eligibility.
- `matchingCustomers` — eligible customer views returned for UI inspection.
- `exclusionReasonSummary` — aggregated exclusion codes, messages, and counts.

Invariant enforced by preview logic: `eligibleCount + excludedCount = totalAudienceCount` for the
evaluated criteria match set.

## Domain Rules

### Saved segments (FR-077)

- `name` is required (max 255 characters).
- `description` is optional.
- `visibility` is one of `PRIVATE`, `TEAM`, `GLOBAL` (defaults to `PRIVATE` when omitted).
- `owner_user_id` is set from the authenticated user on create.
- Criteria may be empty on create and added later; empty criteria means no field filters (still
  subject to active-customer matching rules used by the service).
- Private segments are readable by the owner and Admin; team/global segments are visible to roles
  with segment read access.

### Criteria model

Each criterion stores:

| Field | Purpose |
| --- | --- |
| `field_name` | Filter field (for example `city`, `age_group`, `product_type`) |
| `operator` | `EQUALS`, `NOT_EQUALS`, `CONTAINS`, `IN`, `BETWEEN`, `BEFORE`, `AFTER` |
| `value` | Operator operand (text; multi-value forms supported where applicable) |
| `logical_group` | Optional grouping label for UI/organization |
| `join_operator` | `AND` or `OR` relative to the previous criterion in evaluation order |

Practical field values, aliases, operators, and worked examples are documented in the
[`Segment Criteria Guide`](segment-criteria-guide.md).

### Filter categories (FR-070–076)

| Category | Example fields | Notes |
| --- | --- | --- |
| Age group | `age_group` | `MINOR`, `18_25`, `26_40`, `41_60`, `60_PLUS` (and aliases) |
| Location | `city`, `country`, `address_line` | EQUALS / CONTAINS / IN supported |
| Customer type | `customer_type` | `CUSTOMER`, `PROSPECT`, `BENEFICIARY` |
| Product ownership | `product_type`, `product_id`, `ownership_status` | Uses ownership records |
| Payment history | `payment_status`, `days_overdue` | Statuses include `DUE`, `PAID`, `OVERDUE`, `DEFAULT_RISK` |
| Behavior / status | customer status and related behavior fields | FR-075 interests/behavior targeting |
| Consent status | consent type/status oriented fields | Complements eligibility; not a substitute for preview eligibility |
| Product expiration | `expiring_within_months`, `expiration_date`, `is_expiring` | 3 / 6 / 12 month windows (BR-023 alignment) |

Field names are canonicalized on write where support classes define aliases (for example
`product_expiration_months` → `expiring_within_months`).

### AND / OR logic (FR-078)

- Criteria are evaluated left-to-right using each criterion’s `join_operator`.
- Missing join operators default to `AND`.
- Mixed AND/OR expressions are left-associative (no arbitrary operator precedence beyond sequential
  fold).
- Default combination when building multi-rule audiences is AND unless the user selects OR.

### Preview and eligibility (FR-079, FR-054, FR-055)

1. Resolve active customers matching segment criteria (criteria-only match).
2. For **every** match, call `EligibilityService.evaluateForSegmentPreview(customerId)`.
3. Split results into eligible vs excluded; build exclusion reason summary.
4. Return counts and eligible customer previews.

**Production gate (Sprint 7 item 208 / KB intent):** segmentation must never treat criteria-only
matches as a final campaign audience and must never treat criteria-only matches as a final campaign audience. Segment preview always applies eligibility. Campaign launch
recipient generation (later sprint) must also apply eligibility and must not skip it.

Campaign-duplicate and some campaign-scoped contact-frequency checks may be limited in pure segment
preview (preview is not campaign-scoped); core contactability, opt-out, consent, and guardian rules
still apply through `EligibilityService`. Full preview pipeline, count invariants, exclusion summary
behavior, and path comparison (criteria-only vs preview vs campaign recipients) are documented in
[`Audience Preview Logic Documentation`](audience-preview-logic.md). See also
[`Eligibility Rules Documentation`](../architecture/eligibility-rules.md).

Backend validation is authoritative; frontend criteria builders and labels are a user-experience
layer only.

## Authorization

Spring Security HTTP matchers and method-level authorization (`@authz.*`, `@SegmentCreateAccess`)
are the backend access-control boundary.

| Capability | Expression / roles | Notes |
| --- | --- | --- |
| Create reusable segment | `@authz.canCreateSegments()` | `ADMIN`, `CAMPAIGN_MANAGER` (FR-077, item 201) |
| Update / replace criteria | `@authz.canManageSegments()` | `ADMIN`, `CAMPAIGN_MANAGER` |
| Delete segment | `@authz.canManageSegments()` | `ADMIN`; `CAMPAIGN_MANAGER` for owned segments |
| Read / search / load | `@authz.canReadSegments()` | `ADMIN`, `CAMPAIGN_MANAGER`, `BI_ANALYST`, `COMPLIANCE_OFFICER` |
| Preview | `@authz.canPreviewSegments()` | `ADMIN`, `CAMPAIGN_MANAGER`, `BI_ANALYST` |

**BI Analyst cannot edit segment unless allowed (item 200):** `BI_ANALYST` alone has read and
preview only. Create and manage require `CAMPAIGN_MANAGER` or `ADMIN`. A dual-role user who also
holds `CAMPAIGN_MANAGER` or `ADMIN` may create and edit via those roles.

**Campaign Manager can create reusable segment (item 201):** Campaign Managers may POST a named
segment with visibility and criteria for later campaign selection.

Frontend role checks may hide create/edit controls, but every protected segment workflow must still
be enforced by backend role authorization. See
[`Role-Based Access Documentation`](../architecture/role-based-access.md).

## Audit And Evidence

The segmentation module records audit entries for saved segment changes:

| Action | When |
| --- | --- |
| `CREATE` | Reusable segment created |
| `UPDATE` | Segment metadata or criteria changed (including criteria-only save) |
| `DELETE` | Segment definition deleted |

Audit entity type: `segments`.

Audit payloads include segment id, name, description, visibility, owner user id, criteria count,
and criterion field/operator/value/join details. Segment create and update are sensitive
definition changes used for campaign targeting and must remain reviewable.

## Frontend Boundary

End-user workflows for Campaign Managers and BI Analysts are described in the
[`Segmentation User Guide`](../user-guides/segmentation-user-guide.md).

The frontend segmentation experience is implemented by:

- `SegmentsPage`: saved segment list/search, create form, edit form, details, and preview trigger.
- `SegmentCriteriaBuilder`: multi-rule criteria UI (fields FR-070–076, operators, AND/OR join).
- `SegmentPreviewResults`: total / eligible / excluded counts and matching customers.
- `ExclusionReasonSummaryPanel`: aggregated exclusion reasons from preview.
- `SegmentInsightPanel`: BI Analyst read-only segmentation insights.

Frontend modules:

- `frontend/src/api/segments.ts`
- `frontend/src/features/segments/criteriaFields.ts`
- `frontend/src/features/segments/exclusionReasons.ts`
- `frontend/src/features/segments/segmentInsights.ts`
- `frontend/src/features/auth/permissions.ts` (`canCreateSegments`, `canManageSegments`,
  `canReadSegments`, `canPreviewSegments`)

Role-based segment UI permissions must mirror backend rules without replacing them. BI Analyst
views emphasize read-only insight; Campaign Manager views expose create and edit for reusable
definitions.

## Downstream Use

Saved segments support:

- Campaign builder segment selection (campaign lifecycle sprint).
- Audience size and eligibility preview before submit/approval.
- BI reporting on audience composition and exclusion patterns.
- Consistent targeting rules for product-expiration and location/product-based campaigns.

Product ownership expiration windows (3 / 6 / 12 months), payment statuses, consent evidence, and
customer contactability fields must remain accurate because segment filters and eligibility depend
on them.

### Production gate (item 208)

**Segmentation must never return a final campaign audience without eligibility checks.**

The production gate is satisfied when all of the following remain true:

- `POST /api/segments/preview` / `SegmentService.previewSegment` always applies
  `EligibilityService.evaluateForSegmentPreview` to every criteria match (FR-054, FR-055, items 178
  / 198).
- Preview `matchingCustomers` lists only eligible customers; excluded matches appear only in counts
  and `exclusionReasonSummary` (BR-006).
- Criteria-only matching (`findMatchingCustomers`) is not exposed as a public REST audience endpoint
  and must not be used as a final contact list.
- `totalAudienceCount` may report criteria size, but contactable size is always `eligibleCount`.
- Future campaign recipient generation must re-apply eligibility (including campaign-scoped rules)
  and must not treat saved segment criteria matches alone as launch-ready recipients.

See [`Audience Preview Logic Documentation`](audience-preview-logic.md).

## Related Documentation

- [`Segmentation User Guide`](../user-guides/segmentation-user-guide.md) — Campaign Manager and BI Analyst screen workflows
- [`Segment Criteria Guide`](segment-criteria-guide.md) — field catalog, operators, AND/OR recipes
- [`Audience Preview Logic Documentation`](audience-preview-logic.md) — criteria match → eligibility → counts
- [`Eligibility Rules Documentation`](../architecture/eligibility-rules.md)
- [`Role-Based Access Documentation`](../architecture/role-based-access.md)
- Product ownership and payment modules (filter data sources for FR-073, FR-074, FR-076)

## Evidence

The segmentation module must preserve KB evidence that:

- Users can segment by age group, location, customer/prospect type, product ownership, payment
  history, behavior/status, consent-oriented fields, and product expiration (FR-070–076).
- Campaign Managers (and Admins) can create and save reusable segments (FR-077, item 201).
- Criteria can be combined with AND and OR logic (FR-078).
- Preview returns total audience size plus eligible and excluded counts (FR-079).
- Preview always applies `EligibilityService` and never treats criteria-only match as a final
  campaign audience (FR-054, FR-055, item 208).
- Exclusion reason summaries support compliance review of preview outcomes (BR-006).
- BI Analyst cannot edit segments unless also granted a manage role (item 200).
- Saved segment create/update/delete produce audit logs.
- Unauthorized roles cannot create or mutate protected segment workflows; backend authorization is
  authoritative.
