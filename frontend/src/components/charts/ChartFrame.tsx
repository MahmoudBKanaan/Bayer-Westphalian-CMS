import type { ReactNode } from "react";
import { CHART_HEIGHT } from "@/components/charts/chartTheme";

export type ChartFrameProps = {
  ariaLabel: string;
  isLoading?: boolean;
  loadingMessage?: string;
  isEmpty?: boolean;
  emptyMessage?: string;
  height?: number;
  children: ReactNode;
};

/**
 * Shared chart shell for loading, empty, and accessible Recharts containers (KB item 444).
 */
export function ChartFrame({
  ariaLabel,
  isLoading = false,
  loadingMessage = "Loading chart…",
  isEmpty = false,
  emptyMessage = "No chart data is available yet.",
  height = CHART_HEIGHT.default,
  children,
}: ChartFrameProps) {
  if (isLoading) {
    return <p>{loadingMessage}</p>;
  }
  if (isEmpty) {
    return <p>{emptyMessage}</p>;
  }

  return (
    <div
      className="chart-frame"
      role="img"
      aria-label={ariaLabel}
      style={{ height }}
      data-testid="chart-frame"
    >
      {children}
    </div>
  );
}

export function hasNonZeroValues(values: number[]): boolean {
  return values.some((value) => value !== 0 && !Number.isNaN(value));
}
