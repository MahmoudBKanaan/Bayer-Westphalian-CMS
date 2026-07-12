import { apiRequest } from "@/api/client";
import type { ProductType } from "@/api/products";

export type PaymentStatus = "DUE" | "PAID" | "OVERDUE" | "DEFAULT_RISK";

export type PaymentRecordView = {
  id: string;
  customerId: string | null;
  customerFullName: string | null;
  productOwnershipId: string | null;
  productId: string | null;
  productName: string | null;
  productType: ProductType | null;
  dueDate: string | null;
  paidAt: string | null;
  amountDue: number | null;
  amountPaid: number | null;
  status: PaymentStatus;
  reminderCount: number;
  daysOverdue: number;
  defaultRisk: boolean;
};

export type PaymentRecordSearchFilters = {
  customerId?: string;
  status?: PaymentStatus | "ALL";
};

export type CreatePaymentRecordPayload = {
  customerId: string;
  productOwnershipId: string;
  dueDate: string;
  amountDue: string;
};

export type UpdatePaymentRecordPayload = {
  dueDate: string;
  amountDue: string;
};

export type MarkPaymentPaidPayload = {
  amountPaid: string;
};

type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
};

export async function listPaymentRecords(
  filters?: PaymentRecordSearchFilters,
): Promise<PaymentRecordView[]> {
  const response = await apiRequest<ApiResponse<PaymentRecordView[]>>(
    `/payment-records${paymentRecordSearchQuery(filters)}`,
  );

  return response.data;
}

export async function listCustomerPaymentRecords(customerId: string): Promise<PaymentRecordView[]> {
  return listPaymentRecords({ customerId });
}

export async function createPaymentRecord(
  payload: CreatePaymentRecordPayload,
): Promise<PaymentRecordView> {
  const response = await apiRequest<ApiResponse<PaymentRecordView>>("/payment-records", {
    method: "POST",
    body: JSON.stringify({
      customerId: payload.customerId.trim(),
      productOwnershipId: payload.productOwnershipId.trim(),
      dueDate: payload.dueDate.trim(),
      amountDue: parseRequiredAmount(payload.amountDue, "amountDue"),
    }),
  });

  return response.data;
}

export async function updatePaymentRecord(
  id: string,
  payload: UpdatePaymentRecordPayload,
): Promise<PaymentRecordView> {
  const response = await apiRequest<ApiResponse<PaymentRecordView>>(
    `/payment-records/${encodeURIComponent(id.trim())}`,
    {
      method: "PUT",
      body: JSON.stringify({
        dueDate: payload.dueDate.trim(),
        amountDue: parseRequiredAmount(payload.amountDue, "amountDue"),
      }),
    },
  );

  return response.data;
}

export async function markPaymentPaid(
  id: string,
  payload: MarkPaymentPaidPayload,
): Promise<PaymentRecordView> {
  const response = await apiRequest<ApiResponse<PaymentRecordView>>(
    `/payment-records/${encodeURIComponent(id.trim())}/mark-paid`,
    {
      method: "PATCH",
      body: JSON.stringify({
        amountPaid: parseRequiredAmount(payload.amountPaid, "amountPaid"),
        paidAt: null,
      }),
    },
  );

  return response.data;
}

export async function markPaymentOverdue(id: string): Promise<PaymentRecordView> {
  const response = await apiRequest<ApiResponse<PaymentRecordView>>(
    `/payment-records/${id}/mark-overdue`,
    { method: "PATCH" },
  );

  return response.data;
}

export async function incrementPaymentReminder(id: string): Promise<PaymentRecordView> {
  const response = await apiRequest<ApiResponse<PaymentRecordView>>(
    `/payment-records/${id}/increment-reminder`,
    { method: "PATCH" },
  );

  return response.data;
}

function paymentRecordSearchQuery(filters?: PaymentRecordSearchFilters) {
  if (filters == null) {
    return "";
  }

  const params = new URLSearchParams();
  if (filters.customerId != null && filters.customerId.trim().length > 0) {
    params.set("customerId", filters.customerId.trim());
  }
  if (filters.status != null && filters.status !== "ALL") {
    params.set("status", filters.status);
  }

  const query = params.toString();
  return query.length === 0 ? "" : `?${query}`;
}

function parseAmount(value: string) {
  const trimmed = value.trim();
  if (trimmed.length === 0) {
    return null;
  }

  const parsed = Number(trimmed);
  return Number.isFinite(parsed) ? parsed : null;
}

function parseRequiredAmount(value: string, fieldName: string) {
  const parsed = parseAmount(value);
  if (parsed == null) {
    throw new Error(`${fieldName} must be a valid number.`);
  }
  return parsed;
}
