/**
 * Dashboard analytics load UI integration (KB item 606 / FR-100–FR-108).
 *
 * Full route tree: authorized role opens /dashboard, GET /analytics/dashboard
 * loads KPI groups, charts section headings, and recent metrics table.
 */
import { screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import {
  createDashboardAnalyticsFixture,
  DASHBOARD_ANALYTICS_FIXTURES,
  DASHBOARD_KPI_CARDS_ARIA_LABEL,
  DASHBOARD_PAGE_LEAD,
  DASHBOARD_PAGE_TITLE,
  DASHBOARD_PERFORMANCE_HEADING,
  DASHBOARD_RECENT_METRICS_HEADING,
  DASHBOARD_RECENT_TABLE_ARIA_LABEL,
  DASHBOARD_UNAUTHORIZED_MESSAGE,
  dashboardAnalyticsEndpointPath,
} from "@/features/dashboard/dashboardAnalyticsFlow";
import {
  createFetchRouter,
  jsonError,
  jsonOk,
  renderApp,
} from "@/test/integration/renderApp";

function dashboardAnalyticsHandlers(
  dashboard = createDashboardAnalyticsFixture(),
) {
  return createFetchRouter([
    {
      match: (url) => url.includes(dashboardAnalyticsEndpointPath()),
      response: () => jsonOk(dashboard, "Analytics dashboard loaded"),
    },
  ]);
}

describe("dashboard analytics UI integration (item 606)", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("loads dashboard analytics for BI analysts", async () => {
    const fetchMock = dashboardAnalyticsHandlers();
    vi.stubGlobal("fetch", fetchMock);

    renderApp({ path: "/dashboard", roles: ["BI_ANALYST"] });

    expect(await screen.findByRole("heading", { name: "Dashboard", level: 1 })).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: DASHBOARD_PAGE_TITLE, level: 2 }),
    ).toBeInTheDocument();
    expect(screen.getByTestId("dashboard-analytics-page")).toBeInTheDocument();
    expect(screen.getByText(DASHBOARD_PAGE_LEAD)).toBeInTheDocument();

    expect(await screen.findByLabelText(DASHBOARD_KPI_CARDS_ARIA_LABEL)).toBeInTheDocument();
    expect(screen.getByText(String(DASHBOARD_ANALYTICS_FIXTURES.campaignTotal))).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: DASHBOARD_PERFORMANCE_HEADING }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: DASHBOARD_RECENT_METRICS_HEADING }),
    ).toBeInTheDocument();
    expect(
      await screen.findByRole("table", { name: DASHBOARD_RECENT_TABLE_ARIA_LABEL }),
    ).toBeInTheDocument();
    expect(
      screen.getByText(DASHBOARD_ANALYTICS_FIXTURES.recentCampaignName),
    ).toBeInTheDocument();

    await waitFor(() => {
      expect(
        fetchMock.mock.calls.some(([url]) =>
          String(url).includes(dashboardAnalyticsEndpointPath()),
        ),
      ).toBe(true);
    });
  });

  it("loads dashboard analytics for campaign managers", async () => {
    vi.stubGlobal("fetch", dashboardAnalyticsHandlers());
    renderApp({ path: "/dashboard", roles: ["CAMPAIGN_MANAGER"] });

    expect(await screen.findByLabelText(DASHBOARD_KPI_CARDS_ARIA_LABEL)).toBeInTheDocument();
    expect(screen.getByText("Messages sent")).toBeInTheDocument();
  });

  it("denies dashboard analytics for unauthorized roles", async () => {
    const fetchMock = dashboardAnalyticsHandlers();
    vi.stubGlobal("fetch", fetchMock);

    renderApp({ path: "/dashboard", roles: ["CUSTOMER_SERVICE_AGENT"] });

    expect(await screen.findByText(DASHBOARD_UNAUTHORIZED_MESSAGE)).toBeInTheDocument();
    expect(screen.queryByTestId("dashboard-analytics-page")).not.toBeInTheDocument();
    expect(
      fetchMock.mock.calls.some(([url]) =>
        String(url).includes(dashboardAnalyticsEndpointPath()),
      ),
    ).toBe(false);
  });

  it("surfaces API errors when analytics fails to load", async () => {
    vi.stubGlobal(
      "fetch",
      createFetchRouter([
        {
          match: (url) => url.includes(dashboardAnalyticsEndpointPath()),
          response: () => jsonError(500, "Dashboard analytics unavailable", "SERVER_ERROR"),
        },
      ]),
    );

    renderApp({ path: "/dashboard", roles: ["ADMIN"] });

    expect(await screen.findByTestId("dashboard-analytics-error")).toBeInTheDocument();
  });
});
