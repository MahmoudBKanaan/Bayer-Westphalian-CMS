import type { CampaignStatus } from "@/api/campaigns";
import { formatExclusionReasonTitle } from "@/features/segments/exclusionReasons";

/**
 * Recipient preview clarity helpers (KB item 594 / FR-054–055 / BR-006–007 / BR-001–003).
 *
 * Makes eligibility, exclusions, and launch readiness easier to scan before campaign contact.
 */

export const RECIPIENT_PREVIEW_PAGE_LEAD =
  "Review who is eligible for this campaign, who is excluded and why, then launch only when the audience and status are correct. Preview does not send messages by itself.";

export const RECIPIENT_PREVIEW_GATE_NOTE =
  "Launch is allowed only for APPROVED campaigns and only by campaign managers (or admins). Compliance review and eligibility rules are enforced before contact.";

export type RecipientPreviewGuideItem = {
  id: string;
  title: string;
  description: string;
};

export const RECIPIENT_PREVIEW_GUIDE: RecipientPreviewGuideItem[] = [
  {
    id: "audience-preview",
    title: "Audience preview",
    description:
      "Shows total segment matches, eligible count after EligibilityService, and exclusion reason groups.",
  },
  {
    id: "eligible",
    title: "Eligible recipients",
    description:
      "Contactable rows stored on the campaign. After launch, rows may show Sent when contact events exist.",
  },
  {
    id: "excluded",
    title: "Excluded recipients",
    description:
      "Blocked by do-not-contact, opt-out, invalid consent, duplicates, conversion, or monthly limits — each with a stable reason code.",
  },
];

export type LaunchReadiness =
  | { state: "ready"; message: string }
  | { state: "blocked-role"; message: string }
  | { state: "blocked-status"; message: string }
  | { state: "missing-campaign"; message: string };

export function evaluateLaunchReadiness(options: {
  canManageCampaigns: boolean;
  campaignStatus: CampaignStatus | null | undefined;
  eligibleCount: number | null | undefined;
}): LaunchReadiness {
  if (!options.canManageCampaigns) {
    return {
      state: "blocked-role",
      message:
        "Your role can review recipients but cannot launch. Campaign Managers or Admins launch approved campaigns.",
    };
  }
  if (options.campaignStatus == null) {
    return {
      state: "missing-campaign",
      message: "Campaign details are still loading. Launch becomes available when status is APPROVED.",
    };
  }
  if (options.campaignStatus !== "APPROVED") {
    return {
      state: "blocked-status",
      message: `Launch is disabled while status is ${options.campaignStatus}. Only APPROVED campaigns can be launched (BR-005).`,
    };
  }
  const eligible = options.eligibleCount ?? 0;
  if (eligible === 0) {
    return {
      state: "ready",
      message:
        "Campaign is APPROVED. There are currently no eligible recipients — confirm exclusions before launching.",
    };
  }
  return {
    state: "ready",
    message: `Campaign is APPROVED and ready to launch to ${eligible} eligible recipient${eligible === 1 ? "" : "s"}.`,
  };
}

export function formatRecipientTabLabel(
  base: string,
  count: number | null | undefined,
  loading: boolean,
): string {
  if (loading || count == null) {
    return base;
  }
  return `${base} (${count})`;
}

/**
 * Human title for exclusion reason codes shown in recipient tables (BR-006).
 */
export function presentRecipientExclusionReason(code: string | null | undefined): {
  code: string;
  title: string;
} {
  const normalized = code?.trim() || "UNKNOWN";
  return {
    code: normalized,
    title: formatExclusionReasonTitle(normalized),
  };
}

export function eligibilityRatePercent(eligible: number, total: number): number {
  if (total <= 0) {
    return 0;
  }
  return (eligible / total) * 100;
}

export function formatEligibilityRateLabel(eligible: number, total: number): string {
  if (total <= 0) {
    return "No audience matched yet";
  }
  const rate = eligibilityRatePercent(eligible, total);
  return `${rate.toFixed(1)}% of the matched audience is eligible for contact`;
}
