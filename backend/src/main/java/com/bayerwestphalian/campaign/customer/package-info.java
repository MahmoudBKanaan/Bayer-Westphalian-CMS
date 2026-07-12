/**
 * Customer and prospect package for profile data, search, pagination, validation, and soft-delete
 * workflows (KB epic E07 / FR-010–FR-020).
 *
 * <p>Sensitive actions are audited through {@link
 * com.bayerwestphalian.campaign.audit.AuditService}:
 *
 * <ul>
 *   <li>Create / update profile changes on entity type {@code customers}
 *   <li>Item 523 — soft delete ({@code DELETE} with before/after {@code deleted} flags)
 *   <li>Item 526 — do-not-contact preference changes ({@code UPDATE_DO_NOT_CONTACT} via {@code
 *       AuditService.logDoNotContactUpdate} when the flag changes; also on create when DNC is
 *       initially true)
 * </ul>
 *
 * <p>Customer soft delete sets {@code deletedAt}; permanent hard delete is not part of the MVP.
 * Audit log reads are exposed under {@code /api/audit-logs} for Admin, Compliance Officer, and
 * System Auditor.
 */
package com.bayerwestphalian.campaign.customer;
