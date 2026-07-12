import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  AUDIT_LOG_IS_CREATED_AFTER_CONSENT_CHANGE_FR,
  AUDIT_LOG_IS_CREATED_AFTER_CONSENT_CHANGE_ITEM,
  AUDIT_LOG_IS_CREATED_AFTER_CONSENT_CHANGE_NFR,
  AUDIT_LOG_IS_CREATED_AFTER_CONSENT_CHANGE_STATEMENT,
  AUDIT_LOGGING_DOC_PATH,
  BACKEND_CRITICAL_TEST_CLASS,
  COMPANION_CONSENT_AUDIT_TEST_CLASS,
  CONSENT_AUDIT_ACTIONS,
  CONSENT_AUDIT_ENTITY_TYPE,
  CONSENT_CHANGE_OPERATIONS,
  CONSENT_MODULE_DOC_PATH,
  consentChangeProducedAuditLog,
  isConsentChangeAuditLog,
  primaryAuditActionForConsentChange,
} from "@/features/customers/auditLogIsCreatedAfterConsentChange";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../../..");

function readRepoFile(relativeFromRepo: string): string {
  return readFileSync(path.join(repoRoot, relativeFromRepo), "utf8");
}

describe("auditLogIsCreatedAfterConsentChange (item 658)", () => {
  it("locks the critical KB rule identity", () => {
    expect(AUDIT_LOG_IS_CREATED_AFTER_CONSENT_CHANGE_ITEM).toBe(658);
    expect(AUDIT_LOG_IS_CREATED_AFTER_CONSENT_CHANGE_STATEMENT).toBe(
      "Audit log is created after consent change",
    );
    expect(AUDIT_LOG_IS_CREATED_AFTER_CONSENT_CHANGE_NFR).toEqual(["NFR-008"]);
    expect(AUDIT_LOG_IS_CREATED_AFTER_CONSENT_CHANGE_FR).toEqual(["FR-033"]);
    expect(CONSENT_AUDIT_ENTITY_TYPE).toBe("consent_records");
    expect(CONSENT_AUDIT_ACTIONS).toEqual(["CREATE", "WITHDRAW_CONSENT", "OPT_OUT"]);
    expect(CONSENT_CHANGE_OPERATIONS).toEqual(["record-consent", "withdraw-consent"]);
    expect(BACKEND_CRITICAL_TEST_CLASS).toContain("AuditLogIsCreatedAfterConsentChangeTests");
    expect(COMPANION_CONSENT_AUDIT_TEST_CLASS).toContain("ConsentChangeCreatesAuditLogTests");
  });

  it("maps consent change operations to primary audit actions", () => {
    expect(primaryAuditActionForConsentChange("record-consent")).toBe("CREATE");
    expect(primaryAuditActionForConsentChange("withdraw-consent")).toBe("WITHDRAW_CONSENT");
  });

  it("recognizes valid consent_records audit rows after a consent change", () => {
    const createLog = {
      action: "CREATE",
      entityType: "consent_records",
      entityId: "consent-1",
      actorUserId: "agent-1",
    };
    const withdrawLog = {
      action: "WITHDRAW_CONSENT",
      entityType: "consent_records",
      entityId: "consent-1",
      actorUserId: "agent-1",
    };
    const optOutLog = {
      action: "OPT_OUT",
      entityType: "consent_records",
      entityId: "consent-1",
      actorUserId: "agent-1",
    };
    const wrongEntity = {
      action: "CREATE",
      entityType: "customers",
      entityId: "c-1",
    };

    expect(isConsentChangeAuditLog(createLog)).toBe(true);
    expect(isConsentChangeAuditLog(withdrawLog)).toBe(true);
    expect(isConsentChangeAuditLog(optOutLog)).toBe(true);
    expect(isConsentChangeAuditLog(wrongEntity)).toBe(false);
    expect(isConsentChangeAuditLog(null)).toBe(false);

    expect(
      consentChangeProducedAuditLog([createLog], {
        operation: "record-consent",
        entityId: "consent-1",
      }),
    ).toBe(true);
    expect(
      consentChangeProducedAuditLog([withdrawLog, optOutLog], {
        operation: "withdraw-consent",
        entityId: "consent-1",
      }),
    ).toBe(true);
    expect(
      consentChangeProducedAuditLog([optOutLog], {
        operation: "record-consent",
        entityId: "consent-1",
      }),
    ).toBe(false);
    expect(consentChangeProducedAuditLog([])).toBe(false);
  });

  it("documents consent-change audit in consent and audit module docs", () => {
    const consentDocPath = path.join(repoRoot, CONSENT_MODULE_DOC_PATH);
    const auditDocPath = path.join(repoRoot, AUDIT_LOGGING_DOC_PATH);
    expect(existsSync(consentDocPath)).toBe(true);
    expect(existsSync(auditDocPath)).toBe(true);

    const consentDoc = readRepoFile(CONSENT_MODULE_DOC_PATH);
    expect(consentDoc).toContain("658");
    expect(consentDoc).toContain("AuditLogIsCreatedAfterConsentChangeTests");
    expect(consentDoc).toMatch(/consent_records/);
    expect(consentDoc).toMatch(/CREATE|WITHDRAW_CONSENT/);

    const auditDoc = readRepoFile(AUDIT_LOGGING_DOC_PATH);
    expect(auditDoc).toMatch(/consent_records|Consent/);
    expect(auditDoc).toMatch(/WITHDRAW_CONSENT|CREATE/);
  });
});
