import { formatCampaignEnum, type CampaignStatus } from "@/api/campaigns";

type CampaignStatusBadgeProps = {
  status: CampaignStatus;
};

export const campaignStatusBadgeLabels: Record<CampaignStatus, string> = {
  DRAFT: "Draft",
  SUBMITTED: "Submitted",
  APPROVED: "Approved",
  REJECTED: "Rejected",
  ACTIVE: "Active",
  PAUSED: "Paused",
  COMPLETED: "Completed",
  ARCHIVED: "Archived",
};

export const campaignStatusBadgeTones: Record<CampaignStatus, string> = {
  DRAFT: "campaign-status-draft",
  SUBMITTED: "campaign-status-submitted",
  APPROVED: "campaign-status-approved",
  REJECTED: "campaign-status-rejected",
  ACTIVE: "campaign-status-active",
  PAUSED: "campaign-status-paused",
  COMPLETED: "campaign-status-completed",
  ARCHIVED: "campaign-status-archived",
};

export function CampaignStatusBadge({ status }: CampaignStatusBadgeProps) {
  const label = campaignStatusBadgeLabels[status] ?? formatCampaignEnum(status);
  const tone = campaignStatusBadgeTones[status] ?? "campaign-status-unknown";

  return (
    <span
      className={`status-badge campaign-status-badge ${tone}`}
      aria-label={`Campaign status: ${label}`}
    >
      {label}
    </span>
  );
}
