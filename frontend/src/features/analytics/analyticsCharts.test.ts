import { describe, expect, it } from "vitest";
import type {
  CampaignMetricsView,
  ExecutiveDashboardView,
  ProductPerformanceView,
} from "@/api/analytics";
import { emptyDashboardView, emptyExecutiveDashboardView } from "@/api/analytics";
import {
  toCampaignFinancialChartRows,
  toDashboardEngagementMixRows,
  toExecutiveEngagementMixRows,
  toExecutiveFunnelChartRows,
  toExecutiveInventoryChartRows,
  toProductFinancialChartRows,
  toProductPerformanceChartRows,
  toRateComparisonChartRows,
} from "@/features/analytics/analyticsCharts";

const metrics: CampaignMetricsView = {
  metricsId: null,
  campaignId: "50000000-0000-0000-0000-000000000441",
  campaignName: "Grandchild Education Plan",
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
};

const product: ProductPerformanceView = {
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
};

describe("analyticsCharts (item 441)", () => {
  it("maps campaign metrics into rate comparison chart rows", () => {
    expect(toRateComparisonChartRows([metrics])).toEqual([
      {
        name: "Grandchild Educa…",
        openRate: 50,
        clickRate: 20,
        conversionRate: 5,
      },
    ]);
  });

  it("returns empty rate rows when metrics are missing", () => {
    expect(toRateComparisonChartRows([])).toEqual([]);
    expect(toRateComparisonChartRows(null)).toEqual([]);
  });

  it("uses Campaign label when campaign name is blank", () => {
    expect(toRateComparisonChartRows([{ ...metrics, campaignName: "  " }])[0]?.name).toBe(
      "Campaign",
    );
  });

  it("maps product performance into comparison chart rows", () => {
    expect(toProductPerformanceChartRows([product])).toEqual([
      {
        name: "Life Protection …",
        sent: 150,
        conversions: 10,
        openRatePercent: 50,
        conversionRatePercent: 6.67,
      },
    ]);
  });

  it("returns empty product chart rows when performance list is empty", () => {
    expect(toProductPerformanceChartRows([])).toEqual([]);
    expect(toProductPerformanceChartRows(null)).toEqual([]);
  });

  it("uses Product label when product name is blank", () => {
    expect(
      toProductPerformanceChartRows([{ ...product, productName: null }])[0]?.name,
    ).toBe("Product");
  });

  it("maps executive inventory and funnel chart rows (item 443 / COMP-010)", () => {
    const executive: ExecutiveDashboardView = {
      ...emptyExecutiveDashboardView,
      totalCampaigns: 5,
      activeCampaigns: 2,
      completedCampaigns: 1,
      totalAudience: 100,
      totalEligible: 80,
      totalSent: 70,
      totalOpened: 35,
      totalClicked: 14,
      totalConverted: 7,
    };

    expect(toExecutiveInventoryChartRows(executive)).toEqual([
      { name: "Total", value: 5 },
      { name: "Active", value: 2 },
      { name: "Completed", value: 1 },
    ]);
    expect(toExecutiveFunnelChartRows(executive)).toEqual([
      { name: "Audience", value: 100 },
      { name: "Eligible", value: 80 },
      { name: "Sent", value: 70 },
      { name: "Opened", value: 35 },
      { name: "Clicked", value: 14 },
      { name: "Converted", value: 7 },
    ]);
    expect(toExecutiveInventoryChartRows(null)).toEqual([]);
    expect(toExecutiveFunnelChartRows(undefined)).toEqual([]);
  });

  it("maps engagement mix and financial rows for Recharts visualizations (item 444)", () => {
    expect(
      toDashboardEngagementMixRows({
        ...emptyDashboardView,
        openedCount: 40,
        clickedCount: 16,
        repliedCount: 8,
        convertedCount: 4,
      }),
    ).toEqual([
      { name: "Opened", value: 40 },
      { name: "Clicked", value: 16 },
      { name: "Replied", value: 8 },
      { name: "Converted", value: 4 },
    ]);

    expect(
      toExecutiveEngagementMixRows({
        ...emptyExecutiveDashboardView,
        totalOpened: 35,
        totalClicked: 14,
        totalReplied: 7,
        totalConverted: 5,
      }),
    ).toEqual([
      { name: "Opened", value: 35 },
      { name: "Clicked", value: 14 },
      { name: "Replied", value: 7 },
      { name: "Converted", value: 5 },
    ]);

    expect(toProductFinancialChartRows([product])).toEqual([
      {
        name: "Life Protection …",
        cost: 400,
        revenue: 600,
        roiPercent: 50,
      },
    ]);

    expect(toCampaignFinancialChartRows([metrics])).toEqual([
      {
        name: "Grandchild Educa…",
        cost: 200,
        revenue: 300,
        roiPercent: 50,
      },
    ]);

    expect(toDashboardEngagementMixRows(null)).toEqual([]);
    expect(toProductFinancialChartRows([])).toEqual([]);
    expect(toCampaignFinancialChartRows(null)).toEqual([]);
  });
});
