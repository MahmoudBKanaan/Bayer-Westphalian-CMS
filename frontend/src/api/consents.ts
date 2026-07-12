import { apiRequest } from "@/api/client";

export type ConsentType =
  "MARKETING_EMAIL" | "MARKETING_PHONE" | "MARKETING_SMS" | "GUARDIAN" | "DATA_PROCESSING";

export type ConsentStatus = "GIVEN" | "WITHDRAWN" | "REQUIRED" | "EXPIRED" | "REJECTED";

export type ConsentRecordView = {
  id: string;
  customerId: string | null;
  customerFullName: string | null;
  consentType: ConsentType;
  status: ConsentStatus;
  purpose: string;
  source: string | null;
  grantedAt: string | null;
  withdrawnAt: string | null;
  expiresAt: string | null;
  evidenceFileUrl: string | null;
  createdBy: string | null;
  createdByFullName: string | null;
  createdAt: string | null;
  valid: boolean;
  requiresAction: boolean;
};

export type ConsentSearchFilters = {
  customerId?: string;
  consentType?: ConsentType;
  status?: ConsentStatus;
  validOnly?: boolean;
};

export type RecordConsentPayload = {
  customerId: string;
  consentType: ConsentType;
  status: ConsentStatus;
  purpose: string;
  source: string | null;
  grantedAt?: string | null;
  expiresAt?: string | null;
  evidenceFileUrl: string | null;
  createdBy?: string | null;
};

export type RecordOptOutPayload = {
  customerId: string;
  consentType: Extract<ConsentType, "MARKETING_EMAIL" | "MARKETING_PHONE" | "MARKETING_SMS">;
  source: string | null;
  evidenceFileUrl: string | null;
};

type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
};

export async function listConsents(
  filters: ConsentSearchFilters = {},
): Promise<ConsentRecordView[]> {
  const response = await apiRequest<ApiResponse<ConsentRecordView[]>>(
    `/consents${consentSearchQuery(filters)}`,
  );

  return response.data;
}

export async function recordConsent(payload: RecordConsentPayload): Promise<ConsentRecordView> {
  const response = await apiRequest<ApiResponse<ConsentRecordView>>("/consents", {
    method: "POST",
    body: JSON.stringify(payload),
  });

  return response.data;
}

export async function recordOptOut(payload: RecordOptOutPayload): Promise<ConsentRecordView> {
  return recordConsent({
    customerId: payload.customerId,
    consentType: payload.consentType,
    status: "WITHDRAWN",
    purpose: "Marketing opt-out",
    source: payload.source,
    evidenceFileUrl: payload.evidenceFileUrl,
  });
}

export async function withdrawConsent(consentRecordId: string): Promise<ConsentRecordView> {
  const response = await apiRequest<ApiResponse<ConsentRecordView>>("/consents/withdraw", {
    method: "POST",
    body: JSON.stringify({ consentRecordId }),
  });

  return response.data;
}

function consentSearchQuery(filters: ConsentSearchFilters) {
  const params = new URLSearchParams();
  appendOptionalParam(params, "customerId", filters.customerId);

  if (filters.consentType != null) {
    params.set("consentType", filters.consentType);
  }
  if (filters.status != null) {
    params.set("status", filters.status);
  }
  if (filters.validOnly != null) {
    params.set("validOnly", String(filters.validOnly));
  }

  const queryString = params.toString();
  return queryString.length === 0 ? "" : `?${queryString}`;
}

function appendOptionalParam(params: URLSearchParams, key: string, value?: string) {
  const trimmed = value?.trim() ?? "";
  if (trimmed.length > 0) {
    params.set(key, trimmed);
  }
}
