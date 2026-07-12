import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  ALLOWED_CUSTOMER_UPDATE_ROLES,
  BACKEND_CRITICAL_TEST_CLASS,
  BI_ANALYST_CANNOT_EDIT_CUSTOMERS_FR,
  BI_ANALYST_CANNOT_EDIT_CUSTOMERS_ITEM,
  BI_ANALYST_CANNOT_EDIT_CUSTOMERS_STATEMENT,
  BI_ANALYST_CANNOT_EDIT_CUSTOMERS_TEST_CASES,
  BI_ANALYST_GUIDE_PATH,
  BLOCKED_CUSTOMER_EDIT_ROLE,
  CUSTOMER_MODULE_DOC_PATH,
  CUSTOMER_UPDATE_API_PATH,
  biAnalystCanReadButNotEditCustomers,
  biAnalystCannotMutateCustomers,
  biAnalystCannotUpdateCustomersThroughUi,
  canMutateCustomersThroughUi,
  customerUpdateRolesMatchKbEditors,
} from "@/features/customers/biAnalystCannotEditCustomers";
import {
  CUSTOMER_CREATE_ROLES,
  CUSTOMER_READ_ROLES,
  CUSTOMER_UPDATE_ROLES,
} from "@/features/auth/permissions";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../../..");

function readRepoFile(relativeFromRepo: string): string {
  return readFileSync(path.join(repoRoot, relativeFromRepo), "utf8");
}

describe("biAnalystCannotEditCustomers (item 654)", () => {
  it("locks the critical KB rule identity", () => {
    expect(BI_ANALYST_CANNOT_EDIT_CUSTOMERS_ITEM).toBe(654);
    expect(BI_ANALYST_CANNOT_EDIT_CUSTOMERS_STATEMENT).toBe("BI Analyst cannot edit customers");
    expect(BI_ANALYST_CANNOT_EDIT_CUSTOMERS_TEST_CASES).toEqual(["TC-009"]);
    expect(BI_ANALYST_CANNOT_EDIT_CUSTOMERS_FR).toEqual(["FR-010", "FR-012"]);
    expect(BLOCKED_CUSTOMER_EDIT_ROLE).toBe("BI_ANALYST");
    expect(ALLOWED_CUSTOMER_UPDATE_ROLES).toEqual([
      "ADMIN",
      "CUSTOMER_SERVICE_AGENT",
      "COMPLIANCE_OFFICER",
    ]);
    expect(CUSTOMER_UPDATE_API_PATH).toBe("PUT /api/customers/{id}");
    expect(BACKEND_CRITICAL_TEST_CLASS).toContain("BiAnalystCannotEditCustomersTests");
  });

  it("keeps BI Analyst as customer reader without update/create/import/delete", () => {
    expect(biAnalystCanReadButNotEditCustomers()).toBe(true);
    expect(biAnalystCannotUpdateCustomersThroughUi()).toBe(true);
    expect(biAnalystCannotMutateCustomers()).toBe(true);
    expect(customerUpdateRolesMatchKbEditors()).toBe(true);
    expect(CUSTOMER_READ_ROLES).toContain("BI_ANALYST");
    expect(CUSTOMER_UPDATE_ROLES).not.toContain("BI_ANALYST");
    expect(CUSTOMER_CREATE_ROLES).not.toContain("BI_ANALYST");
    expect(canMutateCustomersThroughUi(["CUSTOMER_SERVICE_AGENT"])).toBe(true);
    expect(canMutateCustomersThroughUi(["BI_ANALYST"])).toBe(false);
  });

  it("documents TC-009 / BI cannot edit customers in module and BI guide", () => {
    const moduleDoc = readRepoFile(CUSTOMER_MODULE_DOC_PATH);
    expect(existsSync(path.join(repoRoot, CUSTOMER_MODULE_DOC_PATH))).toBe(true);
    expect(moduleDoc).toContain("654");
    expect(moduleDoc).toContain("BiAnalystCannotEditCustomersTests");
    expect(moduleDoc).toMatch(/BI_ANALYST|BI Analyst/i);
    expect(moduleDoc).toMatch(/Update customer|CUSTOMER_SERVICE_AGENT/i);

    const biGuide = readRepoFile(BI_ANALYST_GUIDE_PATH);
    expect(existsSync(path.join(repoRoot, BI_ANALYST_GUIDE_PATH))).toBe(true);
    expect(biGuide).toMatch(/cannot.*edit customer|TC-009|654/i);
  });
});
