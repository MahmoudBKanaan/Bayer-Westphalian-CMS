import { useQuery } from "@tanstack/react-query";
import { getDashboard, type CampaignMetricsView, type DashboardView } from "@/api/analytics";
import { isAuthorizationError } from "@/api/client";
import {
  AiRecommendationSections,
  buildAiRecommendationSections,
} from "@/components/AiRecommendationSections";
import { CampaignStatusBadge } from "@/components/CampaignStatusBadge";
import {
  CHART_COLORS,
  EngagementMixPieChart,
  FinancialLineChart,
  MultiSeriesBarChart,
} from "@/components/charts";
import { MetricCard } from "@/components/MetricCard";
import {
  toCampaignFinancialChartRows,
  toDashboardEngagementMixRows,
} from "@/features/analytics/analyticsCharts";
import { usePermissions } from "@/features/auth/usePermissions";
import { toPerformanceChartRows } from "@/features/dashboard/dashboardCharts";
import {
  buildDashboardKpiGroups,
  DASHBOARD_ENGAGEMENT_HEADING,
  DASHBOARD_FINANCIAL_HEADING,
  DASHBOARD_KPI_CARDS_ARIA_LABEL,
  DASHBOARD_LOAD_FAILED_MESSAGE,
  DASHBOARD_PAGE_LEAD,
  DASHBOARD_PAGE_SUBTITLE_IDLE,
  DASHBOARD_PAGE_SUBTITLE_LOADING,
  DASHBOARD_PAGE_TITLE,
  DASHBOARD_PERFORMANCE_HEADING,
  DASHBOARD_RECENT_METRICS_HEADING,
  DASHBOARD_RECENT_TABLE_ARIA_LABEL,
  DASHBOARD_UNAUTHORIZED_MESSAGE,
  dashboardEmptyGuidance,
} from "@/features/dashboard/dashboardAnalyticsFlow";
import type { DashboardKpiGroupModel } from "@/features/dashboard/dashboardReadability";
import { formatNumber, formatRate } from "@/utils/format";

/**
 * Dashboard screen (KB item 440 / FR-100–FR-108 / item 444 Recharts / item 591 readability /
 * item 606 dashboard loads analytics).
 *
 * Loads platform KPIs from {@code GET /api/analytics/dashboard} and renders scan-friendly
 * KPI groups, performance charts, and a recent metrics table.
 */
export function DashboardPage() {
  const permissions = usePermissions();
  const canViewAnalytics = permissions.canViewAnalytics();

  const dashboardQuery = useQuery({
    queryKey: ["analytics", "dashboard"],
    queryFn: getDashboard,
    enabled: canViewAnalytics,
  });

  if (!canViewAnalytics) {
    return (
      <section className="panel dashboard-page" aria-labelledby="dashboard-title">
        <div className="section-heading">
          <h2 id="dashboard-title">{DASHBOARD_PAGE_TITLE}</h2>
          <span>Campaign performance KPIs</span>
        </div>
        <p className="form-error" role="alert">
          {DASHBOARD_UNAUTHORIZED_MESSAGE}
        </p>
      </section>
    );
  }

  const dashboard = dashboardQuery.data;
  const isLoading = dashboardQuery.isLoading;
  const errorMessage = dashboardErrorMessage(dashboardQuery.error);
  const performanceRows = toPerformanceChartRows(dashboard?.recentCampaignMetrics);
  const engagementMix = toDashboardEngagementMixRows(dashboard);
  const financialRows = toCampaignFinancialChartRows(dashboard?.recentCampaignMetrics);
  const aiRecommendationSections = buildAiRecommendationSections(permissions);

  return (
    <section
      className="page-stack dashboard-page"
      aria-labelledby="dashboard-title"
      data-testid="dashboard-analytics-page"
    >
      <header className="panel dashboard-page-header">
        <div className="section-heading">
          <h2 id="dashboard-title">{DASHBOARD_PAGE_TITLE}</h2>
          <span>
            {isLoading ? DASHBOARD_PAGE_SUBTITLE_LOADING : DASHBOARD_PAGE_SUBTITLE_IDLE}
          </span>
        </div>
        <p className="dashboard-page-lead">{DASHBOARD_PAGE_LEAD}</p>
        {errorMessage !== "" ? (
          <p className="form-error" role="alert" data-testid="dashboard-analytics-error">
            {errorMessage}
          </p>
        ) : null}
      </header>

      {isLoading && dashboard == null ? <DashboardKpiLoadingState /> : null}

      {dashboard != null ? (
        <DashboardKpiLayout groups={buildDashboardKpiGroups(dashboard)} />
      ) : null}

      <AiRecommendationSections sections={aiRecommendationSections} />

      <section className="panel dashboard-chart-panel" aria-labelledby="dashboard-performance-heading">
        <div className="section-heading">
          <h2 id="dashboard-performance-heading">{DASHBOARD_PERFORMANCE_HEADING}</h2>
          <span>Sent messages vs conversions (recent campaigns)</span>
        </div>
        <p className="dashboard-section-hint">
          Compare delivery volume with conversions for the most recent campaigns.
        </p>
        <MultiSeriesBarChart
          data={performanceRows}
          series={[
            { dataKey: "sent", name: "Sent", color: CHART_COLORS.sent },
            { dataKey: "conversions", name: "Conversions", color: CHART_COLORS.conversions },
          ]}
          ariaLabel="Sent messages versus conversions chart"
          isLoading={isLoading}
          loadingMessage="Loading performance chart…"
          emptyMessage="No recent campaign metrics are available for the chart yet."
        />
      </section>

      <section className="panel dashboard-chart-panel" aria-labelledby="dashboard-engagement-heading">
        <div className="section-heading">
          <h2 id="dashboard-engagement-heading">{DASHBOARD_ENGAGEMENT_HEADING}</h2>
          <span>Opened, clicked, replied, and converted totals</span>
        </div>
        <p className="dashboard-section-hint">
          Share of engagement outcomes across the platform totals.
        </p>
        <EngagementMixPieChart
          data={engagementMix}
          ariaLabel="Dashboard engagement mix pie chart"
          isLoading={isLoading}
          loadingMessage="Loading engagement mix chart…"
          emptyMessage="No engagement mix data is available yet."
        />
      </section>

      <section className="panel dashboard-chart-panel" aria-labelledby="dashboard-financial-heading">
        <div className="section-heading">
          <h2 id="dashboard-financial-heading">{DASHBOARD_FINANCIAL_HEADING}</h2>
          <span>Estimated cost, revenue, and ROI for recent campaigns</span>
        </div>
        <p className="dashboard-section-hint">
          Financial estimates help prioritize which campaigns are worth repeating.
        </p>
        <FinancialLineChart
          data={financialRows}
          ariaLabel="Dashboard campaign financial line chart"
          isLoading={isLoading}
          loadingMessage="Loading financial chart…"
          emptyMessage="No campaign financial chart data is available yet."
        />
      </section>

      <section className="panel dashboard-table-panel" aria-labelledby="dashboard-recent-heading">
        <div className="section-heading">
          <h2 id="dashboard-recent-heading">{DASHBOARD_RECENT_METRICS_HEADING}</h2>
          <span>Audience, eligibility, and engagement from stored campaign metrics</span>
        </div>
        <p className="dashboard-section-hint">
          Numeric columns are right-aligned for quicker comparison across campaigns.
        </p>
        <RecentCampaignMetricsTable
          metrics={dashboard?.recentCampaignMetrics ?? []}
          isLoading={isLoading}
        />
      </section>
    </section>
  );
}

function DashboardKpiLayout({ groups }: { groups: DashboardKpiGroupModel[] }) {
  return (
    <div className="dashboard-kpi-layout" aria-label={DASHBOARD_KPI_CARDS_ARIA_LABEL}>
      {groups.map((group) => (
        <section
          key={group.id}
          className="panel dashboard-kpi-group"
          aria-labelledby={`dashboard-kpi-group-${group.id}`}
        >
          <div className="dashboard-kpi-group-header">
            <h3 id={`dashboard-kpi-group-${group.id}`} className="dashboard-kpi-group-title">
              {group.title}
            </h3>
            <p className="dashboard-kpi-group-description">{group.description}</p>
          </div>
          <div className={`metric-grid metric-grid--${group.cards.length}`}>
            {group.cards.map((card) => (
              <MetricCard
                key={card.id}
                label={card.label}
                value={card.value}
                detail={card.detail}
                tone={card.tone}
              />
            ))}
          </div>
        </section>
      ))}
    </div>
  );
}

function DashboardKpiLoadingState() {
  return (
    <div className="dashboard-kpi-layout" aria-busy="true" aria-label="Loading dashboard KPIs">
      <section className="panel dashboard-kpi-group">
        <p className="dashboard-kpi-loading">{dashboardEmptyGuidance("metrics")}</p>
        <div className="metric-grid metric-grid--4">
          {Array.from({ length: 4 }, (_, index) => (
            <div key={index} className="metric-card metric-card--skeleton" aria-hidden="true">
              <span className="metric-card-skeleton-line metric-card-skeleton-line--label" />
              <span className="metric-card-skeleton-line metric-card-skeleton-line--value" />
              <span className="metric-card-skeleton-line metric-card-skeleton-line--detail" />
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}

function RecentCampaignMetricsTable({
  metrics,
  isLoading,
}: {
  metrics: CampaignMetricsView[];
  isLoading: boolean;
}) {
  if (isLoading) {
    return <p className="dashboard-inline-status">Loading recent campaign metrics…</p>;
  }
  if (metrics.length === 0) {
    return (
      <div className="dashboard-empty-state" role="status">
        <p>No campaign metrics have been recorded yet.</p>
        <p className="dashboard-section-hint">{dashboardEmptyGuidance("table")}</p>
      </div>
    );
  }

  return (
    <div className="table-scroll">
      <table className="dashboard-metrics-table" aria-label={DASHBOARD_RECENT_TABLE_ARIA_LABEL}>
        <caption className="sr-only">
          Recent campaign metrics including audience, eligibility, send volume, rates, and ROI
        </caption>
        <thead>
          <tr>
            <th scope="col">Campaign</th>
            <th scope="col">Status</th>
            <th scope="col" className="numeric-col">
              Audience
            </th>
            <th scope="col" className="numeric-col">
              Eligible
            </th>
            <th scope="col" className="numeric-col">
              Excluded
            </th>
            <th scope="col" className="numeric-col">
              Sent
            </th>
            <th scope="col" className="numeric-col">
              Open rate
            </th>
            <th scope="col" className="numeric-col">
              Conversion rate
            </th>
            <th scope="col" className="numeric-col">
              ROI
            </th>
          </tr>
        </thead>
        <tbody>
          {metrics.map((row) => (
            <tr key={row.metricsId ?? row.campaignId}>
              <th scope="row" className="dashboard-campaign-name">
                {row.campaignName ?? row.campaignId}
              </th>
              <td>
                {row.campaignStatus != null ? (
                  <CampaignStatusBadge status={row.campaignStatus} />
                ) : (
                  "—"
                )}
              </td>
              <td className="numeric-col">{formatNumber(row.audienceSize)}</td>
              <td className="numeric-col">{formatNumber(row.eligibleCount)}</td>
              <td className="numeric-col">{formatNumber(row.excludedCount)}</td>
              <td className="numeric-col">{formatNumber(row.sentCount)}</td>
              <td className="numeric-col">{formatRate(row.openRate)}</td>
              <td className="numeric-col">{formatRate(row.conversionRate)}</td>
              <td className="numeric-col">{formatRate(row.estimatedRoi)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function dashboardErrorMessage(error: unknown): string {
  if (error == null) {
    return "";
  }
  if (isAuthorizationError(error)) {
    return DASHBOARD_UNAUTHORIZED_MESSAGE;
  }
  if (error instanceof Error && error.message.trim() !== "") {
    return error.message;
  }
  return DASHBOARD_LOAD_FAILED_MESSAGE;
}

// Keep type import used for documentation of payload shape in this module.
export type { DashboardView };
