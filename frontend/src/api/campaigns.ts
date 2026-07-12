import { apiRequest } from "@/api/client";
import type { SegmentPreviewView } from "@/api/segments";

export type CampaignStatus =
  "DRAFT" | "SUBMITTED" | "APPROVED" | "REJECTED" | "ACTIVE" | "PAUSED" | "COMPLETED" | "ARCHIVED";

export type CampaignChannel = "EMAIL" | "SMS" | "PHONE" | "LETTER" | "MIXED";

export type CampaignRecipientStatus =
  "ELIGIBLE" | "EXCLUDED" | "SENT" | "OPENED" | "CLICKED" | "REPLIED" | "CONVERTED" | "FAILED";

export type CampaignView = {
  id: string;
  name: string;
  objective: string;
  status: CampaignStatus;
  ownerUserId: string | null;
  ownerFullName: string | null;
  segmentId: string | null;
  segmentName: string | null;
  channel: CampaignChannel;
  messageSubject: string | null;
  messageBody: string | null;
  startDate: string | null;
  endDate: string | null;
  approvedByUserId: string | null;
  approvedByFullName: string | null;
  approvedAt: string | null;
  rejectionReason: string | null;
  complianceReviewNotes: string | null;
  productIds: string[];
  createdAt: string | null;
  updatedAt: string | null;
};

export type CampaignRecipientView = {
  id: string;
  campaignId: string;
  campaignName: string;
  customerId: string;
  customerFullName: string;
  eligibilityStatus: CampaignRecipientStatus;
  exclusionReason: string | null;
  eligibilityExplanation: string | null;
  sentAt: string | null;
  openedAt: string | null;
  clickedAt: string | null;
  convertedAt: string | null;
  createdAt: string | null;
};

export type CampaignRecipientSummaryView = {
  campaignId: string;
  eligible: number;
  excluded: number;
  sent: number;
  failed: number;
};

export type CampaignSearchFilters = {
  term: string;
  status: CampaignStatus | "ALL";
  ownerUserId: string;
  segmentId: string;
};

export type CampaignFormPayload = {
  name: string;
  objective: string;
  segmentId: string;
  channel: CampaignChannel;
  messageSubject: string;
  messageBody: string;
  startDate: string;
  endDate: string;
};

export type RejectCampaignPayload = {
  rejectionReason: string;
  complianceReviewNotes: string;
};

type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
};

export const campaignStatuses: CampaignStatus[] = [
  "DRAFT",
  "SUBMITTED",
  "APPROVED",
  "REJECTED",
  "ACTIVE",
  "PAUSED",
  "COMPLETED",
  "ARCHIVED",
];

export const campaignChannels: CampaignChannel[] = ["EMAIL", "SMS", "PHONE", "LETTER", "MIXED"];

export const emptyCampaignFilters: CampaignSearchFilters = {
  term: "",
  status: "ALL",
  ownerUserId: "",
  segmentId: "",
};

export const emptyCampaignForm: CampaignFormPayload = {
  name: "",
  objective: "",
  segmentId: "",
  channel: "EMAIL",
  messageSubject: "",
  messageBody: "",
  startDate: "",
  endDate: "",
};

export async function listCampaigns(filters?: CampaignSearchFilters): Promise<CampaignView[]> {
  const response = await apiRequest<ApiResponse<CampaignView[]>>(
    `/campaigns${campaignSearchQuery(filters)}`,
  );
  return response.data;
}

export async function getCampaign(id: string): Promise<CampaignView> {
  const response = await apiRequest<ApiResponse<CampaignView>>(`/campaigns/${id}`);
  return response.data;
}

export async function previewCampaignRecipients(id: string): Promise<SegmentPreviewView> {
  const response = await apiRequest<ApiResponse<SegmentPreviewView>>(
    `/campaigns/${id}/recipients/preview`,
  );
  return response.data;
}

export async function listEligibleCampaignRecipients(id: string): Promise<CampaignRecipientView[]> {
  const response = await apiRequest<ApiResponse<CampaignRecipientView[]>>(
    `/campaigns/${id}/recipients/eligible`,
  );
  return response.data;
}

export async function listExcludedCampaignRecipients(id: string): Promise<CampaignRecipientView[]> {
  const response = await apiRequest<ApiResponse<CampaignRecipientView[]>>(
    `/campaigns/${id}/recipients/excluded`,
  );
  return response.data;
}

export async function getCampaignRecipientSummary(
  id: string,
): Promise<CampaignRecipientSummaryView> {
  const response = await apiRequest<ApiResponse<CampaignRecipientSummaryView>>(
    `/campaigns/${id}/recipients/summary`,
  );
  return response.data;
}

export async function createCampaign(payload: CampaignFormPayload): Promise<CampaignView> {
  const response = await apiRequest<ApiResponse<CampaignView>>("/campaigns", {
    method: "POST",
    body: JSON.stringify(toCampaignWritePayload(payload)),
  });
  return response.data;
}

export async function updateCampaign(
  id: string,
  payload: CampaignFormPayload,
): Promise<CampaignView> {
  const response = await apiRequest<ApiResponse<CampaignView>>(`/campaigns/${id}`, {
    method: "PUT",
    body: JSON.stringify(toCampaignWritePayload(payload)),
  });
  return response.data;
}

export async function selectCampaignProducts(
  id: string,
  productIds: string[],
): Promise<CampaignView> {
  const response = await apiRequest<ApiResponse<CampaignView>>(`/campaigns/${id}/products`, {
    method: "PUT",
    body: JSON.stringify({ productIds }),
  });
  return response.data;
}

export async function submitCampaign(id: string): Promise<CampaignView> {
  return campaignAction(id, "submit");
}

export async function approveCampaign(
  id: string,
  complianceReviewNotes = "",
): Promise<CampaignView> {
  const response = await apiRequest<ApiResponse<CampaignView>>(`/campaigns/${id}/approve`, {
    method: "POST",
    body: JSON.stringify({ complianceReviewNotes: optionalString(complianceReviewNotes) }),
  });
  return response.data;
}

export async function rejectCampaign(
  id: string,
  payload: RejectCampaignPayload,
): Promise<CampaignView> {
  const response = await apiRequest<ApiResponse<CampaignView>>(`/campaigns/${id}/reject`, {
    method: "POST",
    body: JSON.stringify({
      rejectionReason: payload.rejectionReason.trim(),
      complianceReviewNotes: optionalString(payload.complianceReviewNotes),
    }),
  });
  return response.data;
}

export async function launchCampaign(id: string): Promise<CampaignView> {
  return campaignAction(id, "launch");
}

export async function pauseCampaign(id: string): Promise<CampaignView> {
  return campaignAction(id, "pause");
}

export async function completeCampaign(id: string): Promise<CampaignView> {
  return campaignAction(id, "complete");
}

export async function archiveCampaign(id: string): Promise<CampaignView> {
  return campaignAction(id, "archive");
}

export function campaignToForm(campaign: CampaignView): CampaignFormPayload {
  return {
    name: campaign.name,
    objective: campaign.objective,
    segmentId: campaign.segmentId ?? "",
    channel: campaign.channel,
    messageSubject: campaign.messageSubject ?? "",
    messageBody: campaign.messageBody ?? "",
    startDate: campaign.startDate ?? "",
    endDate: campaign.endDate ?? "",
  };
}

export function formatCampaignEnum(value: string) {
  return value
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

function campaignAction(id: string, action: string) {
  return apiRequest<ApiResponse<CampaignView>>(`/campaigns/${id}/${action}`, {
    method: "POST",
  }).then((response) => response.data);
}

function campaignSearchQuery(filters?: CampaignSearchFilters) {
  if (filters == null) {
    return "";
  }

  const params = new URLSearchParams();
  appendOptionalParam(params, "term", filters.term);
  appendOptionalParam(params, "ownerUserId", filters.ownerUserId);
  appendOptionalParam(params, "segmentId", filters.segmentId);
  if (filters.status !== "ALL") {
    params.set("status", filters.status);
  }

  const query = params.toString();
  return query.length === 0 ? "" : `?${query}`;
}

function toCampaignWritePayload(payload: CampaignFormPayload) {
  return {
    name: payload.name.trim(),
    objective: payload.objective.trim(),
    segmentId: optionalString(payload.segmentId),
    channel: payload.channel,
    messageSubject: optionalString(payload.messageSubject),
    messageBody: optionalString(payload.messageBody),
    startDate: optionalString(payload.startDate),
    endDate: optionalString(payload.endDate),
  };
}

function appendOptionalParam(params: URLSearchParams, key: string, value: string) {
  const trimmed = value.trim();
  if (trimmed.length > 0) {
    params.set(key, trimmed);
  }
}

function optionalString(value: string) {
  const trimmed = value.trim();
  return trimmed.length === 0 ? null : trimmed;
}
