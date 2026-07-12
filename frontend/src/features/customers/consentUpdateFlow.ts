/**
 * Consent update UI flow (KB FR-018 / BR-004 / COMP consent rules / item 600).
 *
 * Pure rules for recording consent, marketing opt-out, and withdrawal on the
 * customer details Consent tab.
 */

import type {
  ConsentStatus,
  ConsentType,
  RecordConsentPayload,
  RecordOptOutPayload,
} from "@/api/consents";
import type { SystemRoleName } from "@/auth/sessionStorageStrategy";

/** Roles that can open the Consent tab (matches CONSENT_READ_ROLES). */
export const CONSENT_UI_ROLES: SystemRoleName[] = [
  "ADMIN",
  "CAMPAIGN_MANAGER",
  "COMPLIANCE_OFFICER",
  "CUSTOMER_SERVICE_AGENT",
  "SYSTEM_AUDITOR",
];

export const CONSENT_SECTION_HEADING = "Consent";
export const CONSENT_RECORDS_TABLE_ARIA_LABEL = "Consent records table";
export const CONSENT_RECORD_FORM_ARIA_LABEL = "Record consent";
export const CONSENT_OPT_OUT_FORM_ARIA_LABEL = "Mark opt-out";
export const CONSENT_RECORD_SUBMIT_LABEL = "Record consent";
export const CONSENT_OPT_OUT_SUBMIT_LABEL = "Mark opt-out";
export const CONSENT_WITHDRAW_SUBMIT_LABEL = "Withdraw";

export const CONSENT_RECORDED_NOTICE = "Consent recorded.";
export const CONSENT_OPT_OUT_NOTICE = "Marketing opt-out recorded.";
export const CONSENT_WITHDRAWN_NOTICE = "Consent withdrawn.";
export const CONSENT_EMPTY_STATE = "No consent records are available for this customer.";

export const consentFormValidationMessages = {
  purposeRequired: "Purpose is required.",
  purposeMaxLength: "Purpose must be 255 characters or fewer.",
  sourceMaxLength: "Source must be 100 characters or fewer.",
  customerIdRequired: "Customer is required.",
} as const;

export const CONSENT_TYPES: ConsentType[] = [
  "MARKETING_EMAIL",
  "MARKETING_PHONE",
  "MARKETING_SMS",
  "GUARDIAN",
  "DATA_PROCESSING",
];

export const CONSENT_STATUSES: ConsentStatus[] = [
  "GIVEN",
  "REQUIRED",
  "WITHDRAWN",
  "EXPIRED",
  "REJECTED",
];

export const MARKETING_CONSENT_TYPES = [
  "MARKETING_EMAIL",
  "MARKETING_PHONE",
  "MARKETING_SMS",
] as const;

export type MarketingConsentType = (typeof MARKETING_CONSENT_TYPES)[number];

export type ConsentRecordFormValues = {
  consentType: ConsentType;
  status: ConsentStatus;
  purpose: string;
  source: string;
  evidenceFileUrl: string;
};

export type ConsentOptOutFormValues = {
  consentType: MarketingConsentType;
  source: string;
  evidenceFileUrl: string;
};

export type ConsentFormErrors = {
  purpose?: string;
  source?: string;
  customerId?: string;
};

export type ConsentUpdateStepId =
  | "open-customer-details"
  | "open-consent-tab"
  | "submit-consent-change"
  | "see-updated-consent";

export type ConsentUpdateStepDefinition = {
  id: ConsentUpdateStepId;
  index: number;
  title: string;
  description: string;
};

/** UI acceptance steps for “Consent update works through UI” (item 600). */
export const CONSENT_UPDATE_FLOW_STEPS: ConsentUpdateStepDefinition[] = [
  {
    id: "open-customer-details",
    index: 0,
    title: "Open customer details",
    description: "Authorized employee opens /customers/:id to manage the profile.",
  },
  {
    id: "open-consent-tab",
    index: 1,
    title: "Open Consent panel",
    description: "Consent tab shows records, do-not-contact, record form, and opt-out form.",
  },
  {
    id: "submit-consent-change",
    index: 2,
    title: "Submit consent change",
    description: "Record consent, mark opt-out, or withdraw; client validation then REST call.",
  },
  {
    id: "see-updated-consent",
    index: 3,
    title: "See updated consent",
    description: "Success notice and refreshed consent list reflect the new status.",
  },
];

/** Deterministic fixtures for Playwright / integration consent updates. */
export const CONSENT_UPDATE_FIXTURES = {
  purpose: "UI consent update marketing email",
  source: "UI_CONSENT_UPDATE",
  evidenceFileUrl: "s3://evidence/ui-consent-update.pdf",
  consentType: "MARKETING_EMAIL" as ConsentType,
  status: "GIVEN" as ConsentStatus,
  optOutChannel: "MARKETING_SMS" as MarketingConsentType,
  optOutSource: "PHONE",
  optOutEvidenceUrl: "s3://evidence/ui-sms-opt-out.pdf",
  consentRecordId: "61000000-0000-0000-0000-00000000c600",
  customerId: "20000000-0000-0000-0000-00000000c199",
} as const;

export function emptyConsentRecordForm(): ConsentRecordFormValues {
  return {
    consentType: "MARKETING_EMAIL",
    status: "GIVEN",
    purpose: "",
    source: "",
    evidenceFileUrl: "",
  };
}

export function emptyConsentOptOutForm(): ConsentOptOutFormValues {
  return {
    consentType: "MARKETING_EMAIL",
    source: "",
    evidenceFileUrl: "",
  };
}

/**
 * Validates the record-consent form before POST /api/consents (BR-004 purpose required).
 */
export function validateConsentRecordForm(
  values: ConsentRecordFormValues,
  customerId: string,
): ConsentFormErrors {
  const errors: ConsentFormErrors = {};
  if (customerId.trim().length === 0) {
    errors.customerId = consentFormValidationMessages.customerIdRequired;
  }
  const purpose = values.purpose.trim();
  if (purpose.length === 0) {
    errors.purpose = consentFormValidationMessages.purposeRequired;
  } else if (values.purpose.length > 255) {
    errors.purpose = consentFormValidationMessages.purposeMaxLength;
  }
  if (values.source.length > 100) {
    errors.source = consentFormValidationMessages.sourceMaxLength;
  }
  return errors;
}

export function hasConsentFormErrors(errors: ConsentFormErrors): boolean {
  return Object.values(errors).some((message) => message != null && message.length > 0);
}

export function buildRecordConsentPayload(
  customerId: string,
  values: ConsentRecordFormValues,
): RecordConsentPayload {
  return {
    customerId,
    consentType: values.consentType,
    status: values.status,
    purpose: values.purpose.trim(),
    source: optionalString(values.source),
    evidenceFileUrl: optionalString(values.evidenceFileUrl),
  };
}

export function buildOptOutPayload(
  customerId: string,
  values: ConsentOptOutFormValues,
): RecordOptOutPayload {
  return {
    customerId,
    consentType: values.consentType,
    source: optionalString(values.source),
    evidenceFileUrl: optionalString(values.evidenceFileUrl),
  };
}

/** Purpose written when marketing opt-out is recorded through the UI. */
export const MARKETING_OPT_OUT_PURPOSE = "Marketing opt-out";

export function canUseConsentUi(roles: readonly SystemRoleName[]): boolean {
  return roles.some((role) => CONSENT_UI_ROLES.includes(role));
}

export function consentUpdateStepIdsInOrder(): ConsentUpdateStepId[] {
  return [...CONSENT_UPDATE_FLOW_STEPS]
    .sort((left, right) => left.index - right.index)
    .map((step) => step.id);
}

export function formatConsentUpdateJourney(
  steps: readonly ConsentUpdateStepDefinition[] = CONSENT_UPDATE_FLOW_STEPS,
): string {
  return steps
    .slice()
    .sort((left, right) => left.index - right.index)
    .map((step) => step.title)
    .join(" → ");
}

export function isValidConsentUpdateOrder(observed: readonly ConsentUpdateStepId[]): boolean {
  const expected = consentUpdateStepIdsInOrder();
  if (observed.length !== expected.length) {
    return false;
  }
  return observed.every((stepId, index) => stepId === expected[index]);
}

function optionalString(value: string): string | null {
  const trimmed = value.trim();
  return trimmed.length === 0 ? null : trimmed;
}
