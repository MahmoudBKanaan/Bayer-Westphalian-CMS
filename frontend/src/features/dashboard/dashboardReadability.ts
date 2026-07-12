import type { DashboardView, ExecutiveDashboardView } from "@/api/analytics";
import { eligibilityRatePercent } from "@/features/dashboard/dashboardCharts";
import { formatMoney, formatNumber, formatPercent, formatRate } from "@/utils/format";

/**
 * Dashboard readability helpers (KB item 591 / NFR-005).
 *
 * Groups KPIs into scan-friendly sections so business users can read inventory,
 * engagement, and financial outcomes without decoding dense metric walls.
 */

export type DashboardMetricTone = "inventory" | "engagement" | "financial" | "default";

export type DashboardKpiCardModel = {
  id: string;
  label: string;
  value: string;
  detail: string;
  tone: DashboardMetricTone;
};

export type DashboardKpiGroupModel = {
  id: string;
  title: string;
  description: string;
  cards: DashboardKpiCardModel[];
};

export const DASHBOARD_PAGE_LEAD =
  "Scan campaign inventory, audience reach, engagement rates, and estimated financial outcomes in one place.";

export const EXECUTIVE_DASHBOARD_PAGE_LEAD =
  "Management view of platform-wide inventory, funnel outcomes, engagement, and product performance (aggregated).";

/**
 * Builds grouped KPI cards for the operational analytics dashboard (`/dashboard`).
 */
export function buildDashboardKpiGroups(dashboard: DashboardView): DashboardKpiGroupModel[] {
  const eligibility = eligibilityRatePercent(dashboard);

  return [
    {
      id: "inventory-delivery",
      title: "Inventory & delivery",
      description: "Campaign volume, audience size, and messages already sent",
      cards: [
        {
          id: "campaigns",
          label: "Campaigns",
          value: formatNumber(dashboard.campaignTotal),
          detail: `${formatNumber(dashboard.activeCampaigns)} active now`,
          tone: "inventory",
        },
        {
          id: "audience",
          label: "Audience",
          value: formatNumber(dashboard.audienceSize),
          detail: `${formatPercent(eligibility)} eligible after exclusions`,
          tone: "inventory",
        },
        {
          id: "messages-sent",
          label: "Messages sent",
          value: formatNumber(dashboard.messagesSent),
          detail: `Opened ${formatNumber(dashboard.openedCount)} · Clicked ${formatNumber(dashboard.clickedCount)}`,
          tone: "inventory",
        },
        {
          id: "eligible-excluded",
          label: "Eligible / excluded",
          value: `${formatNumber(dashboard.eligibleCount)} / ${formatNumber(dashboard.excludedCount)}`,
          detail: `Replied ${formatNumber(dashboard.repliedCount)}`,
          tone: "inventory",
        },
      ],
    },
    {
      id: "engagement-rates",
      title: "Engagement rates",
      description: "How recipients respond after messages are sent (opened ÷ sent, etc.)",
      cards: [
        {
          id: "open-rate",
          label: "Open rate",
          value: formatRate(dashboard.openRate),
          detail: "Opened ÷ sent",
          tone: "engagement",
        },
        {
          id: "click-rate",
          label: "Click rate",
          value: formatRate(dashboard.clickRate),
          detail: "Clicked ÷ sent",
          tone: "engagement",
        },
        {
          id: "conversion-rate",
          label: "Conversion rate",
          value: formatRate(dashboard.conversionRate),
          detail: `Converted ${formatNumber(dashboard.convertedCount)}`,
          tone: "engagement",
        },
      ],
    },
    {
      id: "financial-outcomes",
      title: "Financial outcomes",
      description: "Estimated cost, revenue, and ROI from stored campaign metrics",
      cards: [
        {
          id: "estimated-roi",
          label: "Estimated ROI",
          value: formatRate(dashboard.estimatedRoi),
          detail: `Cost ${formatMoney(dashboard.estimatedCost)} · Revenue ${formatMoney(dashboard.estimatedRevenue)}`,
          tone: "financial",
        },
      ],
    },
  ];
}

/**
 * Builds grouped KPI cards for the executive aggregate dashboard (`/executive`).
 */
export function buildExecutiveDashboardKpiGroups(
  dashboard: ExecutiveDashboardView,
): DashboardKpiGroupModel[] {
  return [
    {
      id: "inventory-delivery",
      title: "Inventory & delivery",
      description: "Platform campaign stock and total messages delivered",
      cards: [
        {
          id: "campaigns",
          label: "Campaigns",
          value: formatNumber(dashboard.totalCampaigns),
          detail: `${formatNumber(dashboard.activeCampaigns)} active · ${formatNumber(dashboard.completedCampaigns)} completed`,
          tone: "inventory",
        },
        {
          id: "total-audience",
          label: "Total audience",
          value: formatNumber(dashboard.totalAudience),
          detail: `Eligible ${formatNumber(dashboard.totalEligible)} · Excluded ${formatNumber(dashboard.totalExcluded)}`,
          tone: "inventory",
        },
        {
          id: "messages-sent",
          label: "Messages sent",
          value: formatNumber(dashboard.totalSent),
          detail: `Opened ${formatNumber(dashboard.totalOpened)} · Clicked ${formatNumber(dashboard.totalClicked)}`,
          tone: "inventory",
        },
        {
          id: "products-tracked",
          label: "Products tracked",
          value: formatNumber(dashboard.productPerformance.length),
          detail: "Products linked to campaigns with metrics",
          tone: "inventory",
        },
      ],
    },
    {
      id: "engagement-rates",
      title: "Engagement rates",
      description: "Aggregate rates across the platform (not averages of campaign rates)",
      cards: [
        {
          id: "open-rate",
          label: "Open rate",
          value: formatRate(dashboard.overallOpenRate),
          detail: "Opened ÷ sent (aggregate)",
          tone: "engagement",
        },
        {
          id: "click-rate",
          label: "Click rate",
          value: formatRate(dashboard.overallClickRate),
          detail: "Clicked ÷ sent (aggregate)",
          tone: "engagement",
        },
        {
          id: "conversion-rate",
          label: "Conversion rate",
          value: formatRate(dashboard.overallConversionRate),
          detail: `Converted ${formatNumber(dashboard.totalConverted)} · Replied ${formatNumber(dashboard.totalReplied)}`,
          tone: "engagement",
        },
      ],
    },
    {
      id: "financial-outcomes",
      title: "Financial outcomes",
      description: "Estimated cost, revenue, and ROI at platform level",
      cards: [
        {
          id: "estimated-roi",
          label: "Estimated ROI",
          value: formatRate(dashboard.overallEstimatedRoi),
          detail: `Cost ${formatMoney(dashboard.totalEstimatedCost)} · Revenue ${formatMoney(dashboard.totalEstimatedRevenue)}`,
          tone: "financial",
        },
      ],
    },
  ];
}

/** Count of KPI groups used on both dashboards (for layout stability tests). */
export function dashboardKpiGroupCount(): number {
  return 3;
}

/**
 * Short empty-state guidance when a dashboard has data but a chart/table section is empty.
 */
export function dashboardEmptyGuidance(section: "chart" | "table" | "metrics"): string {
  switch (section) {
    case "chart":
      return "Charts appear after campaign metrics are recorded (for example after a launch).";
    case "table":
      return "Recent campaign rows appear once campaigns have stored metrics.";
    case "metrics":
      return "KPI cards populate when platform analytics are available.";
    default:
      return "No data is available yet.";
  }
}
