import { apiRequest } from "@/api/client";
import type { CampaignChannel } from "@/api/campaigns";

type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
};

export type CampaignCopyRequestPayload = {
  campaignId?: string | null;
  objective: string;
  productName?: string | null;
  channel?: CampaignChannel | null;
  audienceHint?: string | null;
};

export type CampaignCopySuggestionView = {
  campaignId: string | null;
  subject: string;
  body: string;
  callToAction: string | null;
  explanation: string;
  confidenceScore: number | null;
  requiresHumanApproval: boolean;
  humanApproved: boolean;
  approvedByUserId: string | null;
  storedRecommendationId: string | null;
};

export type AiRecommendationView = {
  id: string;
  recommendationType: "PRODUCT" | "SEGMENT" | "COPY" | "RISK" | "DUPLICATE_WARNING";
  targetEntityType: string;
  targetEntityId: string | null;
  inputSummary: string;
  recommendation: string;
  explanation: string;
  confidenceScore: number | null;
  approvedByUserId: string | null;
  approvedByFullName: string | null;
  reviewNotes: string | null;
  approved: boolean;
  createdAt: string | null;
};

export type AiScoreExplanationView = {
  factor: string;
  weight: number | string;
  contribution: number | string;
  detail: string | null;
};

export type AiCustomerSearchHitView = {
  customerId: string;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string | null;
  city: string | null;
  country: string | null;
  customerType: string | null;
  status: string | null;
  doNotContact: boolean;
  score: number | string;
  explainScore: AiScoreExplanationView[];
};

export type AiCustomerSearchView = {
  query: string;
  totalHits: number;
  results: AiCustomerSearchHitView[];
};

export async function searchAiCustomers(query: string, limit = 5): Promise<AiCustomerSearchView> {
  const params = new URLSearchParams({
    q: query.trim(),
    limit: String(limit),
  });
  const response = await apiRequest<ApiResponse<AiCustomerSearchView>>(
    `/ai/customer-search?${params.toString()}`,
  );
  return response.data;
}

export async function generateCampaignCopySuggestion(
  payload: CampaignCopyRequestPayload,
): Promise<CampaignCopySuggestionView> {
  const response = await apiRequest<ApiResponse<CampaignCopySuggestionView>>(
    "/ai/campaign-copy",
    {
      method: "POST",
      body: JSON.stringify({
        campaignId: optionalString(payload.campaignId ?? ""),
        objective: payload.objective.trim(),
        productName: optionalString(payload.productName ?? ""),
        channel: payload.channel ?? null,
        audienceHint: optionalString(payload.audienceHint ?? ""),
      }),
    },
  );
  return response.data;
}

export async function approveCampaignCopySuggestion(
  recommendationId: string,
  reviewNotes = "",
): Promise<AiRecommendationView> {
  const response = await apiRequest<ApiResponse<AiRecommendationView>>(
    `/ai/campaign-copy/${encodeURIComponent(recommendationId.trim())}/approve`,
    {
      method: "POST",
      body: JSON.stringify({ reviewNotes: optionalString(reviewNotes) }),
    },
  );
  return response.data;
}

function optionalString(value: string) {
  const trimmed = value.trim();
  return trimmed.length === 0 ? null : trimmed;
}
