import { apiRequest } from "@/api/client";

export type CommunicationChannel = "EMAIL" | "SMS" | "PHONE" | "IN_APP";

export type ContactEventType =
  "SENT" | "OPENED" | "CLICKED" | "REPLIED" | "FAILED" | "UNSUBSCRIBED" | "CALLED" | "NOTE";

export type ContactOutcome =
  "INTERESTED" | "NOT_INTERESTED" | "CONVERTED" | "NO_RESPONSE" | "FAILED";

export type ContactEventView = {
  id: string;
  customerId: string | null;
  customerFullName: string | null;
  campaignId: string | null;
  campaignName: string | null;
  channel: CommunicationChannel;
  eventType: ContactEventType;
  outcome: ContactOutcome | null;
  notes: string | null;
  occurredAt: string;
  createdByUserId: string | null;
  createdByFullName: string | null;
};

export type ContactTimelineFilters = {
  customerId: string;
  campaignId: string;
  eventType: ContactEventType | "ALL";
};

type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
};

export const contactEventTypes: ContactEventType[] = [
  "SENT",
  "OPENED",
  "CLICKED",
  "REPLIED",
  "FAILED",
  "UNSUBSCRIBED",
  "CALLED",
  "NOTE",
];

export const emptyContactTimelineFilters: ContactTimelineFilters = {
  customerId: "",
  campaignId: "",
  eventType: "ALL",
};

export type RecordContactEventPayload = {
  customerId?: string;
  campaignId?: string;
  channel: CommunicationChannel | "";
  eventType: ContactEventType | "";
  outcome?: ContactOutcome | "";
  notes?: string;
  occurredAt?: string;
};

export const emptyRecordContactEventForm: RecordContactEventPayload = {
  channel: "",
  eventType: "",
  outcome: "",
  notes: "",
};

export async function listContactTimeline(
  filters: ContactTimelineFilters = emptyContactTimelineFilters,
): Promise<ContactEventView[]> {
  const response = await apiRequest<ApiResponse<ContactEventView[]>>(
    `/contact-events/timeline${contactTimelineQuery(filters)}`,
  );
  return response.data;
}

export async function recordContactEvent(
  payload: RecordContactEventPayload,
): Promise<ContactEventView> {
  const response = await apiRequest<ApiResponse<ContactEventView>>("/contact-events", {
    method: "POST",
    body: JSON.stringify(payload),
  });
  return response.data;
}

export function contactTimelineQuery(filters: ContactTimelineFilters) {
  const params = new URLSearchParams();
  appendOptionalParam(params, "customerId", filters.customerId);
  appendOptionalParam(params, "campaignId", filters.campaignId);
  if (filters.eventType !== "ALL") {
    params.set("eventType", filters.eventType);
  }

  const query = params.toString();
  return query.length === 0 ? "" : `?${query}`;
}

export function formatContactEnum(value: string) {
  return value
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

function appendOptionalParam(params: URLSearchParams, key: string, value: string) {
  const trimmed = value.trim();
  if (trimmed.length > 0) {
    params.set(key, trimmed);
  }
}
