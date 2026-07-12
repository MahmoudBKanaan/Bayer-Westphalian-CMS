import { expect, test } from "@playwright/test";
import {
  createDashboardAnalyticsFixture,
  DASHBOARD_ANALYTICS_FIXTURES,
  DASHBOARD_KPI_CARDS_ARIA_LABEL,
  DASHBOARD_PAGE_LEAD,
  DASHBOARD_PAGE_TITLE,
  DASHBOARD_PERFORMANCE_HEADING,
  DASHBOARD_RECENT_METRICS_HEADING,
  DASHBOARD_RECENT_TABLE_ARIA_LABEL,
  dashboardAnalyticsEndpointPath,
} from "../../src/features/dashboard/dashboardAnalyticsFlow";
import {
  createHappyPathMockState,
  handleHappyPathApiRequest,
  type MockHttpMethod,
} from "../../src/features/e2e/happyPathApiMock";
import { loginAsHappyPathAdmin } from "./helpers/uiActions";

/**
 * Item 606 — Dashboard loads analytics (Playwright).
 *
 * Seeds a rich dashboard payload so KPI cards, charts section, and metrics table render.
 */
async function installDashboardAnalyticsApiMock(page: import("@playwright/test").Page) {
  const state = createHappyPathMockState();
  const dashboard = createDashboardAnalyticsFixture();

  await page.route("**/api/**", async (route) => {
    const request = route.request();
    const url = request.url();
    if (url.includes(dashboardAnalyticsEndpointPath()) && request.method().toUpperCase() === "GET") {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          success: true,
          message: "Analytics dashboard loaded",
          data: dashboard,
        }),
      });
      return;
    }

    const response = handleHappyPathApiRequest(state, {
      method: request.method().toUpperCase() as MockHttpMethod,
      url,
      bodyText: request.postData() ?? undefined,
    });
    await route.fulfill({
      status: response.status,
      contentType: "application/json",
      body: JSON.stringify(response.body),
    });
  });
}

test.describe("Dashboard loads analytics (item 606)", () => {
  test("loads KPI groups, performance section, and recent metrics after login", async ({
    page,
  }) => {
    await installDashboardAnalyticsApiMock(page);
    await loginAsHappyPathAdmin(page);

    await expect(page.getByRole("heading", { name: "Dashboard", level: 1 })).toBeVisible();
    await expect(page.getByRole("heading", { name: DASHBOARD_PAGE_TITLE, level: 2 })).toBeVisible();
    await expect(page.getByTestId("dashboard-analytics-page")).toBeVisible();
    await expect(page.getByText(DASHBOARD_PAGE_LEAD)).toBeVisible();

    await expect(page.getByLabel(DASHBOARD_KPI_CARDS_ARIA_LABEL)).toBeVisible();
    await expect(page.getByText(String(DASHBOARD_ANALYTICS_FIXTURES.campaignTotal))).toBeVisible();
    await expect(page.getByRole("heading", { name: DASHBOARD_PERFORMANCE_HEADING })).toBeVisible();
    await expect(
      page.getByRole("heading", { name: DASHBOARD_RECENT_METRICS_HEADING }),
    ).toBeVisible();
    await expect(
      page.getByRole("table", { name: DASHBOARD_RECENT_TABLE_ARIA_LABEL }),
    ).toBeVisible();
    await expect(page.getByText(DASHBOARD_ANALYTICS_FIXTURES.recentCampaignName)).toBeVisible();
  });

  test("navigates to dashboard from shell after login", async ({ page }) => {
    await installDashboardAnalyticsApiMock(page);
    await loginAsHappyPathAdmin(page);

    // Already lands on dashboard after login; re-click ensures shell navigation works.
    await page.getByRole("link", { name: "Dashboard" }).click();
    await expect(page.getByLabel(DASHBOARD_KPI_CARDS_ARIA_LABEL)).toBeVisible();
    await expect(page.getByText("Messages sent")).toBeVisible();
  });
});
