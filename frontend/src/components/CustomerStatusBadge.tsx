import type { CustomerStatus } from "@/api/customers";

type CustomerStatusBadgeProps = {
  status: CustomerStatus;
};

export const customerStatusBadgeLabels: Record<CustomerStatus, string> = {
  ACTIVE: "Active",
  INACTIVE: "Inactive",
  INTERESTED: "Interested",
  UNINTERESTED: "Uninterested",
  CONVERTED: "Converted",
};

export const customerStatusBadgeTones: Record<CustomerStatus, string> = {
  ACTIVE: "customer-status-active",
  INACTIVE: "customer-status-inactive",
  INTERESTED: "customer-status-interested",
  UNINTERESTED: "customer-status-uninterested",
  CONVERTED: "customer-status-converted",
};

export function CustomerStatusBadge({ status }: CustomerStatusBadgeProps) {
  const label = customerStatusBadgeLabels[status];
  const tone = customerStatusBadgeTones[status];

  return (
    <span
      className={`status-badge customer-status-badge ${tone}`}
      aria-label={`Customer status: ${label}`}
    >
      {label}
    </span>
  );
}
