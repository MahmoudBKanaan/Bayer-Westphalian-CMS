import type { DashboardMetricTone } from "@/features/dashboard/dashboardReadability";

interface MetricCardProps {
  label: string;
  value: string;
  detail: string;
  /** Visual grouping tone for dashboard readability (item 591). */
  tone?: DashboardMetricTone;
}

/**
 * Compact KPI tile used on dashboards and analytics (FR-100–FR-108, item 591).
 */
export function MetricCard({ label, value, detail, tone = "default" }: MetricCardProps) {
  const toneClass = tone !== "default" ? ` metric-card--${tone}` : "";

  return (
    <article className={`metric-card${toneClass}`} aria-label={`${label}: ${value}`}>
      <h3 className="metric-card-label">{label}</h3>
      <p className="metric-card-value">{value}</p>
      <p className="metric-card-detail">{detail}</p>
    </article>
  );
}
