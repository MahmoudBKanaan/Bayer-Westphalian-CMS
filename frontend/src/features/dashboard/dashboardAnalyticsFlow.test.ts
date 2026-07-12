import { describe, expect, it } from "vitest";
import {
  buildDashboardKpiGroups,
  canViewDashboardAnalytics,
  createDashboardAnalyticsFixture,
  DASHBOARD_ANALYTICS_FIXTURES,
  DASHBOARD_ANALYTICS_UI_ROLES,
  DASHBOARD_PAGE_LEAD,
  DASHBOARD_PERFORMANCE_HEADING,
  dashboardAnalyticsEndpointPath,
  dashboardAnalyticsStepIdsInOrder,
  formatDashboardAnalyticsJourney,
  hasLoadedDashboardAnalytics,
  isValidDashboardAnalyticsOrder,
} from "@/features/dashboard/dashboardAnalyticsFlow";

describe("dashboardAnalyticsFlow (item 606)", () => {
  it("documents the UI dashboard analytics load journey", () => {
    expect(dashboardAnalyticsStepIdsInOrder()).toEqual([
      "open-dashboard",
      "request-analytics",
      "render-kpis",
      "render-charts-and-table",
    ]);
    expect(formatDashboardAnalyticsJourney()).toBe(
      "Open Dashboard → Request analytics → Render KPI groups → Render charts and metrics table",
    );
    expect(isValidDashboardAnalyticsOrder(dashboardAnalyticsStepIdsInOrder())).toBe(true);
    expect(isValidDashboardAnalyticsOrder(["render-kpis"] as never)).toBe(false);
  });

  it("allows analytics-read roles to view the dashboard", () => {
    expect(DASHBOARD_ANALYTICS_UI_ROLES).toEqual([
      "ADMIN",
      "BI_ANALYST",
      "CAMPAIGN_MANAGER",
      "MARKETING_ANALYST",
      "EXECUTIVE_VIEWER",
    ]);
    expect(canViewDashboardAnalytics(["BI_ANALYST"])).toBe(true);
    expect(canViewDashboardAnalytics(["CAMPAIGN_MANAGER"])).toBe(true);
    expect(canViewDashboardAnalytics(["CUSTOMER_SERVICE_AGENT"])).toBe(false);
    expect(canViewDashboardAnalytics(["COMPLIANCE_OFFICER"])).toBe(false);
  });

  it("builds a fixture payload that counts as loaded analytics", () => {
    const dashboard = createDashboardAnalyticsFixture();
    expect(hasLoadedDashboardAnalytics(dashboard)).toBe(true);
    expect(hasLoadedDashboardAnalytics(null)).toBe(false);
    expect(dashboard.campaignTotal).toBe(DASHBOARD_ANALYTICS_FIXTURES.campaignTotal);
    expect(dashboard.recentCampaignMetrics[0]?.campaignName).toBe(
      DASHBOARD_ANALYTICS_FIXTURES.recentCampaignName,
    );
  });

  it("produces KPI groups from the fixture for UI rendering", () => {
    const groups = buildDashboardKpiGroups(createDashboardAnalyticsFixture());
    expect(groups.map((group) => group.id)).toEqual([
      "inventory-delivery",
      "engagement-rates",
      "financial-outcomes",
    ]);
    expect(groups[0]?.cards.some((card) => card.label === "Campaigns")).toBe(true);
  });

  it("pins endpoint path, lead copy, and performance heading for UI tests", () => {
    expect(dashboardAnalyticsEndpointPath()).toBe("/analytics/dashboard");
    expect(DASHBOARD_PAGE_LEAD).toMatch(/campaign inventory/i);
    expect(DASHBOARD_PERFORMANCE_HEADING).toBe("Campaign performance");
  });
});
