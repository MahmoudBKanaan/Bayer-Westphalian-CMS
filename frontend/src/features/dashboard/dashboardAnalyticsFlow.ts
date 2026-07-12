/**
 * Dashboard analytics load UI flow (KB FR-100–FR-108 / item 440 / item 606).
 *
 * Acceptance contract for loading platform analytics on `/dashboard` via
 * {@code GET /api/analytics/dashboard}. KPI grouping lives in {@link dashboardReadability}.
 */

import type { CampaignMetricsView, DashboardView } from "@/api/analytics";
import type { SystemRoleName } from "@/auth/sessionStorageStrategy";
import {
  buildDashboardKpiGroups,
  DASHBOARD_PAGE_LEAD,
  dashboardEmptyGuidance,
} from "@/features/dashboard/dashboardReadability";

/** Roles that may view dashboard analytics (matches ANALYTICS_READ_ROLES). */
export const DASHBOARD_ANALYTICS_UI_ROLES: SystemRoleName[] = [
  "ADMIN",
  "BI_ANALYST",
  "CAMPAIGN_MANAGER",
  "MARKETING_ANALYST",
  "EXECUTIVE_VIEWER",
];

export const DASHBOARD_PAGE_TITLE = "Dashboard";
export const DASHBOARD_PAGE_SUBTITLE_IDLE = "Platform analytics at a glance";
export const DASHBOARD_PAGE_SUBTITLE_LOADING = "Loading campaign KPIs";
export const DASHBOARD_PERFORMANCE_HEADING = "Campaign performance";
export const DASHBOARD_ENGAGEMENT_HEADING = "Engagement mix";
export const DASHBOARD_FINANCIAL_HEADING = "Campaign financials";
export const DASHBOARD_RECENT_METRICS_HEADING = "Recent campaign metrics";
export const DASHBOARD_KPI_CARDS_ARIA_LABEL = "Dashboard KPI cards";
export const DASHBOARD_RECENT_TABLE_ARIA_LABEL = "Recent campaign metrics table";
export const DASHBOARD_UNAUTHORIZED_MESSAGE =
  "You are not authorized to view dashboard analytics.";
export const DASHBOARD_LOAD_FAILED_MESSAGE = "Dashboard analytics could not be loaded.";

export { DASHBOARD_PAGE_LEAD, buildDashboardKpiGroups, dashboardEmptyGuidance };

export type DashboardAnalyticsStepId =
  | "open-dashboard"
  | "request-analytics"
  | "render-kpis"
  | "render-charts-and-table";

export type DashboardAnalyticsStepDefinition = {
  id: DashboardAnalyticsStepId;
  index: number;
  title: string;
  description: string;
};

/** UI acceptance steps for “Dashboard loads analytics” (item 606). */
export const DASHBOARD_ANALYTICS_FLOW_STEPS: DashboardAnalyticsStepDefinition[] = [
  {
    id: "open-dashboard",
    index: 0,
    title: "Open Dashboard",
    description: "Authorized analytics role opens /dashboard after login.",
  },
  {
    id: "request-analytics",
    index: 1,
    title: "Request analytics",
    description: "Client calls GET /api/analytics/dashboard with the session bearer token.",
  },
  {
    id: "render-kpis",
    index: 2,
    title: "Render KPI groups",
    description: "Inventory, engagement rates, and financial KPI cards populate from the payload.",
  },
  {
    id: "render-charts-and-table",
    index: 3,
    title: "Render charts and metrics table",
    description: "Performance, engagement mix, financial charts, and recent campaign metrics load.",
  },
];

/** Deterministic fixtures for Playwright / integration dashboard analytics. */
export const DASHBOARD_ANALYTICS_FIXTURES = {
  campaignTotal: 4,
  activeCampaigns: 2,
  audienceSize: 120,
  messagesSent: 95,
  eligibleCount: 80,
  excludedCount: 40,
  openedCount: 40,
  clickedCount: 18,
  repliedCount: 6,
  convertedCount: 5,
  openRate: 0.42,
  clickRate: 0.19,
  conversionRate: 0.05,
  estimatedCost: 250,
  estimatedRevenue: 900,
  estimatedRoi: 2.6,
  recentCampaignName: "UI Analytics Spring Drive",
} as const;

export function canViewDashboardAnalytics(roles: readonly SystemRoleName[]): boolean {
  return roles.some((role) => DASHBOARD_ANALYTICS_UI_ROLES.includes(role));
}

export function createDashboardAnalyticsFixture(
  overrides: Partial<DashboardView> = {},
): DashboardView {
  const recent: CampaignMetricsView = {
    metricsId: "55000000-0000-0000-0000-00000000d606",
    campaignId: "50000000-0000-0000-0000-00000000d606",
    campaignName: DASHBOARD_ANALYTICS_FIXTURES.recentCampaignName,
    campaignStatus: "ACTIVE",
    audienceSize: 40,
    eligibleCount: 28,
    excludedCount: 12,
    sentCount: 28,
    openedCount: 12,
    clickedCount: 5,
    repliedCount: 2,
    convertedCount: 1,
    openRate: 0.43,
    clickRate: 0.18,
    conversionRate: 0.04,
    estimatedCost: 80,
    estimatedRevenue: 200,
    estimatedRoi: 1.5,
    updatedAt: "2026-07-12T12:00:00Z",
  };

  return {
    campaignTotal: DASHBOARD_ANALYTICS_FIXTURES.campaignTotal,
    activeCampaigns: DASHBOARD_ANALYTICS_FIXTURES.activeCampaigns,
    audienceSize: DASHBOARD_ANALYTICS_FIXTURES.audienceSize,
    messagesSent: DASHBOARD_ANALYTICS_FIXTURES.messagesSent,
    eligibleCount: DASHBOARD_ANALYTICS_FIXTURES.eligibleCount,
    excludedCount: DASHBOARD_ANALYTICS_FIXTURES.excludedCount,
    openedCount: DASHBOARD_ANALYTICS_FIXTURES.openedCount,
    clickedCount: DASHBOARD_ANALYTICS_FIXTURES.clickedCount,
    repliedCount: DASHBOARD_ANALYTICS_FIXTURES.repliedCount,
    convertedCount: DASHBOARD_ANALYTICS_FIXTURES.convertedCount,
    openRate: DASHBOARD_ANALYTICS_FIXTURES.openRate,
    clickRate: DASHBOARD_ANALYTICS_FIXTURES.clickRate,
    conversionRate: DASHBOARD_ANALYTICS_FIXTURES.conversionRate,
    estimatedCost: DASHBOARD_ANALYTICS_FIXTURES.estimatedCost,
    estimatedRevenue: DASHBOARD_ANALYTICS_FIXTURES.estimatedRevenue,
    estimatedRoi: DASHBOARD_ANALYTICS_FIXTURES.estimatedRoi,
    recentCampaignMetrics: [recent],
    ...overrides,
  };
}

/**
 * True when the dashboard payload has core inventory counters (successful analytics load).
 */
export function hasLoadedDashboardAnalytics(
  dashboard: DashboardView | null | undefined,
): boolean {
  return (
    dashboard != null &&
    typeof dashboard.campaignTotal === "number" &&
    typeof dashboard.messagesSent === "number" &&
    typeof dashboard.openRate === "number"
  );
}

export function dashboardAnalyticsStepIdsInOrder(): DashboardAnalyticsStepId[] {
  return [...DASHBOARD_ANALYTICS_FLOW_STEPS]
    .sort((left, right) => left.index - right.index)
    .map((step) => step.id);
}

export function formatDashboardAnalyticsJourney(
  steps: readonly DashboardAnalyticsStepDefinition[] = DASHBOARD_ANALYTICS_FLOW_STEPS,
): string {
  return steps
    .slice()
    .sort((left, right) => left.index - right.index)
    .map((step) => step.title)
    .join(" → ");
}

export function isValidDashboardAnalyticsOrder(
  observed: readonly DashboardAnalyticsStepId[],
): boolean {
  const expected = dashboardAnalyticsStepIdsInOrder();
  if (observed.length !== expected.length) {
    return false;
  }
  return observed.every((stepId, index) => stepId === expected[index]);
}

export function dashboardAnalyticsEndpointPath(): string {
  return "/analytics/dashboard";
}
