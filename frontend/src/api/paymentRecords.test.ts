import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import {
  createPaymentRecord,
  incrementPaymentReminder,
  listCustomerPaymentRecords,
  markPaymentOverdue,
  markPaymentPaid,
  updatePaymentRecord,
} from "@/api/paymentRecords";
import { AUTH_STORAGE_KEYS, type SystemRoleName } from "@/auth/sessionStorageStrategy";

const paymentRecord = {
  id: "43000000-0000-0000-0000-000000000001",
  customerId: "20000000-0000-0000-0000-000000000001",
  customerFullName: "Ada Policyholder",
  productOwnershipId: "41000000-0000-0000-0000-000000000001",
  productId: "41000000-0000-0000-0000-000000000201",
  productName: "Life Protection",
  productType: "LIFE_INSURANCE",
  dueDate: "2026-07-15",
  paidAt: null,
  amountDue: 129.99,
  amountPaid: null,
  status: "DUE",
  reminderCount: 0,
  daysOverdue: 0,
  defaultRisk: false,
};

describe("paymentRecords api", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("loads payment records for a customer profile", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Payment records loaded",
        data: [paymentRecord],
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(listCustomerPaymentRecords(paymentRecord.customerId as string)).resolves.toEqual([
      paymentRecord,
    ]);

    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/payment-records?customerId=${paymentRecord.customerId}`,
      {
        headers: {
          "Content-Type": "application/json",
          Authorization: "Bearer access-token",
        },
      },
    );
  });

  it("returns saved payment records when listing customer payments", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Payment records loaded",
        data: [paymentRecord],
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    const payments = await listCustomerPaymentRecords(paymentRecord.customerId as string);

    expect(payments).toEqual([paymentRecord]);
    expect(payments[0]?.dueDate).toBe("2026-07-15");
    expect(payments[0]?.amountDue).toBe(129.99);
    expect(payments[0]?.status).toBe("DUE");
  });

  it("creates a payment record", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Payment record created",
        data: paymentRecord,
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    const createdPayment = await createPaymentRecord({
      customerId: paymentRecord.customerId as string,
      productOwnershipId: paymentRecord.productOwnershipId as string,
      dueDate: "2026-07-15",
      amountDue: "129.99",
    });

    expect(createdPayment).toEqual(paymentRecord);
    expect(createdPayment.dueDate).toBe("2026-07-15");
    expect(createdPayment.amountDue).toBe(129.99);
    expect(createdPayment.status).toBe("DUE");

    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/payment-records`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer access-token",
      },
      body: JSON.stringify({
        customerId: paymentRecord.customerId,
        productOwnershipId: paymentRecord.productOwnershipId,
        dueDate: "2026-07-15",
        amountDue: 129.99,
      }),
    });
  });

  it("does not send create requests with an invalid amount due", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      createPaymentRecord({
        customerId: paymentRecord.customerId as string,
        productOwnershipId: paymentRecord.productOwnershipId as string,
        dueDate: "2026-07-15",
        amountDue: "",
      }),
    ).rejects.toThrow("amountDue must be a valid number.");

    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("allows a customer service agent to create a payment record", async () => {
    const agentToken = createAccessToken(["CUSTOMER_SERVICE_AGENT"]);
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, agentToken);
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Payment record created",
        data: paymentRecord,
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    const createdPayment = await createPaymentRecord({
      customerId: paymentRecord.customerId as string,
      productOwnershipId: paymentRecord.productOwnershipId as string,
      dueDate: "2026-08-01",
      amountDue: "89.50",
    });

    expect(createdPayment.status).toBe("DUE");
    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/payment-records`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${agentToken}`,
      },
      body: JSON.stringify({
        customerId: paymentRecord.customerId,
        productOwnershipId: paymentRecord.productOwnershipId,
        dueDate: "2026-08-01",
        amountDue: 89.5,
      }),
    });
  });

  it("updates a payment record due date and amount due", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const updatedPaymentRecord = {
      ...paymentRecord,
      dueDate: "2026-08-01",
      amountDue: 150.25,
    };
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Payment record updated",
        data: updatedPaymentRecord,
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      updatePaymentRecord(paymentRecord.id, {
        dueDate: "2026-08-01",
        amountDue: "150.25",
      }),
    ).resolves.toEqual(updatedPaymentRecord);

    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/payment-records/${paymentRecord.id}`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer access-token",
      },
      body: JSON.stringify({
        dueDate: "2026-08-01",
        amountDue: 150.25,
      }),
    });
  });

  it("does not send update requests with an invalid amount due", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      updatePaymentRecord(paymentRecord.id, {
        dueDate: "2026-08-01",
        amountDue: "not-a-number",
      }),
    ).rejects.toThrow("amountDue must be a valid number.");

    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("marks a payment record as paid", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Payment record marked paid",
        data: {
          ...paymentRecord,
          status: "PAID",
          amountPaid: 129.99,
          paidAt: "2026-07-10T09:30:00Z",
        },
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    const paidPayment = await markPaymentPaid(paymentRecord.id, { amountPaid: "129.99" });

    expect(paidPayment).toEqual({
      ...paymentRecord,
      status: "PAID",
      amountPaid: 129.99,
      paidAt: "2026-07-10T09:30:00Z",
    });
    expect(paidPayment.status).toBe("PAID");
    expect(paidPayment.amountPaid).toBe(129.99);

    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/payment-records/${paymentRecord.id}/mark-paid`,
      {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
          Authorization: "Bearer access-token",
        },
        body: JSON.stringify({
          amountPaid: 129.99,
          paidAt: null,
        }),
      },
    );
  });

  it("does not send mark-paid requests with a blank amount paid", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);

    await expect(markPaymentPaid(paymentRecord.id, { amountPaid: "" })).rejects.toThrow(
      "amountPaid must be a valid number.",
    );

    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("allows a customer service agent to mark a payment record paid", async () => {
    const agentToken = createAccessToken(["CUSTOMER_SERVICE_AGENT"]);
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, agentToken);
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Payment record marked paid",
        data: {
          ...paymentRecord,
          status: "PAID",
          amountPaid: 89.5,
          paidAt: "2026-07-10T09:30:00Z",
        },
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    const paidPayment = await markPaymentPaid(paymentRecord.id, { amountPaid: "89.50" });

    expect(paidPayment.status).toBe("PAID");
    expect(paidPayment.amountPaid).toBe(89.5);
    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/payment-records/${paymentRecord.id}/mark-paid`,
      {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${agentToken}`,
        },
        body: JSON.stringify({
          amountPaid: 89.5,
          paidAt: null,
        }),
      },
    );
  });

  it("marks a payment record overdue and increments reminders", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          success: true,
          message: "Payment record marked overdue",
          data: { ...paymentRecord, status: "OVERDUE", reminderCount: 1 },
        }),
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          success: true,
          message: "Payment record reminder incremented",
          data: { ...paymentRecord, status: "OVERDUE", reminderCount: 2 },
        }),
      });
    vi.stubGlobal("fetch", fetchMock);

    await expect(markPaymentOverdue(paymentRecord.id)).resolves.toEqual({
      ...paymentRecord,
      status: "OVERDUE",
      reminderCount: 1,
    });
    await expect(incrementPaymentReminder(paymentRecord.id)).resolves.toEqual({
      ...paymentRecord,
      status: "OVERDUE",
      reminderCount: 2,
    });

    expect(fetchMock).toHaveBeenNthCalledWith(
      1,
      `${API_BASE_URL}/payment-records/${paymentRecord.id}/mark-overdue`,
      {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
          Authorization: "Bearer access-token",
        },
      },
    );
    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      `${API_BASE_URL}/payment-records/${paymentRecord.id}/increment-reminder`,
      {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
          Authorization: "Bearer access-token",
        },
      },
    );
  });
});

function createAccessToken(roles: SystemRoleName[]) {
  const payload = btoa(JSON.stringify({ roles }))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");

  return `header.${payload}.signature`;
}
