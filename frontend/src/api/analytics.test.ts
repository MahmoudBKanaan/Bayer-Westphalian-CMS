import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import {
  emptyDashboardView,
  emptyExecutiveDashboardView,
  getCampaignAnalytics,
  getDashboard,
  getExecutiveDashboard,
  getProductPerformance,
} from "@/api/analytics";
import { AUTH_STORAGE_KEYS } from "@/auth/sessionStorageStrategy";

const dashboard = {
  campaignTotal: 3,
  activeCampaigns: 1,
  audienceSize: 100,
  messagesSent: 80,
  eligibleCount: 80,
  excludedCount: 20,
  openedCount: 40,
  clickedCount: 16,
  repliedCount: 8,
  convertedCount: 4,
  openRate: 0.5,
  clickRate: 0.2,
  conversionRate: 0.05,
  estimatedCost: 200,
  estimatedRevenue: 300,
  estimatedRoi: 0.5,
  recentCampaignMetrics: [
    {
      metricsId: "51000000-0000-0000-0000-000000000441",
      campaignId: "50000000-0000-0000-0000-000000000441",
      campaignName: "Spring Life Drive",
      campaignStatus: "ACTIVE",
      audienceSize: 100,
      eligibleCount: 80,
      excludedCount: 20,
      sentCount: 80,
      openedCount: 40,
      clickedCount: 16,
      repliedCount: 8,
      convertedCount: 4,
      openRate: 0.5,
      clickRate: 0.2,
      conversionRate: 0.05,
      estimatedCost: 200,
      estimatedRevenue: 300,
      estimatedRoi: 0.5,
      updatedAt: "2026-07-11T10:00:00Z",
    },
  ],
};

const campaignAnalytics = {
  campaignId: "50000000-0000-0000-0000-000000000441",
  campaignName: "Spring Life Drive",
  objective: "Promote life products",
  status: "ACTIVE",
  channel: "EMAIL",
  startDate: "2026-07-01",
  endDate: "2026-07-31",
  ownerUserId: "10000000-0000-0000-0000-000000000441",
  ownerFullName: "Campaign Owner",
  metrics: dashboard.recentCampaignMetrics[0],
  generatedAt: "2026-07-11T12:00:00Z",
};

const productPerformance = [
  {
    productId: "60000000-0000-0000-0000-000000000441",
    productName: "Life Protection Plus",
    productType: "LIFE_INSURANCE",
    campaignCount: 2,
    audienceSize: 200,
    eligibleCount: 160,
    sentCount: 150,
    openedCount: 75,
    clickedCount: 30,
    convertedCount: 10,
    openRate: 0.5,
    clickRate: 0.2,
    conversionRate: 0.0667,
    estimatedCost: 400,
    estimatedRevenue: 600,
    estimatedRoi: 0.5,
  },
];

describe("analytics api", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("loads dashboard KPIs from GET /analytics/dashboard (item 440 / FR-100–107)", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn(async () => ({
      ok: true,
      status: 200,
      json: async () => ({
        success: true,
        message: "Analytics dashboard loaded",
        data: dashboard,
      }),
    }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(getDashboard()).resolves.toEqual(dashboard);

    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/analytics/dashboard`,
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: "Bearer access-token" }),
      }),
    );
  });

  it("falls back to empty dashboard when data is null", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => ({
        ok: true,
        status: 200,
        json: async () => ({
          success: true,
          message: "Analytics dashboard loaded",
          data: null,
        }),
      })),
    );

    await expect(getDashboard()).resolves.toEqual(emptyDashboardView);
  });

  it("loads campaign analytics from GET /analytics/campaigns/{id} (item 441 / 432)", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const campaignId = "50000000-0000-0000-0000-000000000441";
    const fetchMock = vi.fn(async () => ({
      ok: true,
      status: 200,
      json: async () => ({
        success: true,
        message: "Campaign analytics loaded",
        data: campaignAnalytics,
      }),
    }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(getCampaignAnalytics(campaignId)).resolves.toEqual(campaignAnalytics);

    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/analytics/campaigns/${campaignId}`,
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: "Bearer access-token" }),
      }),
    );
  });

  it("rejects empty campaign analytics payloads", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => ({
        ok: true,
        status: 200,
        json: async () => ({
          success: true,
          message: "Campaign analytics loaded",
          data: null,
        }),
      })),
    );

    await expect(
      getCampaignAnalytics("50000000-0000-0000-0000-000000000441"),
    ).rejects.toThrow("Campaign analytics payload was empty");
  });

  it("loads product performance from GET /analytics/products/performance (item 441 / 433)", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn(async () => ({
      ok: true,
      status: 200,
      json: async () => ({
        success: true,
        message: "Product performance loaded",
        data: productPerformance,
      }),
    }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(getProductPerformance()).resolves.toEqual(productPerformance);

    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/analytics/products/performance`,
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: "Bearer access-token" }),
      }),
    );
  });

  it("falls back to empty product performance list when data is null", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => ({
        ok: true,
        status: 200,
        json: async () => ({
          success: true,
          message: "Product performance loaded",
          data: null,
        }),
      })),
    );

    await expect(getProductPerformance()).resolves.toEqual([]);
  });

  it("loads executive dashboard from GET /analytics/executive (item 443 / COMP-010)", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const executive = {
      totalCampaigns: 5,
      activeCampaigns: 2,
      completedCampaigns: 1,
      totalAudience: 100,
      totalEligible: 80,
      totalExcluded: 20,
      totalSent: 70,
      totalOpened: 35,
      totalClicked: 14,
      totalReplied: 7,
      totalConverted: 5,
      overallOpenRate: 0.5,
      overallClickRate: 0.2,
      overallConversionRate: 0.0714,
      totalEstimatedCost: 500,
      totalEstimatedRevenue: 800,
      overallEstimatedRoi: 0.6,
      productPerformance,
    };
    const fetchMock = vi.fn(async () => ({
      ok: true,
      status: 200,
      json: async () => ({
        success: true,
        message: "Executive dashboard loaded",
        data: executive,
      }),
    }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(getExecutiveDashboard()).resolves.toEqual(executive);
    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/analytics/executive`,
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: "Bearer access-token" }),
      }),
    );
  });

  it("falls back to empty executive dashboard when data is null", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => ({
        ok: true,
        status: 200,
        json: async () => ({
          success: true,
          message: "Executive dashboard loaded",
          data: null,
        }),
      })),
    );

    await expect(getExecutiveDashboard()).resolves.toEqual(emptyExecutiveDashboardView);
  });
});
