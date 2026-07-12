/**
 * Schedule package for reminder schedules, payment reminders, product-expiration reminders, and
 * scheduled jobs (KB epic E18).
 *
 * <p>Reminder create/generate/send paths enforce production-gate constraints (item 409): marketing
 * consent and monthly contact limits via {@code EligibilityService.evaluateForReminder} (item 401 /
 * BR-011 / FR-092), payment status (BR-024), and ownership expiration dates/windows (BR-023).
 *
 * <p>{@code ReminderProcessingScheduler} logs every reminder attempt on cron and manual runs (KB
 * FR-089 / item 402).
 *
 * <p>See {@code docs/modules/reminder-scheduling.md} for module documentation, {@code
 * docs/modules/green-yellow-red-reminder-rules.md} for Green/Yellow/Red level rules (KB BR-020–022 /
 * item 405), and {@code docs/modules/reminder-scheduler.md} for cron/manual scheduler operations
 * (item 406 / FR-089).
 */
package com.bayerwestphalian.campaign.schedule;
