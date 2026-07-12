# Audience Preview Logic Documentation

This document describes how the platform computes **audience preview** for segmentation: who matches
segment criteria, who remains eligible to contact, who is excluded, and how exclusion reasons are
aggregated for Campaign Managers and BI Analysts.

Audience preview is **not** campaign launch. It answers: “For these criteria, how large is the
audience, and how many would currently pass contactability rules?” Final campaign recipient
generation (later lifecycle) must apply eligibility again and must not treat criteria-only matches
as a final contact list (Sprint 7 production gate / item 208).

## KB Requirements

| ID | Requirement |
| --- | --- |
| FR-054 | System previews eligible recipients |
| FR-055 | System excludes opt-outs and invalid consent |
| FR-079 | System previews audience size |
| BR-001 | `do_not_contact = true` must never be included |
| BR-002 | Marketing opt-outs are excluded |
| BR-003 | Guardian consent required when applicable |
| BR-006 | Exclusion reasons are shown |
| BR-011 | Monthly marketing contact limit is respected |

Related backlog acceptance: items **178 / 198** (preview applies `EligibilityService`), **179–181 /
199** (total, eligible, excluded counts), **208** (never final audience without eligibility).

## Architecture Overview

```text
POST /api/segments/preview
        │
        ▼
SegmentController.previewSegment
        │
        ▼
SegmentService.previewSegment
        │
        ├─1─ findMatchingCustomers(criteria)     ← criteria only (AND/OR filters)
        │         totalAudienceCount = |matches|
        │
        ├─2─ for each match:
        │         EligibilityService.evaluateForSegmentPreview(customerId)
        │              ├─ eligible → matchingCustomers / eligibleCount
        │              └─ excluded → exclusionDecisions
        │
        └─3─ SegmentExclusionReasonSummarySupport.summarize(...)
                  SegmentPreviewView(total, eligible, excluded, customers, summary)
```

Primary packages:

| Concern | Package / type |
| --- | --- |
| Preview orchestration | `com.bayerwestphalian.campaign.segment.SegmentService` |
| Preview DTO | `SegmentPreviewView`, `SegmentPreviewRequest`, `SegmentPreviewCommand` |
| Exclusion rollup | `SegmentExclusionReasonSummary`, `SegmentExclusionReasonSummarySupport` |
| Eligibility gate | `com.bayerwestphalian.campaign.campaign.EligibilityService` |
| Criteria matching | Filter supports + `SegmentCriteriaLogicSupport` (see [Segment Criteria Guide](segment-criteria-guide.md)) |

## Step 1 — Criteria Matching (Audience Size)

`SegmentService.findMatchingCustomers` loads **active customer profiles** and keeps those that
satisfy the normalized criteria list (FR-070–076 filters, FR-078 AND/OR).

- Soft-deleted / inactive profiles are not in the active set.
- Empty criteria means no field filters on that active set (not “match nobody”).
- **No eligibility** is applied in `findMatchingCustomers`. That method is criteria-only and is
  reused as the first phase of preview and for internal matching.

`totalAudienceCount` = number of criteria matches = pre-eligibility audience size (FR-079).

## Step 2 — Eligibility Gate (Every Match)

For **each** criteria match, preview calls:

```text
EligibilityService.evaluateForSegmentPreview(customerId)
```

Default consent type for segment preview is **`MARKETING_EMAIL`**. An overload accepts an explicit
`ConsentType` when channel-specific preview is required later.

Segment preview **never skips** this gate for matches that have a customer id. Null customer views
are treated as excluded with reason code `UNKNOWN`.

### Rules applied in segment preview

Aligned with campaign contactability, with one intentional difference:

| Rule | Applied in segment preview? | Notes |
| --- | --- | --- |
| Do-not-contact (`BR-001`) | Yes | Never contactable |
| Marketing opt-out (`BR-002`) | Yes | Withdrawn / rejected marketing |
| Valid required consent (`FR-055`) | Yes | Default marketing email consent |
| Guardian consent (`BR-003`) | Yes | When beneficiary requires guardian consent |
| Monthly contact limit (`BR-011`) | Yes | Excessive marketing prevention |
| Same-campaign duplicate (`BR-010`) | **No** | Preview is not campaign-scoped |

Full evaluation order and reason codes for the campaign path are documented in
[`Eligibility Rules Documentation`](../architecture/eligibility-rules.md). Segment preview uses
`evaluateSegmentAudienceRules` (or equivalent internal path) without campaign id / duplicate
recipient checks.

### Authorization for eligibility evaluation on preview

`evaluateForSegmentPreview` is authorized for segment preview roles:
`ADMIN`, `CAMPAIGN_MANAGER`, `BI_ANALYST` (`@authz.canPreviewSegments()`).

Direct campaign eligibility APIs remain restricted to Admin, Campaign Manager, and Compliance
Officer; BI Analyst uses the segment preview path rather than campaign-scoped eligibility APIs.

## Step 3 — Counts, Eligible List, Exclusion Summary

### Count fields

| Field | Meaning | KB |
| --- | --- | --- |
| `totalAudienceCount` | Criteria matches (before eligibility) | FR-079 |
| `eligibleCount` | Matches that pass eligibility | FR-054, item 199 |
| `excludedCount` | `totalAudienceCount - eligibleCount` | FR-055, item 199 |

**Invariant:** `eligibleCount + excludedCount == totalAudienceCount`.

`SegmentPreviewView` enforces non-negative counts, the invariant above, and that exclusion summary
counts sum to `excludedCount`.

### Matching customers list

`matchingCustomers` contains **only eligible** `CustomerView` entries.

- Size equals `eligibleCount`.
- Never the raw criteria-only set when any exclusions exist and eligibility was applied.
- Preview therefore never presents excluded people as contactable matches in this list.

### Exclusion reason summary (BR-006)

`SegmentExclusionReasonSummarySupport.summarize` aggregates ineligible `EligibilityDecision`
values:

| Summary field | Meaning |
| --- | --- |
| `code` | Stable exclusion reason code (for example `DO_NOT_CONTACT`) |
| `message` | Human-readable explanation |
| `count` | Number of matches excluded for that code |

Ordering: descending by `count`, then ascending by `code` for stable API output.

Unknown / missing reason codes map to:

| Code | Message |
| --- | --- |
| `UNKNOWN` | Customer excluded for an unspecified reason |

Typical eligibility codes (see eligibility docs for the full campaign set):

| Code | Meaning |
| --- | --- |
| `DO_NOT_CONTACT` | Customer has do-not-contact enabled |
| `MARKETING_OPT_OUT` | Marketing opt-out / withdrawn or rejected consent |
| `INVALID_CONSENT` | Required marketing consent missing or invalid |
| `MONTHLY_CONTACT_LIMIT` | Monthly marketing contact limit reached |

Guardian-related exclusions surface through the eligibility decision codes/messages returned by
`EligibilityService` when guardian consent is required but not valid.

## API Contract

### Request

`POST /api/segments/preview`

```json
{
  "criteria": [
    {
      "fieldName": "city",
      "operator": "EQUALS",
      "value": "Munich",
      "joinOperator": "AND"
    }
  ]
}
```

Wrapped in the shared `ApiResponse` on success with message `Segment preview loaded`.

### Response data shape (`SegmentPreviewView`)

```json
{
  "totalAudienceCount": 100,
  "eligibleCount": 72,
  "excludedCount": 28,
  "matchingCustomers": [ /* eligible CustomerView only */ ],
  "exclusionReasonSummary": [
    { "code": "MARKETING_OPT_OUT", "message": "...", "count": 15 },
    { "code": "DO_NOT_CONTACT", "message": "...", "count": 10 },
    { "code": "INVALID_CONSENT", "message": "...", "count": 3 }
  ]
}
```

### Authorization

| Layer | Rule |
| --- | --- |
| HTTP | `POST /api/segments/preview` → `ADMIN`, `CAMPAIGN_MANAGER`, `BI_ANALYST` |
| Method | `@PreAuthorize("@authz.canPreviewSegments()")` on `previewSegment` |

Create/manage segment roles are not required for preview. BI Analyst may preview; BI alone may not
create or edit saved segments (item 200).

## Frontend Boundary

| Component | Role in preview |
| --- | --- |
| `SegmentsPage` | Triggers preview from create/edit draft criteria |
| `SegmentPreviewResults` | Shows total / eligible / excluded and eligible customers |
| `ExclusionReasonSummaryPanel` | Renders aggregated exclusion reasons |
| `SegmentInsightPanel` | BI-oriented read of audience outcomes |
| `frontend/src/api/segments.ts` | `previewSegment` API client |

UI must not invent eligibility; it displays backend counts and summary only.

## Criteria Only vs Preview vs Campaign Recipients

| Path | Criteria | Eligibility | Campaign duplicate | Purpose |
| --- | --- | --- | --- | --- |
| `findMatchingCustomers` | Yes | No | No | Internal / criteria size building block |
| `previewSegment` | Yes | Yes (`evaluateForSegmentPreview`) | No | Audience preview for CM / BI |
| Campaign recipient generation (later) | Segment + campaign context | Yes (campaign path) | Yes | Final recipient list before/on launch |

### Production gate (item 208)

**Segmentation must never return a final campaign audience without eligibility checks.**

Enforcement in this module:

- Public contactable audience API: `POST /api/segments/preview` only.
- `findMatchingCustomers` is criteria-only, package-private, and not a REST endpoint.
- `previewSegment` always calls `EligibilityService.evaluateForSegmentPreview` per match.
- Campaign launch (later) must also apply eligibility; criteria-only match is never sufficient for
  contact.

## Worked Example

Criteria: `city = Munich` AND `customer_type = PROSPECT`.

1. Active prospects in Munich → 50 people → `totalAudienceCount = 50`.
2. Eligibility:
   - 5 do-not-contact → excluded
   - 8 marketing opt-out → excluded
   - 2 invalid consent → excluded
   - 35 eligible
3. Result:
   - `eligibleCount = 35`
   - `excludedCount = 15`
   - `matchingCustomers` length 35
   - Summary counts 5 + 8 + 2 = 15

## Common Mistakes

| Mistake | Correct behavior |
| --- | --- |
| Showing criteria matches as “ready to contact” | Show eligible count / list only as contactable |
| Skipping eligibility for performance | Every match must be evaluated in preview |
| Expecting campaign-duplicate exclusions in segment preview | Not applied until campaign-scoped recipient generation |
| Ignoring exclusion summary | BR-006 requires reason visibility for compliance-friendly preview |
| Using `findMatchingCustomers` API path for UI preview | UI must call `POST /api/segments/preview` |

## Related Documentation

- [`Segmentation Module Documentation`](segmentation-module.md)
- [`Segment Criteria Guide`](segment-criteria-guide.md)
- [`Eligibility Rules Documentation`](../architecture/eligibility-rules.md)
- [`Role-Based Access Documentation`](../architecture/role-based-access.md)

## Evidence

This documentation preserves KB evidence that:

- Audience size is previewed as criteria match count (`totalAudienceCount`, FR-079).
- Eligible and excluded counts are returned after `EligibilityService` (FR-054, FR-055, item 199).
- Preview always applies eligibility to every criteria match (items 178 / 198).
- Exclusion reason summaries support compliance review of preview outcomes (BR-006).
- Segment preview omits same-campaign duplicates but still enforces DNC, opt-out, consent, guardian,
  and monthly contact limit rules.
- Criteria-only matching is not a final contactable audience (item 208).
