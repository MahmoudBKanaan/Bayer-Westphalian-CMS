# Reminder Scheduling Documentation

The reminder scheduling module owns payment-due and product-expiration reminder schedules, bulk
generation rules, due-send processing, and scheduler attempt logging (KB epic **E18** Reminder
scheduling). It supports automated Green/Yellow/Red payment escalation, 3/6/12-month product
expiration windows, consent and contact-limit enforcement, and operator-visible reminder worklists.

## Package Boundary

Primary backend package:

```text
com.bayerwestphalian.campaign.schedule
```

Reminder components:

- `ReminderSchedule`: JPA entity mapped to the `reminder_schedules` table.
- `ReminderRepository`: due-reminder lookup, status/customer listing, and persistence.
- `ReminderService`: create, generate, send, cancel, and search workflows with business rules.
- `ReminderController`: REST API boundary under `/api/reminders`.
- `ReminderProcessingScheduler`: cron job and admin manual trigger for due processing (FR-089).
- `PaymentReminderLevelRules`: Green/Yellow/Red escalation from payment `reminder_count` (BR-020–022).
- `ProductExpirationReminderRules`: 3/6/12-month windows and urgency levels (BR-023).
- `ReminderType`, `ReminderLevel`, `ReminderStatus`, request, command, search criteria, and view DTOs.

The module depends on:

- **Customer** module: every reminder requires a customer.
- **Product** module: every reminder requires a product.
- **Payment records** / **product ownership**: candidates for payment-due and expiration generation.
- **EligibilityService** (`campaign` package): consent and monthly contact-limit checks (item 401).

## Data Model

Table: `reminder_schedules` (KB entity fields).

| Field | Notes |
| --- | --- |
| `id` | UUID primary key |
| `customer_id` | Required FK to `customers` |
| `product_id` | Required FK to `products` |
| `reminder_type` | `PAYMENT_DUE` or `PRODUCT_EXPIRATION` |
| `reminder_level` | `GREEN`, `YELLOW`, or `RED` |
| `scheduled_date` | Required date when the reminder is due for processing |
| `status` | `PENDING`, `SENT`, `FAILED`, `CANCELLED` |
| `created_at` | Set on create |
| `sent_at` | Set when status becomes `SENT`; cleared on cancel/fail |

Index guidance from the KB: `idx_reminder_schedules_date` supports due reminder selection.

### Reminder types

- `PAYMENT_DUE` — unpaid payment follow-up (FR-080–084).
- `PRODUCT_EXPIRATION` — ownership nearing expiration (FR-085–087 / BR-023).

### Reminder levels

Full Green/Yellow/Red rules: [Green / Yellow / Red Reminder Rules](green-yellow-red-reminder-rules.md)
(KB BR-020–BR-022, item 405).

Payment escalation (KB BR-020–BR-022):

| Level | Meaning | Payment rule |
| --- | --- | --- |
| `GREEN` | First reminder | `reminder_count == 0` |
| `YELLOW` | Second reminder | `reminder_count == 1` |
| `RED` | Third reminder / likely default risk | `reminder_count >= 2` or status `DEFAULT_RISK` |

Product-expiration urgency (KB BR-023):

| Window before expiration | Level |
| --- | --- |
| 3 months | `RED` (nearest / highest urgency) |
| 6 months | `YELLOW` |
| 12 months | `GREEN` |

### Status lifecycle

- New schedules start as `PENDING`.
- `SENT` records `sent_at` after successful due processing / mark-sent.
- `CANCELLED` is used when send rules fail (payment already completed, ineligible recipient); `sent_at` remains unset.
- `FAILED` marks a failed attempt outcome when applicable.

## REST API

Reminder endpoints return the shared `ApiResponse` wrapper.

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/reminders/payment` | Manually schedule a payment-due reminder. |
| `POST` | `/api/reminders/payment/generate` | Generate payment-due reminders for unpaid due/overdue payments. |
| `POST` | `/api/reminders/expiration` | Manually schedule a product-expiration reminder. |
| `POST` | `/api/reminders/expiration/3-month/generate` | Generate expiration reminders in the 3-month window. |
| `POST` | `/api/reminders/expiration/6-month/generate` | Generate expiration reminders in the 6-month window. |
| `POST` | `/api/reminders/expiration/12-month/generate` | Generate expiration reminders in the 12-month window. |
| `POST` | `/api/reminders/due/send` | Process due reminders (send or cancel per rules). |
| `POST` | `/api/reminders/due/manual-trigger` | Admin manual scheduler run (dev/test environments only). |
| `PUT` | `/api/reminders/{id}/sent` | Mark a single reminder sent (or cancel if rules fail). |
| `PUT` | `/api/reminders/{id}/cancel` | Cancel a reminder. |
| `GET` | `/api/reminders` | Search and list reminder schedules. |

### Search filters

Supported query parameters on `GET /api/reminders`:

- `customerId`
- `status`
- `dueOnOrBefore`

### Generate parameters

Payment and expiration generate endpoints accept optional `asOfDate` (ISO date). When omitted, the
service uses today. Generation is idempotent for the same customer, product, level, and scheduled
date (duplicates are skipped).

## Domain Rules

### Payment reminders (BR-020–BR-024, FR-080–084)

- **BR-020**: the first unpaid payment reminder is `GREEN`.
- **BR-021**: the second unpaid payment reminder is `YELLOW`.
- **BR-022**: the third unpaid payment reminder is `RED` and indicates likely default risk.
- **BR-023**: product-expiration reminders use 12/6/3-month windows for Green/Yellow/Red urgency.
- Payment-due generation selects unpaid `DUE`, `OVERDUE`, and `DEFAULT_RISK` payment records with
  `dueDate` on or before the evaluation date.
- Reminder level is resolved by `PaymentReminderLevelRules` from payment `reminder_count` /
  default-risk status (Green first, Yellow second, Red third).
- **BR-024**: a payment reminder must not be scheduled or sent when the related payment is
  already completed (`PAID`). Create rejects with `PAYMENT_REMINDER_PAYMENT_COMPLETED`; send/
  mark-sent **cancels** the schedule instead of marking `SENT`.

Sprint 16 critical item **660** (*Payment reminder is not sent if payment is completed*):
primary suite `PaymentReminderIsNotSentIfPaymentIsCompletedTests` (companions:
`PaymentReminderNotSentIfPaymentCompletedTests`,
`PaymentReminderNotSentIfPaymentCompletedApiTests`). Frontend catalog:
`frontend/src/features/schedules/paymentReminderIsNotSentIfPaymentIsCompleted.ts`.
- Red payment generation may increment payment reminder history toward likely default risk.

### Product-expiration reminders (BR-023, FR-085–087)

- Manual create requires **active** product ownership for the customer and product.
- Bulk generation searches ownerships expiring between `asOfDate` and
  `asOfDate.plusMonths(windowMonths)` via `ProductOwnershipRepository.findExpiringBetween`.
- Windows: 3 months (`RED`), 6 months (`YELLOW`), 12 months (`GREEN`).
- Existing non-cancelled expiration reminders at the same level for the same customer/product are
  not duplicated.

### Consent and contact limits (item 401 / BR-011 / FR-034 / FR-092)

- Every create, generate, and send path evaluates the recipient through
  `EligibilityService.evaluateForReminder` with `MARKETING_EMAIL` consent.
- Rules include do-not-contact, marketing opt-out, invalid/missing consent (including guardian when
  required), and the monthly marketing contact limit from system settings
  (`SystemSettingsService.monthlyContactLimit()`, item 535; seeded by `app.contact.monthly-limit`).
- Manual create throws `REMINDER_RECIPIENT_INELIGIBLE` when eligibility fails.
- Bulk generate **skips** ineligible candidates.
- Due send / mark-sent **cancels** the schedule (does not set `sent_at`) when eligibility fails.

### Scheduler (FR-089 / items 402 and 406)

Full operator and developer guide: [Reminder Scheduler Documentation](reminder-scheduler.md).

`ReminderProcessingScheduler` processes due reminders on a configurable cron:

```yaml
app.reminders.processing-cron: "0 */15 * * * *"  # default every 15 minutes
```

Each scheduled or manual run logs:

- processing started (`trigger=scheduled|manual`)
- one structured **reminder attempt** line per processed schedule (id, customer, product, type,
  level, status, scheduled date, sentAt)
- zero-attempt summary when no due reminders exist
- completion with `processedCount`, `sentCount`, `cancelledCount`, `failedCount`
- ERROR with stack on failure (not swallowed)

Manual trigger (`POST /api/reminders/due/manual-trigger`) is Admin-only and restricted to `dev`
and `test` Spring profiles. The cron path runs under a system Campaign Manager security principal
and restores the previous authentication after each run.

Backend validation is authoritative; frontend validation is only a user-experience layer.

## Authorization

Spring Security and method-level authorization are the backend access-control boundary.

| Operation | Allowed roles |
| --- | --- |
| Create payment / expiration reminder | `ADMIN`, `CAMPAIGN_MANAGER` |
| Generate payment / 3-6-12 month expiration | `ADMIN`, `CAMPAIGN_MANAGER`, `CUSTOMER_SERVICE_AGENT` |
| Send due / mark sent / cancel | `ADMIN`, `CAMPAIGN_MANAGER` |
| Manual scheduler trigger | `ADMIN` (dev/test only) |
| List / search reminders | `ADMIN`, `CAMPAIGN_MANAGER`, `CUSTOMER_SERVICE_AGENT`, `SALES_AGENT`, `COMPLIANCE_OFFICER` |

KB role summary alignment:

- **Campaign Manager**: create payment and product-expiration reminder campaigns/schedules.
- **Customer Service Agent**: generate and view operational reminders; payment follow-up.
- **Admin**: full access including manual scheduler trigger in non-production profiles.
- **Compliance Officer / Sales Agent**: read access to reminder schedules where authorized.

## Frontend Boundary

- Reminders screen: `frontend/src/pages/RemindersPage.tsx`
- Level badges: `frontend/src/components/ReminderLevelBadge.tsx`
- API client: `frontend/src/api/reminders.ts`
- Dashboard may surface reminder alerts for authorized roles.

## Configuration

| Property | Purpose | Default |
| --- | --- | --- |
| `app.reminders.processing-cron` | Scheduler cron expression | `0 */15 * * * *` |
| System settings / `app.contact.monthly-limit` | Monthly marketing contact limit (runtime via System Settings, item 535; property seeds default) | `3` |

Environment override example: `REMINDER_PROCESSING_CRON`, `CONTACT_MONTHLY_LIMIT`.

## Downstream Use

- Payment records supply unpaid due/overdue candidates and reminder counts for Green/Yellow/Red.
- Product ownerships supply expiration dates for 3/6/12-month windows.
- Eligibility and consent modules block non-compliant reminder contact.
- Follow-up tasks (E17) may be created after reminder contact outcomes (FR-088 / FR-093).
- Analytics and AI default-risk scoring may consume payment reminder history (AI-004).

Related modules:

- [Reminder Scheduler Documentation](reminder-scheduler.md)
- [Green / Yellow / Red Reminder Rules](green-yellow-red-reminder-rules.md)
- [Payment Record Documentation](payment-records.md)
- [Product Ownership Documentation](product-ownership.md)
- [Follow-Up Task Documentation](follow-up-tasks.md)
- [Eligibility Rules Documentation](../architecture/eligibility-rules.md)
- [Communication Tracking Documentation](communication-tracking.md)

## Production Gate (KB item 409)

Reminder logic **must** respect all of the following on create, generate, and send paths:

| Guardrail | Requirement | Primary rules |
| --- | --- | --- |
| **Consent** | Valid marketing consent; respect opt-out / DNC / guardian rules | FR-034, item 401 |
| **Payment status** | No payment-due reminder when payment is `PAID`; cancel on send if paid | BR-024 |
| **Expiration dates** | Expiration reminders only for active ownerships with expiration dates inside 3/6/12-month windows | BR-023 |
| **Contact frequency limits** | Monthly marketing contact limit blocks schedule/generate/send | BR-011, FR-092 |

`ReminderService` enforces these via `EligibilityService.evaluateForReminder`, payment status checks,
and ownership expiration candidate filters. Acceptance coverage:
`ReminderLogicRespectsConsentPaymentExpirationAndContactLimitsTests`.

## Acceptance Criteria (KB / Sprint 11)

- Payment due reminder is generated for unpaid due/overdue payments.
- Green is the first payment reminder; Yellow second; Red third / default risk (BR-020–022).
- Payment reminder is not sent if payment is completed (BR-024).
- Product-expiration reminder is generated 3, 6, and 12 months before expiration (BR-023).
- Reminder respects consent and contact limits (item 401).
- Reminder logic respects consent, payment status, expiration dates, and contact frequency limits
  (production gate item 409).
- Scheduler logs reminder attempts (FR-089 / item 402).
- Unauthorized roles cannot create, generate, or send protected reminder workflows.

## Evidence For Demo And Review

- Reminders screen screenshot with Green/Yellow/Red badges
- Payment reminder generation demonstration
- Product-expiration generation for 3/6/12-month windows
- Paid-payment exclusion / cancel-on-send demonstration
- Scheduler log lines for reminder attempts
- Role permission checks for Campaign Manager vs unauthorized roles
- Sprint 11 review notes linking E18 reminder scheduling
