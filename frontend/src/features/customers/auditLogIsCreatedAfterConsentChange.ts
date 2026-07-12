/**
 * Sprint 16 critical test item **658**: Audit log is created after consent change.
 *
 * KB: NFR-008 (auditability), FR-033 (record consent), COMP-001 / item 524–525.
 * Successful consent record / withdraw write immutable rows on entity type `consent_records`.
 */

export const AUDIT_LOG_IS_CREATED_AFTER_CONSENT_CHANGE_ITEM = 658;

export const AUDIT_LOG_IS_CREATED_AFTER_CONSENT_CHANGE_STATEMENT =
  "Audit log is created after consent change";

export const AUDIT_LOG_IS_CREATED_AFTER_CONSENT_CHANGE_NFR = ["NFR-008"] as const;

export const AUDIT_LOG_IS_CREATED_AFTER_CONSENT_CHANGE_FR = ["FR-033"] as const;

/** Backend audit entity type for consent history. */
export const CONSENT_AUDIT_ENTITY_TYPE = "consent_records" as const;

/** Actions written on successful consent lifecycle changes. */
export const CONSENT_AUDIT_ACTIONS = [
  "CREATE",
  "WITHDRAW_CONSENT",
  "OPT_OUT",
] as const;

export type ConsentAuditAction = (typeof CONSENT_AUDIT_ACTIONS)[number];

export const BACKEND_CRITICAL_TEST_CLASS =
  "com.bayerwestphalian.campaign.consent.AuditLogIsCreatedAfterConsentChangeTests";

export const COMPANION_CONSENT_AUDIT_TEST_CLASS =
  "com.bayerwestphalian.campaign.consent.ConsentChangeCreatesAuditLogTests";

export const CONSENT_MODULE_DOC_PATH = "docs/modules/consent-module.md";

export const AUDIT_LOGGING_DOC_PATH = "docs/modules/audit-logging.md";

/**
 * Consent mutations that must produce an audit trail after success.
 */
export const CONSENT_CHANGE_OPERATIONS = [
  "record-consent",
  "withdraw-consent",
] as const;

export type ConsentChangeOperation = (typeof CONSENT_CHANGE_OPERATIONS)[number];

/**
 * Expected primary audit action for a successful consent change operation.
 * Marketing opt-outs may also emit OPT_OUT in addition to CREATE / WITHDRAW_CONSENT.
 */
export function primaryAuditActionForConsentChange(
  operation: ConsentChangeOperation,
): Extract<ConsentAuditAction, "CREATE" | "WITHDRAW_CONSENT"> {
  if (operation === "record-consent") {
    return "CREATE";
  }
  return "WITHDRAW_CONSENT";
}

export type ConsentAuditLogLike = {
  action?: string | null;
  entityType?: string | null;
  entityId?: string | null;
  actorUserId?: string | null;
};

/**
 * True when a log row is a valid post-consent-change audit entry for compliance review.
 */
export function isConsentChangeAuditLog(
  log: ConsentAuditLogLike | null | undefined,
): boolean {
  if (log == null) {
    return false;
  }
  if (log.entityType !== CONSENT_AUDIT_ENTITY_TYPE) {
    return false;
  }
  if (log.action == null || !CONSENT_AUDIT_ACTIONS.includes(log.action as ConsentAuditAction)) {
    return false;
  }
  if (log.entityId == null || log.entityId === "") {
    return false;
  }
  return true;
}

/**
 * True when at least one audit row evidences a successful consent change.
 */
export function consentChangeProducedAuditLog(
  logs: readonly ConsentAuditLogLike[],
  options?: { operation?: ConsentChangeOperation; entityId?: string },
): boolean {
  const expectedPrimary =
    options?.operation != null
      ? primaryAuditActionForConsentChange(options.operation)
      : null;

  return logs.some((log) => {
    if (!isConsentChangeAuditLog(log)) {
      return false;
    }
    if (options?.entityId != null && log.entityId !== options.entityId) {
      return false;
    }
    if (expectedPrimary != null && log.action !== expectedPrimary && log.action !== "OPT_OUT") {
      // Allow OPT_OUT companion rows; primary CREATE/WITHDRAW must also appear for full proof.
      return false;
    }
    if (expectedPrimary != null) {
      return (
        logs.some(
          (row) =>
            isConsentChangeAuditLog(row) &&
            row.action === expectedPrimary &&
            (options?.entityId == null || row.entityId === options.entityId),
        ) || log.action === expectedPrimary
      );
    }
    return true;
  });
}
