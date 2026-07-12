/**
 * Sprint 16 critical test item **657**: Soft-deleted customers do not appear in active lists.
 *
 * KB: FR-010 (active lists), FR-013 (soft delete). Backend excludes deletedAt != null; UI lists
 * must not present soft-deleted rows as active profiles.
 */

export const SOFT_DELETED_CUSTOMERS_DO_NOT_APPEAR_IN_ACTIVE_LISTS_ITEM = 657;

export const SOFT_DELETED_CUSTOMERS_DO_NOT_APPEAR_IN_ACTIVE_LISTS_STATEMENT =
  "Soft-deleted customers do not appear in active lists";

export const SOFT_DELETED_CUSTOMERS_DO_NOT_APPEAR_IN_ACTIVE_LISTS_FR = [
  "FR-010",
  "FR-013",
] as const;

export const SOFT_DELETE_FIELD = "deletedAt" as const;

export const BACKEND_CRITICAL_TEST_CLASS =
  "com.bayerwestphalian.campaign.customer.SoftDeletedCustomersDoNotAppearInActiveListsTests";

export const CUSTOMER_MODULE_DOC_PATH = "docs/modules/customer-module.md";

export type SoftDeleteAwareCustomer = {
  id: string;
  fullName?: string;
  deletedAt?: string | null;
  deleted?: boolean | null;
  active?: boolean | null;
};

/**
 * True when a customer row is soft-deleted and must be omitted from active UI lists.
 */
export function isSoftDeletedCustomer(
  customer: SoftDeleteAwareCustomer | null | undefined,
): boolean {
  if (customer == null) {
    return false;
  }
  if (customer.deletedAt != null && customer.deletedAt !== "") {
    return true;
  }
  if (customer.deleted === true) {
    return true;
  }
  if (customer.active === false && customer.deletedAt != null) {
    return true;
  }
  return false;
}

/**
 * Filters API/list payloads so soft-deleted customers never render as active rows.
 * Backend should already exclude them; this is defense-in-depth for UI contracts.
 */
export function filterActiveCustomersForList<T extends SoftDeleteAwareCustomer>(
  customers: readonly T[],
): T[] {
  return customers.filter((customer) => !isSoftDeletedCustomer(customer));
}

export function activeListContainsOnlyNonDeleted(
  customers: readonly SoftDeleteAwareCustomer[],
): boolean {
  return customers.every((customer) => !isSoftDeletedCustomer(customer));
}
