/**
 * Sprint 16 critical test item **660**: Payment reminder is not sent if payment is completed.
 *
 * KB: BR-024 — paid payments must not get payment-due reminders (generate skip, create reject,
 * send/mark-sent cancel without sentAt).
 */

export const PAYMENT_REMINDER_IS_NOT_SENT_IF_PAYMENT_IS_COMPLETED_ITEM = 660;

export const PAYMENT_REMINDER_IS_NOT_SENT_IF_PAYMENT_IS_COMPLETED_STATEMENT =
  "Payment reminder is not sent if payment is completed";

export const PAYMENT_REMINDER_IS_NOT_SENT_IF_PAYMENT_IS_COMPLETED_RULES = [
  "BR-024",
] as const;

export const PAYMENT_REMINDER_IS_NOT_SENT_IF_PAYMENT_IS_COMPLETED_FR = [
  "FR-080",
] as const;

/** Payment status that blocks payment-due reminders. */
export const COMPLETED_PAYMENT_STATUS = "PAID" as const;

export const PAYMENT_DUE_REMINDER_TYPE = "PAYMENT_DUE" as const;

/** Reminder schedule outcome when a payment-due send is blocked by PAID status. */
export const CANCELLED_REMINDER_STATUS = "CANCELLED" as const;

export const SENT_REMINDER_STATUS = "SENT" as const;

/** Backend business-rule code when scheduling after payment is completed. */
export const PAYMENT_REMINDER_PAYMENT_COMPLETED_CODE =
  "PAYMENT_REMINDER_PAYMENT_COMPLETED" as const;

export const BACKEND_CRITICAL_TEST_CLASS =
  "com.bayerwestphalian.campaign.schedule.PaymentReminderIsNotSentIfPaymentIsCompletedTests";

export const COMPANION_SERVICE_TEST_CLASS =
  "com.bayerwestphalian.campaign.schedule.PaymentReminderNotSentIfPaymentCompletedTests";

export const COMPANION_API_TEST_CLASS =
  "com.bayerwestphalian.campaign.schedule.PaymentReminderNotSentIfPaymentCompletedApiTests";

export const REMINDER_SCHEDULING_DOC_PATH = "docs/modules/reminder-scheduling.md";

export const PAYMENT_RECORDS_DOC_PATH = "docs/modules/payment-records.md";

export type PaymentStatusLike = string | null | undefined;
export type ReminderTypeLike = string | null | undefined;
export type ReminderStatusLike = string | null | undefined;

/**
 * True when payment status is completed (paid) for BR-024 purposes.
 */
export function isPaymentCompleted(status: PaymentStatusLike): boolean {
  return status === COMPLETED_PAYMENT_STATUS;
}

/**
 * True when a payment-due reminder must not be scheduled or sent for this payment status.
 */
export function mustNotSendPaymentReminder(options: {
  reminderType: ReminderTypeLike;
  paymentStatus: PaymentStatusLike;
}): boolean {
  if (options.reminderType !== PAYMENT_DUE_REMINDER_TYPE) {
    return false;
  }
  return isPaymentCompleted(options.paymentStatus);
}

/**
 * Expected schedule status after a due-send attempt when payment is already PAID.
 * Product-expiration reminders are unaffected by BR-024.
 */
export function expectedStatusAfterDueSend(options: {
  reminderType: ReminderTypeLike;
  paymentStatus: PaymentStatusLike;
}): typeof CANCELLED_REMINDER_STATUS | typeof SENT_REMINDER_STATUS | "PENDING" {
  if (mustNotSendPaymentReminder(options)) {
    return CANCELLED_REMINDER_STATUS;
  }
  if (options.reminderType === PAYMENT_DUE_REMINDER_TYPE && !isPaymentCompleted(options.paymentStatus)) {
    return SENT_REMINDER_STATUS;
  }
  if (options.reminderType === "PRODUCT_EXPIRATION") {
    return SENT_REMINDER_STATUS;
  }
  return "PENDING";
}

/**
 * True when a reminder view proves BR-024: cancelled payment-due with no sentAt after completion.
 */
export function provesPaymentReminderBlockedByCompletedPayment(reminder: {
  reminderType?: ReminderTypeLike;
  status?: ReminderStatusLike;
  sentAt?: string | null;
}): boolean {
  return (
    reminder.reminderType === PAYMENT_DUE_REMINDER_TYPE &&
    reminder.status === CANCELLED_REMINDER_STATUS &&
    (reminder.sentAt == null || reminder.sentAt === "")
  );
}
