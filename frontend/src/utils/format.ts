export function formatNumber(value: number) {
  return new Intl.NumberFormat("en-US").format(value);
}

export function formatPercent(value: number) {
  return `${value.toFixed(1)}%`;
}

/**
 * Formats a 0–1 analytics rate (open/click/conversion/ROI) as a percentage string.
 */
export function formatRate(value: number | null | undefined) {
  if (value == null || Number.isNaN(value)) {
    return "—";
  }
  return formatPercent(value * 100);
}

/**
 * Formats optional monetary dashboard fields (estimated cost/revenue).
 */
export function formatMoney(value: number | null | undefined) {
  if (value == null || Number.isNaN(value)) {
    return "—";
  }
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    maximumFractionDigits: 2,
  }).format(value);
}
