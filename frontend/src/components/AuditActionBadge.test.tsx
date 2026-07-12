import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { AUDIT_ACTION_OPTIONS, formatAuditAction } from "@/api/auditLogs";
import { AuditActionBadge, auditActionBadgeTones } from "@/components/AuditActionBadge";

describe("AuditActionBadge", () => {
  it("renders accessible labels for every KB audit action option", () => {
    for (const action of AUDIT_ACTION_OPTIONS) {
      const { unmount } = render(<AuditActionBadge action={action} />);
      const label = formatAuditAction(action);

      expect(screen.getByLabelText(`Audit action: ${label}`)).toBeInTheDocument();
      expect(screen.getByText(label)).toBeInTheDocument();

      unmount();
    }
  });

  it("uses audit-specific tone classes for sensitive action categories", () => {
    for (const [action, tone] of Object.entries(auditActionBadgeTones)) {
      const { unmount } = render(<AuditActionBadge action={action} />);

      expect(screen.getByLabelText(/Audit action:/)).toHaveClass(
        "status-badge",
        "audit-action-badge",
        tone,
      );

      unmount();
    }
  });
});
