import type { ConsentStatus } from "@/api/consents";

type ConsentStatusBadgeProps = {
  status: ConsentStatus;
};

export const consentStatusBadgeLabels: Record<ConsentStatus, string> = {
  GIVEN: "Given",
  WITHDRAWN: "Withdrawn",
  REQUIRED: "Required",
  EXPIRED: "Expired",
  REJECTED: "Rejected",
};

export const consentStatusBadgeTones: Record<ConsentStatus, string> = {
  GIVEN: "consent-status-given",
  WITHDRAWN: "consent-status-withdrawn",
  REQUIRED: "consent-status-required",
  EXPIRED: "consent-status-expired",
  REJECTED: "consent-status-rejected",
};

export function ConsentStatusBadge({ status }: ConsentStatusBadgeProps) {
  const label = consentStatusBadgeLabels[status];
  const tone = consentStatusBadgeTones[status];

  return (
    <span
      className={`status-badge consent-status-badge ${tone}`}
      aria-label={`Consent status: ${label}`}
    >
      {label}
    </span>
  );
}
