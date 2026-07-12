/**
 * Campaign launch UI flow (KB FR-054–055 / BR-005 / item 605).
 *
 * Acceptance contract for launching APPROVED campaigns from Recipient Preview.
 * Readiness messaging lives in {@link recipientPreviewClarity}.
 */

import type { CampaignStatus } from "@/api/campaigns";
import type { SystemRoleName } from "@/auth/sessionStorageStrategy";
import {
  RECIPIENT_PREVIEW_GATE_NOTE,
  RECIPIENT_PREVIEW_GUIDE,
  RECIPIENT_PREVIEW_PAGE_LEAD,
  evaluateLaunchReadiness,
  type LaunchReadiness,
} from "@/features/campaigns/recipientPreviewClarity";

/** Roles that may launch campaigns through the UI (matches CAMPAIGN_MANAGE_ROLES). */
export const CAMPAIGN_LAUNCH_UI_ROLES: SystemRoleName[] = ["ADMIN", "CAMPAIGN_MANAGER"];

export const RECIPIENT_PREVIEW_PAGE_TITLE = "Recipient Preview";
export const CAMPAIGN_LAUNCH_BUTTON_LABEL = "Launch campaign";
export const CAMPAIGN_LAUNCH_CONFIRM_TITLE = "Confirm campaign launch";
export const CAMPAIGN_LAUNCH_CONFIRM_LABEL = "Confirm launch";
export const CAMPAIGN_LAUNCHED_NOTICE = "Campaign launched.";
export const CAMPAIGN_LAUNCH_RESULT_HEADING = "Launch result";
export const CAMPAIGN_LAUNCH_READINESS_ARIA_LABEL = "Launch readiness";
export const CAMPAIGN_LAUNCH_FAILED_MESSAGE = "Campaign could not be launched.";

export {
  RECIPIENT_PREVIEW_GATE_NOTE,
  RECIPIENT_PREVIEW_GUIDE,
  RECIPIENT_PREVIEW_PAGE_LEAD,
  evaluateLaunchReadiness,
};

export type CampaignLaunchStepId =
  | "open-recipient-preview"
  | "confirm-launch-readiness"
  | "confirm-launch-dialog"
  | "see-launch-result";

export type CampaignLaunchStepDefinition = {
  id: CampaignLaunchStepId;
  index: number;
  title: string;
  description: string;
};

/** UI acceptance steps for “Campaign launch works through UI” (item 605). */
export const CAMPAIGN_LAUNCH_FLOW_STEPS: CampaignLaunchStepDefinition[] = [
  {
    id: "open-recipient-preview",
    index: 0,
    title: "Open recipient preview",
    description: "Campaign Manager or Admin opens /campaigns/:id/recipients/preview.",
  },
  {
    id: "confirm-launch-readiness",
    index: 1,
    title: "Confirm launch readiness",
    description: "Campaign status is APPROVED and launch readiness shows ready (BR-005).",
  },
  {
    id: "confirm-launch-dialog",
    index: 2,
    title: "Confirm launch dialog",
    description: "Launch campaign opens confirmation with eligible/excluded counts.",
  },
  {
    id: "see-launch-result",
    index: 3,
    title: "See launch result",
    description: "POST /api/campaigns/{id}/launch; status ACTIVE, notice, and result metrics.",
  },
];

/** Deterministic fixtures for Playwright / integration campaign launch. */
export const CAMPAIGN_LAUNCH_FIXTURES = {
  campaignId: "50000000-0000-0000-0000-00000000c605",
  campaignName: "UI Launch Ready Outreach",
  objective: "Validate campaign launch through recipient preview UI",
  segmentName: "E2E Eligible Audience",
  channel: "EMAIL" as const,
  messageSubject: "Launch readiness subject",
  messageBody: "Controlled campaign launch message body.",
  eligible: 7,
  excluded: 3,
  startDate: "2026-09-01",
  endDate: "2026-09-30",
} as const;

export function canLaunchCampaignsThroughUi(roles: readonly SystemRoleName[]): boolean {
  return roles.some((role) => CAMPAIGN_LAUNCH_UI_ROLES.includes(role));
}

/**
 * Launch button is enabled only for managers when status is APPROVED (domain BR-005).
 */
export function canEnableLaunchButton(options: {
  canManageCampaigns: boolean;
  campaignStatus: CampaignStatus | null | undefined;
}): boolean {
  return options.canManageCampaigns && options.campaignStatus === "APPROVED";
}

/**
 * True when readiness evaluation reports the ready state for an APPROVED campaign.
 */
export function isLaunchReady(readiness: LaunchReadiness): boolean {
  return readiness.state === "ready";
}

export function campaignLaunchConfirmDescription(options: {
  campaignName: string;
  eligibleCount: number;
  excludedCount: number;
}): string {
  return `Launching will contact ${options.eligibleCount} eligible recipients and move the campaign to ACTIVE. ${options.excludedCount} excluded recipients will not be contacted.`;
}

export function campaignLaunchStepIdsInOrder(): CampaignLaunchStepId[] {
  return [...CAMPAIGN_LAUNCH_FLOW_STEPS]
    .sort((left, right) => left.index - right.index)
    .map((step) => step.id);
}

export function formatCampaignLaunchJourney(
  steps: readonly CampaignLaunchStepDefinition[] = CAMPAIGN_LAUNCH_FLOW_STEPS,
): string {
  return steps
    .slice()
    .sort((left, right) => left.index - right.index)
    .map((step) => step.title)
    .join(" → ");
}

export function isValidCampaignLaunchOrder(observed: readonly CampaignLaunchStepId[]): boolean {
  const expected = campaignLaunchStepIdsInOrder();
  if (observed.length !== expected.length) {
    return false;
  }
  return observed.every((stepId, index) => stepId === expected[index]);
}

export function recipientPreviewPath(campaignId: string): string {
  return `/campaigns/${campaignId}/recipients/preview`;
}
