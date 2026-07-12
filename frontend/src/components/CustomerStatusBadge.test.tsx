import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import type { CustomerStatus } from "@/api/customers";
import {
  CustomerStatusBadge,
  customerStatusBadgeLabels,
  customerStatusBadgeTones,
} from "@/components/CustomerStatusBadge";

const customerStatuses: CustomerStatus[] = [
  "ACTIVE",
  "INACTIVE",
  "INTERESTED",
  "UNINTERESTED",
  "CONVERTED",
];

describe("CustomerStatusBadge", () => {
  it("renders accessible labels for every KB customer status", () => {
    for (const status of customerStatuses) {
      const { unmount } = render(<CustomerStatusBadge status={status} />);

      expect(
        screen.getByLabelText(`Customer status: ${customerStatusBadgeLabels[status]}`),
      ).toBeInTheDocument();

      unmount();
    }
  });

  it("uses customer-specific status badge tone classes", () => {
    for (const status of customerStatuses) {
      const { unmount } = render(<CustomerStatusBadge status={status} />);

      expect(screen.getByText(customerStatusBadgeLabels[status])).toHaveClass(
        "status-badge",
        "customer-status-badge",
        customerStatusBadgeTones[status],
      );

      unmount();
    }
  });
});
