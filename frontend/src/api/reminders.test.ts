import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import {
  cancelReminder,
  createExpirationReminder,
  createPaymentReminder,
  formatReminderEnum,
  listReminders,
  manuallyTriggerReminderProcessing,
  markReminderSent,
  reminderQuery,
  sendDueReminders,
} from "@/api/reminders";
import { AUTH_STORAGE_KEYS } from "@/auth/sessionStorageStrategy";

const reminder = {
  id: "90000000-0000-0000-0000-000000000001",
  customerId: "20000000-0000-0000-0000-000000000001",
  customerFullName: "Ada Lovelace",
  productId: "30000000-0000-0000-0000-000000000001",
  productName: "Car Insurance",
  productType: "AUTO_INSURANCE",
  reminderType: "PAYMENT_DUE",
  reminderLevel: "GREEN",
  scheduledDate: "2026-08-01",
  status: "PENDING",
  createdAt: "2026-07-11T10:00:00Z",
  sentAt: null,
  due: true,
};

const form = {
  customerId: ` ${reminder.customerId} `,
  productId: ` ${reminder.productId} `,
  reminderLevel: "GREEN" as const,
  scheduledDate: "2026-08-01",
};

describe("reminders api", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("loads reminders with KB filters", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = mockReminderResponse([reminder]);
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      listReminders({
        customerId: reminder.customerId,
        status: "PENDING",
        dueOnOrBefore: "2026-08-01",
      }),
    ).resolves.toEqual([reminder]);

    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/reminders?customerId=${reminder.customerId}&status=PENDING&dueOnOrBefore=2026-08-01`,
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: "Bearer access-token" }),
      }),
    );
  });

  it("creates payment and product-expiration reminders with trimmed IDs", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = mockReminderResponse(reminder);
    vi.stubGlobal("fetch", fetchMock);

    await createPaymentReminder(form);
    await createExpirationReminder({ ...form, reminderLevel: "YELLOW" });

    expect(fetchMock.mock.calls[0][0]).toBe(`${API_BASE_URL}/reminders/payment`);
    expect(JSON.parse(fetchMock.mock.calls[0][1]?.body as string)).toEqual({
      customerId: reminder.customerId,
      productId: reminder.productId,
      reminderLevel: "GREEN",
      scheduledDate: "2026-08-01",
    });
    expect(fetchMock.mock.calls[1][0]).toBe(`${API_BASE_URL}/reminders/expiration`);
    expect(JSON.parse(fetchMock.mock.calls[1][1]?.body as string)).toEqual({
      customerId: reminder.customerId,
      productId: reminder.productId,
      reminderLevel: "YELLOW",
      scheduledDate: "2026-08-01",
    });
  });

  it("calls due processing, manual trigger, mark sent, and cancel endpoints", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = mockReminderResponse([reminder]);
    vi.stubGlobal("fetch", fetchMock);

    await sendDueReminders("2026-08-01");
    await manuallyTriggerReminderProcessing();
    await markReminderSent(reminder.id);
    await cancelReminder(reminder.id);

    expect(fetchMock.mock.calls[0][0]).toBe(
      `${API_BASE_URL}/reminders/due/send?asOfDate=2026-08-01`,
    );
    expect(fetchMock.mock.calls[0][1]?.method).toBe("POST");
    expect(fetchMock.mock.calls[1][0]).toBe(`${API_BASE_URL}/reminders/due/manual-trigger`);
    expect(fetchMock.mock.calls[1][1]?.method).toBe("POST");
    expect(fetchMock.mock.calls[2][0]).toBe(`${API_BASE_URL}/reminders/${reminder.id}/sent`);
    expect(fetchMock.mock.calls[2][1]?.method).toBe("PUT");
    expect(fetchMock.mock.calls[3][0]).toBe(`${API_BASE_URL}/reminders/${reminder.id}/cancel`);
    expect(fetchMock.mock.calls[3][1]?.method).toBe("PUT");
  });

  it("omits blank filters and ALL status values", () => {
    expect(
      reminderQuery({
        customerId: " ",
        status: "SENT",
        dueOnOrBefore: "2026-08-31",
      }),
    ).toBe("?status=SENT&dueOnOrBefore=2026-08-31");
    expect(
      reminderQuery({
        customerId: "",
        status: "ALL",
        dueOnOrBefore: "",
      }),
    ).toBe("");
  });

  it("formats reminder enum display text", () => {
    expect(formatReminderEnum("PRODUCT_EXPIRATION")).toBe("Product Expiration");
  });
});

function mockReminderResponse(data: unknown) {
  return vi.fn().mockResolvedValue({
    ok: true,
    json: async () => ({
      success: true,
      message: "OK",
      data,
    }),
  });
}
