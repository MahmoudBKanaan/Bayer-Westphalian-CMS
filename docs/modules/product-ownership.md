# Product Ownership Documentation

The product ownership module tracks which insurance and investment products are owned by customers,
including policy numbers, coverage start dates, expiration dates, and ownership status. Ownership
records connect the customer module and product catalog so campaigns, segmentation, reminders, and
analytics can target customers by owned products and upcoming expirations.

## Package Boundary

Primary backend package:

```text
com.bayerwestphalian.campaign.product
```

Product ownership components:

- `ProductOwnership`: JPA entity mapped to the `product_ownerships` table.
- `ProductOwnershipRepository`: customer ownership lookup, active-product lookup, and expiration
  range queries.
- `ProductOwnershipService`: backend-owned validation, authorization, assignment, ownership
  updates, customer-profile listing, and expiration-window lookup.
- `ProductOwnershipController`: REST API boundary under `/api/product-ownerships`.
- `OwnershipStatus`, request, command, search criteria, search request, and view DTOs.

The module depends on the customer module to validate existing, non-deleted customers and on the
product catalog to validate active, non-deleted products. Payment records reference ownership
records through `product_ownership_id`.

## REST API

Product ownership endpoints return the shared `ApiResponse` wrapper.

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/product-ownerships` | List product ownership records for a customer profile. |
| `POST` | `/api/product-ownerships` | Assign a product to a customer. |

`GET /api/product-ownerships` requires the `customerId` query parameter.

`ProductOwnershipService` also implements ownership update and expiration-window lookup workflows
used by authorized backend roles and later scheduler or segmentation services:

- `updateOwnership(UUID ownershipId, UpdateProductOwnershipCommand command)`
- `findExpiringWithinMonths(int months)`
- `listCustomerProducts(UUID customerId)`

## Domain Rules

- `customerId`, `productId`, and `startDate` are required when assigning ownership.
- `expirationDate` is optional on assign but must be on or after `startDate` when provided.
- `policyNumber` is optional and must be at most 100 characters when provided.
- Policy numbers are unique across ownership records.
- The customer must exist and must not be soft-deleted.
- The product must exist, must not be soft-deleted, and must be active.
- Supported ownership statuses are `ACTIVE`, `EXPIRED`, and `CANCELLED`.
- `ProductOwnership.isActive()` treats ownership as active when status is `ACTIVE` and the
  expiration date is null or not before today.
- `ProductOwnership.isExpiringWithinMonths(int months)` supports KB product-expiration campaign
  windows of 3, 6, and 12 months.
- `ProductOwnershipRepository.findExpiringBetween(LocalDate startDate, LocalDate endDate)` returns
  active ownership records with non-null expiration dates in the requested date range.
- Update commands must include `expirationDate` or `policyNumber`.
- Backend validation is authoritative; frontend validation is only a user-experience layer.

Database constraints and indexes protect ownership integrity and downstream query performance:

- `product_ownerships_expiration_after_start` ensures expiration is not before start date.
- `idx_product_ownership_expiration`, `idx_product_ownerships_status_expiration`, and
  `idx_product_ownerships_product_expiration` support expiration campaign and reminder lookups.

## Authorization

Spring Security and method-level authorization are the backend access-control boundary.

- Assign product ownership: `ADMIN`, `PRODUCT_MANAGER`.
- Update ownership expiration and policy number: `ADMIN`, `PRODUCT_MANAGER`,
  `CUSTOMER_SERVICE_AGENT`.
- List customer ownership records: `ADMIN`, `CAMPAIGN_MANAGER`, `BI_ANALYST`, `PRODUCT_MANAGER`,
  `COMPLIANCE_OFFICER`, `CUSTOMER_SERVICE_AGENT`, `SALES_AGENT`, `EXECUTIVE_VIEWER`.
- Find ownership records expiring within a month window: same read roles as customer ownership
  listing.

Frontend role checks may hide ownership controls, but every protected ownership workflow must still
be enforced by backend role authorization.

## Audit And Evidence

Product ownership assignment and ownership updates create audit log entries through `AuditService`.
Audit payloads include `customerId`, `productId`, `policyNumber`, `startDate`, `expirationDate`,
and `status`.

Product ownership audit behavior is part of the wider product audit model documented in
[`Product Audit Logging Documentation`](product-audit-logging.md).

## Frontend Boundary

The frontend customer details experience includes the product ownership tab. That tab:

- Lists owned products for the selected customer profile.
- Shows product name, product type, policy number, start date, expiration date, status, and active
  coverage state.
- Allows authorized users to assign a product to the customer with start date, expiration date, and
  policy number.

Frontend modules:

- `frontend/src/api/productOwnerships.ts`
- `frontend/src/pages/CustomerDetailsPage.tsx`

## Downstream Use

Product ownership data supports later workflows that depend on accurate expiration dates:

- product-expiration campaigns starting 3, 6, or 12 months before expiration (`BR-023`).
- Segmentation by product ownership and product expiration (`FR-073`, `FR-076`).
- Product-expiration reminder scheduling.
- Product performance and ownership analytics joins.

See also [`Product Module Documentation`](product-module.md) for the wider product-domain boundary.

## Evidence

The product ownership module must preserve KB evidence that:

- A product can be assigned to a customer.
- Product ownership expiration date is saved.
- Ownership records can be listed on the customer profile.
- Inactive or soft-deleted products cannot be assigned.
- Unauthorized roles cannot assign product ownership.
- Ownership assignment and updates create audit logs reviewable through the audit log API.