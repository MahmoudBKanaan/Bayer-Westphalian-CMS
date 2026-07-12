import { describe, expect, it } from "vitest";
import type { CampaignMetricsView, DashboardView } from "@/api/analytics";
import { emptyDashboardView } from "@/api/analytics";
import { eligibilityRatePercent, toPerformanceChartRows } from "@/features/dashboard/dashboardCharts";

const metrics: CampaignMetricsView = {
  metricsId: null,
  campaignId: "50000000-0000-0000-0000-000000000440",
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

describe("dashboardCharts", () => {
  it("maps recent metrics into chart rows for sent vs conversions", () => {
    expect(toPerformanceChartRows([metrics])).toEqual([
      {
        name: "Grandchild Educa…",
        sent: 80,
        conversions: 4,
      },
    ]);
  });

  it("returns empty chart data when metrics are missing", () => {
    expect(toPerformanceChartRows([])).toEqual([]);
    expect(toPerformanceChartRows(null)).toEqual([]);
  });

  it("uses Campaign label when name is blank", () => {
    expect(
      toPerformanceChartRows([{ ...metrics, campaignName: "  " }])[0]?.name,
    ).toBe("Campaign");
  });

  it("computes eligibility rate from dashboard audience totals", () => {
    const dashboard: DashboardView = {
      ...emptyDashboardView,
      audienceSize: 200,
      eligibleCount: 150,
    };
    expect(eligibilityRatePercent(dashboard)).toBe(75);
    expect(eligibilityRatePercent(emptyDashboardView)).toBe(0);
  });
});
