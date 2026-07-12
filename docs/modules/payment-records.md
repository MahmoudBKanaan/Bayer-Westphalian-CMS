# Payment Record Documentation

The payment record module tracks customer payment due dates, paid amounts, reminder counts, and
payment status for owned products. Payment records link customers to `product_ownerships` so payment reminders,
segmentation by payment history, default-risk detection, and product performance
analytics can use reliable billing state.

## Package Boundary

Primary backend package:

```text
com.bayerwestphalian.campaign.product
```

Payment record components:

- `PaymentRecord`: JPA entity mapped to the `payment_records` table.
- `PaymentRecordRepository`: due-payment lookup, overdue/default-risk lookup, and customer payment
  listing.
- `PaymentRecordService`: backend-owned validation, authorization, create/update workflows,
  reminder escalation, and payment search.
- `PaymentRecordController`: REST API boundary under `/api/payment-records`.
- `PaymentStatus`, request, command, search criteria, search request, and view DTOs.

The module depends on the customer module to validate existing, non-deleted customers and on
product ownership records to ensure each payment belongs to the specified customer through
`product_ownership_id`.

## REST API

Payment record endpoints return the shared `ApiResponse` wrapper.

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/payment-records` | Search and list payment records. |
| `POST` | `/api/payment-records` | Create a payment record. |
| `PUT` | `/api/payment-records/{id}` | Update an unpaid payment record due date and amount due. |
| `PATCH` | `/api/payment-records/{id}/mark-paid` | Mark a payment record paid. |
| `PATCH` | `/api/payment-records/{id}/mark-overdue` | Mark a payment record overdue. |
| `PATCH` | `/api/payment-records/{id}/increment-reminder` | Increment payment reminder count. |

Supported search filters on `GET /api/payment-records` are `customerId` and `status`.

`PaymentRecordService` also exposes scheduler-friendly lookups used by authorized backend roles:

- `findDuePayments()`
- `findOverduePayments()`
- `listCustomerPayments(UUID customerId)`

## Domain Rules

- `customerId`, `productOwnershipId`, `dueDate`, and `amountDue` are required when creating a
  payment record.
- `amountDue` and `amountPaid` must be greater than or equal to `0.00` when provided.
- The customer must exist and must not be soft-deleted.
- `productOwnershipId` must belong to the specified customer.
- Supported payment statuses are `DUE`, `PAID`, `OVERDUE`, and `DEFAULT_RISK`.
- New payment records start in `DUE` status with `reminderCount = 0`.
- `updateDetails()` changes editable billing metadata: `dueDate` and `amountDue`.
- Paid records cannot be updated.
- `markPaid()` stores `amountPaid`, `paidAt`, and transitions status to `PAID`.
- `markOverdue()` transitions unpaid records to `OVERDUE`.
- `incrementReminder()` increments `reminderCount` for unpaid records and transitions to
  `DEFAULT_RISK` after the third reminder.
- Paid records cannot be marked overdue or receive additional reminder increments (`BR-024`).
- Sprint 16 critical item **660** (*Payment reminder is not sent if payment is completed*):
  `PaymentReminderIsNotSentIfPaymentIsCompletedTests` asserts generate/create/send paths never
  mark a payment-due reminder `SENT` when status is `PAID` (cancel or skip instead).
- `PaymentRecord.calculateDaysOverdue()` and `PaymentRecord.isDefaultRisk()` support overdue and
  default-risk analytics.
- `PaymentRecordView` exposes `daysOverdue` and `defaultRisk` for UI and reporting consumers.
- Backend validation is authoritative; frontend validation is only a user-experience layer.

Reminder escalation aligns with KB payment reminder levels:

- Green reminder is the first reminder (`BR-020`).
- Yellow reminder is the second reminder (`BR-021`).
- Red reminder is the third reminder and indicates likely default risk (`BR-022`).

Database constraints and indexes protect payment integrity and downstream query performance:

- `payment_records_amount_due_non_negative` and `payment_records_amount_paid_non_negative`.
- `payment_records_reminder_count_non_negative`.
- `payment_records_due_status_idx` and `idx_payment_records_customer_status` support due and overdue
  payment selection for reminder scheduling.

## Authorization

Spring Security and method-level authorization are the backend access-control boundary.

- Create payment records: `ADMIN`, `CUSTOMER_SERVICE_AGENT`.
- Update payment records: `ADMIN`, `CUSTOMER_SERVICE_AGENT`.
- Mark paid, mark overdue, and increment reminder: `ADMIN`, `CUSTOMER_SERVICE_AGENT`.
- Search/list payment records: `ADMIN`, `CAMPAIGN_MANAGER`, `BI_ANALYST`, `COMPLIANCE_OFFICER`,
  `CUSTOMER_SERVICE_AGENT`, `SALES_AGENT`, `EXECUTIVE_VIEWER`, `SYSTEM_AUDITOR`.

Frontend role checks may hide payment controls, but every protected payment workflow must still be
enforced by backend role authorization.

## Audit And Evidence

Payment record create, update, mark paid, mark overdue, and reminder increment actions create audit log
entries through `AuditService`. Audit payloads include `customerId`, `productOwnershipId`,
`dueDate`, `amountDue`, `amountPaid`, `paidAt`, `status`, `reminderCount`, and `defaultRisk`.

Payment record audit behavior is part of the wider product audit model documented in
[`Product Audit Logging Documentation`](product-audit-logging.md).

## Frontend Boundary

The frontend customer details experience includes the payment records tab. That tab:

- Lists payment records for the selected customer profile.
- Shows owned product, due date, amount due, amount paid, status, reminder count, days overdue,
  and default-risk state.
- Allows authorized users to create a payment record for a selected owned product.
- Allows authorized users to update an unpaid payment record due date and amount due.
- Allows authorized users to mark a payment record paid.

Frontend modules:

- `frontend/src/api/paymentRecords.ts`
- `frontend/src/pages/CustomerDetailsPage.tsx`

## Downstream Use

Payment record data supports later workflows that depend on accurate due dates and payment status:

- Payment reminder scheduling and Green/Yellow/Red escalation (`FR-080` to `FR-084`).
- Segmentation by payment history (`FR-074`).
- Default-risk scoring from missed payments, overdue days, and reminder count (`AI-004`).
- Product performance and conversion analytics linked through `product_ownership_id`.

See also [`Product Module Documentation`](product-module.md) and
[`Product Ownership Documentation`](product-ownership.md) for related product-domain boundaries.

## Evidence

The payment record module must preserve KB evidence that:

- A payment record can be created for a customer-owned product.
- A payment record can be updated while unpaid.
- A payment record can be marked paid.
- Payment records can be listed on the customer profile.
- Overdue and reminder workflows update `status` and `reminderCount`.
- Paid payments are excluded from further reminder increments.
- Unauthorized roles cannot create or mutate protected payment workflows.
- Payment record changes create audit logs reviewable through the audit log API.
