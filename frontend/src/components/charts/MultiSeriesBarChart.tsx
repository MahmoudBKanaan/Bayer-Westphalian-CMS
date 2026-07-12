import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { ChartFrame, hasNonZeroValues } from "@/components/charts/ChartFrame";
import { CHART_COLORS, CHART_HEIGHT } from "@/components/charts/chartTheme";

export type MultiSeriesBarSeries = {
  dataKey: string;
  name: string;
  color?: string;
};

export type MultiSeriesBarChartProps = {
  data: Record<string, string | number>[];
  series: MultiSeriesBarSeries[];
  ariaLabel: string;
  isLoading?: boolean;
  loadingMessage?: string;
  emptyMessage?: string;
  height?: number;
  yAxisUnit?: string;
  categoryKey?: string;
};

/**
 * Multi-series bar comparison chart (sent vs conversions, rates, etc.) — KB item 444 / FR-108.
 */
export function MultiSeriesBarChart({
  data,
  series,
  ariaLabel,
  isLoading = false,
  loadingMessage,
  emptyMessage = "No chart data is available yet.",
  height = CHART_HEIGHT.default,
  yAxisUnit,
  categoryKey = "name",
}: MultiSeriesBarChartProps) {
  const isEmpty =
    data.length === 0 ||
    !hasNonZeroValues(
      data.flatMap((row) =>
        series.map((item) => Number(row[item.dataKey] ?? 0)),
      ),
    );

  return (
    <ChartFrame
      ariaLabel={ariaLabel}
      isLoading={isLoading}
      loadingMessage={loadingMessage}
      isEmpty={isEmpty}
      emptyMessage={emptyMessage}
      height={height}
    >
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={data}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey={categoryKey} />
          <YAxis unit={yAxisUnit} allowDecimals={false} />
          <Tooltip />
          <Legend />
          {series.map((item, index) => (
            <Bar
              key={item.dataKey}
              dataKey={item.dataKey}
              name={item.name}
              fill={item.color ?? defaultSeriesColor(index)}
              radius={[4, 4, 0, 0]}
            />
          ))}
        </BarChart>
      </ResponsiveContainer>
    </ChartFrame>
  );
}

function defaultSeriesColor(index: number): string {
  const palette = [
    CHART_COLORS.primary,
    CHART_COLORS.success,
    CHART_COLORS.secondary,
    CHART_COLORS.warning,
  ];
  return palette[index % palette.length] ?? CHART_COLORS.primary;
}
