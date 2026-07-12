import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import { AuthProvider } from "@/auth/AuthProvider";
import { AUTH_STORAGE_KEYS, type SystemRoleName } from "@/auth/sessionStorageStrategy";
import { ExecutiveDashboardPage } from "@/pages/ExecutiveDashboardPage";

const executivePayload = {
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
  productPerformance: [
    {
      productId: "60000000-0000-0000-0000-000000000443",
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

function renderExecutiveDashboardPage(roles: SystemRoleName[] = ["EXECUTIVE_VIEWER"]) {
  sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, createAccessToken(roles));
  sessionStorage.setItem(AUTH_STORAGE_KEYS.refreshToken, "refresh-token");
  sessionStorage.setItem(
    AUTH_STORAGE_KEYS.currentUser,
    JSON.stringify({
      id: "10000000-0000-0000-0000-000000000443",
      email: "executive@bayer-westphalian.test",
      fullName: "Executive Viewer",
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
      <MemoryRouter initialEntries={["/executive"]}>
        <AuthProvider>
          <ExecutiveDashboardPage />
        </AuthProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

function createExecutiveFetchMock(data: unknown = executivePayload) {
  return vi.fn(async (input: RequestInfo | URL) => {
    const url = String(input);
    if (url === `${API_BASE_URL}/analytics/executive`) {
      return jsonResponse({
        success: true,
        message: "Executive dashboard loaded",
        data,
      });
    }
    return jsonResponse({ message: `Unexpected URL ${url}` }, 404);
  });
}

describe("ExecutiveDashboardPage (item 443 / item 591)", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("loads aggregated executive KPIs and product summary for executive viewers (COMP-010)", async () => {
    const fetchMock = createExecutiveFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderExecutiveDashboardPage(["EXECUTIVE_VIEWER"]);

    expect(await screen.findByRole("heading", { name: "Executive dashboard" })).toBeInTheDocument();
    expect(await screen.findByLabelText("Executive dashboard KPI cards")).toBeInTheDocument();
    expect(screen.getAllByText("Campaigns").length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText("5")).toBeInTheDocument();
    expect(screen.getByText("Total audience")).toBeInTheDocument();
    expect(screen.getByText("Messages sent")).toBeInTheDocument();
    expect(screen.getAllByText("Open rate").length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText("Click rate").length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText("Conversion rate").length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText("Estimated ROI").length).toBeGreaterThanOrEqual(1);
    // Open rate 50% and product open rate 50% may both appear.
    expect(screen.getAllByText("50.0%").length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText("20.0%").length).toBeGreaterThanOrEqual(1);

    expect(screen.getByRole("heading", { name: "Campaign inventory" })).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: "Audience and engagement funnel" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: "Product performance summary" }),
    ).toBeInTheDocument();
    expect(
      await screen.findByRole("table", { name: "Executive product performance table" }),
    ).toBeInTheDocument();
    expect(screen.getByText("Life Protection Plus")).toBeInTheDocument();

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        `${API_BASE_URL}/analytics/executive`,
        expect.objectContaining({
          headers: expect.objectContaining({ Authorization: expect.stringContaining("Bearer") }),
        }),
      );
    });
  });

  it("improves executive dashboard readability with grouped KPIs and table structure (item 591)", async () => {
    vi.stubGlobal("fetch", createExecutiveFetchMock());
    renderExecutiveDashboardPage(["EXECUTIVE_VIEWER"]);

    expect(
      await screen.findByText(/Management view of platform-wide inventory/i),
    ).toBeInTheDocument();
    expect(
      await screen.findByRole("heading", { name: "Inventory & delivery", level: 3 }),
    ).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Engagement rates", level: 3 })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Financial outcomes", level: 3 })).toBeInTheDocument();
    expect(
      screen.getByText(/Aggregate rates across the platform/i),
    ).toBeInTheDocument();

    const table = await screen.findByRole("table", {
      name: "Executive product performance table",
    });
    expect(table.querySelector("th.dashboard-campaign-name")?.textContent).toContain(
      "Life Protection Plus",
    );
    expect(table.querySelectorAll("th.numeric-col, td.numeric-col").length).toBeGreaterThan(0);
  });

  it("allows BI analysts and campaign managers to load the executive dashboard", async () => {
    vi.stubGlobal("fetch", createExecutiveFetchMock());
    renderExecutiveDashboardPage(["BI_ANALYST"]);
    expect(await screen.findByLabelText("Executive dashboard KPI cards")).toBeInTheDocument();
  });

  it("denies the executive dashboard for unauthorized roles without calling the API", async () => {
    const fetchMock = createExecutiveFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderExecutiveDashboardPage(["PRODUCT_MANAGER"]);

    expect(
      await screen.findByText("You are not authorized to view the executive dashboard."),
    ).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("shows empty product and zero-inventory messaging when aggregates are empty", async () => {
    vi.stubGlobal(
      "fetch",
      createExecutiveFetchMock({
        totalCampaigns: 0,
        activeCampaigns: 0,
        completedCampaigns: 0,
        totalAudience: 0,
        totalEligible: 0,
        totalExcluded: 0,
        totalSent: 0,
        totalOpened: 0,
        totalClicked: 0,
        totalReplied: 0,
        totalConverted: 0,
        overallOpenRate: 0,
        overallClickRate: 0,
        overallConversionRate: 0,
        totalEstimatedCost: null,
        totalEstimatedRevenue: null,
        overallEstimatedRoi: null,
        productPerformance: [],
      }),
    );

    renderExecutiveDashboardPage(["ADMIN"]);

    expect(
      await screen.findByText("No campaign inventory aggregates are available yet."),
    ).toBeInTheDocument();
    expect(screen.getByText("No funnel aggregates are available yet.")).toBeInTheDocument();
    expect(screen.getByText("No engagement mix data is available yet.")).toBeInTheDocument();
    expect(
      screen.getByText("No product performance aggregates are available yet."),
    ).toBeInTheDocument();
    expect(
      screen.getByText("No product financial chart data is available yet."),
    ).toBeInTheDocument();
    expect(
      screen.getByText("No products are linked to campaign aggregates yet."),
    ).toBeInTheDocument();
  });

  it("renders Recharts inventory, funnel, mix, product, and financial charts (item 444)", async () => {
    vi.stubGlobal("fetch", createExecutiveFetchMock());
    renderExecutiveDashboardPage(["EXECUTIVE_VIEWER"]);

    expect(await screen.findByLabelText("Executive dashboard KPI cards")).toBeInTheDocument();
    expect(screen.getByLabelText("Executive campaign inventory chart")).toBeInTheDocument();
    expect(
      screen.getByLabelText("Executive audience engagement funnel chart"),
    ).toBeInTheDocument();
    expect(screen.getByLabelText("Executive engagement mix pie chart")).toBeInTheDocument();
    expect(
      screen.getByLabelText("Executive product sent versus conversions chart"),
    ).toBeInTheDocument();
    expect(
      screen.getByLabelText("Executive product cost revenue and ROI line chart"),
    ).toBeInTheDocument();
  });

  it("surfaces API authorization errors from the executive endpoint", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () =>
        jsonResponse({ message: "Access is denied", code: "FORBIDDEN" }, 403),
      ),
    );

    renderExecutiveDashboardPage(["EXECUTIVE_VIEWER"]);

    expect(
      await screen.findByText("You are not authorized to view the executive dashboard."),
    ).toBeInTheDocument();
  });
});
