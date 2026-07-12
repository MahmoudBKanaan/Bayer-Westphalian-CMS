import type {
  CampaignMetricsView,
  DashboardView,
  ExecutiveDashboardView,
  ProductPerformanceView,
} from "@/api/analytics";
import type { EngagementMixSlice, FinancialChartRow } from "@/components/charts";

export type RateComparisonChartRow = {
  name: string;
  openRate: number;
  clickRate: number;
  conversionRate: number;
};

export type ProductPerformanceChartRow = {
  name: string;
  sent: number;
  conversions: number;
  openRatePercent: number;
  conversionRatePercent: number;
};

export type NamedCountChartRow = {
  name: string;
  value: number;
};

/**
 * Maps recent campaign metrics into rate comparison rows for the Analytics screen
 * (KB item 441 / FR-108 engagement charts).
 *
 * Rate fields are converted from 0–1 fractions to percentage points for Recharts.
 */
export function toRateComparisonChartRows(
  metrics: CampaignMetricsView[] | null | undefined,
): RateComparisonChartRow[] {
  if (metrics == null || metrics.length === 0) {
    return [];
  }
  return metrics.map((row) => ({
    name: shortenLabel(row.campaignName, "Campaign"),
    openRate: toPercentPoints(row.openRate),
    clickRate: toPercentPoints(row.clickRate),
    conversionRate: toPercentPoints(row.conversionRate),
  }));
}

/**
 * Maps product performance rows into comparison chart data (KB item 441 / item 433).
 */
export function toProductPerformanceChartRows(
  rows: ProductPerformanceView[] | null | undefined,
): ProductPerformanceChartRow[] {
  if (rows == null || rows.length === 0) {
    return [];
  }
  return rows.map((row) => ({
    name: shortenLabel(row.productName, "Product"),
    sent: row.sentCount ?? 0,
    conversions: row.convertedCount ?? 0,
    openRatePercent: toPercentPoints(row.openRate),
    conversionRatePercent: toPercentPoints(row.conversionRate),
  }));
}

/**
 * Campaign inventory bars for the executive dashboard (KB item 443 / COMP-010).
 */
export function toExecutiveInventoryChartRows(
  dashboard: ExecutiveDashboardView | null | undefined,
): NamedCountChartRow[] {
  if (dashboard == null) {
    return [];
  }
  return [
    { name: "Total", value: dashboard.totalCampaigns ?? 0 },
    { name: "Active", value: dashboard.activeCampaigns ?? 0 },
    { name: "Completed", value: dashboard.completedCampaigns ?? 0 },
  ];
}

/**
 * Audience → engagement funnel for the executive dashboard (KB item 443 / COMP-010).
 */
export function toExecutiveFunnelChartRows(
  dashboard: ExecutiveDashboardView | null | undefined,
): NamedCountChartRow[] {
  if (dashboard == null) {
    return [];
  }
  return [
    { name: "Audience", value: dashboard.totalAudience ?? 0 },
    { name: "Eligible", value: dashboard.totalEligible ?? 0 },
    { name: "Sent", value: dashboard.totalSent ?? 0 },
    { name: "Opened", value: dashboard.totalOpened ?? 0 },
    { name: "Clicked", value: dashboard.totalClicked ?? 0 },
    { name: "Converted", value: dashboard.totalConverted ?? 0 },
  ];
}

/**
 * Engagement mix pie slices from platform dashboard totals (KB item 444 / FR-108).
 */
export function toDashboardEngagementMixRows(
  dashboard: DashboardView | null | undefined,
): EngagementMixSlice[] {
  if (dashboard == null) {
    return [];
  }
  return buildEngagementMix(
    dashboard.openedCount,
    dashboard.clickedCount,
    dashboard.repliedCount,
    dashboard.convertedCount,
  );
}

/**
 * Engagement mix pie slices from executive aggregates (KB item 444 / COMP-010).
 */
export function toExecutiveEngagementMixRows(
  dashboard: ExecutiveDashboardView | null | undefined,
): EngagementMixSlice[] {
  if (dashboard == null) {
    return [];
  }
  return buildEngagementMix(
    dashboard.totalOpened,
    dashboard.totalClicked,
    dashboard.totalReplied,
    dashboard.totalConverted,
  );
}

/**
 * Product cost / revenue / ROI rows for financial line charts (KB item 444 / FR-107).
 */
export function toProductFinancialChartRows(
  rows: ProductPerformanceView[] | null | undefined,
): FinancialChartRow[] {
  if (rows == null || rows.length === 0) {
    return [];
  }
  return rows.map((row) => ({
    name: shortenLabel(row.productName, "Product"),
    cost: Number(row.estimatedCost ?? 0),
    revenue: Number(row.estimatedRevenue ?? 0),
    roiPercent: toPercentPoints(row.estimatedRoi),
  }));
}

/**
 * Campaign cost / revenue / ROI rows for financial charts from recent metrics (KB item 444).
 */
export function toCampaignFinancialChartRows(
  metrics: CampaignMetricsView[] | null | undefined,
): FinancialChartRow[] {
  if (metrics == null || metrics.length === 0) {
    return [];
  }
  return metrics.map((row) => ({
    name: shortenLabel(row.campaignName, "Campaign"),
    cost: Number(row.estimatedCost ?? 0),
    revenue: Number(row.estimatedRevenue ?? 0),
    roiPercent: toPercentPoints(row.estimatedRoi),
  }));
}

function buildEngagementMix(
  opened: number,
  clicked: number,
  replied: number,
  converted: number,
): EngagementMixSlice[] {
  return [
    { name: "Opened", value: opened ?? 0 },
    { name: "Clicked", value: clicked ?? 0 },
    { name: "Replied", value: replied ?? 0 },
    { name: "Converted", value: converted ?? 0 },
  ];
}

function toPercentPoints(rate: number | null | undefined): number {
  if (rate == null || Number.isNaN(rate)) {
    return 0;
  }
  return Number((rate * 100).toFixed(2));
}

function shortenLabel(name: string | null | undefined, fallback: string): string {
  if (name == null || name.trim() === "") {
    return fallback;
  }
  const trimmed = name.trim();
  return trimmed.length > 18 ? `${trimmed.slice(0, 16)}…` : trimmed;
}
