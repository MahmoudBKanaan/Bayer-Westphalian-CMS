# Beneficiary Module Documentation

The beneficiary module owns policyholder-to-beneficiary relationship records for the MVP. It links
two existing customer profiles, stores relationship metadata, and captures guardian details when
guardian consent is required.

## Package Boundary

Primary backend package:

```text
com.bayerwestphalian.campaign.beneficiary
```

The module contains:

- `Beneficiary`: JPA entity mapped to the `beneficiaries` table.
- `BeneficiaryRepository`: relationship lookup, duplicate-link checks, and guardian-consent
  filter persistence access.
- `BeneficiaryService`: backend-owned validation, authorization, customer-link validation,
  relationship updates, and guardian-consent behavior.
- `BeneficiaryController`: REST API boundary under `/api/beneficiaries`.
- Request, command, search criteria, search request, and view DTOs.

The module depends on the customer module only to validate existing, non-deleted policyholder and
beneficiary customers. Customer profile ownership remains in
`com.bayerwestphalian.campaign.customer`.

## REST API

Beneficiary endpoints return the shared `ApiResponse` wrapper.

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/beneficiaries` | List beneficiary links with optional filters. |
| `GET` | `/api/beneficiaries/{id}` | Beneficiary relationship details. |
| `POST` | `/api/beneficiaries` | Create a policyholder-to-beneficiary link. |
| `PUT` | `/api/beneficiaries/{id}` | Update relationship and guardian fields. |
| `DELETE` | `/api/beneficiaries/{id}` | Delete a beneficiary relationship link. |

Supported filters are `policyholderCustomerId`, `beneficiaryCustomerId`, and
`guardianConsentRequired`.

## Domain Rules

- `policyholderCustomerId`, `beneficiaryCustomerId`, and `relationship` are required.
- The policyholder customer and beneficiary customer must be different records.
- Both customer records must exist and must not be soft-deleted.
- Duplicate links are rejected by the service and protected by the database unique constraint.
- `guardianName`, `guardianEmail`, and `guardianConsentRequired` are stored on the link.
- Guardian email format is validated by backend rules.
- Setting `guardianConsentRequired` to `true` records that guardian consent is required.
- Setting `guardianConsentRequired` to `false` clears the guardian consent requirement.

## Authorization

Spring Security and method-level authorization are the backend access-control boundary.

- Create beneficiary link: `ADMIN`, `CUSTOMER_SERVICE_AGENT`.
- Update beneficiary link: `ADMIN`, `CUSTOMER_SERVICE_AGENT`, `COMPLIANCE_OFFICER`.
- Delete beneficiary link: `ADMIN`, `CUSTOMER_SERVICE_AGENT`.
- Read/search beneficiary links: `ADMIN`, `CAMPAIGN_MANAGER`, `BI_ANALYST`,
  `COMPLIANCE_OFFICER`, `CUSTOMER_SERVICE_AGENT`, `SYSTEM_AUDITOR`.

Frontend role checks may hide beneficiary controls, but every protected beneficiary workflow must
still be enforced by backend role authorization.

## Frontend Boundary

The frontend customer details experience includes the beneficiaries tab. That tab displays
beneficiary links for a customer and allows authorized users to create or update relationship
details, guardian name, guardian email, and the guardian-consent-required flag.

## Evidence

The beneficiary module must preserve KB evidence that:

- A beneficiary can be linked to a policyholder customer.
- The guardian consent required flag is saved.
- Invalid or duplicate beneficiary links are rejected.

### Critical test (item 650)

Minor beneficiaries with `guardianConsentRequired = true` and without valid guardian consent must
be excluded from campaign audiences (**BR-003**). Automated evidence:

| Layer | Location |
| --- | --- |
| Backend | `MinorBeneficiaryWithoutGuardianConsentIsExcludedTests` |
| Eligibility architecture | [eligibility-rules.md](../architecture/eligibility-rules.md) |
| Frontend catalog | `frontend/src/features/customers/minorBeneficiaryWithoutGuardianConsentIsExcluded.ts` |

The flag on the beneficiary link drives `EligibilityService` guardian checks; contact is blocked
until a valid `GUARDIAN` consent record exists.
- Unauthorized roles cannot modify beneficiary relationships.
