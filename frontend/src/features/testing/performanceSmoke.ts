/**
 * Sprint 16 item **639** / KB **NFR-003** — performance smoke for search and dashboard.
 *
 * Locks budgets, dataset sizing, and client-side smoke helpers used by Vitest.
 * Backend wall-clock smokes live in `PerformanceSmokeTests.java`.
 */

import {
  buildDashboardKpiGroups,
  type DashboardKpiGroupModel,
} from "@/features/dashboard/dashboardReadability";
import {
  normalizeProductSearchFilters,
  type AppliedProductFilterChip,
  describeAppliedProductFilters,
} from "@/features/products/productSearch";
import type { DashboardView } from "@/api/analytics";
import type { ProductSearchFilters, ProductType } from "@/api/products";

/** Repo-relative documentation path. */
export const PERFORMANCE_SMOKE_DOC_PATH = "docs/testing/performance-smoke.md";

export const PERFORMANCE_SMOKE_TITLE = "Performance Smoke Checks";

export const PERFORMANCE_SMOKE_BACKLOG_ITEM = 639;

/** KB NFR-003 budget in milliseconds. */
export const NFR_003_BUDGET_MS = 1_000;

/** Synthetic project-scale catalog size for client filter smokes. */
export const PROJECT_DATASET_SIZE = 5_000;

/** Synthetic recent metrics rows used when building dashboard models repeatedly. */
export const DASHBOARD_SMOKE_ITERATIONS = 500;

export const PERFORMANCE_SMOKE_DOC_REQUIRED_SNIPPETS: string[] = [
  "item **639**",
  "NFR-003",
  "under 1 second",
  "PerformanceSmokeTests",
  "performanceSmoke.ts",
  "do not run any tests",
  "## Acceptance (item 639)",
];

export type PerformanceSmokeSurfaceId = "customer-search" | "product-search" | "dashboard";

export type PerformanceSmokeSurface = {
  id: PerformanceSmokeSurfaceId;
  title: string;
  kbRefs: string[];
  backendEvidence: string;
  frontendEvidence: string;
};

export const PERFORMANCE_SMOKE_SURFACES: PerformanceSmokeSurface[] = [
  {
    id: "customer-search",
    title: "Customer / prospect search",
    kbRefs: ["FR-014", "NFR-003"],
    backendEvidence: "PerformanceSmokeTests.customerStyleSearchCompletesUnderOneSecondForProjectDataset",
    frontendEvidence: "performanceSmoke.test.ts (filterCustomersSmoke)",
  },
  {
    id: "product-search",
    title: "Product catalog search and filters",
    kbRefs: ["FR-044", "NFR-003"],
    backendEvidence: "PerformanceSmokeTests.productStyleSearchCompletesUnderOneSecondForProjectDataset",
    frontendEvidence: "performanceSmoke.test.ts (filterProductsSmoke + productSearch helpers)",
  },
  {
    id: "dashboard",
    title: "Dashboard KPI aggregation and readability model",
    kbRefs: ["FR-100", "FR-108", "NFR-003"],
    backendEvidence: "PerformanceSmokeTests.dashboardAggregationCompletesUnderOneSecondForProjectDataset",
    frontendEvidence: "performanceSmoke.test.ts (buildDashboardKpiGroups smoke)",
  },
];

export type SearchableCustomerRow = {
  firstName: string;
  lastName: string;
  email: string;
  city: string;
  country: string;
  phone: string;
  source: string;
};

export type SearchableProductRow = {
  name: string;
  productType: ProductType;
  description: string;
  active: boolean;
};

export function buildProjectCustomerDataset(size: number = PROJECT_DATASET_SIZE): SearchableCustomerRow[] {
  const rows: SearchableCustomerRow[] = [];
  for (let i = 0; i < size; i += 1) {
    rows.push({
      firstName: `First${i}`,
      lastName: i % 50 === 0 ? "Schmidt" : `Customer${i}`,
      email: `user${i}@example.com`,
      city: `City${i % 40}`,
      country: "DE",
      phone: `+49${1000000 + i}`,
      source: "import",
    });
  }
  return rows;
}

export function buildProjectProductDataset(size: number = PROJECT_DATASET_SIZE): SearchableProductRow[] {
  const types: ProductType[] = [
    "LIFE_INSURANCE",
    "HOMEOWNER_INSURANCE",
    "INVESTMENT_FUND",
    "HEALTH_INSURANCE",
    "AUTO_INSURANCE",
    "OTHER",
  ];
  const rows: SearchableProductRow[] = [];
  for (let i = 0; i < size; i += 1) {
    const productType = types[i % types.length]!;
    rows.push({
      name: productType === "LIFE_INSURANCE" ? `Life Protection Plan ${i}` : `Product Catalog Item ${i}`,
      productType,
      description: `Desc ${i}`,
      active: i % 7 !== 0,
    });
  }
  return rows;
}

export function filterCustomersSmoke(
  customers: SearchableCustomerRow[],
  rawTerm: string,
): SearchableCustomerRow[] {
  const term = rawTerm.trim().toLowerCase();
  if (!term) {
    return customers.slice();
  }
  return customers.filter(
    (c) =>
      c.firstName.toLowerCase().includes(term) ||
      c.lastName.toLowerCase().includes(term) ||
      c.email.toLowerCase().includes(term) ||
      c.city.toLowerCase().includes(term) ||
      c.country.toLowerCase().includes(term) ||
      c.phone.toLowerCase().includes(term) ||
      c.source.toLowerCase().includes(term),
  );
}

export function filterProductsSmoke(
  products: SearchableProductRow[],
  filters: ProductSearchFilters,
): SearchableProductRow[] {
  const normalized = normalizeProductSearchFilters(filters);
  const term = normalized.term.toLowerCase();
  return products.filter((p) => {
    if (normalized.productType !== "ALL" && p.productType !== normalized.productType) {
      return false;
    }
    if (normalized.active === "true" && !p.active) {
      return false;
    }
    if (normalized.active === "false" && p.active) {
      return false;
    }
    if (!term) {
      return true;
    }
    return (
      p.name.toLowerCase().includes(term) ||
      p.description.toLowerCase().includes(term) ||
      p.productType.toLowerCase().includes(term)
    );
  });
}

export function sampleDashboardView(scale: number = 1): DashboardView {
  const factor = Math.max(1, scale);
  return {
    campaignTotal: 120 * factor,
    activeCampaigns: 18 * factor,
    audienceSize: 45_000 * factor,
    eligibleCount: 40_000 * factor,
    excludedCount: 5_000 * factor,
    messagesSent: 38_000 * factor,
    openedCount: 12_000 * factor,
    clickedCount: 4_200 * factor,
    repliedCount: 900 * factor,
    convertedCount: 310 * factor,
    openRate: 0.3158,
    clickRate: 0.1105,
    conversionRate: 0.0082,
    estimatedCost: 12500.5 * factor,
    estimatedRevenue: 48200 * factor,
    estimatedRoi: 2.85,
    recentCampaignMetrics: [],
  };
}

/**
 * Runs dashboard readability model build repeatedly (UI path after API returns).
 */
export function dashboardKpiModelSmoke(iterations: number = DASHBOARD_SMOKE_ITERATIONS): DashboardKpiGroupModel[] {
  let last: DashboardKpiGroupModel[] = [];
  for (let i = 0; i < iterations; i += 1) {
    last = buildDashboardKpiGroups(sampleDashboardView(1 + (i % 5)));
  }
  return last;
}

export function productFilterChipSmoke(filters: ProductSearchFilters): AppliedProductFilterChip[] {
  return describeAppliedProductFilters(normalizeProductSearchFilters(filters));
}

export function measureMs(fn: () => void): number {
  const start = performance.now();
  fn();
  return performance.now() - start;
}

export function isWithinNfr003Budget(elapsedMs: number): boolean {
  return elapsedMs < NFR_003_BUDGET_MS;
}

export function documentationContainsRequiredSnippets(documentation: string): boolean {
  return PERFORMANCE_SMOKE_DOC_REQUIRED_SNIPPETS.every((s) => documentation.includes(s));
}

export function docsIndexMustLinkPerformanceSmoke(indexMarkdown: string): boolean {
  return indexMarkdown.includes("performance-smoke.md") && indexMarkdown.includes("639");
}

export function performanceSmokeSurfaceIds(): PerformanceSmokeSurfaceId[] {
  return PERFORMANCE_SMOKE_SURFACES.map((s) => s.id);
}
