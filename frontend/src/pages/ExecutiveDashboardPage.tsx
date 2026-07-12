import { useQuery } from "@tanstack/react-query";
import {
  getExecutiveDashboard,
  type ExecutiveDashboardView,
  type ProductPerformanceView,
} from "@/api/analytics";
import { isAuthorizationError } from "@/api/client";
import {
  CHART_COLORS,
  EngagementMixPieChart,
  FinancialLineChart,
  MultiSeriesBarChart,
  NamedCountBarChart,
} from "@/components/charts";
import { MetricCard } from "@/components/MetricCard";
import {
  toExecutiveEngagementMixRows,
  toExecutiveFunnelChartRows,
  toExecutiveInventoryChartRows,
  toProductFinancialChartRows,
  toProductPerformanceChartRows,
} from "@/features/analytics/analyticsCharts";
import { usePermissions } from "@/features/auth/usePermissions";
import {
  buildExecutiveDashboardKpiGroups,
  EXECUTIVE_DASHBOARD_PAGE_LEAD,
  dashboardEmptyGuidance,
  type DashboardKpiGroupModel,
} from "@/features/dashboard/dashboardReadability";
import { formatMoney, formatNumber, formatRate } from "@/utils/format";

/**
 * Executive dashboard screen (KB item 443 / E19 / COMP-010 / item 444 / item 591 readability).
 *
 * Loads platform-level aggregated KPIs from {@code GET /api/analytics/executive} and renders
 * inventory, funnel, engagement mix, product, and financial visualizations with grouped KPIs.
 */
export function ExecutiveDashboardPage() {
  const permissions = usePermissions();
  const canView = permissions.canViewExecutiveDashboard();

  const executiveQuery = useQuery({
    queryKey: ["analytics", "executive"],
    queryFn: getExecutiveDashboard,
    enabled: canView,
  });

  if (!canView) {
    return (
      <section className="panel dashboard-page" aria-labelledby="executive-dashboard-title">
        <div className="section-heading">
          <h2 id="executive-dashboard-title">Executive dashboard</h2>
          <span>High-level management KPIs and product outcomes</span>
        </div>
        <p className="form-error" role="alert">
          You are not authorized to view the executive dashboard.
        </p>
      </section>
    );
  }

  const dashboard = executiveQuery.data;
  const isLoading = executiveQuery.isLoading;
  const errorMessage = executiveErrorMessage(executiveQuery.error);
  const products = dashboard?.productPerformance ?? [];

  return (
    <section className="page-stack dashboard-page" aria-labelledby="executive-dashboard-title">
      <header className="panel dashboard-page-header">
        <div className="section-heading">
          <h2 id="executive-dashboard-title">Executive dashboard</h2>
          <span>
            {isLoading
              ? "Loading executive aggregates"
              : "Aggregated management KPIs"}
          </span>
        </div>
        <p className="dashboard-page-lead">{EXECUTIVE_DASHBOARD_PAGE_LEAD}</p>
        {errorMessage !== "" ? (
          <p className="form-error" role="alert">
            {errorMessage}
          </p>
        ) : null}
      </header>

      {isLoading && dashboard == null ? <ExecutiveKpiLoadingState /> : null}

      {dashboard != null ? (
        <ExecutiveKpiLayout groups={buildExecutiveDashboardKpiGroups(dashboard)} />
      ) : null}

      <section className="panel dashboard-chart-panel" aria-labelledby="executive-inventory-heading">
        <div className="section-heading">
          <h2 id="executive-inventory-heading">Campaign inventory</h2>
          <span>Total, active, and completed campaigns</span>
        </div>
        <p className="dashboard-section-hint">
          Inventory counts show how much campaign activity is in flight versus finished.
        </p>
        <NamedCountBarChart
          data={toExecutiveInventoryChartRows(dashboard)}
          ariaLabel="Executive campaign inventory chart"
          isLoading={isLoading}
          loadingMessage="Loading chart…"
          emptyMessage="No campaign inventory aggregates are available yet."
          barColor={CHART_COLORS.primary}
        />
      </section>

      <section className="panel dashboard-chart-panel" aria-labelledby="executive-funnel-heading">
        <div className="section-heading">
          <h2 id="executive-funnel-heading">Audience and engagement funnel</h2>
          <span>Aggregated funnel from audience through conversion</span>
        </div>
        <p className="dashboard-section-hint">
          Read top-to-bottom volume from total audience through conversions.
        </p>
        <NamedCountBarChart
          data={toExecutiveFunnelChartRows(dashboard)}
          ariaLabel="Executive audience engagement funnel chart"
          isLoading={isLoading}
          loadingMessage="Loading chart…"
          emptyMessage="No funnel aggregates are available yet."
          barColor={CHART_COLORS.secondary}
        />
      </section>

      <section className="panel dashboard-chart-panel" aria-labelledby="executive-engagement-heading">
        <div className="section-heading">
          <h2 id="executive-engagement-heading">Engagement mix</h2>
          <span>Aggregate opened, clicked, replied, and converted share</span>
        </div>
        <p className="dashboard-section-hint">
          Mix of engagement outcomes at platform level (COMP-010 aggregates).
        </p>
        <EngagementMixPieChart
          data={toExecutiveEngagementMixRows(dashboard)}
          ariaLabel="Executive engagement mix pie chart"
          isLoading={isLoading}
          loadingMessage="Loading engagement mix chart…"
          emptyMessage="No engagement mix data is available yet."
        />
      </section>

      <section className="panel dashboard-table-panel" aria-labelledby="executive-product-heading">
        <div className="section-heading">
          <h2 id="executive-product-heading">Product performance summary</h2>
          <span>Product-level outcomes from linked campaigns</span>
        </div>
        <p className="dashboard-section-hint">
          Charts show volume and financial trends; the table supports precise comparison.
        </p>
        <div className="dashboard-nested-chart">
          <MultiSeriesBarChart
            data={toProductPerformanceChartRows(products)}
            series={[
              { dataKey: "sent", name: "Sent", color: CHART_COLORS.sent },
              { dataKey: "conversions", name: "Conversions", color: CHART_COLORS.conversions },
            ]}
            ariaLabel="Executive product sent versus conversions chart"
            isLoading={isLoading}
            loadingMessage="Loading product performance chart…"
            emptyMessage="No product performance aggregates are available yet."
          />
        </div>
        <div className="dashboard-nested-chart">
          <FinancialLineChart
            data={toProductFinancialChartRows(products)}
            ariaLabel="Executive product cost revenue and ROI line chart"
            isLoading={isLoading}
            loadingMessage="Loading product financial chart…"
            emptyMessage="No product financial chart data is available yet."
          />
        </div>
        <ProductSummaryTable rows={products} isLoading={isLoading} />
      </section>
    </section>
  );
}

function ExecutiveKpiLayout({ groups }: { groups: DashboardKpiGroupModel[] }) {
  return (
    <div className="dashboard-kpi-layout" aria-label="Executive dashboard KPI cards">
      {groups.map((group) => (
        <section
          key={group.id}
          className="panel dashboard-kpi-group"
          aria-labelledby={`executive-kpi-group-${group.id}`}
        >
          <div className="dashboard-kpi-group-header">
            <h3 id={`executive-kpi-group-${group.id}`} className="dashboard-kpi-group-title">
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

function ExecutiveKpiLoadingState() {
  return (
    <div
      className="dashboard-kpi-layout"
      aria-busy="true"
      aria-label="Loading executive dashboard KPIs"
    >
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

function ProductSummaryTable({
  rows,
  isLoading,
}: {
  rows: ProductPerformanceView[];
  isLoading: boolean;
}) {
  if (isLoading) {
    return <p className="dashboard-inline-status">Loading product performance table…</p>;
  }
  if (rows.length === 0) {
    return (
      <div className="dashboard-empty-state" role="status">
        <p>No products are linked to campaign aggregates yet.</p>
        <p className="dashboard-section-hint">{dashboardEmptyGuidance("table")}</p>
      </div>
    );
  }

  return (
    <div className="table-scroll">
      <table className="dashboard-metrics-table" aria-label="Executive product performance table">
        <caption className="sr-only">
          Product performance including campaign count, send volume, rates, cost, and revenue
        </caption>
        <thead>
          <tr>
            <th scope="col">Product</th>
            <th scope="col">Type</th>
            <th scope="col" className="numeric-col">
              Campaigns
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
            <th scope="col" className="numeric-col">
              Cost
            </th>
            <th scope="col" className="numeric-col">
              Revenue
            </th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.productId}>
              <th scope="row" className="dashboard-campaign-name">
                {row.productName ?? row.productId}
              </th>
              <td>{formatEnumLabel(row.productType)}</td>
              <td className="numeric-col">{formatNumber(row.campaignCount)}</td>
              <td className="numeric-col">{formatNumber(row.sentCount)}</td>
              <td className="numeric-col">{formatRate(row.openRate)}</td>
              <td className="numeric-col">{formatRate(row.conversionRate)}</td>
              <td className="numeric-col">{formatRate(row.estimatedRoi)}</td>
              <td className="numeric-col">{formatMoney(row.estimatedCost)}</td>
              <td className="numeric-col">{formatMoney(row.estimatedRevenue)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function formatEnumLabel(value: string | null | undefined): string {
  if (value == null || value.trim() === "") {
    return "—";
  }
  return value
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

function executiveErrorMessage(error: unknown): string {
  if (error == null) {
    return "";
  }
  if (isAuthorizationError(error)) {
    return "You are not authorized to view the executive dashboard.";
  }
  if (error instanceof Error && error.message.trim() !== "") {
    return error.message;
  }
  return "Unable to load the executive dashboard.";
}

export type { ExecutiveDashboardView };
