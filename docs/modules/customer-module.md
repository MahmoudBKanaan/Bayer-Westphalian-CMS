# Customer Module Documentation

The customer module owns customer and prospect profile workflows for the MVP. It is the backend
source of truth for customer data, search behavior, contactability, profile validation, soft delete,
CSV import, and audit evidence for customer profile changes.

## Package Boundary

Primary backend package:

```text
com.bayerwestphalian.campaign.customer
```

The module contains:

- `Customer`: JPA entity mapped to the `customers` table.
- `CustomerRepository`: active-profile search and filter persistence access.
- `CustomerService`: backend-owned validation, authorization, audit calls, import processing,
  search, pagination, and soft-delete behavior.
- `CustomerController`: REST API boundary under `/api/customers`.
- Request, command, criteria, import result, import error, and view DTOs.

Beneficiary links are implemented in the adjacent `beneficiary` package because they model
relationships between two customer records. That package depends on `CustomerRepository` only for
validating existing, non-deleted policyholder and beneficiary customers.

## REST API

Customer endpoints return the shared `ApiResponse` wrapper.

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/customers` | Paginated customer/prospect list with search and filters. |
| `GET` | `/api/customers/{id}` | Customer profile details. |
| `POST` | `/api/customers` | Create customer or prospect profile. |
| `PUT` | `/api/customers/{id}` | Update profile, status, demographics, and contact preference. |
| `DELETE` | `/api/customers/{id}` | Soft-delete a customer profile. |
| `POST` | `/api/customers/import` | Import customers and prospects from CSV. |

Supported list filters are `term`, `customerType`, `status`, `city`, `country`, `contactable`,
`page`, and `size`. Search covers name, email, phone, city, country, and source fields. Soft-deleted
customers are excluded from active lists, search results, and profile lookups.

Soft-deleted customers are excluded from active lists, search results, and profile lookups.

## Domain Rules

- `customerType`, `firstName`, and `lastName` are required.
- Email and phone formats are validated by backend rules.
- `status` defaults to `ACTIVE` when a created customer does not provide a status.
- `doNotContact` controls contactability and is respected by customer search filters.
- Customer records use soft delete through `deletedAt`; delete requests do not remove rows.
- Backend validation is authoritative; frontend validation is only a user-experience layer.

## Authorization

Spring Security and method-level authorization are the backend access-control boundary.

- Create customer and CSV import: `ADMIN`, `CUSTOMER_SERVICE_AGENT`.
- Update customer: `ADMIN`, `CUSTOMER_SERVICE_AGENT`, `COMPLIANCE_OFFICER`.
- Soft-delete customer: `ADMIN`.
- Read/search customer profiles: `ADMIN`, `CAMPAIGN_MANAGER`, `BI_ANALYST`,
  `COMPLIANCE_OFFICER`, `CUSTOMER_SERVICE_AGENT`, `SALES_AGENT`.

Frontend role checks may hide controls, but every protected customer workflow must still be
enforced by backend role authorization.

## Audit And Evidence

The customer module records audit entries for create, update, and soft-delete workflows. Audit
payloads include customer type, name, contact fields when present, city, country, age group, status,
`doNotContact`, active/deleted flags, and source.

CSV import behavior is documented separately in
[`Customer CSV Import Guide`](../admin/customer-csv-import-guide.md). Valid rows are imported,
invalid rows are rejected, and row-level errors report `lineNumber`, `field`, `message`, and
`value`.

## Frontend Boundary

The frontend customer experience is implemented by `CustomersPage` and `CustomerDetailsPage`.
It provides the customer list, search/filter controls, create/edit form, CSV import UI, customer
details, and the beneficiaries tab. Role-based customer UI permissions must mirror the backend
rules without replacing them.
