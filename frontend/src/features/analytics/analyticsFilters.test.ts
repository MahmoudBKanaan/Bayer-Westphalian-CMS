import { describe, expect, it } from "vitest";
import type { CampaignMetricsView, DashboardView } from "@/api/analytics";
import type { CampaignView } from "@/api/campaigns";
import {
  areAnalyticsFiltersActive,
  dashboardFromMetrics,
  emptyAnalyticsFilters,
  filterCampaignMetrics,
  filterProductPerformance,
  matchesTimeframe,
  resolveTimeframeBounds,
} from "@/features/analytics/analyticsFilters";

const today = new Date(2026, 6, 14); // 2026-07-14 local

const metrics: CampaignMetricsView[] = [
  {
    metricsId: "m1",
    campaignId: "c1",
    campaignName: "Spring",
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
  {
    metricsId: "m2",
    campaignId: "c2",
    campaignName: "Winter",
    campaignStatus: "COMPLETED",
    audienceSize: 50,
    eligibleCount: 40,
    excludedCount: 10,
    sentCount: 40,
    openedCount: 10,
    clickedCount: 4,
    repliedCount: 2,
    convertedCount: 1,
    openRate: 0.25,
    clickRate: 0.1,
    conversionRate: 0.025,
    estimatedCost: 100,
    estimatedRevenue: 50,
    estimatedRoi: -0.5,
    updatedAt: "2026-01-15T10:00:00Z",
  },
];

const campaigns = [
  {
    id: "c1",
    name: "Spring",
    startDate: "2026-07-01",
    endDate: "2026-07-31",
    productIds: ["p1", "p2"],
  },
  {
    id: "c2",
    name: "Winter",
    startDate: "2026-01-01",
    endDate: "2026-01-31",
    productIds: ["p2"],
  },
] as CampaignView[];

const products = [
  {
    productId: "p1",
    productName: "Life",
    productType: "LIFE_INSURANCE" as const,
    campaignCount: 1,
    audienceSize: 100,
    eligibleCount: 80,
    sentCount: 80,
    openedCount: 40,
    clickedCount: 16,
    convertedCount: 4,
    openRate: 0.5,
    clickRate: 0.2,
    conversionRate: 0.05,
    estimatedCost: 200,
    estimatedRevenue: 300,
    estimatedRoi: 0.5,
  },
  {
    productId: "p2",
    productName: "Auto",
    productType: "INVESTMENT_FUND" as const,
    campaignCount: 2,
    audienceSize: 150,
    eligibleCount: 120,
    sentCount: 120,
    openedCount: 50,
    clickedCount: 20,
    convertedCount: 5,
    openRate: 0.4,
    clickRate: 0.16,
    conversionRate: 0.04,
    estimatedCost: 300,
    estimatedRevenue: 350,
    estimatedRoi: 0.16,
  },
];

describe("analyticsFilters (KB item 441)", () => {
  it("resolves preset and custom timeframes", () => {
    expect(resolveTimeframeBounds({ timeframe: "ALL", dateFrom: "", dateTo: "" }, today)).toEqual({
      from: null,
      to: null,
    });
    expect(
      resolveTimeframeBounds({ timeframe: "LAST_7_DAYS", dateFrom: "", dateTo: "" }, today),
    ).toEqual({ from: "2026-07-08", to: "2026-07-14" });
    expect(
      resolveTimeframeBounds(
        { timeframe: "CUSTOM", dateFrom: "2026-06-01", dateTo: "2026-06-30" },
        today,
      ),
    ).toEqual({ from: "2026-06-01", to: "2026-06-30" });
  });

  it("matches overlapping campaign schedules against time bounds", () => {
    expect(
      matchesTimeframe(
        { from: "2026-07-01", to: "2026-07-31" },
        { startDate: "2026-07-10", endDate: "2026-07-20" },
      ),
    ).toBe(true);
    expect(
      matchesTimeframe(
        { from: "2026-07-01", to: "2026-07-31" },
        { startDate: "2026-01-01", endDate: "2026-01-31" },
      ),
    ).toBe(false);
  });

  it("filters campaign metrics by campaign, product, and timeframe", () => {
    const byCampaign = filterCampaignMetrics(
      metrics,
      { ...emptyAnalyticsFilters, campaignId: "c1" },
      campaigns,
      today,
    );
    expect(byCampaign).toHaveLength(1);
    expect(byCampaign[0]?.campaignId).toBe("c1");

    const byProduct = filterCampaignMetrics(
      metrics,
      { ...emptyAnalyticsFilters, productId: "p1" },
      campaigns,
      today,
    );
    expect(byProduct.map((m) => m.campaignId)).toEqual(["c1"]);

    const byTime = filterCampaignMetrics(
      metrics,
      { ...emptyAnalyticsFilters, timeframe: "LAST_30_DAYS" },
      campaigns,
      today,
    );
    expect(byTime.map((m) => m.campaignId)).toEqual(["c1"]);
  });

  it("filters product performance by product and campaign product links", () => {
    expect(
      filterProductPerformance(
        products,
        { ...emptyAnalyticsFilters, productId: "p1" },
        campaigns,
        metrics,
      ),
    ).toHaveLength(1);

    const forC1 = filterProductPerformance(
      products,
      { ...emptyAnalyticsFilters, campaignId: "c1" },
      campaigns,
      metrics.filter((m) => m.campaignId === "c1"),
    );
    expect(forC1.map((p) => p.productId).sort()).toEqual(["p1", "p2"]);
  });

  it("aggregates dashboard KPIs from filtered metrics", () => {
    const base = {
      campaignTotal: 2,
      activeCampaigns: 1,
      audienceSize: 150,
      messagesSent: 120,
      eligibleCount: 120,
      excludedCount: 30,
      openedCount: 50,
      clickedCount: 20,
      repliedCount: 10,
      convertedCount: 5,
      openRate: 0.4,
      clickRate: 0.16,
      conversionRate: 0.04,
      estimatedCost: 300,
      estimatedRevenue: 350,
      estimatedRoi: 0.16,
      recentCampaignMetrics: metrics,
    } satisfies DashboardView;

    const filtered = dashboardFromMetrics(base, [metrics[0]!], true);
    expect(filtered?.messagesSent).toBe(80);
    expect(filtered?.openedCount).toBe(40);
    expect(filtered?.openRate).toBeCloseTo(0.5);
    expect(filtered?.estimatedRoi).toBeCloseTo(0.5);
    expect(filtered?.recentCampaignMetrics).toHaveLength(1);
  });

  it("detects active filters", () => {
    expect(areAnalyticsFiltersActive(emptyAnalyticsFilters)).toBe(false);
    expect(areAnalyticsFiltersActive({ ...emptyAnalyticsFilters, campaignId: "c1" })).toBe(true);
    expect(areAnalyticsFiltersActive({ ...emptyAnalyticsFilters, timeframe: "LAST_7_DAYS" })).toBe(
      true,
    );
  });
});
