import { describe, expect, it } from "vitest";
import { formatNumber, formatPercent } from "@/utils/format";

describe("format utilities", () => {
  it("formats large numbers for dashboard metrics", () => {
    expect(formatNumber(2310)).toBe("2,310");
  });

  it("formats percentages with one decimal place", () => {
    expect(formatPercent(85.021)).toBe("85.0%");
    expect(formatPercent(84.978)).toBe("85.0%");
  });
});
