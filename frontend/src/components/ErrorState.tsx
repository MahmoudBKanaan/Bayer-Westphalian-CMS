import type { ReactNode } from "react";

export type ErrorStateProps = {
  title: string;
  description: string;
  action?: ReactNode;
  className?: string;
  compact?: boolean;
};

export function ErrorState({
  title,
  description,
  action,
  className,
  compact = false,
}: ErrorStateProps) {
  const classes = ["error-state", compact ? "error-state--compact" : "", className ?? ""]
    .filter(Boolean)
    .join(" ");

  return (
    <div className={classes} role="alert">
      <strong>{title}</strong>
      <p>{description}</p>
      {action ? <div className="error-state-action">{action}</div> : null}
    </div>
  );
}
