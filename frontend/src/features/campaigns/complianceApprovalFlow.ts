/**
 * Compliance approval UI flow (KB FR-059 / BR-005 / COMP-006 / item 604).
 *
 * Acceptance contract for approving SUBMITTED campaigns through the Compliance
 * review screen. Checklist and decision copy live in {@link complianceReviewClarity}.
 */

import type { SystemRoleName } from "@/auth/sessionStorageStrategy";
import {
  COMPLIANCE_CHECKLIST,
  COMPLIANCE_REVIEW_GATE_NOTE,
  COMPLIANCE_REVIEW_PAGE_LEAD,
  complianceDecisionConfirmLabel,
  complianceDecisionConfirmTitle,
  complianceDecisionOutcome,
  formatPendingQueueLabel,
  type ComplianceDecisionKind,
} from "@/features/campaigns/complianceReviewClarity";
import {
  campaignFormValidationMessages,
  hasCampaignFormErrors,
  validateRejectCampaignForm,
} from "@/features/campaigns/campaignFormValidation";

/** Roles that may approve/reject campaigns through the UI (matches CAMPAIGN_REVIEW_ROLES). */
export const COMPLIANCE_APPROVE_UI_ROLES: SystemRoleName[] = ["ADMIN", "COMPLIANCE_OFFICER"];

export const COMPLIANCE_REVIEW_PAGE_TITLE = "Compliance review";
export const COMPLIANCE_QUEUE_TABLE_ARIA_LABEL = "Submitted campaigns table";
export const COMPLIANCE_APPROVE_BUTTON_LABEL = "Approve campaign";
export const COMPLIANCE_REJECT_BUTTON_LABEL = "Reject campaign";
export const COMPLIANCE_REVIEW_NOTES_LABEL = "Compliance review notes";
export const COMPLIANCE_APPROVED_NOTICE = "Campaign approved.";
export const COMPLIANCE_REJECTED_NOTICE = "Campaign rejected.";

export {
  COMPLIANCE_CHECKLIST,
  COMPLIANCE_REVIEW_GATE_NOTE,
  COMPLIANCE_REVIEW_PAGE_LEAD,
  campaignFormValidationMessages,
  complianceDecisionConfirmLabel,
  complianceDecisionConfirmTitle,
  complianceDecisionOutcome,
  formatPendingQueueLabel,
};

export type ComplianceApprovalStepId =
  | "open-compliance"
  | "select-submitted-campaign"
  | "confirm-approve"
  | "see-approved";

export type ComplianceApprovalStepDefinition = {
  id: ComplianceApprovalStepId;
  index: number;
  title: string;
  description: string;
};

/** UI acceptance steps for “Compliance approval works through UI” (item 604). */
export const COMPLIANCE_APPROVAL_FLOW_STEPS: ComplianceApprovalStepDefinition[] = [
  {
    id: "open-compliance",
    index: 0,
    title: "Open Compliance review",
    description: "Compliance Officer or Admin opens /compliance with the SUBMITTED queue.",
  },
  {
    id: "select-submitted-campaign",
    index: 1,
    title: "Select submitted campaign",
    description: "Review checklist evidence, message, audience, and recipient summary.",
  },
  {
    id: "confirm-approve",
    index: 2,
    title: "Confirm approval",
    description: "Optional notes, Approve campaign, then Confirm approval dialog.",
  },
  {
    id: "see-approved",
    index: 3,
    title: "See approved result",
    description: "POST /api/campaigns/{id}/approve; notice and queue refresh (status APPROVED).",
  },
];

/** Deterministic fixtures for Playwright / integration compliance approval. */
export const COMPLIANCE_APPROVAL_FIXTURES = {
  campaignId: "50000000-0000-0000-0000-00000000c604",
  campaignName: "UI Compliance Approval Outreach",
  objective: "Validate compliance approval through the UI",
  ownerFullName: "Campaign Manager",
  segmentName: "E2E Eligible Audience",
  channel: "EMAIL",
  messageSubject: "Compliance review subject",
  messageBody: "Controlled compliance approval message body.",
  reviewNotes: "Approved in UI compliance approval flow",
  rejectionReason: "Missing consent language for the channel",
  eligible: 9,
  excluded: 2,
} as const;

export function canApproveCampaignsThroughUi(roles: readonly SystemRoleName[]): boolean {
  return roles.some((role) => COMPLIANCE_APPROVE_UI_ROLES.includes(role));
}

/**
 * Approve requires a selected SUBMITTED campaign; notes are optional.
 */
export function canRequestApproval(options: {
  campaignId: string | null | undefined;
  campaignStatus: string | null | undefined;
}): boolean {
  return (
    options.campaignId != null &&
    options.campaignId.trim().length > 0 &&
    options.campaignStatus === "SUBMITTED"
  );
}

/**
 * Reject requires a formal reason before the confirmation dialog opens (item 242).
 */
export function canRequestRejection(rejectionReason: string): boolean {
  return !hasCampaignFormErrors(
    validateRejectCampaignForm({
      rejectionReason,
      complianceReviewNotes: "",
    }),
  );
}

export function complianceApprovalConfirmCopy(kind: ComplianceDecisionKind = "approve") {
  return {
    title: complianceDecisionConfirmTitle(kind),
    label: complianceDecisionConfirmLabel(kind),
    outcome: complianceDecisionOutcome(kind),
  };
}

export function complianceApprovalStepIdsInOrder(): ComplianceApprovalStepId[] {
  return [...COMPLIANCE_APPROVAL_FLOW_STEPS]
    .sort((left, right) => left.index - right.index)
    .map((step) => step.id);
}

export function formatComplianceApprovalJourney(
  steps: readonly ComplianceApprovalStepDefinition[] = COMPLIANCE_APPROVAL_FLOW_STEPS,
): string {
  return steps
    .slice()
    .sort((left, right) => left.index - right.index)
    .map((step) => step.title)
    .join(" → ");
}

export function isValidComplianceApprovalOrder(
  observed: readonly ComplianceApprovalStepId[],
): boolean {
  const expected = complianceApprovalStepIdsInOrder();
  if (observed.length !== expected.length) {
    return false;
  }
  return observed.every((stepId, index) => stepId === expected[index]);
}
