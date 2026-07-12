# Eligibility Rules Documentation

Campaign eligibility is the compliance gate used before a customer or beneficiary can be included
in recipient preview or contacted by a campaign. The rules combine customer contactability,
consent, opt-out, guardian consent, duplicate recipient, and contact frequency checks.

## Backend Boundary

Primary backend package:

```text
com.bayerwestphalian.campaign.campaign
```

The eligibility boundary contains:

- `EligibilityService`: evaluates campaign communication eligibility.
- `EligibilityDecision`: returns the eligible/excluded decision and explanation.
- `EligibilityExclusionReason`: defines stable exclusion reason codes and messages.
- `EligibilityResponse`: exposes API-ready reason fields for recipient preview and storage.

Eligibility depends on:

- `CustomerRepository` for active, non-deleted customer records and `doNotContact`.
- `ConsentService` for valid consent, marketing opt-outs, and guardian consent.
- `campaign_recipients` for duplicate campaign-recipient checks.
- `contact_events` for monthly marketing contact limit checks.
- `SystemSettingsService` for the Admin-configured monthly contact limit (item **535** / BR-011)
  and uninterested exclusion period (item **537**).
- `customers.status_changed_at` as the start of the uninterested exclusion window (item **537**).
- `beneficiaries` for guardian-consent-required checks.
- `campaigns.channel` for mapping campaign channel to required consent type.

## Evaluation Order

Eligibility is evaluated in this order:

1. Reject customers with `doNotContact = true` (**BR-001** / Sprint 16 critical item **648**).
2. Reject customers marked `UNINTERESTED` only while the Admin-configured exclusion period is still
   open (item **537**: days since `status_changed_at`).
3. Reject customers with marketing opt-outs, including withdrawn or rejected marketing consent.
4. Reject customers without valid required consent for the campaign channel.
5. Reject minor beneficiaries when guardian consent is required but not valid.
6. Reject duplicate recipients already assigned to the same campaign.
7. Reject customers who reached the configured monthly marketing contact limit.
8. Return eligible when no exclusion rule applies.

### Critical test evidence (item 648)

| Layer | Location |
| --- | --- |
| Backend critical suite | `CustomerWithDoNotContactIsExcludedTests` (`backend/.../campaign/`) |
| Related eligibility suite | `EligibilityServiceTests` (DNC short-circuit cases) |
| Frontend catalog | `frontend/src/features/customers/customerWithDoNotContactIsExcluded.ts` |
| Exclusion UI codes | `frontend/src/features/segments/exclusionReasons.ts` (`DO_NOT_CONTACT`) |

A customer with `do_not_contact = true` / `Customer.markDoNotContact()` must never be returned as
eligible from `EligibilityService` campaign, segment-preview, or reminder evaluation. The stable
reason code is `DO_NOT_CONTACT` (“Customer has do-not-contact enabled”). Consent, opt-out, and
monthly-limit checks are skipped after a DNC hit (first-rule short-circuit).

### Critical test evidence (item 649)

| Layer | Location |
| --- | --- |
| Backend critical suite | `CustomerWithoutValidConsentIsExcludedTests` (`backend/.../campaign/`) |
| Related eligibility suite | `EligibilityServiceTests` (invalid / missing consent cases) |
| Frontend catalog | `frontend/src/features/customers/customerWithoutValidConsentIsExcluded.ts` |
| Exclusion UI codes | `frontend/src/features/segments/exclusionReasons.ts` (`INVALID_CONSENT`) |

A customer **without valid required consent** for the campaign channel (or reminder consent type)
must be excluded with stable reason code `INVALID_CONSENT` (“Customer does not have valid required
consent”) — **FR-034** / **FR-055** / Sprint 16 critical item **649**. Enforcement is via
`ConsentService.isCommunicationEligible` on campaign, segment-preview, and reminder paths.
Duplicate and monthly-limit queries must not run after an invalid-consent hit.

Withdrawn or rejected marketing consent is excluded as `MARKETING_OPT_OUT` (**BR-002**) and also
blocks marketing; it is a related but distinct code from `INVALID_CONSENT`.

### Critical test evidence (item 650)

| Layer | Location |
| --- | --- |
| Backend critical suite | `MinorBeneficiaryWithoutGuardianConsentIsExcludedTests` (`backend/.../campaign/`) |
| Related eligibility suite | `EligibilityServiceTests` (guardian consent cases) |
| Frontend catalog | `frontend/src/features/customers/minorBeneficiaryWithoutGuardianConsentIsExcluded.ts` |
| Beneficiary module | [beneficiary-module.md](../modules/beneficiary-module.md) (`guardianConsentRequired`) |

**BR-003 / FR-032 / FR-034:** When a `beneficiaries` row for the customer has
`guardian_consent_required = true`, eligibility evaluation sets `guardianConsentRequired=true`.
`ConsentService.isCommunicationEligible(..., guardianConsentRequired=true)` then requires a valid
`GUARDIAN` consent record in addition to channel marketing consent. If guardian consent is missing
or invalid, the customer is excluded with `INVALID_CONSENT` (“Customer does not have valid required
consent”) on campaign, segment-preview, and reminder paths. Sprint 16 critical item **650**.

### Critical test evidence (item 651)

| Layer | Location |
| --- | --- |
| Backend critical suite | `SameCustomerCannotBeDuplicatedInSameCampaignTests` (`backend/.../campaign/`) |
| Related eligibility suite | `EligibilityServiceTests` (duplicate recipient cases) |
| Frontend catalog | `frontend/src/features/campaigns/sameCustomerCannotBeDuplicatedInSameCampaign.ts` |
| Schema | `CampaignRecipient` unique constraint `campaign_recipients_campaign_customer_unique` |

**BR-010 / FR-056:** The same customer must not appear twice on the same campaign. At evaluation
time, `EligibilityService` checks `campaign_recipients` for an existing `(campaign_id, customer_id)`
row and excludes with `DUPLICATE_CAMPAIGN_RECIPIENT` (“Customer is already assigned to this
campaign”). Persistence also enforces uniqueness on that pair. Segment preview and reminder paths
do **not** apply this check (not campaign-scoped). Sprint 16 critical item **651**.

### Critical test evidence (item 652)

| Layer | Location |
| --- | --- |
| Backend critical suite | `CustomerCannotExceedMonthlyContactLimitTests` (`backend/.../campaign/`) |
| Related suites | `EligibilityServiceTests`, `ConfigurableMonthlyContactLimitTests` (item 535) |
| Frontend catalog | `frontend/src/features/customers/customerCannotExceedMonthlyContactLimit.ts` |
| Settings | [system-settings.md](../modules/system-settings.md) (`monthlyContactLimit`) |

**BR-011 / FR-092 / FR-056:** A customer cannot exceed the Admin-configured monthly marketing contact
limit. `EligibilityService` counts `contact_events` with types `SENT` or `CALLED` in a rolling
**30-day** window and compares to `SystemSettingsService.monthlyContactLimit()` (item **535**). When
count ≥ limit, the decision is `MONTHLY_CONTACT_LIMIT` (“Customer has reached the monthly marketing
contact limit”) on campaign, segment-preview, and reminder paths. Sprint 16 critical item **652**.

Note: step 2 historically treated uninterested as permanent; item **537** makes the window
configurable. `CONVERTED` remains a permanent exclusion independent of the uninterested period.

This order keeps explicit compliance blocks ahead of database contact-frequency checks and returns
the first applicable exclusion reason.

## Consent Type Mapping

Campaign channel determines the required consent type:

| Campaign channel | Required consent type |
| --- | --- |
| `EMAIL` | `MARKETING_EMAIL` |
| `SMS` | `MARKETING_SMS` |
| `PHONE` | `MARKETING_PHONE` |
| `IN_APP` | `DATA_PROCESSING` |

Unsupported campaign channels fail validation instead of defaulting to marketing eligibility.

## Exclusion Reasons

Eligibility exclusions must return stable reason codes and explanations that can be shown in
recipient preview and stored on `campaign_recipients.exclusion_reason` and
`campaign_recipients.eligibility_explanation`.

| Reason code | Explanation |
| --- | --- |
| `DO_NOT_CONTACT` | Customer has do-not-contact enabled. |
| `UNINTERESTED` | Customer is marked uninterested and still inside the configured exclusion period (item 537). |
| `CONVERTED` | Customer has already converted. |
| `MARKETING_OPT_OUT` | Customer has withdrawn or rejected marketing consent. |
| `INVALID_CONSENT` | Customer does not have valid required consent. |
| `DUPLICATE_CAMPAIGN_RECIPIENT` | Customer is already assigned to this campaign. |
| `MONTHLY_CONTACT_LIMIT` | Customer has reached the monthly marketing contact limit. |

## KB Rules Preserved

- `BR-001`: A person with `do_not_contact = true` must never be included in a campaign.
- `BR-002`: A person who opted out of marketing must be excluded from marketing.
- `BR-003`: A beneficiary requiring guardian consent cannot be contacted until guardian consent
  is valid.
- `BR-006`: Campaigns must show recipient eligibility reasons.
- `BR-007`: Campaigns must record excluded contacts and exclusion reasons.
- `BR-010`: Same customer cannot receive the same campaign twice.
- `BR-011`: Same customer cannot receive more than the configured number of marketing messages
  per month. The limit is Admin-configurable via System Settings (item **535**;
  `SystemSettingsService.monthlyContactLimit()`), not a fixed code constant. Critical automated
  evidence: item **652** / `CustomerCannotExceedMonthlyContactLimitTests`.
- `FR-034`: System blocks marketing without valid consent.
- `FR-054`: System previews eligible recipients.
- `FR-055`: System excludes opt-outs and invalid consent.
- `FR-056`: System prevents duplicate or excessive marketing.
- `FR-097`: System respects do-not-contact status.

## Authorization

Eligibility checks are protected by Spring Security method-level authorization. Only `ADMIN`,
`CAMPAIGN_MANAGER`, and `COMPLIANCE_OFFICER` can evaluate campaign eligibility directly.

Frontend role checks may hide recipient preview controls, but backend authorization remains the
source of truth.

## Segment Audience Preview

`SegmentService.previewSegment` applies `EligibilityService.evaluateForSegmentPreview` to every
criteria match before returning contactable customers (KB FR-054 / FR-055 / FR-079, backlog items
178 and **198** — preview applies EligibilityService):

1. Resolve active customers matching segment criteria (`findMatchingCustomers`).
2. Set `totalAudienceCount` to the criteria-match size (pre-eligibility).
3. Call `evaluateForSegmentPreview(customerId)` for each match (marketing-email consent by default).
4. Collect eligible customers only into `matchingCustomers` / `eligibleCount`.
5. Aggregate exclusion reason codes into `exclusionReasonSummary` for the UI.

Preview never returns a criteria-only audience as contactable without this gate.
`findMatchingCustomers` remains criteria-only (no eligibility) for internal matching; only
`previewSegment` applies the EligibilityService path.

### Preview count fields (FR-079 / item 199)

| Field | Meaning |
| --- | --- |
| `totalAudienceCount` | Criteria matches (pre-eligibility) |
| `eligibleCount` | Criteria matches that pass EligibilityService |
| `excludedCount` | `totalAudienceCount - eligibleCount` |

Invariant: `eligibleCount + excludedCount == totalAudienceCount`. `matchingCustomers` lists only
eligible customers (`size == eligibleCount`). Exclusion reason summary counts sum to
`excludedCount`.

Segment preview omits campaign-duplicate checks because it is not campaign-scoped. Monthly contact
limits, do-not-contact, marketing opt-out, invalid consent, and guardian consent still apply.

Detailed pipeline, API contract, frontend boundary, worked examples, and the criteria-only vs
preview vs campaign-recipient comparison are documented in
[`Audience Preview Logic Documentation`](../modules/audience-preview-logic.md).

**Production gate (item 208):** segmentation must never return a final campaign audience without
eligibility checks. Criteria-only matching is not a contactable audience API.

## Recipient Preview And Storage

Recipient preview must display eligible and excluded recipients with the returned reason code and
explanation. When an excluded recipient is persisted, the reason code is stored in
`exclusion_reason` and the readable explanation is stored in `eligibility_explanation`.

Eligible responses use status `ELIGIBLE` and no reasons. Excluded responses use status `EXCLUDED`
and include reason fields ready for preview and persistence.

## Duplicate-Contact Prevention

Duplicate-contact prevention protects customers from receiving the same campaign more than once.
The rule is enforced in several layers:

- `EligibilityService` checks `campaign_recipients` for an existing `(campaign_id, customer_id)`
  row during campaign-scoped eligibility and returns `DUPLICATE_CAMPAIGN_RECIPIENT` before monthly
  contact-limit checks.
- `CampaignRecipientService.generateRecipients` deduplicates repeated candidate customer ids before
  saving the recipient snapshot.
- The database table `campaign_recipients` has the unique constraint
  `campaign_recipients_campaign_customer_unique` on `(campaign_id, customer_id)`.
- Launch reads only stored `ELIGIBLE` recipients, marks them `SENT`, and creates `contact_events`;
  subsequent eligibility checks see both the duplicate campaign recipient and the monthly contact
  history.

The same customer may still appear in a different campaign when eligibility and contact-frequency
rules allow it. Duplicate prevention is scoped to the same campaign/customer pair.
