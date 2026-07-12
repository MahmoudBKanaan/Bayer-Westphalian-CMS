import { describe, expect, it } from "vitest";
import {
  CHART_COLORS,
  CHART_HEIGHT,
  PIE_SLICE_COLORS,
} from "@/components/charts/chartTheme";

describe("chartTheme (item 595 / item 444)", () => {
  it("exposes stable semantic colors for engagement and financial series", () => {
    expect(CHART_COLORS.primary).toMatch(/^#/);
    expect(CHART_COLORS.sent).toBe(CHART_COLORS.primary);
    expect(CHART_COLORS.conversions).toBe(CHART_COLORS.success);
    expect(CHART_COLORS.cost).toBe(CHART_COLORS.danger);
    expect(CHART_COLORS.revenue).toBe(CHART_COLORS.success);
    expect(CHART_COLORS.opened).toBeTruthy();
    expect(CHART_COLORS.clicked).toBeTruthy();
    expect(CHART_COLORS.replied).toBeTruthy();
    expect(CHART_COLORS.converted).toBeTruthy();
  });

  it("defines compact default and tall chart heights for dashboard layouts", () => {
    expect(CHART_HEIGHT.compact).toBeLessThan(CHART_HEIGHT.default);
    expect(CHART_HEIGHT.default).toBeLessThan(CHART_HEIGHT.tall);
    expect(CHART_HEIGHT.compact).toBeGreaterThan(0);
  });

  it("provides pie slice colors covering engagement mix categories", () => {
    expect(PIE_SLICE_COLORS.length).toBeGreaterThanOrEqual(4);
    expect(PIE_SLICE_COLORS).toContain(CHART_COLORS.opened);
    expect(PIE_SLICE_COLORS).toContain(CHART_COLORS.converted);
  });
});
