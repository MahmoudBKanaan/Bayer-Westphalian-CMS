/**
 * Compliance review clarity helpers (KB item 593 / FR-059 / BR-005 / COMP-006).
 *
 * Structures what Compliance Officers must check before approve/reject so the UI
 * presents a clear decision path instead of a flat form.
 */

export type ComplianceChecklistItem = {
  id: string;
  title: string;
  description: string;
};

export const COMPLIANCE_REVIEW_PAGE_LEAD =
  "Review submitted campaigns before they can launch. Confirm messaging, audience eligibility, consent posture, and schedule, then approve or reject with a clear reason.";

export const COMPLIANCE_REVIEW_GATE_NOTE =
  "Campaigns cannot launch until a Compliance Officer or Admin approves them (BR-005). Rejection returns the campaign to the owner with a required formal reason.";

export const COMPLIANCE_CHECKLIST: ComplianceChecklistItem[] = [
  {
    id: "message",
    title: "Message content",
    description: "Subject and body are accurate, non-misleading, and suitable for the channel.",
  },
  {
    id: "audience",
    title: "Audience & segment",
    description: "Target segment matches the campaign objective; owner is not the reviewer.",
  },
  {
    id: "eligibility",
    title: "Eligibility & exclusions",
    description:
      "Recipient preview shows eligible/excluded counts; opt-outs, do-not-contact, and consent rules are respected.",
  },
  {
    id: "products",
    title: "Promoted products",
    description: "Product selection fits the offer and does not conflict with known policy limits.",
  },
  {
    id: "schedule",
    title: "Schedule window",
    description: "Start and end dates are coherent and appropriate for controlled outreach.",
  },
  {
    id: "decision-record",
    title: "Decision record",
    description:
      "Approve with optional notes, or reject with a required formal reason so the manager can revise.",
  },
];

export type ComplianceDecisionKind = "approve" | "reject";

export function complianceDecisionOutcome(kind: ComplianceDecisionKind): string {
  if (kind === "approve") {
    return "Status becomes APPROVED. Campaign Managers may then launch the campaign.";
  }
  return "Status becomes REJECTED. Campaign Managers must revise and resubmit before another review.";
}

export function complianceDecisionConfirmTitle(kind: ComplianceDecisionKind): string {
  return kind === "approve" ? "Confirm campaign approval" : "Confirm campaign rejection";
}

export function complianceDecisionConfirmLabel(kind: ComplianceDecisionKind): string {
  return kind === "approve" ? "Confirm approval" : "Confirm rejection";
}

/**
 * Human-readable queue label for the review queue summary.
 */
export function formatPendingQueueLabel(count: number): string {
  if (count === 1) {
    return "1 campaign waiting for a compliance decision";
  }
  return `${count} campaigns waiting for a compliance decision`;
}

export function recipientPreviewPath(campaignId: string): string {
  return `/campaigns/${campaignId}/recipients/preview`;
}
