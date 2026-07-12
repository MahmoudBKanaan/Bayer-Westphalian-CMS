import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import type { ConsentStatus } from "@/api/consents";
import {
  ConsentStatusBadge,
  consentStatusBadgeLabels,
  consentStatusBadgeTones,
} from "@/components/ConsentStatusBadge";

const consentStatuses: ConsentStatus[] = ["GIVEN", "WITHDRAWN", "REQUIRED", "EXPIRED", "REJECTED"];

describe("ConsentStatusBadge", () => {
  it("renders accessible labels for every KB consent status", () => {
    for (const status of consentStatuses) {
      const { unmount } = render(<ConsentStatusBadge status={status} />);

      expect(
        screen.getByLabelText(`Consent status: ${consentStatusBadgeLabels[status]}`),
      ).toBeInTheDocument();

      unmount();
    }
  });

  it("uses consent-specific status badge tone classes", () => {
    for (const status of consentStatuses) {
      const { unmount } = render(<ConsentStatusBadge status={status} />);

      expect(screen.getByText(consentStatusBadgeLabels[status])).toHaveClass(
        "status-badge",
        "consent-status-badge",
        consentStatusBadgeTones[status],
      );

      unmount();
    }
  });
});
