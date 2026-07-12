import { describe, expect, it } from "vitest";
import type { DashboardView, ExecutiveDashboardView } from "@/api/analytics";
import {
  buildDashboardKpiGroups,
  buildExecutiveDashboardKpiGroups,
  DASHBOARD_PAGE_LEAD,
  EXECUTIVE_DASHBOARD_PAGE_LEAD,
  dashboardEmptyGuidance,
  dashboardKpiGroupCount,
} from "@/features/dashboard/dashboardReadability";

const sampleDashboard: DashboardView = {
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
  recentCampaignMetrics: [],
};

const sampleExecutive: ExecutiveDashboardView = {
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
      productId: "p1",
      productName: "Life",
      productType: "LIFE_INSURANCE",
      campaignCount: 1,
      audienceSize: 10,
      eligibleCount: 8,
      sentCount: 7,
      openedCount: 3,
      clickedCount: 1,
      convertedCount: 1,
      openRate: 0.4,
      clickRate: 0.1,
      conversionRate: 0.1,
      estimatedCost: 50,
      estimatedRevenue: 80,
      estimatedRoi: 0.6,
    },
  ],
};

describe("dashboardReadability (item 591)", () => {
  it("exposes plain-language page leads for operational and executive dashboards", () => {
    expect(DASHBOARD_PAGE_LEAD.toLowerCase()).toContain("inventory");
    expect(DASHBOARD_PAGE_LEAD.toLowerCase()).toContain("engagement");
    expect(EXECUTIVE_DASHBOARD_PAGE_LEAD.toLowerCase()).toContain("aggregated");
    expect(dashboardKpiGroupCount()).toBe(3);
  });

  it("groups operational dashboard KPIs into inventory, engagement, and financial sections", () => {
    const groups = buildDashboardKpiGroups(sampleDashboard);

    expect(groups).toHaveLength(3);
    expect(groups.map((group) => group.id)).toEqual([
      "inventory-delivery",
      "engagement-rates",
      "financial-outcomes",
    ]);
    expect(groups[0]?.title).toBe("Inventory & delivery");
    expect(groups[1]?.title).toBe("Engagement rates");
    expect(groups[2]?.title).toBe("Financial outcomes");

    const labels = groups.flatMap((group) => group.cards.map((card) => card.label));
    expect(labels).toEqual([
      "Campaigns",
      "Audience",
      "Messages sent",
      "Eligible / excluded",
      "Open rate",
      "Click rate",
      "Conversion rate",
      "Estimated ROI",
    ]);

    const openRate = groups[1]?.cards.find((card) => card.id === "open-rate");
    expect(openRate?.value).toBe("50.0%");
    expect(openRate?.tone).toBe("engagement");
    expect(openRate?.detail).not.toMatch(/FR-\d+/);

    const roi = groups[2]?.cards[0];
    expect(roi?.tone).toBe("financial");
    expect(roi?.detail).toContain("Cost");
    expect(roi?.detail).toContain("Revenue");
  });

  it("groups executive dashboard KPIs with aggregate wording", () => {
    const groups = buildExecutiveDashboardKpiGroups(sampleExecutive);

    expect(groups).toHaveLength(3);
    expect(groups[0]?.cards.map((card) => card.label)).toContain("Products tracked");
    expect(groups[1]?.description.toLowerCase()).toContain("aggregate");
    expect(groups[1]?.cards.find((card) => card.id === "open-rate")?.detail).toContain(
      "aggregate",
    );
    expect(groups[2]?.cards[0]?.value).toBe("60.0%");
  });

  it("provides empty-state guidance without technical jargon", () => {
    expect(dashboardEmptyGuidance("chart").toLowerCase()).toContain("metrics");
    expect(dashboardEmptyGuidance("table").toLowerCase()).toContain("campaign");
    expect(dashboardEmptyGuidance("metrics").toLowerCase()).toContain("kpi");
  });

  it("formats eligibility detail as a human-readable percent of audience", () => {
    const audience = buildDashboardKpiGroups(sampleDashboard)[0]?.cards.find(
      (card) => card.id === "audience",
    );
    expect(audience?.detail).toBe("80.0% eligible after exclusions");
  });
});
