import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { type KeyboardEvent, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  approveCampaign,
  formatCampaignEnum,
  getCampaignRecipientSummary,
  listCampaigns,
  rejectCampaign,
  type CampaignRecipientSummaryView,
  type CampaignView,
  type RejectCampaignPayload,
} from "@/api/campaigns";
import { isAuthorizationError } from "@/api/client";
import { CampaignStatusBadge } from "@/components/CampaignStatusBadge";
import { ConfirmationDialog } from "@/components/ConfirmationDialog";
import { usePermissions } from "@/features/auth/usePermissions";
import {
  COMPLIANCE_APPROVE_BUTTON_LABEL,
  COMPLIANCE_APPROVED_NOTICE,
  COMPLIANCE_CHECKLIST,
  COMPLIANCE_QUEUE_TABLE_ARIA_LABEL,
  COMPLIANCE_REJECT_BUTTON_LABEL,
  COMPLIANCE_REJECTED_NOTICE,
  COMPLIANCE_REVIEW_GATE_NOTE,
  COMPLIANCE_REVIEW_NOTES_LABEL,
  COMPLIANCE_REVIEW_PAGE_LEAD,
  COMPLIANCE_REVIEW_PAGE_TITLE,
  complianceDecisionConfirmLabel,
  complianceDecisionConfirmTitle,
  complianceDecisionOutcome,
  formatPendingQueueLabel,
} from "@/features/campaigns/complianceApprovalFlow";
import { recipientPreviewPath, type ComplianceDecisionKind } from "@/features/campaigns/complianceReviewClarity";
import {
  campaignFormValidationMessages,
  hasCampaignFormErrors,
  validateRejectCampaignForm,
  type RejectCampaignFormErrors,
} from "@/features/campaigns/campaignFormValidation";

/**
 * Compliance Review screen (KB FR-059 / BR-005 / COMP-006 / item 593 clarity).
 *
 * Lists SUBMITTED campaigns, surfaces structured review evidence, and separates
 * approve vs reject decisions with confirmation and formal rejection reason.
 */
export function CompliancePage() {
  const permissions = usePermissions();
  const queryClient = useQueryClient();
  const [selectedCampaignId, setSelectedCampaignId] = useState("");
  const [reviewNotes, setReviewNotes] = useState("");
  const [rejectForm, setRejectForm] = useState<RejectCampaignPayload>({
    rejectionReason: "",
    complianceReviewNotes: "",
  });
  const [rejectErrors, setRejectErrors] = useState<RejectCampaignFormErrors>({});
  const [notice, setNotice] = useState("");
  const [pendingDecision, setPendingDecision] = useState<ComplianceDecisionKind | null>(null);

  const canReviewCampaigns = permissions.canReviewCampaigns();
  const pendingQuery = useQuery({
    queryKey: ["campaigns", "compliance-review", "SUBMITTED"],
    queryFn: () =>
      listCampaigns({
        term: "",
        status: "SUBMITTED",
        ownerUserId: "",
        segmentId: "",
      }),
    enabled: canReviewCampaigns,
  });

  const campaigns = useMemo(() => pendingQuery.data ?? [], [pendingQuery.data]);
  const selectedCampaign = useMemo(() => {
    if (selectedCampaignId === "") {
      return campaigns[0];
    }
    return campaigns.find((campaign) => campaign.id === selectedCampaignId);
  }, [campaigns, selectedCampaignId]);

  const recipientSummaryQuery = useQuery({
    queryKey: ["campaigns", selectedCampaign?.id, "recipients", "summary"],
    queryFn: () => getCampaignRecipientSummary(selectedCampaign?.id ?? ""),
    enabled: canReviewCampaigns && selectedCampaign != null,
  });

  const approveMutation = useMutation({
    mutationFn: () => approveCampaign(selectedCampaign?.id ?? "", reviewNotes),
    onSuccess: async () => {
      setNotice(COMPLIANCE_APPROVED_NOTICE);
      setReviewNotes("");
      setSelectedCampaignId("");
      setPendingDecision(null);
      await queryClient.invalidateQueries({ queryKey: ["campaigns"] });
    },
    onError: () => {
      setPendingDecision(null);
    },
  });

  const rejectMutation = useMutation({
    mutationFn: () => rejectCampaign(selectedCampaign?.id ?? "", rejectForm),
    onSuccess: async () => {
      setNotice(COMPLIANCE_REJECTED_NOTICE);
      setRejectForm({ rejectionReason: "", complianceReviewNotes: "" });
      setRejectErrors({});
      setSelectedCampaignId("");
      setPendingDecision(null);
      await queryClient.invalidateQueries({ queryKey: ["campaigns"] });
    },
    onError: () => {
      setPendingDecision(null);
    },
  });

  const isBusy = approveMutation.isPending || rejectMutation.isPending;
  const errorMessage =
    authorizationErrorMessage(pendingQuery.error, approveMutation.error, rejectMutation.error) ||
    generalErrorMessage(pendingQuery.error, approveMutation.error, rejectMutation.error);

  if (!canReviewCampaigns) {
    return (
      <section className="panel compliance-review-page" aria-labelledby="compliance-review-title">
        <div className="section-heading">
          <h2 id="compliance-review-title">{COMPLIANCE_REVIEW_PAGE_TITLE}</h2>
          <span>Campaign approvals and rejection decisions</span>
        </div>
        <p className="form-error" role="alert">
          You are not authorized to review campaigns.
        </p>
      </section>
    );
  }

  function selectCampaign(campaignId: string) {
    setSelectedCampaignId(campaignId);
    setRejectErrors({});
    setPendingDecision(null);
  }

  function requestApprove() {
    setNotice("");
    setPendingDecision("approve");
  }

  function requestReject() {
    setNotice("");
    const nextErrors = validateRejectCampaignForm(rejectForm);
    setRejectErrors(nextErrors);
    if (hasCampaignFormErrors(nextErrors)) {
      return;
    }
    setPendingDecision("reject");
  }

  function confirmPendingDecision() {
    if (pendingDecision === "approve") {
      approveMutation.mutate();
      return;
    }
    if (pendingDecision === "reject") {
      rejectMutation.mutate();
    }
  }

  return (
    <section className="page-stack compliance-review-page" aria-labelledby="compliance-review-title">
      <header className="panel compliance-review-header">
        <div className="section-heading">
          <h2 id="compliance-review-title">{COMPLIANCE_REVIEW_PAGE_TITLE}</h2>
          <span>Approve compliant campaigns or reject with a formal reason</span>
        </div>
        <p className="compliance-review-lead">{COMPLIANCE_REVIEW_PAGE_LEAD}</p>
        <p className="compliance-review-gate" role="note">
          {COMPLIANCE_REVIEW_GATE_NOTE}
        </p>
        <div className="work-list" aria-label="Compliance review summary">
          <div>
            <strong>{campaigns.length}</strong>
            <span>Pending campaign approvals</span>
          </div>
          <div>
            <strong>{formatPendingQueueLabel(campaigns.length)}</strong>
            <span>Only SUBMITTED campaigns appear in this queue</span>
          </div>
        </div>
        {notice ? (
          <p className="form-success" role="status" data-testid="compliance-decision-notice">
            {notice}
          </p>
        ) : null}
        {errorMessage ? (
          <p className="form-error" role="alert">
            {errorMessage}
          </p>
        ) : null}
      </header>

      <section className="panel" aria-labelledby="compliance-checklist-heading">
        <div className="section-heading">
          <h2 id="compliance-checklist-heading">What to check before deciding</h2>
          <span>Use this checklist for every submitted campaign</span>
        </div>
        <ol className="compliance-checklist" aria-label="Compliance review checklist">
          {COMPLIANCE_CHECKLIST.map((item, index) => (
            <li key={item.id} className="compliance-checklist-item">
              <span className="compliance-checklist-index" aria-hidden="true">
                {index + 1}
              </span>
              <div>
                <strong className="compliance-checklist-title">{item.title}</strong>
                <p className="compliance-checklist-description">{item.description}</p>
              </div>
            </li>
          ))}
        </ol>
      </section>

      <section className="panel" aria-labelledby="compliance-queue-heading">
        <div className="section-heading">
          <h2 id="compliance-queue-heading">Submitted campaign queue</h2>
          <span>Select a campaign to inspect message, audience, and decide</span>
        </div>
        {pendingQuery.isLoading ? (
          <p className="table-state">Loading submitted campaigns.</p>
        ) : null}
        {!pendingQuery.isLoading && campaigns.length === 0 ? (
          <div className="compliance-empty-state" role="status">
            <p>No submitted campaigns require review.</p>
            <p className="compliance-section-hint">
              New items appear when Campaign Managers submit drafts for compliance.
            </p>
          </div>
        ) : null}
        {!pendingQuery.isLoading && campaigns.length > 0 ? (
          <div className="table-scroll">
            <table className="compliance-queue-table" aria-label={COMPLIANCE_QUEUE_TABLE_ARIA_LABEL}>
              <caption className="sr-only">
                Submitted campaigns waiting for compliance approve or reject
              </caption>
              <thead>
                <tr>
                  <th scope="col">Campaign</th>
                  <th scope="col">Owner</th>
                  <th scope="col">Segment</th>
                  <th scope="col">Channel</th>
                  <th scope="col">Status</th>
                  <th scope="col">Last updated</th>
                  <th scope="col">Select</th>
                </tr>
              </thead>
              <tbody>
                {campaigns.map((campaign) => {
                  const isSelected = selectedCampaign?.id === campaign.id;
                  return (
                    <tr
                      key={campaign.id}
                      className={isSelected ? "selected-table-row" : undefined}
                      aria-selected={isSelected}
                      tabIndex={0}
                      onClick={() => selectCampaign(campaign.id)}
                      onKeyDown={(event: KeyboardEvent<HTMLTableRowElement>) => {
                        if (event.key === "Enter" || event.key === " ") {
                          event.preventDefault();
                          selectCampaign(campaign.id);
                        }
                      }}
                    >
                      <th scope="row">
                        <span className="table-primary-text">{campaign.name}</span>
                        <span className="table-secondary-text">{campaign.objective}</span>
                      </th>
                      <td>{campaign.ownerFullName ?? "Unassigned"}</td>
                      <td>{campaign.segmentName ?? "No segment"}</td>
                      <td>{formatCampaignEnum(campaign.channel)}</td>
                      <td>
                        <CampaignStatusBadge status={campaign.status} />
                      </td>
                      <td>{formatDateTime(campaign.updatedAt)}</td>
                      <td>
                        <button
                          type="button"
                          className="secondary-button"
                          aria-pressed={isSelected}
                          onClick={(event) => {
                            event.stopPropagation();
                            selectCampaign(campaign.id);
                          }}
                        >
                          {isSelected ? "Selected" : "Review"}
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        ) : null}
      </section>

      {selectedCampaign != null ? (
        <section className="panel" aria-labelledby="decision-heading">
          <div className="section-heading">
            <h2 id="decision-heading">Review decision</h2>
            <span>{selectedCampaign.name}</span>
          </div>

          <CampaignReviewDetails
            campaign={selectedCampaign}
            recipientSummary={recipientSummaryQuery.data}
            recipientSummaryLoading={recipientSummaryQuery.isLoading}
            recipientSummaryError={recipientSummaryQuery.isError}
          />

          <div className="compliance-decision-grid">
            <section
              className="compliance-decision-card compliance-decision-card--approve"
              aria-labelledby="approve-decision-heading"
            >
              <h3 id="approve-decision-heading">Approve</h3>
              <p className="compliance-decision-outcome">
                {complianceDecisionOutcome("approve")}
              </p>
              <label>
                {COMPLIANCE_REVIEW_NOTES_LABEL}
                <span className="compliance-field-hint">Optional — stored with the approval</span>
                <textarea
                  aria-label={COMPLIANCE_REVIEW_NOTES_LABEL}
                  value={reviewNotes}
                  onChange={(event) => setReviewNotes(event.target.value)}
                />
              </label>
              <button type="button" disabled={isBusy} onClick={requestApprove}>
                {COMPLIANCE_APPROVE_BUTTON_LABEL}
              </button>
            </section>

            <section
              className="compliance-decision-card compliance-decision-card--reject"
              aria-labelledby="reject-decision-heading"
            >
              <h3 id="reject-decision-heading">Reject</h3>
              <p className="compliance-decision-outcome">
                {complianceDecisionOutcome("reject")}
              </p>
              <label>
                Rejection reason
                <span className="compliance-field-hint">Required — shown to the campaign owner</span>
                <input
                  aria-label="Rejection reason"
                  value={rejectForm.rejectionReason}
                  aria-invalid={Boolean(rejectErrors.rejectionReason)}
                  onChange={(event) => {
                    setRejectErrors((current) => ({ ...current, rejectionReason: undefined }));
                    setRejectForm({ ...rejectForm, rejectionReason: event.target.value });
                  }}
                />
                <FieldError message={rejectErrors.rejectionReason} />
              </label>
              <label>
                Rejection notes
                <span className="compliance-field-hint">Optional guidance for the manager</span>
                <textarea
                  aria-label="Rejection compliance notes"
                  value={rejectForm.complianceReviewNotes}
                  onChange={(event) =>
                    setRejectForm({ ...rejectForm, complianceReviewNotes: event.target.value })
                  }
                />
              </label>
              <button
                type="button"
                className="danger-button"
                disabled={isBusy}
                onClick={requestReject}
              >
                {COMPLIANCE_REJECT_BUTTON_LABEL}
              </button>
            </section>
          </div>
        </section>
      ) : null}

      {pendingDecision != null && selectedCampaign != null ? (
        <ConfirmationDialog
          id={`compliance-${pendingDecision}-confirmation`}
          title={complianceDecisionConfirmTitle(pendingDecision)}
          description={
            <div className="compliance-confirm-body">
              <p>
                Campaign: <strong>{selectedCampaign.name}</strong>
              </p>
              <p>{complianceDecisionOutcome(pendingDecision)}</p>
              {pendingDecision === "reject" ? (
                <p>
                  Reason: <strong>{rejectForm.rejectionReason.trim()}</strong>
                </p>
              ) : null}
              {pendingDecision === "approve" && reviewNotes.trim() !== "" ? (
                <p>
                  Notes: <strong>{reviewNotes.trim()}</strong>
                </p>
              ) : null}
            </div>
          }
          confirmLabel={complianceDecisionConfirmLabel(pendingDecision)}
          confirmVariant={pendingDecision === "reject" ? "danger" : "primary"}
          busy={isBusy}
          onCancel={() => setPendingDecision(null)}
          onConfirm={confirmPendingDecision}
        />
      ) : null}
    </section>
  );
}

function CampaignReviewDetails({
  campaign,
  recipientSummary,
  recipientSummaryLoading,
  recipientSummaryError,
}: {
  campaign: CampaignView;
  recipientSummary: CampaignRecipientSummaryView | undefined;
  recipientSummaryLoading: boolean;
  recipientSummaryError: boolean;
}) {
  return (
    <div className="compliance-review-details" aria-label="Selected campaign review details">
      <section className="compliance-detail-block" aria-labelledby="compliance-overview-heading">
        <h3 id="compliance-overview-heading">Campaign overview</h3>
        <dl className="detail-list">
          <div>
            <dt>Owner</dt>
            <dd>{campaign.ownerFullName ?? "Unassigned"}</dd>
          </div>
          <div>
            <dt>Status</dt>
            <dd>
              <CampaignStatusBadge status={campaign.status} />
            </dd>
          </div>
          <div>
            <dt>Channel</dt>
            <dd>{formatCampaignEnum(campaign.channel)}</dd>
          </div>
          <div>
            <dt>Objective</dt>
            <dd>{campaign.objective}</dd>
          </div>
        </dl>
      </section>

      <section className="compliance-detail-block" aria-labelledby="compliance-message-heading">
        <h3 id="compliance-message-heading">Message content</h3>
        <dl className="detail-list">
          <div>
            <dt>Message subject</dt>
            <dd>{campaign.messageSubject ?? "Not provided"}</dd>
          </div>
          <div>
            <dt>Message body</dt>
            <dd className="compliance-message-body">{campaign.messageBody ?? "Not provided"}</dd>
          </div>
        </dl>
      </section>

      <section className="compliance-detail-block" aria-labelledby="compliance-audience-heading">
        <h3 id="compliance-audience-heading">Audience, products & eligibility</h3>
        <dl className="detail-list">
          <div>
            <dt>Segment</dt>
            <dd>{campaign.segmentName ?? "No segment"}</dd>
          </div>
          <div>
            <dt>Promoted products</dt>
            <dd>
              {campaign.productIds.length === 0
                ? "None selected"
                : `${campaign.productIds.length} product(s) linked`}
            </dd>
          </div>
          <div>
            <dt>Schedule</dt>
            <dd>{formatSchedule(campaign)}</dd>
          </div>
          <div>
            <dt>Recipient snapshot</dt>
            <dd>
              {recipientSummaryLoading
                ? "Loading eligibility snapshot…"
                : recipientSummaryError
                  ? "Eligibility snapshot unavailable — open full recipient preview."
                  : recipientSummary != null
                    ? `Eligible ${recipientSummary.eligible} · Excluded ${recipientSummary.excluded} · Sent ${recipientSummary.sent}`
                    : "No snapshot yet"}
            </dd>
          </div>
        </dl>
        <p className="compliance-section-hint">
          Open recipient preview to inspect exclusion reasons (opt-out, consent, do-not-contact,
          monthly limits) before approving.
        </p>
        <Link
          className="secondary-link-button"
          to={recipientPreviewPath(campaign.id)}
        >
          Open recipient preview
        </Link>
      </section>
    </div>
  );
}

function FieldError({ message }: { message?: string }) {
  return message == null ? null : <span className="field-error">{message}</span>;
}

function formatSchedule(campaign: CampaignView) {
  if (campaign.startDate == null && campaign.endDate == null) {
    return "Not scheduled";
  }
  return `${campaign.startDate ?? "Open"} → ${campaign.endDate ?? "Open"}`;
}

function formatDateTime(value: string | null) {
  if (value == null) {
    return "Not available";
  }
  return new Intl.DateTimeFormat("en", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function authorizationErrorMessage(...errors: unknown[]) {
  return errors.some(isAuthorizationError) ? "You are not authorized to review campaigns." : "";
}

function generalErrorMessage(...errors: unknown[]) {
  return errors.some(Boolean) ? "Compliance review action failed." : "";
}

export const complianceReviewValidationMessages = campaignFormValidationMessages;
