import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { campaignStatuses } from "@/api/campaigns";
import {
  CampaignStatusBadge,
  campaignStatusBadgeLabels,
  campaignStatusBadgeTones,
} from "@/components/CampaignStatusBadge";

describe("CampaignStatusBadge", () => {
  it("renders a label for every KB campaign status", () => {
    for (const status of campaignStatuses) {
      const { unmount } = render(<CampaignStatusBadge status={status} />);

      expect(
        screen.getByLabelText(`Campaign status: ${campaignStatusBadgeLabels[status]}`),
      ).toBeInTheDocument();

      unmount();
    }
  });

  it("uses campaign-specific status badge tone classes", () => {
    for (const status of campaignStatuses) {
      const { unmount } = render(<CampaignStatusBadge status={status} />);

      expect(screen.getByText(campaignStatusBadgeLabels[status])).toHaveClass(
        "status-badge",
        "campaign-status-badge",
        campaignStatusBadgeTones[status],
      );

      unmount();
    }
  });
});
