import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  BACKEND_CRITICAL_TEST_CLASS,
  CUSTOMER_MODULE_DOC_PATH,
  SOFT_DELETED_CUSTOMERS_DO_NOT_APPEAR_IN_ACTIVE_LISTS_FR,
  SOFT_DELETED_CUSTOMERS_DO_NOT_APPEAR_IN_ACTIVE_LISTS_ITEM,
  SOFT_DELETED_CUSTOMERS_DO_NOT_APPEAR_IN_ACTIVE_LISTS_STATEMENT,
  SOFT_DELETE_FIELD,
  activeListContainsOnlyNonDeleted,
  filterActiveCustomersForList,
  isSoftDeletedCustomer,
} from "@/features/customers/softDeletedCustomersDoNotAppearInActiveLists";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../../..");

function readRepoFile(relativeFromRepo: string): string {
  return readFileSync(path.join(repoRoot, relativeFromRepo), "utf8");
}

describe("softDeletedCustomersDoNotAppearInActiveLists (item 657)", () => {
  it("locks the critical KB rule identity", () => {
    expect(SOFT_DELETED_CUSTOMERS_DO_NOT_APPEAR_IN_ACTIVE_LISTS_ITEM).toBe(657);
    expect(SOFT_DELETED_CUSTOMERS_DO_NOT_APPEAR_IN_ACTIVE_LISTS_STATEMENT).toBe(
      "Soft-deleted customers do not appear in active lists",
    );
    expect(SOFT_DELETED_CUSTOMERS_DO_NOT_APPEAR_IN_ACTIVE_LISTS_FR).toEqual([
      "FR-010",
      "FR-013",
    ]);
    expect(SOFT_DELETE_FIELD).toBe("deletedAt");
    expect(BACKEND_CRITICAL_TEST_CLASS).toContain(
      "SoftDeletedCustomersDoNotAppearInActiveListsTests",
    );
  });

  it("filters soft-deleted customers out of active list models", () => {
    const active = { id: "1", fullName: "Ada Active", deletedAt: null, active: true };
    const deleted = {
      id: "2",
      fullName: "Ben Deleted",
      deletedAt: "2026-07-12T12:00:00Z",
      active: false,
      deleted: true,
    };

    expect(isSoftDeletedCustomer(active)).toBe(false);
    expect(isSoftDeletedCustomer(deleted)).toBe(true);
    expect(filterActiveCustomersForList([active, deleted])).toEqual([active]);
    expect(activeListContainsOnlyNonDeleted([active])).toBe(true);
    expect(activeListContainsOnlyNonDeleted([active, deleted])).toBe(false);
  });

  it("documents soft-delete exclusion from active lists in customer module docs", () => {
    const docPath = path.join(repoRoot, CUSTOMER_MODULE_DOC_PATH);
    expect(existsSync(docPath)).toBe(true);
    const documentation = readRepoFile(CUSTOMER_MODULE_DOC_PATH);
    expect(documentation).toContain("657");
    expect(documentation).toContain("SoftDeletedCustomersDoNotAppearInActiveListsTests");
    expect(documentation).toMatch(/soft-delete|deletedAt/i);
    expect(documentation).toMatch(/active lists|search results|Soft-deleted customers are excluded/i);
  });
});
