# Consent Module Documentation

The consent module owns consent, opt-out, guardian consent, and communication eligibility behavior
for the MVP. It is the backend source of truth for whether a customer or beneficiary can be
contacted by a campaign, and it preserves audit evidence for consent changes.

## Package Boundary

Primary backend package:

```text
com.bayerwestphalian.campaign.consent
```

The module contains:

- `ConsentRecord`: JPA entity mapped to the `consent_records` table.
- `ConsentRepository`: consent history lookup, latest status lookup, opt-out lookup, and valid
  consent query access.
- `ConsentService`: backend-owned validation, authorization, status transitions, audit calls,
  opt-out checks, guardian consent checks, and communication eligibility rules.
- `ConsentController`: REST API boundary under `/api/consents`.
- `ConsentType`, `ConsentStatus`, request, command, criteria, search, and view DTOs.

The consent module depends on the customer module to validate existing customer records and to
enforce customer-level contactability such as `doNotContact`.

## REST API

Consent endpoints return the shared `ApiResponse` wrapper.

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/consents` | List consent records with optional filters. |
| `GET` | `/api/consents/status` | Load the latest consent status for a customer and consent type. |
| `GET` | `/api/consents/eligibility` | Check whether a customer is communication eligible for a consent type. |
| `POST` | `/api/consents` | Record a new consent, opt-out, guardian consent, or data-processing record. |
| `POST` | `/api/consents/withdraw` | Withdraw an existing consent record. |

Supported list filters are `customerId`, `consentType`, `status`, and `validOnly`.

## Domain Rules

- Consent must include `consentType`, `status`, `purpose`, `source`, and a customer reference.
- Supported consent types are `MARKETING_EMAIL`, `MARKETING_PHONE`, `MARKETING_SMS`, `GUARDIAN`,
  and `DATA_PROCESSING`.
- Supported consent statuses are `GIVEN`, `WITHDRAWN`, `REQUIRED`, `EXPIRED`, and `REJECTED`.
- A valid marketing consent must be `GIVEN`, not expired, and not withdrawn.
- A customer with `doNotContact = true` is not communication eligible.
- A customer with marketing consent status `WITHDRAWN` or `REJECTED` is treated as opted out and
  excluded from marketing.
- A minor beneficiary requiring guardian consent cannot be contacted until valid `GUARDIAN`
  consent exists.
- A beneficiary with valid guardian consent can continue the eligibility check for the requested
  campaign communication type.
- Consent evidence is stored with `evidenceFileUrl`, `grantedAt`, `withdrawnAt`, `expiresAt`, and
  the recorder information when available.
- Backend validation is authoritative; frontend display and form checks are only a
  user-experience layer.

## Authorization

Spring Security and method-level authorization are the backend access-control boundary.

- Record consent and withdraw consent: `ADMIN`, `CUSTOMER_SERVICE_AGENT`, `COMPLIANCE_OFFICER`.
- Read/search consent records, consent status, opt-out state, guardian consent status, and
  communication eligibility: `ADMIN`, `CAMPAIGN_MANAGER`, `COMPLIANCE_OFFICER`,
  `CUSTOMER_SERVICE_AGENT`, `SYSTEM_AUDITOR`.

Frontend role checks may hide consent controls, but every protected consent workflow must still be
enforced by backend role authorization.

## Audit And Evidence

The consent module records audit entries for consent creation, consent changes, and consent
withdrawal. Audit payloads include the consent record id, customer id, consent type, status,
purpose, source, granted date, withdrawn date, expiration date, evidence URL, and recorder when
available.
Consent withdrawal is audited with old and new consent values. The consent withdrawal event remains
reviewable for compliance and system audit users.

Consent changes must be audit-log ready because compliance officers and system auditors need to
review consent history, opt-outs, guardian consent, and campaign eligibility decisions.

## Frontend Boundary

The frontend customer details experience includes the consent tab. That tab displays current
consent status, validity, action-required state, consent type, purpose, source, granted date,
withdrawn date, expiration date, evidence URL, and recorder information. Authorized users can
record consent, mark opt-outs, withdraw consent, and update the customer `doNotContact` override.

## Evidence

The consent module must preserve KB evidence that:

- Consent, opt-outs, guardian consent, and do-not-contact status are managed in one compliance
  workflow.
- Customers without valid marketing consent are excluded.
- Customers with withdrawn consent are excluded.
- Customers with marketing opt-outs are excluded.
- Customers with `doNotContact = true` are excluded.
- Minor beneficiaries without guardian consent are excluded.
- Valid guardian consent allows eligibility checks to continue.
- Exclusion reasons are returned and stored-ready by recipient preview workflows.
- Consent changes create audit logs.
- Unauthorized roles cannot approve compliance-controlled consent or campaign approval decisions.
