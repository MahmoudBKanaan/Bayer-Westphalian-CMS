import type { SegmentExclusionReasonSummary } from "@/api/segments";

/** Stable KB exclusion reason codes (BR-001–003, BR-010–011, FR-034, FR-055). */
export const KNOWN_EXCLUSION_REASON_CODES = [
  "DO_NOT_CONTACT",
  "MARKETING_OPT_OUT",
  "INVALID_CONSENT",
  "DUPLICATE_CAMPAIGN_RECIPIENT",
  "MONTHLY_CONTACT_LIMIT",
] as const;

export type KnownExclusionReasonCode = (typeof KNOWN_EXCLUSION_REASON_CODES)[number];

export type ExclusionReasonPresentation = {
  code: string;
  title: string;
  message: string;
  count: number;
  shareOfExcluded: number;
  severity: "critical" | "warning" | "info";
  ruleHint: string;
};

const REASON_META: Record<
  KnownExclusionReasonCode,
  { title: string; severity: ExclusionReasonPresentation["severity"]; ruleHint: string }
> = {
  DO_NOT_CONTACT: {
    title: "Do not contact",
    severity: "critical",
    ruleHint: "BR-001 — never include customers marked do-not-contact",
  },
  MARKETING_OPT_OUT: {
    title: "Marketing opt-out",
    severity: "critical",
    ruleHint: "BR-002 — exclude withdrawn or rejected marketing consent",
  },
  INVALID_CONSENT: {
    title: "Invalid or missing consent",
    severity: "warning",
    ruleHint: "FR-034 / BR-003 — require valid channel or guardian consent",
  },
  DUPLICATE_CAMPAIGN_RECIPIENT: {
    title: "Already on campaign",
    severity: "info",
    ruleHint: "BR-010 — prevent duplicate campaign recipients",
  },
  MONTHLY_CONTACT_LIMIT: {
    title: "Monthly contact limit",
    severity: "warning",
    ruleHint: "BR-011 — respect configured marketing contact frequency",
  },
};

export function isKnownExclusionReasonCode(code: string): code is KnownExclusionReasonCode {
  return (KNOWN_EXCLUSION_REASON_CODES as readonly string[]).includes(code);
}

export function formatExclusionReasonTitle(code: string): string {
  if (isKnownExclusionReasonCode(code)) {
    return REASON_META[code].title;
  }
  return code
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

export function exclusionReasonSeverity(code: string): ExclusionReasonPresentation["severity"] {
  if (isKnownExclusionReasonCode(code)) {
    return REASON_META[code].severity;
  }
  return "info";
}

export function exclusionReasonRuleHint(code: string): string {
  if (isKnownExclusionReasonCode(code)) {
    return REASON_META[code].ruleHint;
  }
  return "Eligibility exclusion applied by the platform";
}

export function presentExclusionReasons(
  summary: SegmentExclusionReasonSummary[],
  excludedCount: number,
): ExclusionReasonPresentation[] {
  const totalExcluded =
    excludedCount > 0
      ? excludedCount
      : summary.reduce((sum, entry) => sum + Math.max(0, entry.count), 0);

  return [...summary]
    .map((entry) => {
      const count = Math.max(0, entry.count);
      return {
        code: entry.code,
        title: formatExclusionReasonTitle(entry.code),
        message: entry.message?.trim() || formatExclusionReasonTitle(entry.code),
        count,
        shareOfExcluded: totalExcluded === 0 ? 0 : (count / totalExcluded) * 100,
        severity: exclusionReasonSeverity(entry.code),
        ruleHint: exclusionReasonRuleHint(entry.code),
      };
    })
    .sort((left, right) => {
      if (right.count !== left.count) {
        return right.count - left.count;
      }
      return left.code.localeCompare(right.code);
    });
}

export function summarizeExclusionTotals(
  summary: SegmentExclusionReasonSummary[],
  excludedCount: number,
): { reasonGroups: number; accountedFor: number; excludedCount: number } {
  const accountedFor = summary.reduce((sum, entry) => sum + Math.max(0, entry.count), 0);
  return {
    reasonGroups: summary.length,
    accountedFor,
    excludedCount,
  };
}
