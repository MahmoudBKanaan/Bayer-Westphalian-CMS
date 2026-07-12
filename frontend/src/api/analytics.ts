import { apiRequest } from "@/api/client";
import type { CampaignChannel, CampaignStatus } from "@/api/campaigns";
import type { ProductType } from "@/api/products";

/**
 * Analytics API client (KB epic E19 / FR-100–FR-108 / items 440–443).
 *
 * <ul>
 *   <li>{@code GET /api/analytics/dashboard} — platform KPIs</li>
 *   <li>{@code GET /api/analytics/campaigns/{campaignId}} — campaign analytics detail</li>
 *   <li>{@code GET /api/analytics/products/performance} — product performance rows</li>
 *   <li>{@code GET /api/analytics/executive} — executive aggregate dashboard (COMP-010)</li>
 * </ul>
 */

export type CampaignMetricsView = {
  metricsId: string | null;
  campaignId: string;
  campaignName: string | null;
  campaignStatus: CampaignStatus | null;
  audienceSize: number;
  eligibleCount: number;
  excludedCount: number;
  sentCount: number;
  openedCount: number;
  clickedCount: number;
  repliedCount: number;
  convertedCount: number;
  openRate: number;
  clickRate: number;
  conversionRate: number;
  estimatedCost: number | null;
  estimatedRevenue: number | null;
  estimatedRoi: number | null;
  updatedAt: string | null;
};

/** Platform dashboard payload (KB item 431 / FR-100–FR-107). */
export type DashboardView = {
  campaignTotal: number;
  activeCampaigns: number;
  audienceSize: number;
  messagesSent: number;
  eligibleCount: number;
  excludedCount: number;
  openedCount: number;
  clickedCount: number;
  repliedCount: number;
  convertedCount: number;
  openRate: number;
  clickRate: number;
  conversionRate: number;
  estimatedCost: number | null;
  estimatedRevenue: number | null;
  estimatedRoi: number | null;
  recentCampaignMetrics: CampaignMetricsView[];
};

/** Campaign analytics detail (KB item 432 / Analytics screen drill-down). */
export type CampaignAnalyticsView = {
  campaignId: string;
  campaignName: string | null;
  objective: string | null;
  status: CampaignStatus | null;
  channel: CampaignChannel | null;
  startDate: string | null;
  endDate: string | null;
  ownerUserId: string | null;
  ownerFullName: string | null;
  metrics: CampaignMetricsView | null;
  generatedAt: string | null;
};

/** Product performance row (KB item 433 / product comparisons). */
export type ProductPerformanceView = {
  productId: string;
  productName: string | null;
  productType: ProductType | null;
  campaignCount: number;
  audienceSize: number;
  eligibleCount: number;
  sentCount: number;
  openedCount: number;
  clickedCount: number;
  convertedCount: number;
  openRate: number;
  clickRate: number;
  conversionRate: number;
  estimatedCost: number | null;
  estimatedRevenue: number | null;
  estimatedRoi: number | null;
};

/**
 * Executive aggregate dashboard (KB item 434 / 443 / COMP-010).
 *
 * Platform-level aggregates for management reporting — rates and ROI are derived from
 * summed counters, not averages of per-campaign rates.
 */
export type ExecutiveDashboardView = {
  totalCampaigns: number;
  activeCampaigns: number;
  completedCampaigns: number;
  totalAudience: number;
  totalEligible: number;
  totalExcluded: number;
  totalSent: number;
  totalOpened: number;
  totalClicked: number;
  totalReplied: number;
  totalConverted: number;
  overallOpenRate: number;
  overallClickRate: number;
  overallConversionRate: number;
  totalEstimatedCost: number | null;
  totalEstimatedRevenue: number | null;
  overallEstimatedRoi: number | null;
  productPerformance: ProductPerformanceView[];
};

type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
};

export const emptyDashboardView: DashboardView = {
  campaignTotal: 0,
  activeCampaigns: 0,
  audienceSize: 0,
  messagesSent: 0,
  eligibleCount: 0,
  excludedCount: 0,
  openedCount: 0,
  clickedCount: 0,
  repliedCount: 0,
  convertedCount: 0,
  openRate: 0,
  clickRate: 0,
  conversionRate: 0,
  estimatedCost: null,
  estimatedRevenue: null,
  estimatedRoi: null,
  recentCampaignMetrics: [],
};

export const emptyExecutiveDashboardView: ExecutiveDashboardView = {
  totalCampaigns: 0,
  activeCampaigns: 0,
  completedCampaigns: 0,
  totalAudience: 0,
  totalEligible: 0,
  totalExcluded: 0,
  totalSent: 0,
  totalOpened: 0,
  totalClicked: 0,
  totalReplied: 0,
  totalConverted: 0,
  overallOpenRate: 0,
  overallClickRate: 0,
  overallConversionRate: 0,
  totalEstimatedCost: null,
  totalEstimatedRevenue: null,
  overallEstimatedRoi: null,
  productPerformance: [],
};

/** Loads platform dashboard KPIs (KB item 431 / FR-100–FR-107 / item 440 Dashboard screen). */
export async function getDashboard(): Promise<DashboardView> {
  const response = await apiRequest<ApiResponse<DashboardView>>("/analytics/dashboard");
  return response.data ?? emptyDashboardView;
}

/**
 * Loads single-campaign analytics detail (KB item 432 / item 441 Analytics screen).
 *
 * @param campaignId campaign UUID
 */
export async function getCampaignAnalytics(campaignId: string): Promise<CampaignAnalyticsView> {
  const response = await apiRequest<ApiResponse<CampaignAnalyticsView>>(
    `/analytics/campaigns/${encodeURIComponent(campaignId)}`,
  );
  if (response.data == null) {
    throw new Error("Campaign analytics payload was empty");
  }
  return response.data;
}

/**
 * Loads product performance aggregates (KB item 433 / item 441 Analytics screen).
 */
export async function getProductPerformance(): Promise<ProductPerformanceView[]> {
  const response = await apiRequest<ApiResponse<ProductPerformanceView[]>>(
    "/analytics/products/performance",
  );
  return response.data ?? [];
}

/**
 * Loads executive aggregate dashboard KPIs (KB item 434 / 443 / COMP-010).
 */
export async function getExecutiveDashboard(): Promise<ExecutiveDashboardView> {
  const response = await apiRequest<ApiResponse<ExecutiveDashboardView>>(
    "/analytics/executive",
  );
  return response.data ?? emptyExecutiveDashboardView;
}
