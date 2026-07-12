/**
 * Shared Recharts visualization components (KB item 444 / FR-108).
 */

export { ChartFrame, hasNonZeroValues } from "@/components/charts/ChartFrame";
export { CHART_COLORS, CHART_HEIGHT, PIE_SLICE_COLORS } from "@/components/charts/chartTheme";
export {
  EngagementMixPieChart,
  type EngagementMixPieChartProps,
  type EngagementMixSlice,
} from "@/components/charts/EngagementMixPieChart";
export {
  FinancialLineChart,
  type FinancialChartRow,
  type FinancialLineChartProps,
} from "@/components/charts/FinancialLineChart";
export {
  MultiSeriesBarChart,
  type MultiSeriesBarChartProps,
  type MultiSeriesBarSeries,
} from "@/components/charts/MultiSeriesBarChart";
export {
  NamedCountBarChart,
  type NamedCountBarChartProps,
  type NamedCountRow,
} from "@/components/charts/NamedCountBarChart";
