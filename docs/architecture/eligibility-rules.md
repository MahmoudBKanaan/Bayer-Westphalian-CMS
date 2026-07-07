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
- `beneficiaries` for guardian-consent-required checks.
- `campaigns.channel` for mapping campaign channel to required consent type.

## Evaluation Order

Eligibility is evaluated in this order:

1. Reject customers with `doNotContact = true`.
2. Reject customers with marketing opt-outs, including withdrawn or rejected marketing consent.
3. Reject customers without valid required consent for the campaign channel.
4. Reject minor beneficiaries when guardian consent is required but not valid.
5. Reject duplicate recipients already assigned to the same campaign.
6. Reject customers who reached the configured monthly marketing contact limit.
7. Return eligible when no exclusion rule applies.

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
  per month.
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

## Recipient Preview And Storage

Recipient preview must display eligible and excluded recipients with the returned reason code and
explanation. When an excluded recipient is persisted, the reason code is stored in
`exclusion_reason` and the readable explanation is stored in `eligibility_explanation`.

Eligible responses use status `ELIGIBLE` and no reasons. Excluded responses use status `EXCLUDED`
and include reason fields ready for preview and persistence.
