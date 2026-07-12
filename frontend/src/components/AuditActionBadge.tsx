import { formatAuditAction } from "@/api/auditLogs";

type AuditActionBadgeProps = {
  action: string;
};

export const auditActionBadgeTones: Record<string, string> = {
  CREATE: "audit-action-create",
  UPDATE: "audit-action-update",
  DELETE: "audit-action-delete",
  ASSIGN_ROLE: "audit-action-access",
  DISABLE_USER: "audit-action-access",
  WITHDRAW_CONSENT: "audit-action-consent",
  OPT_OUT: "audit-action-consent",
  UPDATE_DO_NOT_CONTACT: "audit-action-consent",
  SUBMIT: "audit-action-workflow",
  APPROVE: "audit-action-approval",
  REJECT: "audit-action-rejection",
  LAUNCH: "audit-action-workflow",
  EXPORT_REPORT: "audit-action-export",
};

export function AuditActionBadge({ action }: AuditActionBadgeProps) {
  const label = formatAuditAction(action);
  const tone = auditActionBadgeTones[action] ?? "audit-action-other";

  return (
    <span
      className={`status-badge audit-action-badge ${tone}`}
      aria-label={`Audit action: ${label}`}
    >
      {label}
    </span>
  );
}
