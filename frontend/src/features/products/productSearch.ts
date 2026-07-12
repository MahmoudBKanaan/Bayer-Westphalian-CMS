import type { ProductSearchFilters, ProductType } from "@/api/products";

export const PRODUCT_TYPE_FILTERS: Array<ProductType | "ALL"> = [
  "ALL",
  "HOMEOWNER_INSURANCE",
  "LIFE_INSURANCE",
  "INVESTMENT_FUND",
  "HEALTH_INSURANCE",
  "AUTO_INSURANCE",
  "OTHER",
];

export const PRODUCT_ACTIVE_FILTERS: ProductSearchFilters["active"][] = ["ALL", "true", "false"];

export const emptyProductSearchFilters: ProductSearchFilters = {
  term: "",
  productType: "ALL",
  active: "ALL",
};

export type AppliedProductFilterChip = {
  key: string;
  label: string;
};

export function normalizeProductSearchFilters(filters: ProductSearchFilters): ProductSearchFilters {
  return {
    ...filters,
    term: filters.term.trim(),
  };
}

export function countActiveProductFilters(filters: ProductSearchFilters) {
  return [
    filters.term,
    filters.productType === "ALL" ? "" : filters.productType,
    filters.active === "ALL" ? "" : filters.active,
  ].filter((value) => value.trim().length > 0).length;
}

export function hasActiveProductFilters(filters: ProductSearchFilters) {
  return countActiveProductFilters(filters) > 0;
}

export function describeAppliedProductFilters(
  filters: ProductSearchFilters,
): AppliedProductFilterChip[] {
  const chips: AppliedProductFilterChip[] = [];

  if (filters.term.trim().length > 0) {
    chips.push({
      key: "term",
      label: `Search: ${filters.term.trim()}`,
    });
  }
  if (filters.productType !== "ALL") {
    chips.push({
      key: "productType",
      label: `Type: ${formatProductEnum(filters.productType)}`,
    });
  }
  if (filters.active !== "ALL") {
    chips.push({
      key: "active",
      label: `Status: ${formatProductActiveFilter(filters.active)}`,
    });
  }

  return chips;
}

export function formatProductCatalogSummary(count: number, filtered: boolean) {
  const noun = count === 1 ? "product" : "products";
  return filtered ? `${count} matching ${noun}` : `${count} ${noun}`;
}

export function formatProductEnum(value: string) {
  if (value === "ALL") {
    return "All";
  }
  return value
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

export function formatProductActiveFilter(value: ProductSearchFilters["active"]) {
  if (value === "ALL") {
    return "All";
  }
  return value === "true" ? "Active" : "Inactive";
}

export function buildProductSearchQuery(filters?: ProductSearchFilters) {
  if (filters == null) {
    return "";
  }

  const params = new URLSearchParams();
  const normalized = normalizeProductSearchFilters(filters);

  if (normalized.term.length > 0) {
    params.set("term", normalized.term);
  }
  if (normalized.productType !== "ALL") {
    params.set("productType", normalized.productType);
  }
  if (normalized.active !== "ALL") {
    params.set("active", normalized.active);
  }

  const query = params.toString();
  return query.length === 0 ? "" : `?${query}`;
}
