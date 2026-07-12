# Segment Criteria Guide

This guide explains how to build audience filter rules (segment criteria) for the Bayer-Westphalian
Campaign Management Platform. Criteria define **who matches a segment**. Matching alone is not a
final campaign audience: preview and launch always apply eligibility checks (consent, opt-out,
do-not-contact, guardian rules). See the
[`Segmentation Module Documentation`](segmentation-module.md) and
[`Eligibility Rules Documentation`](../architecture/eligibility-rules.md).

KB scope:

| ID | Topic |
| --- | --- |
| FR-070 | Age group |
| FR-071 | Location |
| FR-072 | Customer / prospect type |
| FR-073 | Product ownership |
| FR-074 | Payment history |
| FR-075 | Behavior / interests (status, source, interest, do-not-contact) |
| FR-076 | Product expiration |
| FR-078 | AND / OR combination |

## Criteria Shape

Each criterion is one filter rule stored on `segment_criteria` (or sent in create/update/preview
JSON).

| Property | API / UI name | Required | Description |
| --- | --- | --- | --- |
| Field | `fieldName` | Yes | What to filter (for example `city`, `age_group`) |
| Operator | `operator` | Yes | How to compare (see Operators) |
| Value | `value` | Yes | Operand text (lists, ranges, booleans as text) |
| Logical group | `logicalGroup` | No | Optional label for UI organization (for example `location`) |
| Join operator | `joinOperator` | No | How this rule combines with the **previous accumulated result**. Default **`AND`**. Ignored on the first rule. |

### JSON example (create / update / preview)

```json
{
  "criteria": [
    {
      "fieldName": "city",
      "operator": "EQUALS",
      "value": "Munich",
      "logicalGroup": "location",
      "joinOperator": "AND"
    },
    {
      "fieldName": "customer_type",
      "operator": "EQUALS",
      "value": "PROSPECT",
      "logicalGroup": "type",
      "joinOperator": "AND"
    }
  ]
}
```

First rule join is ignored. Second rule `AND` means: must match Munich **and** be a prospect.

## Operators

Supported operators (`SegmentOperator`):

| Operator | Meaning | Typical value form |
| --- | --- | --- |
| `EQUALS` | Exact match (after field-specific normalize) | Single token |
| `NOT_EQUALS` | Not equal | Single token |
| `CONTAINS` | Substring / partial text match | Free text |
| `IN` | Value is one of a list | Comma-separated list (for example `Munich,Berlin`) |
| `BETWEEN` | Inclusive range where supported | Two bounds (field-specific format) |
| `BEFORE` | Less than / earlier than | Number or date |
| `AFTER` | Greater than / later than | Number or date |

Not every operator is meaningful for every field. Prefer the suggested operators for each field in
the criteria builder UI (`frontend/src/features/segments/criteriaFields.ts`).

## Join Logic (FR-078)

Evaluation is **left-to-right** via `SegmentCriteriaLogicSupport`:

1. Evaluate criterion 0 → initial result.
2. For each next criterion `i`, combine with `joinOperator` on criterion `i`:
   - **`AND`**: accumulated result **and** criterion `i` matches (intersection).
   - **`OR`**: accumulated result **or** criterion `i` matches (union).
3. Null / missing `joinOperator` → **`AND`** (KB default).

### AND examples

| Criteria chain | Matches |
| --- | --- |
| `city = Munich` AND `customer_type = PROSPECT` | Only Munich prospects |
| `age_group = 26_40` AND `payment_status = OVERDUE` | Overdue payers aged 26–40 |

Partial matches fail pure AND chains: a Munich **CUSTOMER** fails the first example.

### OR examples

| Criteria chain | Matches |
| --- | --- |
| `city = Munich` OR `city = Berlin` | Munich or Berlin residents |
| `product_type = LIFE_INSURANCE` OR `product_type = HOMEOWNER_INSURANCE` | Owns either product type |

### Mixed AND / OR (left-associative)

Example chain stored as:

1. `customer_type = PROSPECT` (join ignored)
2. `city = Munich` with join `AND`
3. `city = Berlin` with join `OR`

Evaluates as: **`(PROSPECT AND Munich) OR Berlin`**.

There is no operator precedence beyond sequential fold. Order rules carefully when mixing AND and
OR.

## Field Catalog (FR-070–076)

Canonical field names are stored after backend normalization. Known aliases are accepted on write
and rewritten to the canonical name.

### Age group (FR-070)

| Canonical field | Values | Suggested operators |
| --- | --- | --- |
| `age_group` | `MINOR`, `18_25`, `26_40`, `41_60`, `60_PLUS` | `EQUALS`, `NOT_EQUALS`, `IN` |

Aliases / accept forms include enum-style names such as `AGE_26_40` where the age-group support
normalizes them.

Example: `age_group` `EQUALS` `26_40`.

### Location (FR-071)

| Canonical field | Source | Suggested operators |
| --- | --- | --- |
| `city` | Customer city | `EQUALS`, `NOT_EQUALS`, `CONTAINS`, `IN` |
| `country` | Customer country | same |
| `address_line` | Customer address line | same |

Aliases:

- `location` → `city`
- `addressline` → `address_line`

`IN` values: comma-separated cities (blank tokens dropped). Values are trimmed; city/country max
length 100, address line max 255.

Example: `city` `IN` `Munich,Berlin,Hamburg`.

### Customer / prospect type (FR-072)

| Canonical field | Values | Suggested operators |
| --- | --- | --- |
| `customer_type` | `CUSTOMER`, `PROSPECT`, `BENEFICIARY` | `EQUALS`, `NOT_EQUALS`, `IN` |

Aliases: `customertype`, `type` → `customer_type`.

Example: `customer_type` `EQUALS` `PROSPECT`.

### Product ownership (FR-073)

| Canonical field | Values / notes | Suggested operators |
| --- | --- | --- |
| `product_type` | `HOMEOWNER_INSURANCE`, `LIFE_INSURANCE`, `INVESTMENT_FUND`, `HEALTH_INSURANCE`, `AUTO_INSURANCE`, `OTHER` | `EQUALS`, `NOT_EQUALS`, `IN` |
| `product_id` | Product UUID | `EQUALS`, `NOT_EQUALS`, `IN` |
| `ownership_status` | `ACTIVE`, `EXPIRED`, `CANCELLED` | `EQUALS`, `NOT_EQUALS`, `IN` |

Aliases include `producttype`, `owned_product_type`, `product_ownership` → `product_type`;
`productid` → `product_id`; `ownershipstatus` → `ownership_status`.

Matching uses the customer’s product ownership records, not the product catalog alone.

Example: `product_type` `EQUALS` `LIFE_INSURANCE` AND `ownership_status` `EQUALS` `ACTIVE`.

### Payment history (FR-074)

| Canonical field | Values / notes | Suggested operators |
| --- | --- | --- |
| `payment_status` | `DUE`, `PAID`, `OVERDUE`, `DEFAULT_RISK` | `EQUALS`, `NOT_EQUALS`, `IN` |
| `reminder_count` | Non-negative integer (max across records) | `EQUALS`, `NOT_EQUALS`, `BEFORE`, `AFTER`, `BETWEEN` |
| `days_overdue` | Non-negative integer | same numeric operators |
| `default_risk` | `true` / `false` | `EQUALS`, `NOT_EQUALS` |

Aliases:

- `payment_history`, `paymenthistory`, `paymentstatus` → `payment_status`
- `remindercount` → `reminder_count`
- `daysoverdue` → `days_overdue`
- `defaultrisk` → `default_risk`

Boolean values also accept `yes`/`no`/`1`/`0` where payment support normalizes them.

Do **not** use deprecated UI status labels such as `PENDING` or `DEFAULTED`; use KB payment statuses
above.

Example: `payment_status` `EQUALS` `OVERDUE` AND `days_overdue` `AFTER` `30`.

### Behavior / interests (FR-075)

| Canonical field | Values / notes | Suggested operators |
| --- | --- | --- |
| `status` | Customer status (for example `ACTIVE`, `INTERESTED`, `UNINTERESTED`, `CONVERTED`) | `EQUALS`, `NOT_EQUALS`, `IN` |
| `interest` | Interest / behavior text where stored | text operators |
| `source` | Lead / customer source | text operators |
| `do_not_contact` | `true` / `false` | `EQUALS`, `NOT_EQUALS` |

Filtering by `do_not_contact = false` does not replace eligibility: preview still excludes
do-not-contact customers via `EligibilityService` when contactability rules apply.

Example: `status` `EQUALS` `INTERESTED` AND `source` `CONTAINS` `referral`.

### Consent-oriented criteria

These filters select customers by consent evidence. They **complement** but do **not** replace
preview eligibility.

| Canonical field | Values / notes | Suggested operators |
| --- | --- | --- |
| `consent_status` | `GIVEN`, `WITHDRAWN`, `REQUIRED`, `EXPIRED`, `REJECTED` | `EQUALS`, `NOT_EQUALS`, `IN` |
| `consent_type` | `MARKETING_EMAIL`, `MARKETING_PHONE`, `MARKETING_SMS`, `GUARDIAN`, `DATA_PROCESSING` | `EQUALS`, `NOT_EQUALS`, `IN` |
| `has_valid_marketing_consent` | `true` / `false` | `EQUALS`, `NOT_EQUALS` |
| `opt_out` | `true` / `false` (withdrawn/rejected marketing) | `EQUALS`, `NOT_EQUALS` |
| `has_valid_guardian_consent` | `true` / `false` | `EQUALS`, `NOT_EQUALS` |

UI may show `guardian_consent` as a label; backend canonicalizes guardian aliases to
`has_valid_guardian_consent`.

Example: `opt_out` `EQUALS` `false` AND `has_valid_marketing_consent` `EQUALS` `true`.

### Product expiration (FR-076)

Aligned with product-expiration campaign windows (3 / 6 / 12 months).

| Canonical field | Values / notes | Suggested operators |
| --- | --- | --- |
| `expiring_within_months` | Months until ownership expiration (typically `3`, `6`, `12`) | `EQUALS`, `NOT_EQUALS`, `IN`, `BEFORE`, `AFTER`, `BETWEEN` |
| `expiration_date` | ISO date `YYYY-MM-DD` | `EQUALS`, `NOT_EQUALS`, `BEFORE`, `AFTER`, `BETWEEN` |
| `is_expiring` | `true` / `false` (active ownership expires within 12 months) | `EQUALS`, `NOT_EQUALS` |

Aliases:

- `product_expiration`, `product_expiration_months`, `expiringwithinmonths` → `expiring_within_months`
- `product_expiration_date`, `expirationdate` → `expiration_date`
- `product_expiring`, `isexpiring` → `is_expiring`

Example: `expiring_within_months` `EQUALS` `6` AND `ownership_status` may be implied via active
ownership evaluation in expiration support (pair with ownership filters when needed).

## Building Criteria in the UI

1. Open **Segmentation** as Campaign Manager or Admin (create/edit) or BI Analyst (preview-focused).
2. Use **Criteria builder** on create or edit forms.
3. **Add criterion** for each rule; set field, operator, value, and join (`AND` / `OR` for rules
   after the first).
4. Optional **logical group** labels organize rules for humans; they do not change evaluation order.
5. **Preview** draft criteria before save (eligibility applied).
6. **Create segment** / **Save changes** to persist a reusable definition (FR-077).

Frontend catalog: `frontend/src/features/segments/criteriaFields.ts`  
Component: `SegmentCriteriaBuilder`.

## Building Criteria via API

| Endpoint | Body criteria |
| --- | --- |
| `POST /api/segments` | Optional `criteria` array on create |
| `PUT /api/segments/{id}` | Optional full criteria replacement |
| `POST /api/segments/preview` | Criteria for ad-hoc or draft preview |

Backend class entry points: `CreateSegmentCriteriaCommand`, `SegmentService.createSegment`,
`updateSegment`, `previewSegment`, filter supports under
`com.bayerwestphalian.campaign.segment`.

## Worked Recipes

### Munich prospects for life insurance renewal outreach

1. `city` `EQUALS` `Munich`  
2. `customer_type` `EQUALS` `PROSPECT` join `AND`  
3. `product_type` `EQUALS` `LIFE_INSURANCE` join `AND`  
4. `expiring_within_months` `EQUALS` `6` join `AND`

Preview will still drop matches that fail consent / do-not-contact / guardian eligibility.

### Overdue payers in Munich or Berlin

1. `payment_status` `EQUALS` `OVERDUE`  
2. `city` `EQUALS` `Munich` join `AND`  
3. `city` `EQUALS` `Berlin` join `OR`  

Evaluates as `(OVERDUE AND Munich) OR Berlin`. If the intent is overdue in either city, prefer:

1. `city` `IN` `Munich,Berlin`  
2. `payment_status` `EQUALS` `OVERDUE` join `AND`

### Interested customers with valid marketing consent (criteria only)

1. `status` `EQUALS` `INTERESTED`  
2. `has_valid_marketing_consent` `EQUALS` `true` join `AND`  
3. `opt_out` `EQUALS` `false` join `AND`

Eligibility still re-checks contactability on preview.

## Common Mistakes

| Mistake | Correction |
| --- | --- |
| Treating criteria match as final audience | Always preview; eligibility is mandatory |
| Wrong payment statuses (`PENDING`) | Use `DUE`, `PAID`, `OVERDUE`, `DEFAULT_RISK` |
| Assuming OR has higher precedence than AND | Evaluation is left-to-right only |
| Putting join on first criterion only | Join on rule `i` combines with results of `0..i-1` |
| Filtering `do_not_contact` and skipping eligibility | Preview still applies full eligibility rules |
| Using product catalog alone for ownership | Use ownership fields (`product_type`, `ownership_status`, …) |
| Expecting empty criteria to mean “no one” | Empty criteria means no field filters on active profiles (then eligibility) |

## Related Documentation

- [`Segmentation Module Documentation`](segmentation-module.md)
- [`Audience Preview Logic Documentation`](audience-preview-logic.md) — how criteria matches become eligible/excluded counts
- [`Eligibility Rules Documentation`](../architecture/eligibility-rules.md)
- [`Role-Based Access Documentation`](../architecture/role-based-access.md)
- Product ownership and payment module docs for underlying data quality

## Evidence

This guide preserves KB evidence that implementers and Campaign Managers can:

- Build segment filters for age, location, type, ownership, payment, behavior, consent-oriented
  fields, and product expiration (FR-070–076).
- Combine criteria with AND (default) and OR left-to-right (FR-078).
- Use documented operators and value formats for API and UI criteria builders.
- Understand that criteria matching is not final contact permission without eligibility.
