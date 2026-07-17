import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  getCampaignAnalytics,
  getDashboard,
  getProductPerformance,
  type CampaignAnalyticsView,
  type CampaignMetricsView,
  type DashboardView,
  type ProductPerformanceView,
} from "@/api/analytics";
import { isAuthorizationError } from "@/api/client";
import { listCampaigns, type CampaignView } from "@/api/campaigns";
import { CampaignStatusBadge } from "@/components/CampaignStatusBadge";
import {
  CHART_COLORS,
  EngagementMixPieChart,
  FinancialLineChart,
  MultiSeriesBarChart,
} from "@/components/charts";
import { MetricCard } from "@/components/MetricCard";
import {
  toDashboardEngagementMixRows,
  toProductFinancialChartRows,
  toProductPerformanceChartRows,
  toRateComparisonChartRows,
} from "@/features/analytics/analyticsCharts";
import {
  ANALYTICS_TIMEFRAME_OPTIONS,
  areAnalyticsFiltersActive,
  dashboardFromMetrics,
  emptyAnalyticsFilters,
  filterCampaignMetrics,
  filterProductPerformance,
  type AnalyticsFilterState,
} from "@/features/analytics/analyticsFilters";
import { usePermissions } from "@/features/auth/usePermissions";
import { formatMoney, formatNumber, formatRate } from "@/utils/format";

/**
 * Analytics screen (KB item 441 / E19 / FR-104–FR-108 / item 444 Recharts).
 *
 * Loads engagement, conversion, ROI, product performance comparisons, and optional
 * campaign analytics drill-down. Supports selectors/filters for **campaign**, **product**,
 * and **time frame** so analysts can narrow KPIs and charts (KB Analytics workflows).
 */
export function AnalyticsPage() {
  const permissions = usePermissions();
  const canViewAnalytics = permissions.canViewAnalytics();
  const canReadCampaigns = permissions.canReadCampaigns();
  const [filters, setFilters] = useState<AnalyticsFilterState>(emptyAnalyticsFilters);

  const dashboardQuery = useQuery({
    queryKey: ["analytics", "dashboard"],
    queryFn: getDashboard,
    enabled: canViewAnalytics,
  });

  const productPerformanceQuery = useQuery({
    queryKey: ["analytics", "products", "performance"],
    queryFn: getProductPerformance,
    enabled: canViewAnalytics,
  });

  const campaignsQuery = useQuery({
    queryKey: ["campaigns", "analytics-picker"],
    queryFn: () => listCampaigns(),
    enabled: canViewAnalytics && canReadCampaigns,
  });

  const campaignOptions = useMemo(
    () =>
      buildCampaignOptions(campaignsQuery.data, dashboardQuery.data?.recentCampaignMetrics),
    [campaignsQuery.data, dashboardQuery.data?.recentCampaignMetrics],
  );

  const productOptions = useMemo(
    () => buildProductOptions(productPerformanceQuery.data),
    [productPerformanceQuery.data],
  );

  const filtersActive = areAnalyticsFiltersActive(filters);

  const filteredMetrics = useMemo(
    () =>
      filterCampaignMetrics(
        dashboardQuery.data?.recentCampaignMetrics,
        filters,
        campaignsQuery.data,
      ),
    [dashboardQuery.data?.recentCampaignMetrics, filters, campaignsQuery.data],
  );

  const filteredProducts = useMemo(
    () =>
      filterProductPerformance(
        productPerformanceQuery.data,
        filters,
        campaignsQuery.data,
        filteredMetrics,
      ),
    [productPerformanceQuery.data, filters, campaignsQuery.data, filteredMetrics],
  );

  const filteredDashboard = useMemo(
    () => dashboardFromMetrics(dashboardQuery.data, filteredMetrics, filtersActive),
    [dashboardQuery.data, filteredMetrics, filtersActive],
  );

  const resolvedCampaignId =
    filters.campaignId !== ""
      ? filters.campaignId
      : (campaignOptions[0]?.id ?? "");

  const campaignAnalyticsQuery = useQuery({
    queryKey: ["analytics", "campaigns", resolvedCampaignId],
    queryFn: () => getCampaignAnalytics(resolvedCampaignId),
    enabled: canViewAnalytics && resolvedCampaignId !== "",
  });

  if (!canViewAnalytics) {
    return (
      <section className="panel">
        <div className="section-heading">
          <h2>Analytics</h2>
          <span>Engagement, conversion, ROI, and product comparisons</span>
        </div>
        <p className="form-error" role="alert">
          You are not authorized to view analytics.
        </p>
      </section>
    );
  }

  const dashboard = filteredDashboard;
  const products = filteredProducts;
  const isLoadingOverview =
    dashboardQuery.isLoading || productPerformanceQuery.isLoading;
  const overviewError = firstErrorMessage(
    [dashboardQuery.error, productPerformanceQuery.error],
    "Unable to load analytics.",
  );

  return (
    <section className="page-stack">
      <div className="panel">
        <div className="section-heading">
          <h2>Analytics</h2>
          <span>
            {isLoadingOverview
              ? "Loading engagement and product performance"
              : "Engagement, conversion, ROI, and comparisons (FR-104–FR-108)"}
          </span>
        </div>
        {overviewError !== "" ? (
          <p className="form-error" role="alert">
            {overviewError}
          </p>
        ) : null}
      </div>

      <AnalyticsFiltersPanel
        filters={filters}
        campaignOptions={campaignOptions}
        productOptions={productOptions}
        campaignsLoading={campaignsQuery.isLoading}
        productsLoading={productPerformanceQuery.isLoading}
        filtersActive={filtersActive}
        onChange={setFilters}
        onReset={() => setFilters(emptyAnalyticsFilters)}
      />

      {dashboard != null ? <EngagementKpiGrid dashboard={dashboard} /> : null}

      <section className="panel">
        <div className="section-heading">
          <h2>Campaign rate comparison</h2>
          <span>Open, click, and conversion rates across filtered campaigns</span>
        </div>
        <MultiSeriesBarChart
          data={toRateComparisonChartRows(dashboard?.recentCampaignMetrics)}
          series={[
            { dataKey: "openRate", name: "Open rate %", color: CHART_COLORS.openRate },
            { dataKey: "clickRate", name: "Click rate %", color: CHART_COLORS.clickRate },
            {
              dataKey: "conversionRate",
              name: "Conversion rate %",
              color: CHART_COLORS.conversionRate,
            },
          ]}
          ariaLabel="Campaign open click and conversion rate comparison chart"
          isLoading={dashboardQuery.isLoading}
          loadingMessage="Loading campaign rate comparison…"
          emptyMessage="No campaign metrics match the current filters."
          yAxisUnit="%"
        />
      </section>

      <section className="panel">
        <div className="section-heading">
          <h2>Engagement mix</h2>
          <span>Share of opened, clicked, replied, and converted events (FR-108)</span>
        </div>
        <EngagementMixPieChart
          data={toDashboardEngagementMixRows(dashboard)}
          ariaLabel="Analytics engagement mix pie chart"
          isLoading={dashboardQuery.isLoading}
          loadingMessage="Loading engagement mix chart…"
          emptyMessage="No engagement mix data matches the current filters."
        />
      </section>

      <section className="panel">
        <div className="section-heading">
          <h2>Product performance</h2>
          <span>Aggregated metrics by linked product (item 433)</span>
        </div>
        <div style={{ marginBottom: "1rem" }}>
          <MultiSeriesBarChart
            data={toProductPerformanceChartRows(products)}
            series={[
              { dataKey: "sent", name: "Sent", color: CHART_COLORS.sent },
              { dataKey: "conversions", name: "Conversions", color: CHART_COLORS.conversions },
            ]}
            ariaLabel="Product sent messages versus conversions chart"
            isLoading={productPerformanceQuery.isLoading}
            loadingMessage="Loading product performance chart…"
            emptyMessage="No product performance data matches the current filters."
          />
        </div>
        <FinancialLineChart
          data={toProductFinancialChartRows(products)}
          ariaLabel="Product cost revenue and ROI line chart"
          isLoading={productPerformanceQuery.isLoading}
          loadingMessage="Loading product financial chart…"
          emptyMessage="No product financial chart data matches the current filters."
        />
        <ProductPerformanceTable
          rows={products}
          isLoading={productPerformanceQuery.isLoading}
        />
      </section>

      <section className="panel">
        <div className="section-heading">
          <h2>Campaign analytics</h2>
          <span>Drill-down for a single campaign (item 432)</span>
        </div>
        <div className="form-grid">
          <label>
            Campaign
            <select
              aria-label="Select campaign for analytics"
              value={resolvedCampaignId}
              onChange={(event) =>
                setFilters((current) => ({ ...current, campaignId: event.target.value }))
              }
              disabled={campaignOptions.length === 0}
            >
              {campaignOptions.length === 0 ? (
                <option value="">No campaigns available</option>
              ) : (
                campaignOptions.map((option) => (
                  <option key={option.id} value={option.id}>
                    {option.label}
                  </option>
                ))
              )}
            </select>
          </label>
        </div>
        <CampaignAnalyticsDetail
          analytics={campaignAnalyticsQuery.data}
          isLoading={campaignAnalyticsQuery.isLoading}
          errorMessage={analyticsErrorMessage(
            campaignAnalyticsQuery.error,
            "Unable to load campaign analytics.",
          )}
          hasSelection={resolvedCampaignId !== ""}
        />
      </section>
    </section>
  );
}

function AnalyticsFiltersPanel({
  filters,
  campaignOptions,
  productOptions,
  campaignsLoading,
  productsLoading,
  filtersActive,
  onChange,
  onReset,
}: {
  filters: AnalyticsFilterState;
  campaignOptions: CampaignOption[];
  productOptions: ProductOption[];
  campaignsLoading: boolean;
  productsLoading: boolean;
  filtersActive: boolean;
  onChange: (filters: AnalyticsFilterState) => void;
  onReset: () => void;
}) {
  return (
    <section className="panel" aria-label="Analytics filters">
      <div className="section-heading">
        <h2>Filters</h2>
        <span>
          {filtersActive
            ? "Results narrowed by campaign, product, and/or time frame"
            : "Filter by campaign, product, and time frame (KB Analytics)"}
        </span>
      </div>
      <div className="form-grid">
        <label>
          Campaign
          <select
            aria-label="Filter analytics by campaign"
            value={filters.campaignId}
            disabled={campaignsLoading && campaignOptions.length === 0}
            onChange={(event) => onChange({ ...filters, campaignId: event.target.value })}
          >
            <option value="">All campaigns</option>
            {campaignOptions.map((option) => (
              <option key={option.id} value={option.id}>
                {option.label}
              </option>
            ))}
          </select>
        </label>
        <label>
          Product
          <select
            aria-label="Filter analytics by product"
            value={filters.productId}
            disabled={productsLoading && productOptions.length === 0}
            onChange={(event) => onChange({ ...filters, productId: event.target.value })}
          >
            <option value="">All products</option>
            {productOptions.map((option) => (
              <option key={option.id} value={option.id}>
                {option.label}
              </option>
            ))}
          </select>
        </label>
        <label>
          Time frame
          <select
            aria-label="Filter analytics by time frame"
            value={filters.timeframe}
            onChange={(event) =>
              onChange({
                ...filters,
                timeframe: event.target.value as AnalyticsFilterState["timeframe"],
              })
            }
          >
            {ANALYTICS_TIMEFRAME_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>
        {filters.timeframe === "CUSTOM" ? (
          <>
            <label>
              From
              <input
                type="date"
                aria-label="Analytics filter date from"
                value={filters.dateFrom}
                onChange={(event) => onChange({ ...filters, dateFrom: event.target.value })}
              />
            </label>
            <label>
              To
              <input
                type="date"
                aria-label="Analytics filter date to"
                value={filters.dateTo}
                onChange={(event) => onChange({ ...filters, dateTo: event.target.value })}
              />
            </label>
          </>
        ) : null}
        <div className="form-actions">
          <button type="button" className="secondary-button" onClick={onReset}>
            Reset filters
          </button>
        </div>
      </div>
    </section>
  );
}

function EngagementKpiGrid({ dashboard }: { dashboard: DashboardView }) {
  return (
    <div className="metric-grid" aria-label="Analytics engagement KPI cards">
      <MetricCard
        label="Open rate"
        value={formatRate(dashboard.openRate)}
        detail={`Opened ${formatNumber(dashboard.openedCount)} of ${formatNumber(dashboard.messagesSent)} sent (FR-104)`}
      />
      <MetricCard
        label="Click rate"
        value={formatRate(dashboard.clickRate)}
        detail={`Clicked ${formatNumber(dashboard.clickedCount)} (FR-105)`}
      />
      <MetricCard
        label="Conversion rate"
        value={formatRate(dashboard.conversionRate)}
        detail={`Converted ${formatNumber(dashboard.convertedCount)} (FR-106)`}
      />
      <MetricCard
        label="Estimated ROI"
        value={formatRate(dashboard.estimatedRoi)}
        detail={`Cost ${formatMoney(dashboard.estimatedCost)} · Revenue ${formatMoney(dashboard.estimatedRevenue)} (FR-107)`}
      />
      <MetricCard
        label="Messages sent"
        value={formatNumber(dashboard.messagesSent)}
        detail={`Replied ${formatNumber(dashboard.repliedCount)}`}
      />
      <MetricCard
        label="Audience"
        value={formatNumber(dashboard.audienceSize)}
        detail={`Eligible ${formatNumber(dashboard.eligibleCount)} · Excluded ${formatNumber(dashboard.excludedCount)}`}
      />
    </div>
  );
}

function ProductPerformanceTable({
  rows,
  isLoading,
}: {
  rows: ProductPerformanceView[];
  isLoading: boolean;
}) {
  if (isLoading) {
    return <p>Loading product performance table…</p>;
  }
  if (rows.length === 0) {
    return <p>No products match the current filters.</p>;
  }

  return (
    <table aria-label="Product performance table">
      <thead>
        <tr>
          <th>Product</th>
          <th>Type</th>
          <th>Campaigns</th>
          <th>Sent</th>
          <th>Open rate</th>
          <th>Click rate</th>
          <th>Conversion rate</th>
          <th>ROI</th>
        </tr>
      </thead>
      <tbody>
        {rows.map((row) => (
          <tr key={row.productId}>
            <td>{row.productName ?? row.productId}</td>
            <td>{formatEnumLabel(row.productType)}</td>
            <td>{formatNumber(row.campaignCount)}</td>
            <td>{formatNumber(row.sentCount)}</td>
            <td>{formatRate(row.openRate)}</td>
            <td>{formatRate(row.clickRate)}</td>
            <td>{formatRate(row.conversionRate)}</td>
            <td>{formatRate(row.estimatedRoi)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function CampaignAnalyticsDetail({
  analytics,
  isLoading,
  errorMessage,
  hasSelection,
}: {
  analytics: CampaignAnalyticsView | undefined;
  isLoading: boolean;
  errorMessage: string;
  hasSelection: boolean;
}) {
  if (!hasSelection) {
    return <p>Select a campaign to view detailed analytics.</p>;
  }
  if (isLoading) {
    return <p>Loading campaign analytics…</p>;
  }
  if (errorMessage !== "") {
    return (
      <p className="form-error" role="alert">
        {errorMessage}
      </p>
    );
  }
  if (analytics == null) {
    return <p>No campaign analytics are available for the selected campaign.</p>;
  }

  const metrics = analytics.metrics;

  return (
    <div className="page-stack" aria-label="Campaign analytics detail">
      <dl className="detail-list">
        <div>
          <dt>Campaign</dt>
          <dd>{analytics.campaignName ?? analytics.campaignId}</dd>
        </div>
        <div>
          <dt>Status</dt>
          <dd>
            {analytics.status != null ? (
              <CampaignStatusBadge status={analytics.status} />
            ) : (
              "—"
            )}
          </dd>
        </div>
        <div>
          <dt>Channel</dt>
          <dd>{formatEnumLabel(analytics.channel)}</dd>
        </div>
        <div>
          <dt>Owner</dt>
          <dd>{analytics.ownerFullName ?? "—"}</dd>
        </div>
        <div>
          <dt>Objective</dt>
          <dd>{analytics.objective?.trim() ? analytics.objective : "—"}</dd>
        </div>
        <div>
          <dt>Schedule</dt>
          <dd>{formatDateRange(analytics.startDate, analytics.endDate)}</dd>
        </div>
      </dl>

      {metrics == null ? (
        <p>This campaign has no stored metrics yet (not launched or no contact activity).</p>
      ) : (
        <CampaignMetricsDetailTable metrics={metrics} />
      )}
    </div>
  );
}

function CampaignMetricsDetailTable({ metrics }: { metrics: CampaignMetricsView }) {
  return (
    <table aria-label="Selected campaign metrics table">
      <thead>
        <tr>
          <th>Audience</th>
          <th>Eligible</th>
          <th>Excluded</th>
          <th>Sent</th>
          <th>Opened</th>
          <th>Clicked</th>
          <th>Converted</th>
          <th>Open rate</th>
          <th>Click rate</th>
          <th>Conversion rate</th>
          <th>ROI</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td>{formatNumber(metrics.audienceSize)}</td>
          <td>{formatNumber(metrics.eligibleCount)}</td>
          <td>{formatNumber(metrics.excludedCount)}</td>
          <td>{formatNumber(metrics.sentCount)}</td>
          <td>{formatNumber(metrics.openedCount)}</td>
          <td>{formatNumber(metrics.clickedCount)}</td>
          <td>{formatNumber(metrics.convertedCount)}</td>
          <td>{formatRate(metrics.openRate)}</td>
          <td>{formatRate(metrics.clickRate)}</td>
          <td>{formatRate(metrics.conversionRate)}</td>
          <td>{formatRate(metrics.estimatedRoi)}</td>
        </tr>
      </tbody>
    </table>
  );
}

type CampaignOption = { id: string; label: string };
type ProductOption = { id: string; label: string };

function buildCampaignOptions(
  campaigns: CampaignView[] | undefined,
  recentMetrics: CampaignMetricsView[] | undefined,
): CampaignOption[] {
  const options: CampaignOption[] = [];
  const seen = new Set<string>();

  for (const campaign of campaigns ?? []) {
    if (seen.has(campaign.id)) {
      continue;
    }
    seen.add(campaign.id);
    options.push({
      id: campaign.id,
      label: `${campaign.name} (${campaign.status})`,
    });
  }

  for (const metrics of recentMetrics ?? []) {
    if (seen.has(metrics.campaignId)) {
      continue;
    }
    seen.add(metrics.campaignId);
    const name = metrics.campaignName?.trim() || metrics.campaignId;
    const status = metrics.campaignStatus ?? "UNKNOWN";
    options.push({
      id: metrics.campaignId,
      label: `${name} (${status})`,
    });
  }

  return options;
}

function buildProductOptions(
  products: ProductPerformanceView[] | undefined,
): ProductOption[] {
  return (products ?? []).map((row) => ({
    id: row.productId,
    label: row.productName?.trim() || row.productId,
  }));
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

function formatDateRange(start: string | null | undefined, end: string | null | undefined): string {
  if ((start == null || start === "") && (end == null || end === "")) {
    return "—";
  }
  return `${start ?? "—"} → ${end ?? "—"}`;
}

function firstErrorMessage(errors: unknown[], fallback: string): string {
  for (const error of errors) {
    const message = analyticsErrorMessage(error, fallback);
    if (message !== "") {
      return message;
    }
  }
  return "";
}

function analyticsErrorMessage(error: unknown, fallback: string): string {
  if (error == null) {
    return "";
  }
  if (isAuthorizationError(error)) {
    return "You are not authorized to view analytics.";
  }
  if (error instanceof Error && error.message.trim() !== "") {
    return error.message;
  }
  return fallback;
}
