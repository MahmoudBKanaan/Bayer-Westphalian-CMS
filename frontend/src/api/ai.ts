import { apiRequest } from "@/api/client";
import type { CampaignChannel } from "@/api/campaigns";
import type { ProductType } from "@/api/products";
import type { SegmentCriteriaPayload } from "@/api/segments";

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

export type ProductRecommendationView = {
  productId: string;
  productName: string | null;
  productType: ProductType | null;
  recommendation: string;
  explanation: string;
  confidenceScore: number | null;
  storedRecommendationId: string | null;
};

export type ProductRecommendationListResponse = {
  customerId: string;
  recommendations: ProductRecommendationView[];
};

export type SuggestedSegmentCriterion = {
  fieldName: string;
  operator: string;
  value: string;
  logicalGroup: string | null;
  joinOperator: string | null;
};

export type SegmentSuggestionView = {
  suggestedName: string;
  description: string | null;
  suggestedCriteria: SuggestedSegmentCriterion[];
  suggestedCriteriaSummary: string[];
  explanation: string;
  confidenceScore: number | null;
  storedRecommendationId: string | null;
};

export type SegmentSuggestionListResponse = {
  suggestions: SegmentSuggestionView[];
};

export type SegmentSuggestionRequestPayload = {
  customerId?: string | null;
  city?: string | null;
  country?: string | null;
  productTypeHint?: string | null;
  expirationWithinMonths?: number | null;
};

export type DefaultRiskScoreView = {
  customerId: string;
  riskScore: number | string;
  riskLevel: string;
  explanation: string;
  factors: AiScoreExplanationView[];
  storedRecommendationId: string | null;
};

/** Request body for POST /api/ai/duplicate-contact-warning (AI-006). */
export type DuplicateContactWarningRequest = {
  customerId: string;
  /** Recipient Preview always supplies the current campaign. */
  campaignId: string;
};

/**
 * Backend {@code DuplicateContactRiskView} (AI-006).
 * Field names match the Java DTO JSON contract exactly.
 */
export type DuplicateContactRiskView = {
  customerId: string;
  campaignId: string | null;
  riskDetected: boolean;
  warning: string | null;
  explanation: string;
  contactsInCurrentMonth: number;
  monthlyContactLimit: number | null;
  sameCampaignAlreadyContacted: boolean;
  storedRecommendationId: string | null;
};

/** Alias preferred by UI modules for the duplicate-contact warning response. */
export type DuplicateContactWarningView = DuplicateContactRiskView;

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

export async function generateProductRecommendations(
  customerId: string,
): Promise<ProductRecommendationListResponse> {
  const response = await apiRequest<ApiResponse<ProductRecommendationListResponse>>(
    "/ai/product-recommendations",
    {
      method: "POST",
      body: JSON.stringify({ customerId: customerId.trim() }),
    },
  );
  return response.data;
}

export async function generateSegmentSuggestions(
  payload: SegmentSuggestionRequestPayload = {},
): Promise<SegmentSuggestionListResponse> {
  const response = await apiRequest<ApiResponse<SegmentSuggestionListResponse>>(
    "/ai/segment-suggestions",
    {
      method: "POST",
      body: JSON.stringify({
        customerId: optionalString(payload.customerId ?? ""),
        city: optionalString(payload.city ?? ""),
        country: optionalString(payload.country ?? ""),
        productTypeHint: optionalString(payload.productTypeHint ?? ""),
        expirationWithinMonths: payload.expirationWithinMonths ?? null,
      }),
    },
  );
  return response.data;
}

export async function generateDefaultRiskScore(customerId: string): Promise<DefaultRiskScoreView> {
  const response = await apiRequest<ApiResponse<DefaultRiskScoreView>>("/ai/default-risk-score", {
    method: "POST",
    body: JSON.stringify({ customerId: customerId.trim() }),
  });
  return response.data;
}

/**
 * AI-006 duplicate-contact risk warning — backend is the source of truth.
 * Does not compute risk in the browser.
 */
export async function generateDuplicateContactWarning(
  payload: DuplicateContactWarningRequest,
): Promise<DuplicateContactWarningView> {
  const customerId = payload.customerId.trim();
  const campaignId = payload.campaignId.trim();

  if (customerId === "") {
    throw new Error("Customer ID is required.");
  }

  if (campaignId === "") {
    throw new Error("Campaign ID is required.");
  }

  const response = await apiRequest<ApiResponse<DuplicateContactWarningView>>(
    "/ai/duplicate-contact-warning",
    {
      method: "POST",
      body: JSON.stringify({
        customerId,
        campaignId,
      }),
    },
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

export type ApproveCampaignCopyPayload = {
  reviewNotes?: string;
  editedSubject?: string | null;
  editedMessageBody?: string | null;
  editedCallToAction?: string | null;
};

/**
 * Human approval of AI-005 campaign copy (does not compliance-approve or launch the campaign).
 * Optional edited fields override the stored suggestion before applying to a DRAFT campaign.
 */
export async function approveCampaignCopySuggestion(
  recommendationId: string,
  reviewNotesOrPayload: string | ApproveCampaignCopyPayload = "",
): Promise<AiRecommendationView> {
  const payload: ApproveCampaignCopyPayload =
    typeof reviewNotesOrPayload === "string"
      ? { reviewNotes: reviewNotesOrPayload }
      : reviewNotesOrPayload;

  const response = await apiRequest<ApiResponse<AiRecommendationView>>(
    `/ai/campaign-copy/${encodeURIComponent(recommendationId.trim())}/approve`,
    {
      method: "POST",
      body: JSON.stringify({
        reviewNotes: optionalString(payload.reviewNotes ?? ""),
        editedSubject: optionalString(payload.editedSubject ?? ""),
        editedMessageBody: optionalString(payload.editedMessageBody ?? ""),
        editedCallToAction: optionalString(payload.editedCallToAction ?? ""),
      }),
    },
  );
  return response.data;
}

/** Maps AI structured criteria into segment-builder payload rows. */
export function mapSuggestedCriteriaToSegmentPayload(
  criteria: SuggestedSegmentCriterion[],
): SegmentCriteriaPayload[] {
  return criteria.map((criterion, index) => ({
    fieldName: criterion.fieldName,
    operator: (criterion.operator || "EQUALS") as SegmentCriteriaPayload["operator"],
    value: criterion.value,
    logicalGroup: criterion.logicalGroup ?? undefined,
    joinOperator:
      index === 0
        ? undefined
        : ((criterion.joinOperator || "AND") as SegmentCriteriaPayload["joinOperator"]),
  }));
}

function optionalString(value: string) {
  const trimmed = value.trim();
  return trimmed.length === 0 ? null : trimmed;
}
