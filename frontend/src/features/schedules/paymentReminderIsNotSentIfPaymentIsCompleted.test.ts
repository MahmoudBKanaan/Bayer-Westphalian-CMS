import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  BACKEND_CRITICAL_TEST_CLASS,
  CANCELLED_REMINDER_STATUS,
  COMPLETED_PAYMENT_STATUS,
  COMPANION_API_TEST_CLASS,
  COMPANION_SERVICE_TEST_CLASS,
  PAYMENT_DUE_REMINDER_TYPE,
  PAYMENT_RECORDS_DOC_PATH,
  PAYMENT_REMINDER_IS_NOT_SENT_IF_PAYMENT_IS_COMPLETED_FR,
  PAYMENT_REMINDER_IS_NOT_SENT_IF_PAYMENT_IS_COMPLETED_ITEM,
  PAYMENT_REMINDER_IS_NOT_SENT_IF_PAYMENT_IS_COMPLETED_RULES,
  PAYMENT_REMINDER_IS_NOT_SENT_IF_PAYMENT_IS_COMPLETED_STATEMENT,
  PAYMENT_REMINDER_PAYMENT_COMPLETED_CODE,
  REMINDER_SCHEDULING_DOC_PATH,
  SENT_REMINDER_STATUS,
  expectedStatusAfterDueSend,
  isPaymentCompleted,
  mustNotSendPaymentReminder,
  provesPaymentReminderBlockedByCompletedPayment,
} from "@/features/schedules/paymentReminderIsNotSentIfPaymentIsCompleted";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../../..");

function readRepoFile(relativeFromRepo: string): string {
  return readFileSync(path.join(repoRoot, relativeFromRepo), "utf8");
}

describe("paymentReminderIsNotSentIfPaymentIsCompleted (item 660)", () => {
  it("locks the critical KB rule identity", () => {
    expect(PAYMENT_REMINDER_IS_NOT_SENT_IF_PAYMENT_IS_COMPLETED_ITEM).toBe(660);
    expect(PAYMENT_REMINDER_IS_NOT_SENT_IF_PAYMENT_IS_COMPLETED_STATEMENT).toBe(
      "Payment reminder is not sent if payment is completed",
    );
    expect(PAYMENT_REMINDER_IS_NOT_SENT_IF_PAYMENT_IS_COMPLETED_RULES).toEqual(["BR-024"]);
    expect(PAYMENT_REMINDER_IS_NOT_SENT_IF_PAYMENT_IS_COMPLETED_FR).toEqual(["FR-080"]);
    expect(COMPLETED_PAYMENT_STATUS).toBe("PAID");
    expect(PAYMENT_DUE_REMINDER_TYPE).toBe("PAYMENT_DUE");
    expect(PAYMENT_REMINDER_PAYMENT_COMPLETED_CODE).toBe("PAYMENT_REMINDER_PAYMENT_COMPLETED");
    expect(BACKEND_CRITICAL_TEST_CLASS).toContain(
      "PaymentReminderIsNotSentIfPaymentIsCompletedTests",
    );
    expect(COMPANION_SERVICE_TEST_CLASS).toContain("PaymentReminderNotSentIfPaymentCompletedTests");
    expect(COMPANION_API_TEST_CLASS).toContain("PaymentReminderNotSentIfPaymentCompletedApiTests");
  });

  it("blocks payment-due send/schedule only when payment is PAID", () => {
    expect(isPaymentCompleted("PAID")).toBe(true);
    expect(isPaymentCompleted("DUE")).toBe(false);

    expect(
      mustNotSendPaymentReminder({
        reminderType: "PAYMENT_DUE",
        paymentStatus: "PAID",
      }),
    ).toBe(true);
    expect(
      mustNotSendPaymentReminder({
        reminderType: "PAYMENT_DUE",
        paymentStatus: "DUE",
      }),
    ).toBe(false);
    expect(
      mustNotSendPaymentReminder({
        reminderType: "PRODUCT_EXPIRATION",
        paymentStatus: "PAID",
      }),
    ).toBe(false);

    expect(
      expectedStatusAfterDueSend({
        reminderType: "PAYMENT_DUE",
        paymentStatus: "PAID",
      }),
    ).toBe(CANCELLED_REMINDER_STATUS);
    expect(
      expectedStatusAfterDueSend({
        reminderType: "PAYMENT_DUE",
        paymentStatus: "DUE",
      }),
    ).toBe(SENT_REMINDER_STATUS);
    expect(
      expectedStatusAfterDueSend({
        reminderType: "PRODUCT_EXPIRATION",
        paymentStatus: "PAID",
      }),
    ).toBe(SENT_REMINDER_STATUS);

    expect(
      provesPaymentReminderBlockedByCompletedPayment({
        reminderType: "PAYMENT_DUE",
        status: "CANCELLED",
        sentAt: null,
      }),
    ).toBe(true);
    expect(
      provesPaymentReminderBlockedByCompletedPayment({
        reminderType: "PAYMENT_DUE",
        status: "SENT",
        sentAt: "2026-07-12T10:00:00Z",
      }),
    ).toBe(false);
  });

  it("documents BR-024 payment-complete exclusion in reminder and payment docs", () => {
    const reminderDocPath = path.join(repoRoot, REMINDER_SCHEDULING_DOC_PATH);
    const paymentDocPath = path.join(repoRoot, PAYMENT_RECORDS_DOC_PATH);
    expect(existsSync(reminderDocPath)).toBe(true);
    expect(existsSync(paymentDocPath)).toBe(true);

    const reminderDoc = readRepoFile(REMINDER_SCHEDULING_DOC_PATH);
    expect(reminderDoc).toContain("660");
    expect(reminderDoc).toContain("PaymentReminderIsNotSentIfPaymentIsCompletedTests");
    expect(reminderDoc).toMatch(/BR-024/);
    expect(reminderDoc).toMatch(/PAID|completed/i);

    const paymentDoc = readRepoFile(PAYMENT_RECORDS_DOC_PATH);
    expect(paymentDoc).toMatch(/reminder|PAID|BR-024/i);
  });
});
