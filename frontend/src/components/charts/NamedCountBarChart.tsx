import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { ChartFrame, hasNonZeroValues } from "@/components/charts/ChartFrame";
import { CHART_COLORS, CHART_HEIGHT } from "@/components/charts/chartTheme";

export type NamedCountRow = {
  name: string;
  value: number;
};

export type NamedCountBarChartProps = {
  data: NamedCountRow[];
  ariaLabel: string;
  isLoading?: boolean;
  loadingMessage?: string;
  emptyMessage?: string;
  height?: number;
  barColor?: string;
  valueName?: string;
};

/**
 * Single-series named count bars (inventory, funnel stages) — KB item 444 / COMP-010.
 */
export function NamedCountBarChart({
  data,
  ariaLabel,
  isLoading = false,
  loadingMessage,
  emptyMessage = "No chart data is available yet.",
  height = CHART_HEIGHT.compact,
  barColor = CHART_COLORS.primary,
  valueName = "Count",
}: NamedCountBarChartProps) {
  const isEmpty = data.length === 0 || !hasNonZeroValues(data.map((row) => row.value));

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
          <XAxis dataKey="name" />
          <YAxis allowDecimals={false} />
          <Tooltip />
          <Bar dataKey="value" name={valueName} fill={barColor} radius={[4, 4, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </ChartFrame>
  );
}
