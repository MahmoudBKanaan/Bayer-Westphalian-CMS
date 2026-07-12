import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import { AuthProvider } from "@/auth/AuthProvider";
import { AUTH_STORAGE_KEYS, type SystemRoleName } from "@/auth/sessionStorageStrategy";
import {
  DASHBOARD_KPI_CARDS_ARIA_LABEL,
  DASHBOARD_PAGE_TITLE,
  DASHBOARD_PERFORMANCE_HEADING,
  DASHBOARD_RECENT_METRICS_HEADING,
  DASHBOARD_RECENT_TABLE_ARIA_LABEL,
  DASHBOARD_UNAUTHORIZED_MESSAGE,
} from "@/features/dashboard/dashboardAnalyticsFlow";
import { DashboardPage } from "@/pages/DashboardPage";

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
      metricsId: "51000000-0000-0000-0000-000000000440",
      campaignId: "50000000-0000-0000-0000-000000000440",
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

function renderDashboardPage(roles: SystemRoleName[] = ["BI_ANALYST"]) {
  sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, createAccessToken(roles));
  sessionStorage.setItem(AUTH_STORAGE_KEYS.refreshToken, "refresh-token");
  sessionStorage.setItem(
    AUTH_STORAGE_KEYS.currentUser,
    JSON.stringify({
      id: "10000000-0000-0000-0000-000000000440",
      email: "bi@bayer-westphalian.test",
      fullName: "BI Analyst",
      status: "ACTIVE",
      lastLoginAt: "2026-07-11T10:00:00Z",
      roles,
    }),
  );

  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={["/dashboard"]}>
        <AuthProvider>
          <DashboardPage />
        </AuthProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

function createDashboardFetchMock(data: unknown = dashboardPayload) {
  return vi.fn(async (input: RequestInfo | URL) => {
    const url = String(input);
    if (url === `${API_BASE_URL}/analytics/dashboard`) {
      return jsonResponse({
        success: true,
        message: "Analytics dashboard loaded",
        data,
      });
    }
    return jsonResponse({ message: `Unexpected URL ${url}` }, 404);
  });
}

describe("DashboardPage (item 440 / item 591)", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("loads dashboard KPIs for BI analysts (FR-100–FR-107)", async () => {
    const fetchMock = createDashboardFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderDashboardPage(["BI_ANALYST"]);

    expect(
      await screen.findByRole("heading", { name: DASHBOARD_PAGE_TITLE }),
    ).toBeInTheDocument();
    expect(await screen.findByLabelText(DASHBOARD_KPI_CARDS_ARIA_LABEL)).toBeInTheDocument();
    expect(screen.getByTestId("dashboard-analytics-page")).toBeInTheDocument();
    expect(screen.getByText("Campaigns")).toBeInTheDocument();
    expect(screen.getByText("3")).toBeInTheDocument();
    expect(screen.getByText("Messages sent")).toBeInTheDocument();
    expect(screen.getAllByText("80").length).toBeGreaterThanOrEqual(1);
    // Labels appear on KPI cards and table headers.
    expect(screen.getAllByText("Open rate").length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText("Click rate").length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText("20.0%").length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText("Conversion rate").length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText("5.0%").length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText("Estimated ROI")).toBeInTheDocument();
    // Open rate and estimated ROI both format as 50.0% for this fixture.
    expect(screen.getAllByText("50.0%").length).toBeGreaterThanOrEqual(2);
    expect(
      screen.getByRole("heading", { name: DASHBOARD_PERFORMANCE_HEADING }),
    ).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "AI recommendations" })).toBeInTheDocument();
    expect(screen.getByText("Customer search")).toBeInTheDocument();
    expect(screen.getByText("Default-risk scoring")).toBeInTheDocument();
    expect(screen.getByText("Segment suggestions")).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: DASHBOARD_RECENT_METRICS_HEADING }),
    ).toBeInTheDocument();
    expect(
      await screen.findByRole("table", { name: DASHBOARD_RECENT_TABLE_ARIA_LABEL }),
    ).toBeInTheDocument();
    expect(screen.getByText("Spring Life Drive")).toBeInTheDocument();

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        `${API_BASE_URL}/analytics/dashboard`,
        expect.objectContaining({
          headers: expect.objectContaining({ Authorization: expect.stringContaining("Bearer") }),
        }),
      );
    });
  });

  it("improves dashboard readability with grouped KPIs, lead text, and scannable table (item 591)", async () => {
    vi.stubGlobal("fetch", createDashboardFetchMock());
    renderDashboardPage(["BI_ANALYST"]);

    expect(
      await screen.findByText(/Scan campaign inventory, audience reach, engagement rates/i),
    ).toBeInTheDocument();
    expect(
      await screen.findByRole("heading", { name: "Inventory & delivery", level: 3 }),
    ).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Engagement rates", level: 3 })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Financial outcomes", level: 3 })).toBeInTheDocument();
    expect(screen.getByText("Campaign volume, audience size, and messages already sent")).toBeInTheDocument();
    expect(
      screen.getByText("Numeric columns are right-aligned for quicker comparison across campaigns."),
    ).toBeInTheDocument();

    const metricsTable = await screen.findByRole("table", {
      name: "Recent campaign metrics table",
    });
    expect(metricsTable.querySelectorAll("th.numeric-col").length).toBeGreaterThanOrEqual(3);
    expect(metricsTable.querySelector("th.dashboard-campaign-name")?.textContent).toContain(
      "Spring Life Drive",
    );
    // Detail copy should stay business-readable (no FR codes in KPI cards).
    expect(screen.queryByText(/FR-\d+/)).not.toBeInTheDocument();
  });

  it("allows campaign managers and executive viewers to load the dashboard", async () => {
    vi.stubGlobal("fetch", createDashboardFetchMock());
    renderDashboardPage(["CAMPAIGN_MANAGER"]);
    expect(await screen.findByLabelText("Dashboard KPI cards")).toBeInTheDocument();
    expect(screen.getByText("Campaign copy")).toBeInTheDocument();
  });

  it("denies dashboard analytics for unauthorized roles", async () => {
    const fetchMock = createDashboardFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderDashboardPage(["PRODUCT_MANAGER"]);

    expect(
      await screen.findByText(DASHBOARD_UNAUTHORIZED_MESSAGE),
    ).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("shows empty chart and table messaging when no metrics exist", async () => {
    vi.stubGlobal(
      "fetch",
      createDashboardFetchMock({
        ...dashboardPayload,
        recentCampaignMetrics: [],
      }),
    );

    renderDashboardPage(["ADMIN"]);

    expect(
      await screen.findByText("No recent campaign metrics are available for the chart yet."),
    ).toBeInTheDocument();
    expect(screen.getByText("No campaign metrics have been recorded yet.")).toBeInTheDocument();
    expect(
      screen.getByText("No campaign financial chart data is available yet."),
    ).toBeInTheDocument();
  });

  it("renders engagement mix and financial Recharts visualizations (item 444)", async () => {
    vi.stubGlobal("fetch", createDashboardFetchMock());
    renderDashboardPage(["BI_ANALYST"]);

    expect(await screen.findByLabelText("Dashboard KPI cards")).toBeInTheDocument();
    expect(
      screen.getByLabelText("Sent messages versus conversions chart"),
    ).toBeInTheDocument();
    expect(screen.getByLabelText("Dashboard engagement mix pie chart")).toBeInTheDocument();
    expect(
      screen.getByLabelText("Dashboard campaign financial line chart"),
    ).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Engagement mix" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Campaign financials" })).toBeInTheDocument();
  });

  it("surfaces API authorization errors from the dashboard endpoint", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () =>
        jsonResponse({ message: "Access is denied", code: "FORBIDDEN" }, 403),
      ),
    );

    renderDashboardPage(["BI_ANALYST"]);

    expect(
      await screen.findByText(DASHBOARD_UNAUTHORIZED_MESSAGE),
    ).toBeInTheDocument();
  });
});
