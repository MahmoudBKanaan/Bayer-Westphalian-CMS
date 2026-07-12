import { describe, expect, it } from "vitest";
import {
  COMPLIANCE_CHECKLIST,
  COMPLIANCE_REVIEW_GATE_NOTE,
  COMPLIANCE_REVIEW_PAGE_LEAD,
  complianceDecisionConfirmLabel,
  complianceDecisionConfirmTitle,
  complianceDecisionOutcome,
  formatPendingQueueLabel,
  recipientPreviewPath,
} from "@/features/campaigns/complianceReviewClarity";

describe("complianceReviewClarity (item 593)", () => {
  it("documents a six-point review checklist covering message through decision record", () => {
    expect(COMPLIANCE_CHECKLIST).toHaveLength(6);
    expect(COMPLIANCE_CHECKLIST.map((item) => item.id)).toEqual([
      "message",
      "audience",
      "eligibility",
      "products",
      "schedule",
      "decision-record",
    ]);
    expect(COMPLIANCE_REVIEW_PAGE_LEAD.toLowerCase()).toContain("approve");
    expect(COMPLIANCE_REVIEW_GATE_NOTE).toContain("BR-005");
  });

  it("explains approve vs reject outcomes for reviewers", () => {
    expect(complianceDecisionOutcome("approve")).toContain("APPROVED");
    expect(complianceDecisionOutcome("approve").toLowerCase()).toContain("launch");
    expect(complianceDecisionOutcome("reject")).toContain("REJECTED");
    expect(complianceDecisionOutcome("reject").toLowerCase()).toContain("resubmit");
    expect(complianceDecisionConfirmTitle("approve")).toMatch(/approval/i);
    expect(complianceDecisionConfirmLabel("reject")).toMatch(/rejection/i);
  });

  it("formats queue and recipient preview paths clearly", () => {
    expect(formatPendingQueueLabel(0)).toBe("0 campaigns waiting for a compliance decision");
    expect(formatPendingQueueLabel(1)).toBe("1 campaign waiting for a compliance decision");
    expect(formatPendingQueueLabel(3)).toContain("3 campaigns");
    expect(recipientPreviewPath("50000000-0000-0000-0000-000000000002")).toBe(
      "/campaigns/50000000-0000-0000-0000-000000000002/recipients/preview",
    );
  });
});
