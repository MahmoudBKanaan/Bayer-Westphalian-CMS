import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { ChartFrame, hasNonZeroValues } from "@/components/charts/ChartFrame";
import { CHART_COLORS, CHART_HEIGHT } from "@/components/charts/chartTheme";

export type FinancialChartRow = {
  name: string;
  cost: number;
  revenue: number;
  roiPercent: number;
};

export type FinancialLineChartProps = {
  data: FinancialChartRow[];
  ariaLabel: string;
  isLoading?: boolean;
  loadingMessage?: string;
  emptyMessage?: string;
  height?: number;
};

/**
 * Cost / revenue / ROI line chart for product or campaign financials — KB item 444 / FR-107–108.
 */
export function FinancialLineChart({
  data,
  ariaLabel,
  isLoading = false,
  loadingMessage,
  emptyMessage = "No financial chart data is available yet.",
  height = CHART_HEIGHT.default,
}: FinancialLineChartProps) {
  const isEmpty =
    data.length === 0 ||
    !hasNonZeroValues(
      data.flatMap((row) => [row.cost, row.revenue, row.roiPercent]),
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
        <LineChart data={data}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="name" />
          <YAxis yAxisId="money" allowDecimals={false} />
          <YAxis yAxisId="roi" orientation="right" unit="%" allowDecimals={false} />
          <Tooltip />
          <Legend />
          <Line
            yAxisId="money"
            type="monotone"
            dataKey="cost"
            name="Cost"
            stroke={CHART_COLORS.cost}
            strokeWidth={2}
            dot={{ r: 3 }}
          />
          <Line
            yAxisId="money"
            type="monotone"
            dataKey="revenue"
            name="Revenue"
            stroke={CHART_COLORS.revenue}
            strokeWidth={2}
            dot={{ r: 3 }}
          />
          <Line
            yAxisId="roi"
            type="monotone"
            dataKey="roiPercent"
            name="ROI %"
            stroke={CHART_COLORS.roi}
            strokeWidth={2}
            dot={{ r: 3 }}
          />
        </LineChart>
      </ResponsiveContainer>
    </ChartFrame>
  );
}
