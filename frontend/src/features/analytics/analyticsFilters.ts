import type { CampaignMetricsView, DashboardView, ProductPerformanceView } from "@/api/analytics";
import type { CampaignView } from "@/api/campaigns";

/**
 * Analytics screen filters (KB E19 / item 441 / FR-104–FR-108).
 *
 * Users can narrow engagement, conversion, ROI, and product comparisons by campaign,
 * product, and time frame. Filtering is applied client-side on loaded analytics payloads
 * when the API does not accept filter query parameters.
 */

export type AnalyticsTimeframe =
  | "ALL"
  | "LAST_7_DAYS"
  | "LAST_30_DAYS"
  | "LAST_90_DAYS"
  | "THIS_YEAR"
  | "CUSTOM";

export type AnalyticsFilterState = {
  campaignId: string;
  productId: string;
  timeframe: AnalyticsTimeframe;
  dateFrom: string;
  dateTo: string;
};

export const emptyAnalyticsFilters: AnalyticsFilterState = {
  campaignId: "",
  productId: "",
  timeframe: "ALL",
  dateFrom: "",
  dateTo: "",
};

export const ANALYTICS_TIMEFRAME_OPTIONS: { value: AnalyticsTimeframe; label: string }[] = [
  { value: "ALL", label: "All time" },
  { value: "LAST_7_DAYS", label: "Last 7 days" },
  { value: "LAST_30_DAYS", label: "Last 30 days" },
  { value: "LAST_90_DAYS", label: "Last 90 days" },
  { value: "THIS_YEAR", label: "This year" },
  { value: "CUSTOM", label: "Custom range" },
];

export type DateBounds = { from: string | null; to: string | null };

/** Resolves inclusive ISO date bounds for a timeframe selection (local calendar days). */
export function resolveTimeframeBounds(
  filters: Pick<AnalyticsFilterState, "timeframe" | "dateFrom" | "dateTo">,
  today: Date = new Date(),
): DateBounds {
  if (filters.timeframe === "ALL") {
    return { from: null, to: null };
  }
  if (filters.timeframe === "CUSTOM") {
    const from = filters.dateFrom.trim() || null;
    const to = filters.dateTo.trim() || null;
    return { from, to };
  }

  const end = startOfLocalDay(today);
  let start = new Date(end);
  if (filters.timeframe === "LAST_7_DAYS") {
    start.setDate(start.getDate() - 6);
  } else if (filters.timeframe === "LAST_30_DAYS") {
    start.setDate(start.getDate() - 29);
  } else if (filters.timeframe === "LAST_90_DAYS") {
    start.setDate(start.getDate() - 89);
  } else if (filters.timeframe === "THIS_YEAR") {
    start = new Date(end.getFullYear(), 0, 1);
  }
  return { from: toIsoDate(start), to: toIsoDate(end) };
}

/**
 * True when a campaign schedule or metrics update date overlaps the selected timeframe.
 */
export function matchesTimeframe(
  bounds: DateBounds,
  options: {
    startDate?: string | null;
    endDate?: string | null;
    updatedAt?: string | null;
  },
): boolean {
  if (bounds.from == null && bounds.to == null) {
    return true;
  }
  const rangeStart = options.startDate?.trim() || isoDateFromInstant(options.updatedAt);
  const rangeEnd =
    options.endDate?.trim() || isoDateFromInstant(options.updatedAt) || rangeStart;
  if (rangeStart == null && rangeEnd == null) {
    // No date context — keep the row when filtering by time so data is not silently dropped.
    return true;
  }
  const start = rangeStart ?? rangeEnd!;
  const end = rangeEnd ?? rangeStart!;
  if (bounds.from != null && end < bounds.from) {
    return false;
  }
  if (bounds.to != null && start > bounds.to) {
    return false;
  }
  return true;
}

export function filterCampaignMetrics(
  metrics: CampaignMetricsView[] | null | undefined,
  filters: AnalyticsFilterState,
  campaigns: CampaignView[] | null | undefined,
  today: Date = new Date(),
): CampaignMetricsView[] {
  const rows = metrics ?? [];
  const bounds = resolveTimeframeBounds(filters, today);
  const campaignById = new Map((campaigns ?? []).map((c) => [c.id, c]));

  return rows.filter((row) => {
    if (filters.campaignId !== "" && row.campaignId !== filters.campaignId) {
      return false;
    }
    const campaign = campaignById.get(row.campaignId);
    if (filters.productId !== "") {
      const productIds = campaign?.productIds ?? [];
      if (productIds.length > 0 && !productIds.includes(filters.productId)) {
        return false;
      }
      // When campaign list lacks product links, do not hide metrics solely by product.
      if (productIds.length === 0 && campaigns != null && campaigns.length > 0 && campaign == null) {
        return false;
      }
    }
    return matchesTimeframe(bounds, {
      startDate: campaign?.startDate,
      endDate: campaign?.endDate,
      updatedAt: row.updatedAt,
    });
  });
}

export function filterProductPerformance(
  products: ProductPerformanceView[] | null | undefined,
  filters: AnalyticsFilterState,
  campaigns: CampaignView[] | null | undefined,
  filteredMetrics: CampaignMetricsView[],
): ProductPerformanceView[] {
  const rows = products ?? [];
  if (filters.productId !== "") {
    return rows.filter((row) => row.productId === filters.productId);
  }
  if (filters.campaignId !== "") {
    const campaign = (campaigns ?? []).find((c) => c.id === filters.campaignId);
    const productIds = campaign?.productIds ?? [];
    if (productIds.length > 0) {
      return rows.filter((row) => productIds.includes(row.productId));
    }
    // No product links on campaign — keep all products when only campaign filter is set.
  }
  // When time/campaign filters reduce metrics, prefer products still represented.
  if (
    (filters.timeframe !== "ALL" || filters.campaignId !== "") &&
    filteredMetrics.length > 0 &&
    (campaigns ?? []).some((c) => (c.productIds?.length ?? 0) > 0)
  ) {
    const allowed = new Set<string>();
    for (const metric of filteredMetrics) {
      const campaign = (campaigns ?? []).find((c) => c.id === metric.campaignId);
      for (const productId of campaign?.productIds ?? []) {
        allowed.add(productId);
      }
    }
    if (allowed.size > 0) {
      return rows.filter((row) => allowed.has(row.productId));
    }
  }
  return rows;
}

/**
 * Builds KPI-style dashboard numbers from filtered campaign metrics so the Analytics
 * overview respects campaign / product / time filters (FR-104–FR-107).
 */
export function dashboardFromMetrics(
  base: DashboardView | undefined,
  metrics: CampaignMetricsView[],
  filtersActive: boolean,
): DashboardView | undefined {
  if (base == null) {
    return undefined;
  }
  if (!filtersActive || metrics.length === 0) {
    if (!filtersActive) {
      return { ...base, recentCampaignMetrics: metrics.length > 0 ? metrics : base.recentCampaignMetrics };
    }
    // Filters active but no matching campaigns — zero engagement slice while keeping inventory when possible.
    return {
      ...base,
      messagesSent: 0,
      openedCount: 0,
      clickedCount: 0,
      repliedCount: 0,
      convertedCount: 0,
      openRate: 0,
      clickRate: 0,
      conversionRate: 0,
      estimatedCost: 0,
      estimatedRevenue: 0,
      estimatedRoi: 0,
      audienceSize: 0,
      eligibleCount: 0,
      excludedCount: 0,
      recentCampaignMetrics: [],
    };
  }

  let sent = 0;
  let opened = 0;
  let clicked = 0;
  let replied = 0;
  let converted = 0;
  let audience = 0;
  let eligible = 0;
  let excluded = 0;
  let cost = 0;
  let revenue = 0;
  let hasCost = false;
  let hasRevenue = false;

  for (const row of metrics) {
    sent += row.sentCount ?? 0;
    opened += row.openedCount ?? 0;
    clicked += row.clickedCount ?? 0;
    replied += row.repliedCount ?? 0;
    converted += row.convertedCount ?? 0;
    audience += row.audienceSize ?? 0;
    eligible += row.eligibleCount ?? 0;
    excluded += row.excludedCount ?? 0;
    if (row.estimatedCost != null) {
      cost += row.estimatedCost;
      hasCost = true;
    }
    if (row.estimatedRevenue != null) {
      revenue += row.estimatedRevenue;
      hasRevenue = true;
    }
  }

  const openRate = sent > 0 ? opened / sent : 0;
  const clickRate = sent > 0 ? clicked / sent : 0;
  const conversionRate = sent > 0 ? converted / sent : 0;
  const estimatedRoi = cost > 0 ? (revenue - cost) / cost : revenue > 0 ? 1 : 0;

  return {
    ...base,
    messagesSent: sent,
    openedCount: opened,
    clickedCount: clicked,
    repliedCount: replied,
    convertedCount: converted,
    openRate,
    clickRate,
    conversionRate,
    estimatedCost: hasCost ? cost : null,
    estimatedRevenue: hasRevenue ? revenue : null,
    estimatedRoi: hasCost || hasRevenue ? estimatedRoi : null,
    audienceSize: audience,
    eligibleCount: eligible,
    excludedCount: excluded,
    recentCampaignMetrics: metrics,
  };
}

export function areAnalyticsFiltersActive(filters: AnalyticsFilterState): boolean {
  if (filters.campaignId !== "" || filters.productId !== "") {
    return true;
  }
  if (filters.timeframe === "ALL") {
    return false;
  }
  if (filters.timeframe === "CUSTOM") {
    return filters.dateFrom.trim() !== "" || filters.dateTo.trim() !== "";
  }
  return true;
}

function startOfLocalDay(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

function toIsoDate(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

function isoDateFromInstant(value: string | null | undefined): string | null {
  if (value == null || value.trim() === "") {
    return null;
  }
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value.slice(0, 10);
  }
  return toIsoDate(parsed);
}
