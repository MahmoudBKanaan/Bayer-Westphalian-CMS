import { afterEach, describe, expect, it, vi } from "vitest";
import {
  approveCampaignCopySuggestion,
  generateCampaignCopySuggestion,
  searchAiCustomers,
} from "@/api/ai";
import { API_BASE_URL } from "@/api/client";
import { AUTH_STORAGE_KEYS } from "@/auth/sessionStorageStrategy";

const suggestion = {
  campaignId: "50000000-0000-0000-0000-000000000492",
  subject: "Protect your next chapter",
  body: "A tailored life insurance option is ready for review.",
  callToAction: "Review the offer",
  explanation: "Rule-based AI-005 draft using campaign objective and product context.",
  confidenceScore: 72,
  requiresHumanApproval: true,
  humanApproved: false,
  approvedByUserId: null,
  storedRecommendationId: "70000000-0000-0000-0000-000000000492",
};

const approvedRecommendation = {
  id: "70000000-0000-0000-0000-000000000492",
  recommendationType: "COPY",
  targetEntityType: "campaign",
  targetEntityId: suggestion.campaignId,
  inputSummary: "campaignId=50000000-0000-0000-0000-000000000492",
  recommendation: "Subject: Protect your next chapter",
  explanation: suggestion.explanation,
  confidenceScore: 72,
  approvedByUserId: "10000000-0000-0000-0000-000000000101",
  approvedByFullName: "Campaign Manager",
  reviewNotes: "Reviewed by manager",
  approved: true,
  createdAt: "2026-07-11T10:00:00Z",
};

const customerSearchResults = {
  query: "Ada",
  totalHits: 1,
  results: [
    {
      customerId: "20000000-0000-0000-0000-000000000001",
      firstName: "Ada",
      lastName: "Policyholder",
      fullName: "Ada Policyholder",
      email: "ada@bayer-westphalian.test",
      city: "Berlin",
      country: "Germany",
      customerType: "CUSTOMER",
      status: "ACTIVE",
      doNotContact: false,
      score: 73,
      explainScore: [
        {
          factor: "full name",
          weight: 45,
          contribution: 40,
          detail: "fuzzy match (full name: Ada Policyholder)",
        },
      ],
    },
  ],
};

describe("ai api", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("generates campaign copy suggestions with nullable context", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue(apiResponse(suggestion));
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      generateCampaignCopySuggestion({
        campaignId: "",
        objective: " Promote renewals ",
        productName: " Life Protection ",
        channel: "EMAIL",
        audienceHint: "",
      }),
    ).resolves.toEqual(suggestion);

    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/ai/campaign-copy`,
      expect.objectContaining({
        method: "POST",
        headers: expect.objectContaining({ Authorization: "Bearer access-token" }),
      }),
    );
    expect(JSON.parse(fetchMock.mock.calls[0][1]?.body as string)).toEqual({
      campaignId: null,
      objective: "Promote renewals",
      productName: "Life Protection",
      channel: "EMAIL",
      audienceHint: null,
    });
  });

  it("approves campaign copy recommendations with human review notes", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue(apiResponse(approvedRecommendation));
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      approveCampaignCopySuggestion(suggestion.storedRecommendationId, "Reviewed by manager"),
    ).resolves.toEqual(approvedRecommendation);

    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/ai/campaign-copy/${suggestion.storedRecommendationId}/approve`,
      expect.objectContaining({
        method: "POST",
        headers: expect.objectContaining({ Authorization: "Bearer access-token" }),
      }),
    );
    expect(JSON.parse(fetchMock.mock.calls[0][1]?.body as string)).toEqual({
      reviewNotes: "Reviewed by manager",
    });
  });

  it("searches AI customer matches with score explanations", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue(apiResponse(customerSearchResults));
    vi.stubGlobal("fetch", fetchMock);

    await expect(searchAiCustomers(" Ada ", 3)).resolves.toEqual(customerSearchResults);

    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/ai/customer-search?q=Ada&limit=3`,
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: "Bearer access-token" }),
      }),
    );
  });
});

function apiResponse(data: unknown) {
  return {
    ok: true,
    json: async () => ({
      success: true,
      message: "OK",
      data,
    }),
  };
}
