# Product Module Documentation

The product module owns insurance and investment product data for the MVP. It is the backend source
of truth for product catalog CRUD, product search and filtering, product disable and soft-delete
behavior, product ownership assignment, payment records, product-change requests, and audit evidence
for product-related mutations.

## Package Boundary

Primary backend package:

```text
com.bayerwestphalian.campaign.product
```

Core product catalog components:

- `Product`: JPA entity mapped to the `products` table.
- `ProductRepository`: active-product search, type filtering, and persistence access.
- `ProductService`: backend-owned validation, authorization, audit calls, search, disable, and
  soft-delete behavior.
- `ProductController`: REST API boundary under `/api/products`.
- `ProductType`, request, command, search criteria, search request, and view DTOs.

Related product-domain components in the same package:

- `ProductOwnership`, `ProductOwnershipRepository`, `ProductOwnershipService`,
  `ProductOwnershipController`
- `ProductChangeRequest`, `ProductChangeRequestRepository`, `ProductChangeRequestService`,
  `ProductChangeRequestController`
- `PaymentRecord`, `PaymentRecordRepository`, `PaymentRecordService`, `PaymentRecordController`

The product module depends on the customer module for ownership and payment validation, and on
`AuditService` for sensitive-action logging. Product audit behavior is documented separately in
[`Product Audit Logging Documentation`](product-audit-logging.md).

## REST API

Product catalog endpoints return the shared `ApiResponse` wrapper.

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/products` | Search and filter the product catalog. |
| `GET` | `/api/products/{id}` | Product details. |
| `POST` | `/api/products` | Create a product. |
| `PUT` | `/api/products/{id}` | Update product details, pricing, duration, and active state. |
| `PATCH` | `/api/products/{id}/disable` | Disable a product without deleting it. |
| `DELETE` | `/api/products/{id}` | Soft-delete a product. |

Supported search filters are `term`, `productType`, and `active`. Search covers product name and
description. Soft-deleted products are excluded from active lists, search results, and profile
lookups.

Related product-domain APIs:

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/product-ownerships` | List product ownership records. |
| `POST` | `/api/product-ownerships` | Assign a product to a customer. |
| `PUT` | `/api/product-ownerships/{id}` | Update ownership expiration and policy number. |
| `GET` | `/api/product-change-requests` | List product-change requests. |
| `POST` | `/api/product-change-requests` | Create a product-change request. |
| `PUT` | `/api/product-change-requests/{id}` | Update an open request description. |
| `PATCH` | `/api/product-change-requests/{id}/approve` | Approve an open request. |
| `PATCH` | `/api/product-change-requests/{id}/reject` | Reject an open request. |
| `PATCH` | `/api/product-change-requests/{id}/mark-implemented` | Mark an approved request implemented. |
| `GET` | `/api/payment-records` | List payment records. |
| `POST` | `/api/payment-records` | Create a payment record. |
| `PATCH` | `/api/payment-records/{id}/mark-paid` | Mark a payment record paid. |
| `PATCH` | `/api/payment-records/{id}/mark-overdue` | Mark a payment record overdue. |
| `PATCH` | `/api/payment-records/{id}/increment-reminder` | Increment payment reminder count. |

## Domain Rules

- `name` and `productType` are required for product create and update workflows.
- Supported product types are `HOMEOWNER_INSURANCE`, `LIFE_INSURANCE`, `INVESTMENT_FUND`,
  `HEALTH_INSURANCE`, `AUTO_INSURANCE`, and `OTHER`.
- `price` must be greater than or equal to `0.00` when provided.
- `durationMonths` must be positive when provided.
- `expirationPolicy` stores product expiration rules used by later reminder and campaign flows.
- `active` controls whether a product can be promoted or assigned while still remaining visible in
  historical records.
- Product records use soft delete through `deletedAt`; delete requests do not remove rows.
- Disabled products remain stored but are not treated as active catalog offers.
- Backend validation is authoritative; frontend validation is only a user-experience layer.

Product ownership, payment, and product-change workflows reuse the same product catalog records and
must reject inactive, disabled, or soft-deleted products where business rules require active
catalog data.

## Authorization

Spring Security and method-level authorization are the backend access-control boundary.

- Create, update, disable, delete products: `ADMIN`, `PRODUCT_MANAGER`.
- Read/search products: `ADMIN`, `CAMPAIGN_MANAGER`, `BI_ANALYST`, `PRODUCT_MANAGER`,
  `COMPLIANCE_OFFICER`, `CUSTOMER_SERVICE_AGENT`, `SALES_AGENT`, `EXECUTIVE_VIEWER`.
- Manage product ownership and product-change requests: `ADMIN`, `PRODUCT_MANAGER`.
- Create and update payment records: `ADMIN`, `CUSTOMER_SERVICE_AGENT`.
- Read payment records: `ADMIN`, `CAMPAIGN_MANAGER`, `BI_ANALYST`, `COMPLIANCE_OFFICER`,
  `CUSTOMER_SERVICE_AGENT`, `SALES_AGENT`, `EXECUTIVE_VIEWER`, `SYSTEM_AUDITOR`.

Frontend role checks may hide product controls, but every protected product workflow must still be
enforced by backend role authorization.

## Audit And Evidence

The product module records audit entries for product create, update, disable, soft-delete,
ownership assignment, payment status changes, and product-change workflow transitions. Audit
payloads include product identity, pricing, duration, expiration policy, active/deleted flags,
ownership dates, payment status, and change-request workflow status.

Product changes create audit logs. See
[`Product Audit Logging Documentation`](product-audit-logging.md) for entity types, actions, and
review guidance.

## Frontend Boundary

The frontend product experience is implemented by:

- `ProductsPage`: product list, search/filter controls, create form, and inline edit workflow.
- `ProductDetailsPage`: product detail view and edit workflow.
- `ProductChangeRequestsPage`: product-change request tracker and workflow actions.
- `CustomerDetailsPage`: product ownership tab and payment records tab.

Frontend modules:

- `frontend/src/api/products.ts`
- `frontend/src/api/productOwnerships.ts`
- `frontend/src/api/productChangeRequests.ts`
- `frontend/src/api/paymentRecords.ts`
- `frontend/src/components/ProductSearchFilters.tsx`
- `frontend/src/features/products/productSearch.ts`
- `frontend/src/features/auth/permissions.ts`

Role-based product UI permissions must mirror backend rules without replacing them.

## Downstream Use

Product and ownership data support later segmentation, campaign targeting, payment reminders,
product-expiration campaigns, and analytics. Product expiration dates, payment due dates, and
payment status fields must remain accurate because reminder and segmentation workflows depend on
them.

### Product-expiration campaigns

KB business rule `BR-023` allows product-expiration campaigns to start 3, 6, or 12 months before
ownership expiration. The product domain exposes:

- `ProductOwnership.expirationDate` and `ProductOwnership.isExpiringWithinMonths(int months)` for
  in-memory eligibility checks.
- `ProductOwnershipRepository.findExpiringBetween(LocalDate startDate, LocalDate endDate)` for
  active ownership records with non-null expiration dates in a date window.
- `ProductOwnershipService.findExpiringWithinMonths(int months)` for authorized read access to
  ownership records expiring within the requested month window.

Database indexes `idx_product_ownership_expiration`, `idx_product_ownerships_status_expiration`,
and `idx_product_ownerships_product_expiration` keep expiration lookups efficient for campaign
generation and reminder scheduling.

### Segmentation

Saved segment criteria can filter audiences by product ownership, payment history, and product
expiration. The product domain provides segmentation-ready fields and lookups:

- `ProductOwnershipSearchCriteria` supports `customerId`, `productId`, `status`, `expiringFrom`,
  and `expiringTo` filters.
- `ProductOwnershipRepository.findByCustomerId`, `findActiveByProduct`, and `findExpiringBetween`
  support ownership-based audience queries.
- `PaymentRecordRepository.findDuePayments`, `findOverduePayments`, and `findByCustomerId`
  support payment-history segmentation.
- `ProductOwnershipView` exposes `customerId`, `customerFullName`, `productId`, `productName`,
  `productType`, `expirationDate`, and `status` for preview and analytics joins.

### Reminders

Payment and product-expiration reminder workflows depend on persisted ownership and payment state:

- `PaymentRecord.dueDate`, `status`, `reminderCount`, `markPaid()`, `markOverdue()`, and
  `incrementReminder()` support Green/Yellow/Red payment reminder escalation and
  `BR-024` payment-complete exclusion.
- `PaymentRecord.calculateDaysOverdue()` and `isDefaultRisk()` support default-risk detection.
- `Product.expirationPolicy` stores catalog expiration rules used when ownership expiration dates
  are assigned or updated.
- `payment_records_due_status_idx` and `idx_payment_records_customer_status` keep due and overdue
  payment selection efficient for scheduler jobs.

### Analytics

Product performance and campaign analytics require stable links between customers, products,
ownership, and payments:

- `ProductType`, `Product.price`, and `Product.durationMonths` support product-level reporting.
- `ProductOwnershipView.productType` and ownership `status` support ownership and expiration
  analytics.
- `PaymentRecord` links each payment to both `customer_id` and `product_ownership_id` so product
  performance can be aggregated by customer and owned product.
- `PaymentStatus` values `DUE`, `PAID`, `OVERDUE`, and `DEFAULT_RISK` support payment and
  conversion analytics.

## Evidence

The product module must preserve KB evidence that:

- Product Manager and Admin can create, edit, search, and disable products.
- Unauthorized roles cannot create or mutate protected product workflows.
- Products can be assigned to customers with expiration dates.
- Payment records can be created and tracked for customer profiles.
- Product-change requests can be created, updated, approved, rejected, and marked implemented.
- Product search and filter UI supports catalog discovery by term, type, and active state.
- Product changes create audit logs reviewable through the audit log API.

### Production gate

Product and ownership data must support product-expiration campaigns, segmentation, reminders, and
analytics without requiring schema changes in later sprints. The production gate is satisfied when
all of the following remain true:

- Ownership expiration dates are persisted and queryable for 3-, 6-, and 12-month campaign windows
  (`BR-023`, `FR-076`).
- Ownership and payment repositories expose filters needed for product-ownership and payment-history
  segmentation (`FR-073`, `FR-074`).
- Payment due dates, reminder counts, and status transitions support payment reminder scheduling
  (`FR-080` to `FR-084`, `BR-020` to `BR-024`).
- Product, ownership, and payment views expose customer and product join fields needed for analytics
  and product performance reporting.