/**
 * User management package for employee accounts, role assignment, disabling users, and access
 * administration (KB FR-005 / E06).
 *
 * <p>Sensitive actions are audited through {@link
 * com.bayerwestphalian.campaign.audit.AuditService}:
 *
 * <ul>
 *   <li>Item 520 — user creation ({@code CREATE} / entity type {@code users})
 *   <li>Item 521 — role changes ({@code ASSIGN_ROLE} / {@code AuditService.logRoleChange} with
 *       before/after role sets)
 *   <li>Item 522 — user disable ({@code DISABLE_USER})
 * </ul>
 *
 * <p>Password hashes and raw passwords are never included in audit payloads. Audit log reads are
 * exposed under {@code /api/audit-logs} (items 518–519) for Admin, Compliance Officer, and System
 * Auditor.
 */
package com.bayerwestphalian.campaign.user;
