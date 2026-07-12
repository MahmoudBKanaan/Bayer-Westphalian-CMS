import { formatReminderEnum, type ReminderLevel } from "@/api/reminders";

type ReminderLevelBadgeProps = {
  level: ReminderLevel;
};

export const reminderLevelBadgeLabels: Record<ReminderLevel, string> = {
  GREEN: "Green",
  YELLOW: "Yellow",
  RED: "Red",
};

export const reminderLevelBadgeTones: Record<ReminderLevel, string> = {
  GREEN: "reminder-level-green",
  YELLOW: "reminder-level-yellow",
  RED: "reminder-level-red",
};

export function ReminderLevelBadge({ level }: ReminderLevelBadgeProps) {
  const label = reminderLevelBadgeLabels[level] ?? formatReminderEnum(level);
  const tone = reminderLevelBadgeTones[level] ?? "reminder-level-unknown";

  return (
    <span
      className={`status-badge reminder-level-badge ${tone}`}
      aria-label={`Reminder level: ${label}`}
    >
      {label}
    </span>
  );
}
