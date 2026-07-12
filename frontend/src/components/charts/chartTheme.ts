/**
 * Shared Recharts theme for analytics visualizations (KB item 444 / FR-108).
 */

export const CHART_COLORS = {
  primary: "#2563eb",
  secondary: "#7c3aed",
  success: "#16a34a",
  warning: "#d97706",
  danger: "#dc2626",
  muted: "#64748b",
  opened: "#2563eb",
  clicked: "#7c3aed",
  replied: "#d97706",
  converted: "#16a34a",
  sent: "#2563eb",
  conversions: "#16a34a",
  cost: "#dc2626",
  revenue: "#16a34a",
  roi: "#7c3aed",
  openRate: "#2563eb",
  clickRate: "#7c3aed",
  conversionRate: "#16a34a",
} as const;

export const CHART_HEIGHT = {
  compact: 240,
  default: 280,
  tall: 320,
} as const;

export const PIE_SLICE_COLORS = [
  CHART_COLORS.opened,
  CHART_COLORS.clicked,
  CHART_COLORS.replied,
  CHART_COLORS.converted,
  CHART_COLORS.warning,
  CHART_COLORS.muted,
] as const;
