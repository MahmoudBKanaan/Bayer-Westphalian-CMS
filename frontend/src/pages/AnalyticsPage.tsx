import { MetricCard } from "@/components/MetricCard";

export function AnalyticsPage() {
  return (
    <section className="metric-grid">
      <MetricCard label="Open rate" value="42.5%" detail="Campaign engagement" />
      <MetricCard label="Click rate" value="13.8%" detail="Message interaction" />
      <MetricCard label="Conversion rate" value="7.4%" detail="Estimated policy interest" />
      <MetricCard label="ROI estimate" value="3.2x" detail="Campaign return indicator" />
    </section>
  );
}
