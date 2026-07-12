import { describe, expect, it } from "vitest";
import {
  buildProductSearchQuery,
  countActiveProductFilters,
  describeAppliedProductFilters,
  emptyProductSearchFilters,
  formatProductCatalogSummary,
  hasActiveProductFilters,
  normalizeProductSearchFilters,
} from "@/features/products/productSearch";

describe("productSearch helpers", () => {
  it("normalizes and counts active KB product search filters", () => {
    const normalized = normalizeProductSearchFilters({
      term: "  life  ",
      productType: "LIFE_INSURANCE",
      active: "true",
    });

    expect(normalized).toEqual({
      term: "life",
      productType: "LIFE_INSURANCE",
      active: "true",
    });
    expect(countActiveProductFilters(normalized)).toBe(3);
    expect(hasActiveProductFilters(normalized)).toBe(true);
  });

  it("describes applied filters for the product search UI", () => {
    expect(
      describeAppliedProductFilters({
        term: "protection",
        productType: "INVESTMENT_FUND",
        active: "false",
      }),
    ).toEqual([
      { key: "term", label: "Search: protection" },
      { key: "productType", label: "Type: Investment Fund" },
      { key: "active", label: "Status: Inactive" },
    ]);
  });

  it("reports when no product search filters are active", () => {
    expect(hasActiveProductFilters(emptyProductSearchFilters)).toBe(false);
    expect(countActiveProductFilters(emptyProductSearchFilters)).toBe(0);
    expect(describeAppliedProductFilters(emptyProductSearchFilters)).toEqual([]);
  });

  it("builds partial product search query strings for individual filters", () => {
    expect(buildProductSearchQuery({ term: "life", productType: "ALL", active: "ALL" })).toBe(
      "?term=life",
    );
    expect(
      buildProductSearchQuery({ term: "", productType: "INVESTMENT_FUND", active: "ALL" }),
    ).toBe("?productType=INVESTMENT_FUND");
    expect(buildProductSearchQuery({ term: "", productType: "ALL", active: "false" })).toBe(
      "?active=false",
    );
  });

  it("builds the backend product search query string", () => {
    expect(
      buildProductSearchQuery({
        term: "life",
        productType: "LIFE_INSURANCE",
        active: "true",
      }),
    ).toBe("?term=life&productType=LIFE_INSURANCE&active=true");
    expect(buildProductSearchQuery()).toBe("");
  });

  it("formats catalog summaries for filtered and unfiltered results", () => {
    expect(formatProductCatalogSummary(1, false)).toBe("1 product");
    expect(formatProductCatalogSummary(2, true)).toBe("2 matching products");
  });
});
