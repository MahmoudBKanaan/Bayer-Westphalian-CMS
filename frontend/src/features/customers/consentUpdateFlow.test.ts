import { describe, expect, it } from "vitest";
import {
  buildOptOutPayload,
  buildRecordConsentPayload,
  canUseConsentUi,
  CONSENT_RECORDED_NOTICE,
  CONSENT_UPDATE_FIXTURES,
  CONSENT_UI_ROLES,
  consentFormValidationMessages,
  consentUpdateStepIdsInOrder,
  emptyConsentRecordForm,
  formatConsentUpdateJourney,
  hasConsentFormErrors,
  isValidConsentUpdateOrder,
  MARKETING_OPT_OUT_PURPOSE,
  validateConsentRecordForm,
} from "@/features/customers/consentUpdateFlow";

describe("consentUpdateFlow (item 600)", () => {
  it("documents the UI consent update journey", () => {
    expect(consentUpdateStepIdsInOrder()).toEqual([
      "open-customer-details",
      "open-consent-tab",
      "submit-consent-change",
      "see-updated-consent",
    ]);
    expect(formatConsentUpdateJourney()).toBe(
      "Open customer details → Open Consent panel → Submit consent change → See updated consent",
    );
    expect(isValidConsentUpdateOrder(consentUpdateStepIdsInOrder())).toBe(true);
    expect(isValidConsentUpdateOrder(["submit-consent-change"] as never)).toBe(false);
  });

  it("allows consent-capable roles on the Consent tab", () => {
    expect(CONSENT_UI_ROLES).toContain("ADMIN");
    expect(CONSENT_UI_ROLES).toContain("CUSTOMER_SERVICE_AGENT");
    expect(canUseConsentUi(["ADMIN"])).toBe(true);
    expect(canUseConsentUi(["CUSTOMER_SERVICE_AGENT"])).toBe(true);
    expect(canUseConsentUi(["BI_ANALYST"])).toBe(false);
    expect(canUseConsentUi(["EXECUTIVE_VIEWER"])).toBe(false);
  });

  it("requires purpose before recording consent", () => {
    const errors = validateConsentRecordForm(emptyConsentRecordForm(), "customer-1");
    expect(errors.purpose).toBe(consentFormValidationMessages.purposeRequired);
    expect(hasConsentFormErrors(errors)).toBe(true);
  });

  it("accepts a well-formed record-consent form", () => {
    const errors = validateConsentRecordForm(
      {
        consentType: CONSENT_UPDATE_FIXTURES.consentType,
        status: CONSENT_UPDATE_FIXTURES.status,
        purpose: CONSENT_UPDATE_FIXTURES.purpose,
        source: CONSENT_UPDATE_FIXTURES.source,
        evidenceFileUrl: CONSENT_UPDATE_FIXTURES.evidenceFileUrl,
      },
      CONSENT_UPDATE_FIXTURES.customerId,
    );
    expect(errors).toEqual({});
    expect(hasConsentFormErrors(errors)).toBe(false);
  });

  it("builds record and opt-out payloads for the consents API", () => {
    expect(
      buildRecordConsentPayload(CONSENT_UPDATE_FIXTURES.customerId, {
        consentType: "GUARDIAN",
        status: "REQUIRED",
        purpose: "  Guardian consent required  ",
        source: "PHONE",
        evidenceFileUrl: "s3://evidence/guardian.pdf",
      }),
    ).toEqual({
      customerId: CONSENT_UPDATE_FIXTURES.customerId,
      consentType: "GUARDIAN",
      status: "REQUIRED",
      purpose: "Guardian consent required",
      source: "PHONE",
      evidenceFileUrl: "s3://evidence/guardian.pdf",
    });

    expect(
      buildOptOutPayload(CONSENT_UPDATE_FIXTURES.customerId, {
        consentType: "MARKETING_SMS",
        source: " PHONE ",
        evidenceFileUrl: "",
      }),
    ).toEqual({
      customerId: CONSENT_UPDATE_FIXTURES.customerId,
      consentType: "MARKETING_SMS",
      source: "PHONE",
      evidenceFileUrl: null,
    });
    expect(MARKETING_OPT_OUT_PURPOSE).toBe("Marketing opt-out");
    expect(CONSENT_RECORDED_NOTICE).toBe("Consent recorded.");
  });

  it("rejects missing customer id and overlong purpose", () => {
    const errors = validateConsentRecordForm(
      {
        ...emptyConsentRecordForm(),
        purpose: "A".repeat(256),
      },
      "",
    );
    expect(errors.customerId).toBe(consentFormValidationMessages.customerIdRequired);
    expect(errors.purpose).toBe(consentFormValidationMessages.purposeMaxLength);
  });
});
