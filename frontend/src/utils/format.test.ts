import { describe, expect, it } from "vitest";
import { formatMoney, formatNumber, formatPercent, formatRate } from "@/utils/format";

describe("format utilities", () => {
  it("formats large numbers for dashboard metrics", () => {
    expect(formatNumber(2310)).toBe("2,310");
  });

  it("formats percentages with one decimal place", () => {
    expect(formatPercent(85.021)).toBe("85.0%");
    expect(formatPercent(84.978)).toBe("85.0%");
  });

  it("formats 0–1 analytics rates as percentages", () => {
    expect(formatRate(0.5)).toBe("50.0%");
    expect(formatRate(0.05)).toBe("5.0%");
    expect(formatRate(null)).toBe("—");
  });

  it("formats optional monetary dashboard fields", () => {
    expect(formatMoney(200)).toBe("$200.00");
    expect(formatMoney(null)).toBe("—");
  });
});
