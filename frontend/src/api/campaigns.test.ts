import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import {
  approveCampaign,
  createCampaign,
  getCampaignRecipientSummary,
  getCampaign,
  listEligibleCampaignRecipients,
  listExcludedCampaignRecipients,
  listCampaigns,
  previewCampaignRecipients,
  rejectCampaign,
  selectCampaignProducts,
  submitCampaign,
  updateCampaign,
  type CampaignFormPayload,
} from "@/api/campaigns";
import { AUTH_STORAGE_KEYS } from "@/auth/sessionStorageStrategy";

const campaign = {
  id: "50000000-0000-0000-0000-000000000001",
  name: "Life renewal outreach",
  objective: "Promote life insurance renewals",
  status: "DRAFT",
  ownerUserId: "10000000-0000-0000-0000-000000000101",
  ownerFullName: "Campaign Manager",
  segmentId: null,
  segmentName: null,
  channel: "EMAIL",
  messageSubject: "Renew your cover",
  messageBody: "Dear customer, ...",
  startDate: "2026-09-01",
  endDate: "2026-09-30",
  approvedByUserId: null,
  approvedByFullName: null,
  approvedAt: null,
  rejectionReason: null,
  complianceReviewNotes: null,
  productIds: [],
  createdAt: "2026-07-09T10:15:00Z",
  updatedAt: "2026-07-09T10:30:00Z",
};

const form: CampaignFormPayload = {
  name: " Life renewal outreach ",
  objective: " Promote life insurance renewals ",
  segmentId: "",
  channel: "EMAIL",
  messageSubject: " Renew your cover ",
  messageBody: " Dear customer, ... ",
  startDate: "2026-09-01",
  endDate: "2026-09-30",
};

describe("campaigns api", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("loads campaigns with KB search filters", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = mockCampaignResponse([campaign]);
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      listCampaigns({
        term: "life",
        status: "SUBMITTED",
        ownerUserId: "10000000-0000-0000-0000-000000000101",
        segmentId: "",
      }),
    ).resolves.toEqual([campaign]);

    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/campaigns?term=life&ownerUserId=10000000-0000-0000-0000-000000000101&status=SUBMITTED`,
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: "Bearer access-token" }),
      }),
    );
  });

  it("creates and updates campaign definitions with nullable optional fields", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = mockCampaignResponse(campaign);
    vi.stubGlobal("fetch", fetchMock);

    await createCampaign(form);
    await updateCampaign(campaign.id, form);

    expect(JSON.parse(fetchMock.mock.calls[0][1]?.body as string)).toEqual({
      name: "Life renewal outreach",
      objective: "Promote life insurance renewals",
      segmentId: null,
      channel: "EMAIL",
      messageSubject: "Renew your cover",
      messageBody: "Dear customer, ...",
      startDate: "2026-09-01",
      endDate: "2026-09-30",
    });
    expect(fetchMock.mock.calls[1][0]).toBe(`${API_BASE_URL}/campaigns/${campaign.id}`);
    expect(fetchMock.mock.calls[1][1]?.method).toBe("PUT");
  });

  it("calls submit, approve, and reject workflow endpoints", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = mockCampaignResponse(campaign);
    vi.stubGlobal("fetch", fetchMock);

    await submitCampaign(campaign.id);
    await selectCampaignProducts(campaign.id, ["41000000-0000-0000-0000-000000000201"]);
    await approveCampaign(campaign.id, "Approved");
    await rejectCampaign(campaign.id, {
      rejectionReason: "Missing consent language",
      complianceReviewNotes: "Add consent wording",
    });

    expect(fetchMock.mock.calls[0][0]).toBe(`${API_BASE_URL}/campaigns/${campaign.id}/submit`);
    expect(fetchMock.mock.calls[1][0]).toBe(`${API_BASE_URL}/campaigns/${campaign.id}/products`);
    expect(JSON.parse(fetchMock.mock.calls[1][1]?.body as string)).toEqual({
      productIds: ["41000000-0000-0000-0000-000000000201"],
    });
    expect(fetchMock.mock.calls[2][0]).toBe(`${API_BASE_URL}/campaigns/${campaign.id}/approve`);
    expect(JSON.parse(fetchMock.mock.calls[2][1]?.body as string)).toEqual({
      complianceReviewNotes: "Approved",
    });
    expect(fetchMock.mock.calls[3][0]).toBe(`${API_BASE_URL}/campaigns/${campaign.id}/reject`);
    expect(JSON.parse(fetchMock.mock.calls[3][1]?.body as string)).toEqual({
      rejectionReason: "Missing consent language",
      complianceReviewNotes: "Add consent wording",
    });
  });

  it("loads campaign details and recipient preview", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const preview = {
      totalAudienceCount: 3,
      eligibleCount: 2,
      excludedCount: 1,
      matchingCustomers: [],
      exclusionReasonSummary: [{ code: "INVALID_CONSENT", message: "Missing consent", count: 1 }],
    };
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.endsWith("/recipients/preview")) {
        return apiResponse(preview);
      }
      return apiResponse(campaign);
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(getCampaign(campaign.id)).resolves.toEqual(campaign);
    await expect(previewCampaignRecipients(campaign.id)).resolves.toEqual(preview);

    expect(fetchMock.mock.calls[0][0]).toBe(`${API_BASE_URL}/campaigns/${campaign.id}`);
    expect(fetchMock.mock.calls[1][0]).toBe(
      `${API_BASE_URL}/campaigns/${campaign.id}/recipients/preview`,
    );
  });

  it("loads stored eligible campaign recipients", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const recipients = [
      {
        id: "62000000-0000-0000-0000-000000000286",
        campaignId: campaign.id,
        campaignName: "Life renewal outreach",
        customerId: "20000000-0000-0000-0000-000000000286",
        customerFullName: "Ada Eligible",
        eligibilityStatus: "ELIGIBLE",
        exclusionReason: null,
        eligibilityExplanation: "Customer is eligible for campaign contact",
        sentAt: null,
        openedAt: null,
        clickedAt: null,
        convertedAt: null,
        createdAt: "2026-07-09T10:15:30Z",
      },
    ];
    const fetchMock = mockCampaignResponse(recipients);
    vi.stubGlobal("fetch", fetchMock);

    await expect(listEligibleCampaignRecipients(campaign.id)).resolves.toEqual(recipients);

    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/campaigns/${campaign.id}/recipients/eligible`,
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: "Bearer access-token" }),
      }),
    );
  });

  it("loads stored excluded campaign recipients", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const recipients = [
      {
        id: "62000000-0000-0000-0000-000000000287",
        campaignId: campaign.id,
        campaignName: "Life renewal outreach",
        customerId: "20000000-0000-0000-0000-000000000287",
        customerFullName: "Grace Excluded",
        eligibilityStatus: "EXCLUDED",
        exclusionReason: "INVALID_CONSENT",
        eligibilityExplanation: "Customer does not have valid required consent",
        sentAt: null,
        openedAt: null,
        clickedAt: null,
        convertedAt: null,
        createdAt: "2026-07-09T10:45:30Z",
      },
    ];
    const fetchMock = mockCampaignResponse(recipients);
    vi.stubGlobal("fetch", fetchMock);

    await expect(listExcludedCampaignRecipients(campaign.id)).resolves.toEqual(recipients);

    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/campaigns/${campaign.id}/recipients/excluded`,
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: "Bearer access-token" }),
      }),
    );
  });

  it("loads campaign recipient launch response summary", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const summary = {
      campaignId: campaign.id,
      eligible: 8,
      excluded: 2,
      sent: 7,
      failed: 1,
    };
    const fetchMock = mockCampaignResponse(summary);
    vi.stubGlobal("fetch", fetchMock);

    await expect(getCampaignRecipientSummary(campaign.id)).resolves.toEqual(summary);

    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/campaigns/${campaign.id}/recipients/summary`,
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: "Bearer access-token" }),
      }),
    );
  });
});

function mockCampaignResponse(data: unknown) {
  return vi.fn().mockResolvedValue(apiResponse(data));
}

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
