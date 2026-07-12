# Product Audit Logging Documentation

The product domain records audit entries for every sensitive product, ownership, payment, and
product-change workflow mutation. Audit logs are persisted through `AuditService` inside the same
database transaction as the product change so compliance officers and system auditors can review
what changed, when it changed, and which actor performed the action.

## Package Boundary

Primary backend packages:

```text
com.bayerwestphalian.campaign.product
com.bayerwestphalian.campaign.audit
```

Product services that emit audit logs:

- `ProductService`: product create, update, disable, and soft-delete actions.
- `ProductOwnershipService`: product ownership assignment and ownership updates.
- `PaymentRecordService`: payment record create, mark paid, mark overdue, and reminder updates.
- `ProductChangeRequestService`: product-change request create, update, approve, reject, and
  mark implemented actions.

Audit persistence boundary:

- `AuditService`: creates immutable audit log entries.
- `AuditLog`: JPA entity mapped to the `audit_logs` table.
- `AuditLogRepository`: stores and lists audit history.
- `AuditController`: exposes `GET /api/audit-logs` for authorized audit review.

## Audited Entity Types

| Entity type | Service | Audited actions |
| --- | --- | --- |
| `products` | `ProductService` | `CREATE`, `UPDATE`, `DELETE` |
| `product_ownerships` | `ProductOwnershipService` | `CREATE`, `UPDATE` |
| `payment_records` | `PaymentRecordService` | `CREATE`, `UPDATE` |
| `product_change_requests` | `ProductChangeRequestService` | `CREATE`, `UPDATE`, `APPROVE`, `REJECT` |

## Audit Payload Fields

### Products

Audit payloads for products include `id`, `name`, `productType`, `description`, `price`,
`durationMonths`, `expirationPolicy`, `active`, and `deleted`.

Item 527 (log product changes):

- `POST /api/products` → `CREATE` on `products` with Product Manager/Admin actor and product payload.
- `PUT /api/products/{id}` → `UPDATE` with before/after product fields (including price, active flag).
- Disable path → `UPDATE` with `active` true → false.
- Soft delete → `DELETE` with `deleted` false → true.
- Validation failures and missing products do not create audit rows.

### Product ownerships

Audit payloads for product ownerships include `customerId`, `productId`, `policyNumber`,
`startDate`, `expirationDate`, and `status`.

### Payment records

Audit payloads for payment records include `customerId`, `productOwnershipId`, `dueDate`,
`amountDue`, `amountPaid`, `paidAt`, `status`, `reminderCount`, and `defaultRisk`.

### Product change requests

Audit payloads for product change requests include `productId`, `requestType`, `description`,
`status`, and `requestedByUserId`.

Product-change approvals and rejections store old and new workflow status values so auditors can
trace OPEN to APPROVED, OPEN to REJECTED, or APPROVED to IMPLEMENTED transitions.

## Authorization And Review

Spring Security and method-level authorization protect both product mutations and audit review.

- Product create, update, disable, ownership assignment, payment updates, and product-change
  workflow actions: `ADMIN`, `PRODUCT_MANAGER`, or role-specific payment permissions where
  applicable.
- Audit log review: `ADMIN`, `COMPLIANCE_OFFICER`, `SYSTEM_AUDITOR`.

Frontend role checks may hide product-management controls, but audit logging is enforced in the
backend service layer and must not depend on UI-only checks.

## Audit API

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/audit-logs` | List persisted audit entries for authorized audit review. |

Audit list responses include `action`, `entityType`, `entityId`, `oldValue`, `newValue`,
`actorUserId`, and `createdAt`.

## Evidence

The product domain must preserve KB evidence that:

- Product create, edit, disable, and soft-delete actions create audit logs (item 527).
- Product ownership assignment and ownership updates create audit logs.
- Payment record create and payment status updates create audit logs.
- Product-change request create, update, approve, reject, and implement actions create audit logs.
- Product changes create audit logs that remain reviewable through the audit log API.
- Compliance officers and system auditors can inspect product-related sensitive actions without
  modifying product data.