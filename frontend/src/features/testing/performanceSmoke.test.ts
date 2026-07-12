import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  buildProjectCustomerDataset,
  buildProjectProductDataset,
  dashboardKpiModelSmoke,
  docsIndexMustLinkPerformanceSmoke,
  documentationContainsRequiredSnippets,
  filterCustomersSmoke,
  filterProductsSmoke,
  isWithinNfr003Budget,
  measureMs,
  NFR_003_BUDGET_MS,
  PERFORMANCE_SMOKE_BACKLOG_ITEM,
  PERFORMANCE_SMOKE_DOC_PATH,
  PERFORMANCE_SMOKE_SURFACES,
  PERFORMANCE_SMOKE_TITLE,
  performanceSmokeSurfaceIds,
  productFilterChipSmoke,
  PROJECT_DATASET_SIZE,
} from "@/features/testing/performanceSmoke";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../../..");

function readRepoFile(relativeFromRepo: string): string {
  return readFileSync(path.join(repoRoot, relativeFromRepo), "utf8");
}

describe("performanceSmoke (item 639 / NFR-003)", () => {
  it("locks KB budget, surfaces, and backlog identity", () => {
    expect(PERFORMANCE_SMOKE_BACKLOG_ITEM).toBe(639);
    expect(PERFORMANCE_SMOKE_TITLE).toBe("Performance Smoke Checks");
    expect(NFR_003_BUDGET_MS).toBe(1_000);
    expect(PROJECT_DATASET_SIZE).toBe(5_000);
    expect(performanceSmokeSurfaceIds()).toEqual([
      "customer-search",
      "product-search",
      "dashboard",
    ]);
    expect(PERFORMANCE_SMOKE_SURFACES).toHaveLength(3);
    expect(PERFORMANCE_SMOKE_SURFACES.every((s) => s.kbRefs.includes("NFR-003"))).toBe(true);
  });

  it("customer search smoke finishes under NFR-003 budget on project dataset", () => {
    const customers = buildProjectCustomerDataset(PROJECT_DATASET_SIZE);
    let hits: ReturnType<typeof filterCustomersSmoke> = [];
    const elapsed = measureMs(() => {
      hits = filterCustomersSmoke(customers, "schmidt");
    });
    expect(hits.length).toBeGreaterThan(0);
    expect(hits.every((c) => c.lastName.toLowerCase().includes("schmidt"))).toBe(true);
    expect(isWithinNfr003Budget(elapsed)).toBe(true);
  });

  it("product search smoke finishes under NFR-003 budget on project dataset", () => {
    const products = buildProjectProductDataset(PROJECT_DATASET_SIZE);
    let hits: ReturnType<typeof filterProductsSmoke> = [];
    const elapsed = measureMs(() => {
      hits = filterProductsSmoke(products, {
        term: "life",
        productType: "LIFE_INSURANCE",
        active: "true",
      });
    });
    expect(hits.length).toBeGreaterThan(0);
    expect(
      hits.every((p) => p.productType === "LIFE_INSURANCE" && p.active && /life/i.test(p.name)),
    ).toBe(true);
    expect(productFilterChipSmoke({ term: " life ", productType: "LIFE_INSURANCE", active: "true" })).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ key: "term" }),
        expect.objectContaining({ key: "productType" }),
      ]),
    );
    expect(isWithinNfr003Budget(elapsed)).toBe(true);
  });

  it("dashboard KPI model smoke finishes under NFR-003 budget", () => {
    let groups: ReturnType<typeof dashboardKpiModelSmoke> = [];
    const elapsed = measureMs(() => {
      groups = dashboardKpiModelSmoke();
    });
    expect(groups.length).toBeGreaterThanOrEqual(2);
    expect(groups.some((g) => g.id === "inventory-delivery")).toBe(true);
    expect(isWithinNfr003Budget(elapsed)).toBe(true);
  });

  it("keeps performance smoke markdown as delivery evidence", () => {
    const docPath = path.join(repoRoot, PERFORMANCE_SMOKE_DOC_PATH);
    expect(existsSync(docPath), `Missing ${PERFORMANCE_SMOKE_DOC_PATH}`).toBe(true);
    const documentation = readRepoFile(PERFORMANCE_SMOKE_DOC_PATH);
    expect(documentationContainsRequiredSnippets(documentation)).toBe(true);
    expect(documentation).toContain("customer-search");
    expect(documentation).toContain("product-search");
    expect(documentation).toContain("dashboard");
  });

  it("links performance smoke from the documentation index", () => {
    const index = readRepoFile("docs/README.md");
    expect(docsIndexMustLinkPerformanceSmoke(index)).toBe(true);
  });
});
