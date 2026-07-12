/**
 * Sprint 16 critical test item **654**: BI Analyst cannot edit customers.
 *
 * KB: TC-009, FR-010 (view), FR-012 (edit by authorized roles only). BI Analyst is a customer
 * reader for analytics context, not a profile editor.
 */

import type { SystemRoleName } from "@/auth/sessionStorageStrategy";
import {
  CUSTOMER_CREATE_ROLES,
  CUSTOMER_DELETE_ROLES,
  CUSTOMER_IMPORT_ROLES,
  CUSTOMER_READ_ROLES,
  CUSTOMER_UPDATE_ROLES,
} from "@/features/auth/permissions";

export const BI_ANALYST_CANNOT_EDIT_CUSTOMERS_ITEM = 654;

export const BI_ANALYST_CANNOT_EDIT_CUSTOMERS_STATEMENT = "BI Analyst cannot edit customers";

export const BI_ANALYST_CANNOT_EDIT_CUSTOMERS_TEST_CASES = ["TC-009"] as const;

export const BI_ANALYST_CANNOT_EDIT_CUSTOMERS_FR = ["FR-010", "FR-012"] as const;

export const BLOCKED_CUSTOMER_EDIT_ROLE: SystemRoleName = "BI_ANALYST";

export const ALLOWED_CUSTOMER_UPDATE_ROLES: SystemRoleName[] = [
  "ADMIN",
  "CUSTOMER_SERVICE_AGENT",
  "COMPLIANCE_OFFICER",
];

export const CUSTOMER_UPDATE_API_PATH = "PUT /api/customers/{id}";

export const BACKEND_CRITICAL_TEST_CLASS =
  "com.bayerwestphalian.campaign.customer.BiAnalystCannotEditCustomersTests";

export const CUSTOMER_MODULE_DOC_PATH = "docs/modules/customer-module.md";

export const BI_ANALYST_GUIDE_PATH = "docs/user-guides/bi-analyst-guide.md";

export function biAnalystCanReadButNotEditCustomers(): boolean {
  return (
    CUSTOMER_READ_ROLES.includes("BI_ANALYST") &&
    !CUSTOMER_UPDATE_ROLES.includes("BI_ANALYST") &&
    !CUSTOMER_CREATE_ROLES.includes("BI_ANALYST") &&
    !CUSTOMER_DELETE_ROLES.includes("BI_ANALYST") &&
    !CUSTOMER_IMPORT_ROLES.includes("BI_ANALYST")
  );
}

export function biAnalystCannotUpdateCustomersThroughUi(): boolean {
  return !CUSTOMER_UPDATE_ROLES.includes(BLOCKED_CUSTOMER_EDIT_ROLE);
}

export function customerUpdateRolesMatchKbEditors(): boolean {
  const allowed = new Set(ALLOWED_CUSTOMER_UPDATE_ROLES);
  return (
    CUSTOMER_UPDATE_ROLES.length === allowed.size &&
    CUSTOMER_UPDATE_ROLES.every((r) => allowed.has(r))
  );
}

/** True when roles allow customer profile mutation (create/update/delete/import). */
export function canMutateCustomersThroughUi(roles: readonly SystemRoleName[]): boolean {
  return roles.some(
    (role) =>
      CUSTOMER_CREATE_ROLES.includes(role) ||
      CUSTOMER_UPDATE_ROLES.includes(role) ||
      CUSTOMER_DELETE_ROLES.includes(role) ||
      CUSTOMER_IMPORT_ROLES.includes(role),
  );
}

export function biAnalystCannotMutateCustomers(): boolean {
  return !canMutateCustomersThroughUi(["BI_ANALYST"]);
}
