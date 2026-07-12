import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { reminderLevels } from "@/api/reminders";
import {
  ReminderLevelBadge,
  reminderLevelBadgeLabels,
  reminderLevelBadgeTones,
} from "@/components/ReminderLevelBadge";

describe("ReminderLevelBadge", () => {
  it("renders a label for every KB reminder level", () => {
    for (const level of reminderLevels) {
      const { unmount } = render(<ReminderLevelBadge level={level} />);

      expect(
        screen.getByLabelText(`Reminder level: ${reminderLevelBadgeLabels[level]}`),
      ).toBeInTheDocument();

      unmount();
    }
  });

  it("uses dedicated traffic-light badge classes for reminder levels", () => {
    for (const level of reminderLevels) {
      const { unmount } = render(<ReminderLevelBadge level={level} />);

      expect(screen.getByText(reminderLevelBadgeLabels[level])).toHaveClass(
        "status-badge",
        "reminder-level-badge",
        reminderLevelBadgeTones[level],
      );

      unmount();
    }
  });
});
