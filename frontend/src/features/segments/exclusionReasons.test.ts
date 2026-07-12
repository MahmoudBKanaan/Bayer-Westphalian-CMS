import { describe, expect, it } from "vitest";
import {
  exclusionReasonRuleHint,
  exclusionReasonSeverity,
  formatExclusionReasonTitle,
  isKnownExclusionReasonCode,
  presentExclusionReasons,
  summarizeExclusionTotals,
} from "@/features/segments/exclusionReasons";

describe("exclusion reason presentation", () => {
  it("recognizes KB exclusion reason codes", () => {
    expect(isKnownExclusionReasonCode("DO_NOT_CONTACT")).toBe(true);
    expect(isKnownExclusionReasonCode("MONTHLY_CONTACT_LIMIT")).toBe(true);
    expect(isKnownExclusionReasonCode("UNKNOWN")).toBe(false);
  });

  it("formats known and unknown reason titles", () => {
    expect(formatExclusionReasonTitle("DO_NOT_CONTACT")).toBe("Do not contact");
    expect(formatExclusionReasonTitle("MARKETING_OPT_OUT")).toBe("Marketing opt-out");
    expect(formatExclusionReasonTitle("CUSTOM_BLOCK")).toBe("Custom Block");
  });

  it("maps severities and rule hints for known codes", () => {
    expect(exclusionReasonSeverity("DO_NOT_CONTACT")).toBe("critical");
    expect(exclusionReasonSeverity("INVALID_CONSENT")).toBe("warning");
    expect(exclusionReasonSeverity("DUPLICATE_CAMPAIGN_RECIPIENT")).toBe("info");
    expect(exclusionReasonRuleHint("MARKETING_OPT_OUT")).toMatch(/BR-002/);
    expect(exclusionReasonRuleHint("CUSTOM")).toMatch(/Eligibility exclusion/);
  });

  it("sorts reasons by count descending and computes exclusion share", () => {
    const presented = presentExclusionReasons(
      [
        {
          code: "INVALID_CONSENT",
          message: "Customer does not have valid required consent",
          count: 1,
        },
        {
          code: "DO_NOT_CONTACT",
          message: "Customer has do-not-contact enabled",
          count: 3,
        },
        {
          code: "MONTHLY_CONTACT_LIMIT",
          message: "Customer has reached the monthly marketing contact limit",
          count: 1,
        },
      ],
      5,
    );

    expect(presented.map((entry) => entry.code)).toEqual([
      "DO_NOT_CONTACT",
      "INVALID_CONSENT",
      "MONTHLY_CONTACT_LIMIT",
    ]);
    expect(presented[0].shareOfExcluded).toBe(60);
    expect(presented[0].title).toBe("Do not contact");
    expect(presented[1].shareOfExcluded).toBe(20);
  });

  it("falls back to summary counts when excludedCount is zero but reasons exist", () => {
    const presented = presentExclusionReasons(
      [{ code: "DO_NOT_CONTACT", message: "Customer has do-not-contact enabled", count: 2 }],
      0,
    );
    expect(presented[0].shareOfExcluded).toBe(100);
  });

  it("summarizes totals for the panel header", () => {
    expect(
      summarizeExclusionTotals(
        [
          { code: "DO_NOT_CONTACT", message: "…", count: 2 },
          { code: "MARKETING_OPT_OUT", message: "…", count: 1 },
        ],
        3,
      ),
    ).toEqual({ reasonGroups: 2, accountedFor: 3, excludedCount: 3 });
  });
});
