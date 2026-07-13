# Production Scheduler Logging

**Sprint 18 item 730** - Configure scheduler logging.

The reminder scheduler uses the production stdout/rotation policy from item **729** and adds stable
scheduler fields for operations. Its dedicated logger level is controlled by
`LOG_LEVEL_SCHEDULER`, defaulting to `INFO`.

## Schedule identity

- `REMINDER_PROCESSING_CRON` defaults to every 15 minutes.
- `REMINDER_PROCESSING_ZONE` defaults explicitly to `UTC`, avoiding host-timezone drift.
- Every invocation generates a `runId` UUID and places it in the `schedulerRunId` MDC field.
- The same run ID appears explicitly on start, every reminder attempt, completion, and failure.
- MDC is cleared after the run, including exceptional completion.

## Stable events

| `schedulerEvent` | Level | Required fields |
| --- | --- | --- |
| `run_started` | INFO | trigger, runId, active/default profiles |
| `reminder_attempt` | INFO | trigger, runId, reminder/customer/product IDs, type, level, status, dates; or `attemptCount=0` |
| `run_completed` | INFO | trigger, runId, durationMs, processed/sent/cancelled/failed counts |
| `run_failed` | ERROR | trigger, runId, durationMs, error type and throwable |
| `manual_blocked` | WARN | active/default profiles |

The IDs are operational references, not authorization evidence. Scheduler logs do not replace
contact-event history, consent checks, eligibility decisions, or immutable audit logs.

## Operator queries

```powershell
docker compose --env-file .env.production -f docker-compose.prod.yml logs --since 1h backend `
  | Select-String "schedulerEvent="

docker compose --env-file .env.production -f docker-compose.prod.yml logs backend `
  | Select-String "schedulerEvent=run_failed"
```

Copy a `runId` from a start/failure line to retrieve all lines for one invocation. Keep log evidence
value-free and do not export customer details beyond approved operational access.

Alert when `run_failed` occurs, `failedCount` is nonzero, a scheduled `run_completed` event is absent
for more than the expected cron interval plus tolerance, or duration rises materially. A zero-attempt
run is healthy when no reminders are due.

Changing cron or timezone requires release/change approval and backend recreation. Validate the cron
outside production first; an invalid expression fails startup rather than silently disabling the
job.

Automated evidence: `ProductionSchedulerLoggingTests`, `SchedulerLogsReminderAttemptsTests`, and
`SchedulerLoggingConfigurationDocumentationTests`.
