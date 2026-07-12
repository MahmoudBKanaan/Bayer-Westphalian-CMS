import type { CampaignMetricsView, DashboardView } from "@/api/analytics";

export type PerformanceChartRow = {
  name: string;
  sent: number;
  conversions: number;
};

/**
 * Maps recent campaign metrics into Recharts rows for the Dashboard screen (KB item 440).
 */
export function toPerformanceChartRows(
  metrics: CampaignMetricsView[] | null | undefined,
): PerformanceChartRow[] {
  if (metrics == null || metrics.length === 0) {
    return [];
  }
  return metrics.map((row) => ({
    name: shortenCampaignName(row.campaignName),
    sent: row.sentCount ?? 0,
    conversions: row.convertedCount ?? 0,
  }));
}

/**
 * Eligibility rate as a percentage of audience (for secondary dashboard detail).
 */
export function eligibilityRatePercent(dashboard: DashboardView): number {
  if (dashboard.audienceSize <= 0) {
    return 0;
  }
  return (dashboard.eligibleCount / dashboard.audienceSize) * 100;
}

function shortenCampaignName(name: string | null | undefined): string {
  if (name == null || name.trim() === "") {
    return "Campaign";
  }
  const trimmed = name.trim();
  return trimmed.length > 18 ? `${trimmed.slice(0, 16)}…` : trimmed;
}
