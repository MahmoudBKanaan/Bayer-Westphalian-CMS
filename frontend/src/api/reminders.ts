import { apiRequest } from "@/api/client";
import type { ProductType } from "@/api/products";

export type ReminderType = "PAYMENT_DUE" | "PRODUCT_EXPIRATION";
export type ReminderLevel = "GREEN" | "YELLOW" | "RED";
export type ReminderStatus = "PENDING" | "SENT" | "FAILED" | "CANCELLED";

export type ReminderScheduleView = {
  id: string;
  customerId: string;
  customerFullName: string;
  productId: string;
  productName: string;
  productType: ProductType;
  reminderType: ReminderType;
  reminderLevel: ReminderLevel;
  scheduledDate: string;
  status: ReminderStatus;
  createdAt: string | null;
  sentAt: string | null;
  due: boolean;
};

export type ReminderFilters = {
  customerId: string;
  status: ReminderStatus | "ALL";
  dueOnOrBefore: string;
};

export type ReminderFormPayload = {
  customerId: string;
  productId: string;
  reminderLevel: ReminderLevel;
  scheduledDate: string;
};

type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
};

export const reminderStatuses: ReminderStatus[] = ["PENDING", "SENT", "FAILED", "CANCELLED"];
export const reminderLevels: ReminderLevel[] = ["GREEN", "YELLOW", "RED"];

export const emptyReminderFilters: ReminderFilters = {
  customerId: "",
  status: "ALL",
  dueOnOrBefore: "",
};

export const emptyReminderForm: ReminderFormPayload = {
  customerId: "",
  productId: "",
  reminderLevel: "GREEN",
  scheduledDate: "",
};

export async function listReminders(
  filters: ReminderFilters = emptyReminderFilters,
): Promise<ReminderScheduleView[]> {
  const response = await apiRequest<ApiResponse<ReminderScheduleView[]>>(
    `/reminders${reminderQuery(filters)}`,
  );
  return response.data;
}

export async function createPaymentReminder(
  payload: ReminderFormPayload,
): Promise<ReminderScheduleView> {
  const response = await apiRequest<ApiResponse<ReminderScheduleView>>("/reminders/payment", {
    method: "POST",
    body: JSON.stringify(toReminderPayload(payload)),
  });
  return response.data;
}

export async function createExpirationReminder(
  payload: ReminderFormPayload,
): Promise<ReminderScheduleView> {
  const response = await apiRequest<ApiResponse<ReminderScheduleView>>("/reminders/expiration", {
    method: "POST",
    body: JSON.stringify(toReminderPayload(payload)),
  });
  return response.data;
}

export async function sendDueReminders(asOfDate = ""): Promise<ReminderScheduleView[]> {
  const query = asOfDate.trim().length === 0 ? "" : `?asOfDate=${encodeURIComponent(asOfDate)}`;
  const response = await apiRequest<ApiResponse<ReminderScheduleView[]>>(
    `/reminders/due/send${query}`,
    { method: "POST" },
  );
  return response.data;
}

export async function manuallyTriggerReminderProcessing(): Promise<ReminderScheduleView[]> {
  const response = await apiRequest<ApiResponse<ReminderScheduleView[]>>(
    "/reminders/due/manual-trigger",
    { method: "POST" },
  );
  return response.data;
}

export async function markReminderSent(id: string): Promise<ReminderScheduleView> {
  const response = await apiRequest<ApiResponse<ReminderScheduleView>>(`/reminders/${id}/sent`, {
    method: "PUT",
  });
  return response.data;
}

export async function cancelReminder(id: string): Promise<ReminderScheduleView> {
  const response = await apiRequest<ApiResponse<ReminderScheduleView>>(`/reminders/${id}/cancel`, {
    method: "PUT",
  });
  return response.data;
}

export function reminderQuery(filters: ReminderFilters) {
  const params = new URLSearchParams();
  appendOptionalParam(params, "customerId", filters.customerId);
  if (filters.status !== "ALL") {
    params.set("status", filters.status);
  }
  appendOptionalParam(params, "dueOnOrBefore", filters.dueOnOrBefore);

  const query = params.toString();
  return query.length === 0 ? "" : `?${query}`;
}

export function formatReminderEnum(value: string) {
  return value
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

function toReminderPayload(payload: ReminderFormPayload) {
  return {
    customerId: payload.customerId.trim(),
    productId: payload.productId.trim(),
    reminderLevel: payload.reminderLevel,
    scheduledDate: payload.scheduledDate,
  };
}

function appendOptionalParam(params: URLSearchParams, key: string, value: string) {
  const trimmed = value.trim();
  if (trimmed.length > 0) {
    params.set(key, trimmed);
  }
}
