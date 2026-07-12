import { Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip } from "recharts";
import { ChartFrame, hasNonZeroValues } from "@/components/charts/ChartFrame";
import { CHART_HEIGHT, PIE_SLICE_COLORS } from "@/components/charts/chartTheme";

export type EngagementMixSlice = {
  name: string;
  value: number;
};

export type EngagementMixPieChartProps = {
  data: EngagementMixSlice[];
  ariaLabel: string;
  isLoading?: boolean;
  loadingMessage?: string;
  emptyMessage?: string;
  height?: number;
};

/**
 * Engagement mix pie chart (opened / clicked / replied / converted) — KB item 444 / FR-108.
 */
export function EngagementMixPieChart({
  data,
  ariaLabel,
  isLoading = false,
  loadingMessage,
  emptyMessage = "No engagement mix data is available yet.",
  height = CHART_HEIGHT.default,
}: EngagementMixPieChartProps) {
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
        <PieChart>
          <Pie
            data={data}
            dataKey="value"
            nameKey="name"
            cx="50%"
            cy="50%"
            outerRadius={90}
            label
          >
            {data.map((entry, index) => (
              <Cell
                key={`${entry.name}-${index}`}
                fill={PIE_SLICE_COLORS[index % PIE_SLICE_COLORS.length]}
              />
            ))}
          </Pie>
          <Tooltip />
          <Legend />
        </PieChart>
      </ResponsiveContainer>
    </ChartFrame>
  );
}
