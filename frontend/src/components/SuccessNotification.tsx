import type { ReactNode } from "react";

export type SuccessNotificationProps = {
  message: string;
  action?: ReactNode;
  className?: string;
  compact?: boolean;
};

export function SuccessNotification({
  message,
  action,
  className,
  compact = false,
}: SuccessNotificationProps) {
  const classes = [
    "success-notification",
    compact ? "success-notification--compact" : "",
    className ?? "",
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <div className={classes} role="status" aria-live="polite">
      <p>{message}</p>
      {action ? <div className="success-notification-action">{action}</div> : null}
    </div>
  );
}
