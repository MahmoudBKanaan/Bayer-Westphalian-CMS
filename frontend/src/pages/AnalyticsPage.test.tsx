import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import { AuthProvider } from "@/auth/AuthProvider";
import { AUTH_STORAGE_KEYS, type SystemRoleName } from "@/auth/sessionStorageStrategy";
import { AnalyticsPage } from "@/pages/AnalyticsPage";

const campaignId = "50000000-0000-0000-0000-000000000441";
const secondCampaignId = "50000000-0000-0000-0000-000000000442";

const dashboardPayload = {
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
      campaignId,
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

const productPerformancePayload = [
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

const campaignsPayload = [
  {
    id: campaignId,
    name: "Spring Life Drive",
    objective: "Promote life products",
    status: "ACTIVE",
    ownerUserId: "10000000-0000-0000-0000-000000000441",
    ownerFullName: "Campaign Owner",
    segmentId: null,
    segmentName: null,
    channel: "EMAIL",
    messageSubject: "Hello",
    messageBody: "Body",
    startDate: "2026-07-01",
    endDate: "2026-07-31",
    approvedByUserId: null,
    approvedByFullName: null,
    approvedAt: null,
    rejectionReason: null,
    complianceReviewNotes: null,
    productIds: [],
    createdAt: "2026-07-01T00:00:00Z",
    updatedAt: "2026-07-11T00:00:00Z",
  },
  {
    id: secondCampaignId,
    name: "Auto Renewal Push",
    objective: "Renewals",
    status: "COMPLETED",
    ownerUserId: "10000000-0000-0000-0000-000000000441",
    ownerFullName: "Campaign Owner",
    segmentId: null,
    segmentName: null,
    channel: "SMS",
    messageSubject: null,
    messageBody: "SMS body",
    startDate: "2026-06-01",
    endDate: "2026-06-30",
    approvedByUserId: null,
    approvedByFullName: null,
    approvedAt: null,
    rejectionReason: null,
    complianceReviewNotes: null,
    productIds: [],
    createdAt: "2026-06-01T00:00:00Z",
    updatedAt: "2026-06-30T00:00:00Z",
  },
];

function campaignAnalyticsPayload(id: string) {
  const campaign = campaignsPayload.find((row) => row.id === id) ?? campaignsPayload[0];
  return {
    campaignId: campaign.id,
    campaignName: campaign.name,
    objective: campaign.objective,
    status: campaign.status,
    channel: campaign.channel,
    startDate: campaign.startDate,
    endDate: campaign.endDate,
    ownerUserId: campaign.ownerUserId,
    ownerFullName: campaign.ownerFullName,
    metrics: {
      metricsId: "51000000-0000-0000-0000-000000000441",
      campaignId: campaign.id,
      campaignName: campaign.name,
      campaignStatus: campaign.status,
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
    generatedAt: "2026-07-11T12:00:00Z",
  };
}

function createAccessToken(roles: string[]) {
  const payload = btoa(JSON.stringify({ roles }))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");
  return `header.${payload}.signature`;
}

function jsonResponse(body: unknown, status = 200) {
  return Promise.resolve({
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
  });
}

function renderAnalyticsPage(roles: SystemRoleName[] = ["BI_ANALYST"]) {
  const token = createAccessToken(roles);
  const userJson = JSON.stringify({
    id: "10000000-0000-0000-0000-000000000441",
    email: "bi@bayer-westphalian.test",
    fullName: "BI Analyst",
    status: "ACTIVE",
    lastLoginAt: "2026-07-11T10:00:00Z",
    roles,
  });
  localStorage.setItem(AUTH_STORAGE_KEYS.accessToken, token);
  localStorage.setItem(AUTH_STORAGE_KEYS.refreshToken, "refresh-token");
  localStorage.setItem(AUTH_STORAGE_KEYS.currentUser, userJson);
  sessionStorage.clear();

  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={["/analytics"]}>
        <AuthProvider>
          <AnalyticsPage />
        </AuthProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

function createAnalyticsFetchMock(options?: {
  dashboard?: unknown;
  products?: unknown;
  campaigns?: unknown;
}) {
  const dashboard = options?.dashboard ?? dashboardPayload;
  const products = options?.products ?? productPerformancePayload;
  const campaigns = options?.campaigns ?? campaignsPayload;

  return vi.fn(async (input: RequestInfo | URL) => {
    const url = String(input);
    if (url === `${API_BASE_URL}/analytics/dashboard`) {
      return jsonResponse({
        success: true,
        message: "Analytics dashboard loaded",
        data: dashboard,
      });
    }
    if (url === `${API_BASE_URL}/analytics/products/performance`) {
      return jsonResponse({
        success: true,
        message: "Product performance loaded",
        data: products,
      });
    }
    if (url.startsWith(`${API_BASE_URL}/campaigns`)) {
      return jsonResponse({
        success: true,
        message: "Campaigns loaded",
        data: campaigns,
      });
    }
    if (url.startsWith(`${API_BASE_URL}/analytics/campaigns/`)) {
      const id = url.slice(`${API_BASE_URL}/analytics/campaigns/`.length);
      return jsonResponse({
        success: true,
        message: "Campaign analytics loaded",
        data: campaignAnalyticsPayload(id),
      });
    }
    return jsonResponse({ message: `Unexpected URL ${url}` }, 404);
  });
}

describe("AnalyticsPage (item 441)", () => {
  afterEach(() => {
    sessionStorage.clear();
    localStorage.clear();
    vi.unstubAllGlobals();
  });

  it("loads engagement KPIs, product performance, and campaign analytics for BI analysts", async () => {
    const fetchMock = createAnalyticsFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderAnalyticsPage(["BI_ANALYST"]);

    expect(await screen.findByRole("heading", { name: "Analytics" })).toBeInTheDocument();
    expect(await screen.findByLabelText("Analytics engagement KPI cards")).toBeInTheDocument();
    expect(screen.getAllByText("Open rate").length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText("Click rate").length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText("Conversion rate").length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText("Estimated ROI")).toBeInTheDocument();
    // Open rate and ROI both format as 50.0% for this fixture.
    expect(screen.getAllByText("50.0%").length).toBeGreaterThanOrEqual(2);
    expect(screen.getAllByText("20.0%").length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText("5.0%")).toBeInTheDocument();

    expect(screen.getByRole("heading", { name: "Campaign rate comparison" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Product performance" })).toBeInTheDocument();
    expect(
      await screen.findByRole("table", { name: "Product performance table" }),
    ).toBeInTheDocument();
    // Product name appears in the product filter select and the performance table.
    expect(screen.getAllByText("Life Protection Plus").length).toBeGreaterThanOrEqual(1);

    expect(screen.getByRole("heading", { name: "Campaign analytics" })).toBeInTheDocument();
    expect(
      await screen.findByLabelText("Campaign analytics detail"),
    ).toBeInTheDocument();
    expect(screen.getByText("Spring Life Drive")).toBeInTheDocument();
    expect(
      await screen.findByRole("table", { name: "Selected campaign metrics table" }),
    ).toBeInTheDocument();

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        `${API_BASE_URL}/analytics/dashboard`,
        expect.objectContaining({
          headers: expect.objectContaining({ Authorization: expect.stringContaining("Bearer") }),
        }),
      );
      expect(fetchMock).toHaveBeenCalledWith(
        `${API_BASE_URL}/analytics/products/performance`,
        expect.objectContaining({
          headers: expect.objectContaining({ Authorization: expect.stringContaining("Bearer") }),
        }),
      );
      expect(fetchMock).toHaveBeenCalledWith(
        `${API_BASE_URL}/analytics/campaigns/${campaignId}`,
        expect.objectContaining({
          headers: expect.objectContaining({ Authorization: expect.stringContaining("Bearer") }),
        }),
      );
    });
  });

  it("allows campaign managers and executive viewers to load analytics", async () => {
    vi.stubGlobal("fetch", createAnalyticsFetchMock());
    renderAnalyticsPage(["CAMPAIGN_MANAGER"]);
    expect(await screen.findByLabelText("Analytics engagement KPI cards")).toBeInTheDocument();
  });

  it("denies analytics for unauthorized roles without calling analytics APIs", async () => {
    const fetchMock = createAnalyticsFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderAnalyticsPage(["PRODUCT_MANAGER"]);

    expect(await screen.findByText("You are not authorized to view analytics.")).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("shows empty product and rate messaging when no comparison data exists", async () => {
    vi.stubGlobal(
      "fetch",
      createAnalyticsFetchMock({
        dashboard: { ...dashboardPayload, recentCampaignMetrics: [] },
        products: [],
        campaigns: [],
      }),
    );

    renderAnalyticsPage(["ADMIN"]);

    expect(
      await screen.findByText("No campaign metrics match the current filters."),
    ).toBeInTheDocument();
    expect(
      screen.getByText("No product performance data matches the current filters."),
    ).toBeInTheDocument();
    expect(screen.getByText("No products match the current filters.")).toBeInTheDocument();
    expect(
      screen.getByText("No product financial chart data matches the current filters."),
    ).toBeInTheDocument();
    expect(screen.getByText("Select a campaign to view detailed analytics.")).toBeInTheDocument();
  });

  it("exposes campaign, product, and time-frame selectors and filters results", async () => {
    const secondProduct = {
      productId: "60000000-0000-0000-0000-000000000442",
      productName: "Auto Saver Plan",
      productType: "INVESTMENT",
      campaignCount: 1,
      audienceSize: 50,
      eligibleCount: 40,
      sentCount: 40,
      openedCount: 10,
      clickedCount: 4,
      convertedCount: 1,
      openRate: 0.25,
      clickRate: 0.1,
      conversionRate: 0.025,
      estimatedCost: 100,
      estimatedRevenue: 80,
      estimatedRoi: -0.2,
    };
    vi.stubGlobal(
      "fetch",
      createAnalyticsFetchMock({
        products: [...productPerformancePayload, secondProduct],
        campaigns: [
          { ...campaignsPayload[0], productIds: [productPerformancePayload[0].productId] },
          { ...campaignsPayload[1], productIds: [secondProduct.productId] },
        ],
      }),
    );

    renderAnalyticsPage(["BI_ANALYST"]);
    expect(await screen.findByLabelText("Analytics filters")).toBeInTheDocument();
    const campaignFilter = screen.getByLabelText("Filter analytics by campaign");
    const productFilter = screen.getByLabelText("Filter analytics by product");
    const timeFilter = screen.getByLabelText("Filter analytics by time frame");
    expect(campaignFilter).toBeInTheDocument();
    expect(productFilter).toBeInTheDocument();
    expect(timeFilter).toBeInTheDocument();

    await waitFor(() => {
      expect(productFilter.querySelectorAll("option").length).toBeGreaterThanOrEqual(3);
    });
    expect(productFilter).toHaveTextContent("Life Protection Plus");
    expect(productFilter).toHaveTextContent("Auto Saver Plan");

    fireEvent.change(productFilter, {
      target: { value: productPerformancePayload[0].productId },
    });
    await waitFor(() => {
      expect(screen.getByRole("table", { name: "Product performance table" })).toHaveTextContent(
        "Life Protection Plus",
      );
      expect(screen.queryByRole("table", { name: "Product performance table" })).not.toHaveTextContent(
        "Auto Saver Plan",
      );
    });

    fireEvent.change(timeFilter, { target: { value: "CUSTOM" } });
    expect(screen.getByLabelText("Analytics filter date from")).toBeInTheDocument();
    expect(screen.getByLabelText("Analytics filter date to")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Reset filters" }));
    expect(campaignFilter).toHaveValue("");
    expect(productFilter).toHaveValue("");
  });

  it("renders Recharts rate, mix, product, and financial visualizations (item 444)", async () => {
    vi.stubGlobal("fetch", createAnalyticsFetchMock());
    renderAnalyticsPage(["BI_ANALYST"]);

    expect(await screen.findByLabelText("Analytics engagement KPI cards")).toBeInTheDocument();
    expect(
      screen.getByLabelText("Campaign open click and conversion rate comparison chart"),
    ).toBeInTheDocument();
    expect(screen.getByLabelText("Analytics engagement mix pie chart")).toBeInTheDocument();
    expect(
      screen.getByLabelText("Product sent messages versus conversions chart"),
    ).toBeInTheDocument();
    expect(
      screen.getByLabelText("Product cost revenue and ROI line chart"),
    ).toBeInTheDocument();
  });

  it("loads another campaign when the analytics campaign selector changes", async () => {
    const user = userEvent.setup();
    const fetchMock = createAnalyticsFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderAnalyticsPage(["BI_ANALYST"]);

    expect(await screen.findByLabelText("Campaign analytics detail")).toBeInTheDocument();

    const select = screen.getByLabelText("Select campaign for analytics");
    await user.selectOptions(select, secondCampaignId);

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        `${API_BASE_URL}/analytics/campaigns/${secondCampaignId}`,
        expect.objectContaining({
          headers: expect.objectContaining({ Authorization: expect.stringContaining("Bearer") }),
        }),
      );
    });
    expect(await screen.findByText("Auto Renewal Push")).toBeInTheDocument();
  });

  it("surfaces API authorization errors from analytics endpoints", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => jsonResponse({ message: "Access is denied", code: "FORBIDDEN" }, 403)),
    );

    renderAnalyticsPage(["BI_ANALYST"]);

    expect(await screen.findByText("You are not authorized to view analytics.")).toBeInTheDocument();
  });

  it("uses recent metrics for campaign options when campaigns list is unavailable", async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url === `${API_BASE_URL}/analytics/dashboard`) {
        return jsonResponse({
          success: true,
          message: "Analytics dashboard loaded",
          data: dashboardPayload,
        });
      }
      if (url === `${API_BASE_URL}/analytics/products/performance`) {
        return jsonResponse({
          success: true,
          message: "Product performance loaded",
          data: productPerformancePayload,
        });
      }
      if (url.startsWith(`${API_BASE_URL}/campaigns`)) {
        return jsonResponse({ message: "Forbidden", code: "FORBIDDEN" }, 403);
      }
      if (url.startsWith(`${API_BASE_URL}/analytics/campaigns/`)) {
        return jsonResponse({
          success: true,
          message: "Campaign analytics loaded",
          data: campaignAnalyticsPayload(campaignId),
        });
      }
      return jsonResponse({ message: `Unexpected URL ${url}` }, 404);
    });
    vi.stubGlobal("fetch", fetchMock);

    // MARKETING_ANALYST can view analytics but is not in campaign read roles.
    renderAnalyticsPage(["MARKETING_ANALYST"]);

    expect(await screen.findByLabelText("Analytics engagement KPI cards")).toBeInTheDocument();
    expect(await screen.findByLabelText("Campaign analytics detail")).toBeInTheDocument();
    expect(screen.getByText("Spring Life Drive")).toBeInTheDocument();

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        `${API_BASE_URL}/analytics/campaigns/${campaignId}`,
        expect.anything(),
      );
    });
    expect(
      fetchMock.mock.calls.some(([url]) => {
        const path = String(url);
        // Campaign catalog only — not analytics campaign detail.
        return (
          path === `${API_BASE_URL}/campaigns` ||
          path.startsWith(`${API_BASE_URL}/campaigns?`)
        );
      }),
    ).toBe(false);
  });
});
