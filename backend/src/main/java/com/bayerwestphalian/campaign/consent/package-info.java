/**
 * Consent package for marketing consent, opt-outs, guardian consent, and eligibility checks (KB
 * epic E09 / FR-033–FR-034 / COMP-001–COMP-004).
 *
 * <p>Sensitive actions are audited through {@link
 * com.bayerwestphalian.campaign.audit.AuditService} on entity type {@code consent_records}:
 *
 * <ul>
 *   <li>Item 524 / Sprint 16 critical <b>658</b> — consent record and withdrawal ({@code CREATE} /
 *       {@code WITHDRAW_CONSENT}); marketing opt-outs also write {@code OPT_OUT}
 *   <li>Item 525 — marketing opt-out changes ({@code OPT_OUT} via {@code
 *       AuditService.logOptOutChange} for marketing REJECTED/WITHDRAWN)
 * </ul>
 *
 * <p>Audit payloads include consent id, customer id, type, status, purpose, source, grant/withdraw
 * timestamps, expiration, evidence URL, and recorder when available.
 */
package com.bayerwestphalian.campaign.consent;
