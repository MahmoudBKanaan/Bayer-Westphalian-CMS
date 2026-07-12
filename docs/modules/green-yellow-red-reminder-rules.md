# Green / Yellow / Red Reminder Rules

This document defines the **Green / Yellow / Red** reminder level rules used by the Bayer-Westphalian
campaign platform (KB **BR-020**, **BR-021**, **BR-022**, and level mapping under **BR-023** for
product-expiration windows). Levels appear on `reminder_schedules.reminder_level` and in the
Reminders UI badge.

Primary implementation:

```text
com.bayerwestphalian.campaign.schedule.PaymentReminderLevelRules
com.bayerwestphalian.campaign.schedule.ProductExpirationReminderRules
com.bayerwestphalian.campaign.schedule.ReminderLevel
```

Full module context: [Reminder Scheduling Documentation](reminder-scheduling.md).

## Business Rules (KB)

| Rule ID | Statement |
| --- | --- |
| **BR-020** | Green reminder is the **first** reminder |
| **BR-021** | Yellow reminder is the **second** reminder |
| **BR-022** | Red reminder is the **third** reminder and indicates **likely default risk** |
| **BR-023** | Product-expiration campaigns/reminders may start 3, 6, or 12 months before expiration (levels map to urgency windows) |
| **BR-024** | Payment reminder must not be sent if payment is completed (level rules only apply to unpaid candidates) |

Related functional requirements:

- **FR-081** — System sends Green first reminder
- **FR-082** — System sends Yellow second reminder
- **FR-083** — System sends Red third reminder
- **FR-084** — System identifies likely payment default
- **TC-007** — Red reminder appears after previous reminders

## ReminderLevel Enumeration

`ReminderLevel` values:

| Level | UI label | Payment meaning | Default-risk signal |
| --- | --- | --- | --- |
| `GREEN` | Green | First payment reminder | No |
| `YELLOW` | Yellow | Second payment reminder | No |
| `RED` | Red | Third payment reminder | Yes (likely default risk) |

Frontend badge: `frontend/src/components/ReminderLevelBadge.tsx` (`reminder-level-green|yellow|red`).

## Payment Escalation Rules (BR-020–BR-022)

Payment-due generation uses `PaymentReminderLevelRules` to map unpaid payment history to a level
before creating a `PAYMENT_DUE` `ReminderSchedule`.

### Count thresholds

Constants on `PaymentReminderLevelRules`:

| Constant | Value | Meaning |
| --- | --- | --- |
| `FIRST_REMINDER_COUNT` | `0` | No prior payment reminders → Green |
| `SECOND_REMINDER_COUNT` | `1` | One prior reminder → Yellow |
| `THIRD_REMINDER_COUNT` | `2` | Two or more prior reminders → Red |

### Resolution algorithm

`PaymentReminderLevelRules.resolve(PaymentRecord payment)`:

1. If payment is null → reject (required argument).
2. If `payment.status == DEFAULT_RISK` → **RED** (always).
3. Else resolve from `payment.reminderCount`:
   - `reminder_count == 0` (or unexpected negative) → **GREEN** (BR-020)
   - `reminder_count == 1` → **YELLOW** (BR-021)
   - `reminder_count >= 2` → **RED** (BR-022)

`resolveFromReminderCount(int reminderCount)` implements the count mapping alone.

Helper predicates:

- `isFirstReminder(level)` → true only for `GREEN`
- `isSecondReminder(level)` → true only for `YELLOW`
- `isThirdReminder(level)` → true only for `RED`

### Escalation sequence

Typical unpaid payment journey:

```text
reminder_count=0  →  GREEN  (first reminder)
reminder_count=1  →  YELLOW (second reminder)
reminder_count>=2 →  RED    (third reminder / default risk)
status=DEFAULT_RISK → RED   (regardless of count)
```

New payment records start with `reminderCount = 0` and status `DUE`, so the first generated
payment-due reminder is **Green**.

### Interaction with payment records

- Payment records store `reminder_count` and statuses `DUE`, `PAID`, `OVERDUE`, `DEFAULT_RISK`
  (see [Payment Record Documentation](payment-records.md)).
- `PaymentRecord.incrementReminder()` advances the count; after the configured threshold the
  payment may transition to `DEFAULT_RISK`.
- When a **Red** payment-due reminder is generated, `ReminderService` may call payment reminder
  increment logic so default-risk state advances after the third-level reminder (FR-084).
- **BR-024** / critical item **660**: paid payments are never candidates for payment-due generation or successful send
  (`PaymentReminderIsNotSentIfPaymentIsCompletedTests`);
  level rules are not applied to completed payments.

## Product-Expiration Level Mapping (BR-023)

Product-expiration reminders reuse the same `ReminderLevel` enum, but levels represent **urgency of
the expiration window**, not payment reminder_count:

| Window before expiration | Level | Rule helper |
| --- | --- | --- |
| 3 months | `RED` | `ProductExpirationReminderRules.threeMonthReminderLevel()` |
| 6 months | `YELLOW` | `ProductExpirationReminderRules.sixMonthReminderLevel()` |
| 12 months | `GREEN` | `ProductExpirationReminderRules.twelveMonthReminderLevel()` |

Generation endpoints:

- `POST /api/reminders/expiration/3-month/generate` → RED
- `POST /api/reminders/expiration/6-month/generate` → YELLOW
- `POST /api/reminders/expiration/12-month/generate` → GREEN

Do not confuse payment escalation (count-based) with expiration urgency (window-based). Both store
`GREEN` / `YELLOW` / `RED` on `reminder_schedules` but for different business meanings.

## Where Levels Are Applied

| Path | Behavior |
| --- | --- |
| `ReminderService.generatePaymentDueReminders` | Sets level via `PaymentReminderLevelRules.resolve(payment)` |
| Manual `POST /api/reminders/payment` | Client supplies `reminderLevel` on the schedule command |
| Expiration generate (3/6/12 month) | Level fixed by `ProductExpirationReminderRules` for the window |
| Manual `POST /api/reminders/expiration` | Client supplies `reminderLevel` |
| UI list / badges | Display `reminderLevel` from `ReminderScheduleView` |

## Authorization And Compliance Notes

- Generating payment-due and expiration reminders: `ADMIN`, `CAMPAIGN_MANAGER`,
  `CUSTOMER_SERVICE_AGENT`.
- Creating manual schedules: `ADMIN`, `CAMPAIGN_MANAGER`.
- Levels do not bypass consent or monthly contact limits; ineligible recipients are still skipped
  or cancelled (see reminder scheduling item 401 rules).
- Red indicates **likely** default risk for operators; it is not an automated legal or credit
  decision without human review.

## Configuration Notes

Escalation thresholds are currently **code constants** on `PaymentReminderLevelRules` (0 / 1 / 2).
Sprint retrospective notes suggest making reminder rules configurable in system settings in a later
sprint; until then, BR-020–022 mapping is fixed in application logic for transparent, testable
behavior.

## Acceptance Criteria

- Green is resolved when `reminder_count == 0` (BR-020).
- Yellow is resolved when `reminder_count == 1` (BR-021).
- Red is resolved when `reminder_count >= 2` (BR-022).
- Default-risk payment status always resolves to Red.
- Escalation order is Green → Yellow → Red.
- Product-expiration uses Red/Yellow/Green for 3/6/12-month windows respectively (BR-023).
- UI exposes Green/Yellow/Red badges for operators.
- Unauthorized roles cannot generate payment reminders that apply these levels.

## Evidence For Demo And Review

- Payment with `reminder_count=0` generates Green
- Payment after one prior reminder generates Yellow
- Payment after two prior reminders generates Red
- Default-risk payment generates Red
- 3/6/12-month expiration levels Red/Yellow/Green
- ReminderLevelBadge screenshot on Reminders page
- Unit tests: `PaymentReminderLevelRulesTests`, `GreenReminderIsFirstReminderTests`,
  `YellowReminderIsSecondReminderTests`, `RedReminderIsThirdReminderTests`
