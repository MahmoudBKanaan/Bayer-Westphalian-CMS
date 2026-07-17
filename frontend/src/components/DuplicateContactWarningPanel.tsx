import { useMutation } from "@tanstack/react-query";
import {
  generateDuplicateContactWarning,
  type DuplicateContactWarningView,
} from "@/api/ai";
import { ApiError, isAuthorizationError } from "@/api/client";
import { StatusBadge } from "@/components/StatusBadge";
import { usePermissions } from "@/features/auth/usePermissions";

export type DuplicateContactWarningPanelProps = {
  customerId: string;
  customerName: string;
  campaignId: string;
  campaignName: string;
  currentEligibilityStatus?: string | null;
  currentExclusionReason?: string | null;
};

/**
 * AI-006 duplicate-contact risk panel (Sprint 13 evidence).
 * Calls POST /api/ai/duplicate-contact-warning only on explicit user action.
 * Does not override eligibility, consent, or contact limits.
 */
export function DuplicateContactWarningPanel({
  customerId,
  customerName,
  campaignId,
  campaignName,
  currentEligibilityStatus,
  currentExclusionReason,
}: DuplicateContactWarningPanelProps) {
  const permissions = usePermissions();
  const canUse = permissions.canUseDuplicateContactWarning();

  const warningMutation = useMutation({
    mutationFn: () =>
      generateDuplicateContactWarning({
        customerId,
        campaignId,
      }),
  });

  if (!canUse) {
    return null;
  }

  const idsReady = customerId.trim() !== "" && campaignId.trim() !== "";
  const errorMessage = warningMutation.isError
    ? resolveErrorMessage(warningMutation.error)
    : "";

  return (
    <section
      className="panel duplicate-contact-warning-panel"
      aria-labelledby="duplicate-contact-heading"
      data-testid="duplicate-contact-warning-panel"
    >
      <div className="section-heading">
        <div>
          <h2 id="duplicate-contact-heading">Duplicate-Contact Risk</h2>
          <p className="table-secondary-text">
            Rule-based decision support using campaign and communication history (AI-006).
          </p>
        </div>
        <button
          type="button"
          disabled={warningMutation.isPending || !idsReady}
          onClick={() => warningMutation.mutate()}
          aria-label="Check Duplicate-Contact Risk"
        >
          {warningMutation.isPending ? "Checking…" : "Check Duplicate-Contact Risk"}
        </button>
      </div>

      {!warningMutation.isPending && !warningMutation.isSuccess && !warningMutation.isError ? (
        <div className="state-panel" role="status" data-testid="duplicate-contact-idle">
          <strong>Risk not checked</strong>
          <p>
            Duplicate-contact risk has not been checked. Run the check to inspect previous
            campaign and marketing-contact activity.
          </p>
        </div>
      ) : null}

      {warningMutation.isPending ? (
        <div className="table-state" role="status" aria-live="polite">
          Checking contact history…
        </div>
      ) : null}

      {warningMutation.isError ? (
        <div className="error-panel form-error" role="alert" data-testid="duplicate-contact-error">
          <strong>Unable to complete the check</strong>
          <p>{errorMessage}</p>
          <div className="form-actions">
            <button type="button" onClick={() => warningMutation.mutate()}>
              Retry
            </button>
          </div>
        </div>
      ) : null}

      {warningMutation.data != null ? (
        <DuplicateContactWarningResult
          result={warningMutation.data}
          customerName={customerName}
          campaignName={campaignName}
          currentEligibilityStatus={currentEligibilityStatus}
          currentExclusionReason={currentExclusionReason}
        />
      ) : null}
    </section>
  );
}

type DuplicateContactWarningResultProps = {
  result: DuplicateContactWarningView;
  customerName: string;
  campaignName: string;
  currentEligibilityStatus?: string | null;
  currentExclusionReason?: string | null;
};

function DuplicateContactWarningResult({
  result,
  customerName,
  campaignName,
  currentEligibilityStatus,
  currentExclusionReason,
}: DuplicateContactWarningResultProps) {
  const riskDetected = result.riskDetected;

  return (
    <div
      className={
        riskDetected
          ? "risk-panel risk-panel--high"
          : "risk-panel risk-panel--clear"
      }
      role={riskDetected ? "alert" : "status"}
      aria-live="polite"
      data-testid="duplicate-contact-result"
    >
      <div className="risk-panel__header">
        <strong>
          {riskDetected
            ? "Duplicate-contact risk detected"
            : "No duplicate-contact risk detected"}
        </strong>
        <StatusBadge value={riskDetected ? "RISK DETECTED" : "NO RISK"} />
      </div>

      <dl className="details-grid duplicate-contact-details">
        <div>
          <dt>Customer</dt>
          <dd>{customerName}</dd>
        </div>
        <div>
          <dt>Campaign</dt>
          <dd>{campaignName}</dd>
        </div>
        <div>
          <dt>Same campaign already contacted</dt>
          <dd>{result.sameCampaignAlreadyContacted ? "Yes" : "No"}</dd>
        </div>
        <div>
          <dt>Marketing contacts in evaluation period</dt>
          <dd>{result.contactsInCurrentMonth}</dd>
        </div>
        <div>
          <dt>Configured monthly limit</dt>
          <dd>
            {result.monthlyContactLimit == null
              ? "Not configured"
              : result.monthlyContactLimit}
          </dd>
        </div>
        {currentEligibilityStatus ? (
          <div>
            <dt>Current eligibility</dt>
            <dd>{formatEnumLabel(currentEligibilityStatus)}</dd>
          </div>
        ) : null}
      </dl>

      <div className="explanation-block">
        <h3>Warning</h3>
        <p>
          {result.warning?.trim()
            ? result.warning
            : riskDetected
              ? "Duplicate-contact risk detected."
              : "No duplicate-contact risk detected."}
        </p>
      </div>

      <div className="explanation-block">
        <h3>Explanation</h3>
        <p>{result.explanation}</p>
      </div>

      {currentExclusionReason ? (
        <div className="explanation-block">
          <h3>Recipient exclusion reason</h3>
          <p>{formatEnumLabel(currentExclusionReason)}</p>
        </div>
      ) : null}

      {riskDetected && currentEligibilityStatus?.toUpperCase() === "EXCLUDED" ? (
        <div className="explanation-block" data-testid="duplicate-contact-eligibility-blocked">
          <h3>Eligibility</h3>
          <p>Blocked by normal eligibility rules.</p>
        </div>
      ) : null}

      <div className="compliance-notice" data-testid="eligibility-service-notice">
        <strong>EligibilityService remains authoritative.</strong>
        <p>
          This result does not replace consent, opt-out, do-not-contact, guardian-consent,
          duplicate-campaign, or contact-frequency rules. This warning cannot override
          EligibilityService.
        </p>
      </div>
    </div>
  );
}

function resolveErrorMessage(error: unknown): string {
  if (error instanceof ApiError && error.status === 403) {
    return "You do not have permission to generate duplicate-contact warnings.";
  }
  if (error instanceof ApiError && error.status === 401) {
    return "Your session has expired. Sign in again to continue.";
  }
  if (error instanceof ApiError && error.status === 404) {
    return "The customer or campaign could not be found.";
  }
  if (error instanceof ApiError && error.status === 400) {
    return "The duplicate-contact check could not be completed. Check the customer and campaign selection.";
  }
  if (error instanceof ApiError && error.status === 409) {
    return "The duplicate-contact check conflicts with the current campaign or recipient state. Refresh and try again.";
  }
  if (isAuthorizationError(error)) {
    return "You do not have permission to generate duplicate-contact warnings.";
  }
  if (error instanceof Error && error.message.trim() !== "") {
    // Client-side validation only (blank IDs). Never surface stack traces.
    if (
      error.message === "Customer ID is required." ||
      error.message === "Campaign ID is required."
    ) {
      return error.message;
    }
  }
  return "The duplicate-contact check could not be completed.";
}

function formatEnumLabel(value: string): string {
  return value
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}
