import type { ReactNode } from "react";

export type EmptyStateProps = {
  title: string;
  description: string;
  action?: ReactNode;
  className?: string;
  compact?: boolean;
};

export function EmptyState({
  title,
  description,
  action,
  className,
  compact = false,
}: EmptyStateProps) {
  const classes = ["empty-state", compact ? "empty-state--compact" : "", className ?? ""]
    .filter(Boolean)
    .join(" ");

  return (
    <div className={classes} role="status">
      <strong>{title}</strong>
      <p>{description}</p>
      {action ? <div className="empty-state-action">{action}</div> : null}
    </div>
  );
}
