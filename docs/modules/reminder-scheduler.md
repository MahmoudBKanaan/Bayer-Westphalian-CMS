# Reminder Scheduler Documentation

This document describes the **due-reminder processing scheduler** for the Bayer-Westphalian campaign
platform (KB epic **E18**, **FR-089** “System logs all reminder attempts”, Sprint 11 items 385–386 /
402 / **406**).

The scheduler is the operational job that processes **pending** `reminder_schedules` that are due,
applies send rules (payment completed, consent, contact limits), and writes structured logs for
every attempt.

Related documents:

- [Reminder Scheduling Documentation](reminder-scheduling.md) — full reminder module (create,
  generate, API, domain rules)
- [Green / Yellow / Red Reminder Rules](green-yellow-red-reminder-rules.md) — level semantics

## Package Boundary

| Component | Role |
| --- | --- |
| `com.bayerwestphalian.campaign.schedule.ReminderProcessingScheduler` | Cron job + manual trigger orchestration and logging |
| `ReminderService.sendDueReminders()` | Business send/cancel rules for due schedules |
| `CampaignApplication` | Enables scheduling via `@EnableScheduling` |
| `ReminderController` | Admin manual trigger endpoint |

Primary class:

```text
com.bayerwestphalian.campaign.schedule.ReminderProcessingScheduler
```

## Enabling Scheduling

Spring scheduling is enabled on the application entry point:

```java
@SpringBootApplication
@EnableScheduling
public class CampaignApplication { ... }
```

Without `@EnableScheduling`, the `@Scheduled` method on `ReminderProcessingScheduler` would not
run. Unit tests assert that `CampaignApplication` declares `@EnableScheduling`.

## Cron Configuration

The scheduled method:

```java
@Scheduled(cron = PROCESSING_CRON)
public void processDueReminders() { ... }
```

| Item | Value |
| --- | --- |
| Property | `app.reminders.processing-cron` |
| Placeholder constant | `ReminderProcessingScheduler.PROCESSING_CRON` |
| Default expression | `0 */15 * * * *` (every 15 minutes) |
| Environment override | `REMINDER_PROCESSING_CRON` |

Example in `application.yml`:

```yaml
app:
  reminders:
    processing-cron: "${REMINDER_PROCESSING_CRON:0 */15 * * * *}"
```

Operators may tighten or relax the interval in production via environment variables. The default
fits Sprint 11 operational reminder processing without continuous polling.

## Scheduled Run (`processDueReminders`)

### Security context

Method-secured `ReminderService.sendDueReminders()` requires `ADMIN` or `CAMPAIGN_MANAGER`. The
cron path therefore:

1. Saves the previous `SecurityContext` authentication (if any).
2. Installs a **system Campaign Manager** principal:
   - User id: `00000000-0000-0000-0000-000000000000`
   - Email: `system-reminder-scheduler@bayerwestphalian.local`
   - Authority: `ROLE_CAMPAIGN_MANAGER`
3. Invokes processing.
4. **Always** restores the previous authentication (or clears the context) in a `finally` block.

This prevents the background job from leaking system credentials into subsequent request threads
and allows `@PreAuthorize` on send to succeed without a human login.

### Processing flow

1. Log processing **started** (`trigger=scheduled`).
2. Call `reminderService.sendDueReminders()` (due date = today when no `asOfDate` is used by the
   no-arg overload).
3. Log each **reminder attempt** (or zero-attempt summary).
4. Log processing **completed** with outcome counts.
5. On runtime failure: log **failed** with error type/message and stack, then rethrow (not
   swallowed).

`sendDueReminders` applies:

- **BR-024** / critical item **660** — cancel payment-due reminders when payment is already paid
  (no `sent_at`); suite `PaymentReminderIsNotSentIfPaymentIsCompletedTests`.
- **Item 401** — cancel when consent / monthly contact limit / DNC eligibility fails.
- Otherwise mark reminder **SENT** with `sent_at` set.

## Manual Trigger (`triggerManualProcessing`)

### API

| Method | Path | Role |
| --- | --- | --- |
| `POST` | `/api/reminders/due/manual-trigger` | `ADMIN` only |

Controller message on success: `Manual reminder processing triggered`.

Also available: `POST /api/reminders/due/send` for `ADMIN` / `CAMPAIGN_MANAGER` to process due
reminders with optional `asOfDate` **without** going through the scheduler wrapper (no scheduler
start/attempt/completion log markers unless the service path is used from the scheduler).

### Environment guard

Manual trigger is allowed only when active or default Spring profiles include **`dev`** or
**`test`**.

Outside those profiles (for example `prod`):

- Logs a **WARN**: `Reminder scheduler manual trigger blocked ...`
- Throws `ForbiddenException`:  
  `Manual reminder processing is available only in development or test environments`
- Does **not** call `ReminderService`

This keeps production due processing on the cron job only, while still supporting demo/test
operator control.

### Manual logging

Uses the same structured logging as the cron path with `trigger=manual`.

## Scheduler Logging (FR-089 / item 402)

All log markers are package-visible constants on `ReminderProcessingScheduler` for consistent tests
and log scrapes:

| Constant | Marker text |
| --- | --- |
| `LOG_PROCESSING_STARTED` | `Reminder scheduler processing started` |
| `LOG_REMINDER_ATTEMPT` | `Reminder scheduler reminder attempt` |
| `LOG_PROCESSING_COMPLETED` | `Reminder scheduler processing completed` |
| `LOG_PROCESSING_FAILED` | `Reminder scheduler processing failed` |

### Start line (INFO)

```text
Reminder scheduler processing started trigger={scheduled|manual}
  activeProfiles=... defaultProfiles=...
```

### Per-attempt line (INFO)

One line per processed schedule:

```text
Reminder scheduler reminder attempt trigger=... reminderId=... customerId=... productId=...
  reminderType=... reminderLevel=... status=... scheduledDate=... sentAt=...
```

Status may be `SENT`, `CANCELLED`, or `FAILED` after send rules.

### Empty batch (INFO)

When no due reminders are returned:

```text
Reminder scheduler reminder attempt trigger=... attemptCount=0 (no due reminders processed)
```

### Completion line (INFO)

```text
Reminder scheduler processing completed trigger=... processedCount=...
  sentCount=... cancelledCount=... failedCount=...
```

### Failure line (ERROR)

```text
Reminder scheduler processing failed trigger=... errorType=... errorMessage=...
```

Includes the throwable for stack traces. Failures are rethrown after logging.

## Operational Guidance

| Topic | Guidance |
| --- | --- |
| Production | Rely on cron; keep manual trigger blocked by non-dev/test profiles |
| Dev / Test | Use Admin `POST /api/reminders/due/manual-trigger` for demos |
| Observability | Grep for `Reminder scheduler reminder attempt` and completion counts |
| Tuning | Adjust `REMINDER_PROCESSING_CRON` / `app.reminders.processing-cron` |
| Eligibility | Contact limits use Admin System Settings monthly limit (item 535; default from `app.contact.monthly-limit`) during send rules |
| Time zone | Spring cron default follows JVM/system zone; align deployment TZ with business day |

## Authorization Summary

| Action | Roles / constraints |
| --- | --- |
| Cron `processDueReminders` | System principal `CAMPAIGN_MANAGER` (internal) |
| Manual API trigger | `ADMIN` + profile `dev` or `test` |
| Direct due send API | `ADMIN`, `CAMPAIGN_MANAGER` |

## Acceptance Criteria (KB item 406 / FR-089)

- Scheduling is enabled on the Spring Boot application.
- A configurable cron processes due reminders on a schedule.
- Scheduled runs install and restore security context safely.
- Manual trigger works in development and test; blocked (and logged) elsewhere.
- Every run logs start, each attempt (or zero-attempt summary), and completion counts.
- Failures are logged at ERROR with stack and not silently swallowed.
- Scheduler documentation is linked from the documentation index and reminder scheduling module doc.

## Evidence For Demo And Review

- Application log snippet showing `trigger=scheduled` start / attempt / completion
- Manual trigger demonstration in `dev` or `test`
- Forbidden response when manual trigger is called under `prod` profile
- Cron property in `application.yml` / environment
- Unit tests: `ReminderProcessingSchedulerTests`, `SchedulerLogsReminderAttemptsTests`

## Test References

| Test class | Focus |
| --- | --- |
| `ReminderProcessingSchedulerTests` | Cron annotation, security context restore, env guard, log markers |
| `SchedulerLogsReminderAttemptsTests` | FR-089 attempt logging (item 402) |
| `ReminderControllerTests` | Manual trigger API wiring |
