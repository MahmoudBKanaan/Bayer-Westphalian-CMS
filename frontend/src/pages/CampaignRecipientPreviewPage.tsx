import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { type KeyboardEvent, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  type CampaignRecipientSummaryView,
  type CampaignRecipientView,
  formatCampaignEnum,
  getCampaignRecipientSummary,
  getCampaign,
  listEligibleCampaignRecipients,
  listExcludedCampaignRecipients,
  launchCampaign,
  previewCampaignRecipients,
} from "@/api/campaigns";
import type { SegmentExclusionReasonSummary } from "@/api/segments";
import { CampaignStatusBadge } from "@/components/CampaignStatusBadge";
import { ConfirmationDialog } from "@/components/ConfirmationDialog";
import { ExclusionReasonSummaryPanel } from "@/components/ExclusionReasonSummaryPanel";
import { MetricCard } from "@/components/MetricCard";
import { SegmentPreviewResults } from "@/components/SegmentPreviewResults";
import { StatusBadge } from "@/components/StatusBadge";
import { usePermissions } from "@/features/auth/usePermissions";
import {
  CAMPAIGN_LAUNCHED_NOTICE,
  CAMPAIGN_LAUNCH_BUTTON_LABEL,
  CAMPAIGN_LAUNCH_CONFIRM_LABEL,
  CAMPAIGN_LAUNCH_CONFIRM_TITLE,
  CAMPAIGN_LAUNCH_FAILED_MESSAGE,
  CAMPAIGN_LAUNCH_READINESS_ARIA_LABEL,
  CAMPAIGN_LAUNCH_RESULT_HEADING,
  RECIPIENT_PREVIEW_GATE_NOTE,
  RECIPIENT_PREVIEW_GUIDE,
  RECIPIENT_PREVIEW_PAGE_LEAD,
  RECIPIENT_PREVIEW_PAGE_TITLE,
  evaluateLaunchReadiness,
} from "@/features/campaigns/campaignLaunchFlow";
import {
  formatEligibilityRateLabel,
  formatRecipientTabLabel,
  presentRecipientExclusionReason,
} from "@/features/campaigns/recipientPreviewClarity";
import { formatNumber } from "@/utils/format";

type RecipientPreviewTab = "preview" | "eligible" | "excluded";

/**
 * Campaign recipient preview (KB FR-054–055 / BR-006 / item 594 clarity).
 *
 * Shows eligibility-aware audience, excluded reasons, and launch readiness before contact.
 */
export function CampaignRecipientPreviewPage() {
  const { campaignId = "" } = useParams();
  const [activeTab, setActiveTab] = useState<RecipientPreviewTab>("preview");
  const [notice, setNotice] = useState("");
  const [confirmLaunchOpen, setConfirmLaunchOpen] = useState(false);
  const [showLaunchResult, setShowLaunchResult] = useState(false);
  const queryClient = useQueryClient();
  const permissions = usePermissions();
  const canReadCampaigns = permissions.canReadCampaigns();
  const canManageCampaigns = permissions.canManageCampaigns();

  const campaignQuery = useQuery({
    queryKey: ["campaign", campaignId],
    queryFn: () => getCampaign(campaignId),
    enabled: canReadCampaigns && campaignId !== "",
  });
  const previewQuery = useQuery({
    queryKey: ["campaign-recipient-preview", campaignId],
    queryFn: () => previewCampaignRecipients(campaignId),
    enabled: canReadCampaigns && campaignId !== "",
  });
  const eligibleRecipientsQuery = useQuery({
    queryKey: ["campaign-recipients", "eligible", campaignId],
    queryFn: () => listEligibleCampaignRecipients(campaignId),
    enabled: canReadCampaigns && campaignId !== "",
  });
  const excludedRecipientsQuery = useQuery({
    queryKey: ["campaign-recipients", "excluded", campaignId],
    queryFn: () => listExcludedCampaignRecipients(campaignId),
    enabled: canReadCampaigns && campaignId !== "",
  });
  const recipientSummaryQuery = useQuery({
    queryKey: ["campaign-recipients", "summary", campaignId],
    queryFn: () => getCampaignRecipientSummary(campaignId),
    enabled: canReadCampaigns && campaignId !== "",
  });
  const launchMutation = useMutation({
    mutationFn: () => launchCampaign(campaignId),
    onSuccess: async (launchedCampaign) => {
      setConfirmLaunchOpen(false);
      setShowLaunchResult(true);
      setNotice(CAMPAIGN_LAUNCHED_NOTICE);
      queryClient.setQueryData(["campaign", campaignId], launchedCampaign);
      await queryClient.invalidateQueries({
        queryKey: ["campaign-recipients", "summary", campaignId],
      });
      await queryClient.invalidateQueries({ queryKey: ["campaigns"] });
    },
  });

  const campaign = campaignQuery.data ?? null;
  const preview = previewQuery.data ?? null;
  const eligibleCount =
    recipientSummaryQuery.data?.eligible ??
    eligibleRecipientsQuery.data?.length ??
    preview?.eligibleCount ??
    null;
  const excludedCount =
    recipientSummaryQuery.data?.excluded ??
    excludedRecipientsQuery.data?.length ??
    preview?.excludedCount ??
    null;
  const totalAudience = preview?.totalAudienceCount ?? null;

  const launchReadiness = evaluateLaunchReadiness({
    canManageCampaigns,
    campaignStatus: campaign?.status,
    eligibleCount: eligibleCount ?? 0,
  });
  const canLaunchCampaign = canManageCampaigns && campaign?.status === "APPROVED";

  const recipientPreviewTabs = useMemo(
    () => [
      {
        id: "recipient-preview-tab",
        label: "Audience preview",
        panelId: "recipient-preview-panel",
        value: "preview" as const,
        count: totalAudience,
        loading: previewQuery.isLoading,
      },
      {
        id: "eligible-recipients-tab",
        label: formatRecipientTabLabel(
          "Eligible recipients",
          eligibleCount,
          eligibleRecipientsQuery.isLoading && eligibleCount == null,
        ),
        panelId: "eligible-recipients-panel",
        value: "eligible" as const,
        count: eligibleCount,
        loading: eligibleRecipientsQuery.isLoading,
      },
      {
        id: "excluded-recipients-tab",
        label: formatRecipientTabLabel(
          "Excluded recipients",
          excludedCount,
          excludedRecipientsQuery.isLoading && excludedCount == null,
        ),
        panelId: "excluded-recipients-panel",
        value: "excluded" as const,
        count: excludedCount,
        loading: excludedRecipientsQuery.isLoading,
      },
    ],
    [
      totalAudience,
      eligibleCount,
      excludedCount,
      previewQuery.isLoading,
      eligibleRecipientsQuery.isLoading,
      excludedRecipientsQuery.isLoading,
    ],
  );

  function focusRecipientPreviewTab(tab: RecipientPreviewTab) {
    const tabDefinition = recipientPreviewTabs.find((candidate) => candidate.value === tab);
    window.setTimeout(() => {
      document.getElementById(tabDefinition?.id ?? "")?.focus();
    }, 0);
  }

  function handleRecipientPreviewTabKeyDown(
    event: KeyboardEvent<HTMLButtonElement>,
    currentTab: RecipientPreviewTab,
  ) {
    const currentIndex = recipientPreviewTabs.findIndex((tab) => tab.value === currentTab);
    let nextIndex: number;

    if (event.key === "ArrowRight" || event.key === "ArrowDown") {
      nextIndex = (currentIndex + 1) % recipientPreviewTabs.length;
    } else if (event.key === "ArrowLeft" || event.key === "ArrowUp") {
      nextIndex =
        (currentIndex - 1 + recipientPreviewTabs.length) % recipientPreviewTabs.length;
    } else if (event.key === "Home") {
      nextIndex = 0;
    } else if (event.key === "End") {
      nextIndex = recipientPreviewTabs.length - 1;
    } else {
      return;
    }

    event.preventDefault();
    const nextTab = recipientPreviewTabs[nextIndex];
    if (nextTab == null) {
      return;
    }

    setActiveTab(nextTab.value);
    focusRecipientPreviewTab(nextTab.value);
  }

  if (!canReadCampaigns) {
    return (
      <section className="panel recipient-preview-page" aria-labelledby="recipient-preview-title">
        <div className="section-heading">
          <h2 id="recipient-preview-title">{RECIPIENT_PREVIEW_PAGE_TITLE}</h2>
          <span>Campaign audience eligibility</span>
        </div>
        <p className="form-error" role="alert">
          You are not authorized to view campaign recipient previews.
        </p>
      </section>
    );
  }

  const errorMessage =
    campaignQuery.isError || previewQuery.isError
      ? "Campaign recipient preview could not be loaded."
      : undefined;

  return (
    <section
      className="page-stack recipient-preview-page"
      aria-labelledby="recipient-preview-title"
    >
      <header className="panel recipient-preview-header">
        <div className="section-heading">
          <h2 id="recipient-preview-title">{RECIPIENT_PREVIEW_PAGE_TITLE}</h2>
          <span>Campaign-scoped eligibility before launch</span>
        </div>
        <p className="recipient-preview-lead">{RECIPIENT_PREVIEW_PAGE_LEAD}</p>
        <p className="recipient-preview-gate" role="note">
          {RECIPIENT_PREVIEW_GATE_NOTE}
        </p>
        <div className="button-row">
          <Link className="table-action-link" to="/campaigns">
            Back to campaigns
          </Link>
          {canManageCampaigns ? (
            <button
              type="button"
              disabled={!canLaunchCampaign || launchMutation.isPending}
              onClick={() => {
                setNotice("");
                setConfirmLaunchOpen(true);
              }}
            >
              {CAMPAIGN_LAUNCH_BUTTON_LABEL}
            </button>
          ) : null}
        </div>
        <p
          className={
            launchReadiness.state === "ready"
              ? "recipient-launch-readiness recipient-launch-readiness--ready"
              : "recipient-launch-readiness recipient-launch-readiness--blocked"
          }
          role="status"
          aria-label={CAMPAIGN_LAUNCH_READINESS_ARIA_LABEL}
        >
          {launchReadiness.message}
        </p>
        {notice ? (
          <p className="form-success" role="status" data-testid="campaign-launch-notice">
            {notice}
          </p>
        ) : null}
        {launchMutation.isError ? (
          <p className="form-error" role="alert">
            {CAMPAIGN_LAUNCH_FAILED_MESSAGE}
          </p>
        ) : null}
      </header>

      {confirmLaunchOpen && campaign != null ? (
        <LaunchConfirmationDialog
          campaignName={campaign.name}
          eligibleCount={eligibleCount ?? 0}
          excludedCount={excludedCount ?? 0}
          isLaunching={launchMutation.isPending}
          onCancel={() => setConfirmLaunchOpen(false)}
          onConfirm={() => launchMutation.mutate()}
        />
      ) : null}

      {showLaunchResult ? (
        <LaunchResultPanel
          summary={recipientSummaryQuery.data ?? null}
          loading={recipientSummaryQuery.isLoading || recipientSummaryQuery.isFetching}
          error={recipientSummaryQuery.isError}
        />
      ) : null}

      <section className="panel" aria-labelledby="recipient-preview-guide-heading">
        <div className="section-heading">
          <h2 id="recipient-preview-guide-heading">How to read this preview</h2>
          <span>Three views of the same campaign audience</span>
        </div>
        <ol className="recipient-preview-guide" aria-label="Recipient preview guide">
          {RECIPIENT_PREVIEW_GUIDE.map((item, index) => (
            <li key={item.id} className="recipient-preview-guide-item">
              <span className="recipient-preview-guide-index" aria-hidden="true">
                {index + 1}
              </span>
              <div>
                <strong>{item.title}</strong>
                <p>{item.description}</p>
              </div>
            </li>
          ))}
        </ol>
      </section>

      <section className="panel" aria-labelledby="recipient-preview-campaign-heading">
        <div className="section-heading">
          <h2 id="recipient-preview-campaign-heading">
            {campaignQuery.isLoading ? "Loading campaign" : (campaign?.name ?? "Campaign")}
          </h2>
          <span>
            {campaign?.objective ?? "Audience preview uses the campaign selected segment"}
          </span>
        </div>
        {campaign == null && campaignQuery.isLoading ? (
          <p className="table-state">Loading campaign details.</p>
        ) : null}
        {campaign != null ? (
          <div
            className="metric-grid segment-preview-metrics"
            aria-label="Campaign preview context"
          >
            <div className="metric-card">
              <span>Status</span>
              <strong>
                <CampaignStatusBadge status={campaign.status} />
              </strong>
              <small>Current lifecycle state</small>
            </div>
            <div className="metric-card">
              <span>Segment</span>
              <strong>{campaign.segmentName ?? "No segment"}</strong>
              <small>Selected campaign audience</small>
            </div>
            <div className="metric-card">
              <span>Channel</span>
              <strong>{formatCampaignEnum(campaign.channel)}</strong>
              <small>Eligibility consent channel</small>
            </div>
          </div>
        ) : null}
      </section>

      <section className="panel" aria-labelledby="recipient-snapshot-heading">
        <div className="section-heading">
          <h2 id="recipient-snapshot-heading">Audience snapshot</h2>
          <span>
            {preview != null
              ? formatEligibilityRateLabel(preview.eligibleCount, preview.totalAudienceCount)
              : "Eligibility totals for this campaign"}
          </span>
        </div>
        <div className="metric-grid segment-preview-metrics" aria-label="Audience snapshot metrics">
          <MetricCard
            label="Total matched"
            value={formatNumber(totalAudience ?? 0)}
            detail="Segment criteria matches before eligibility"
            tone="inventory"
          />
          <MetricCard
            label="Eligible"
            value={formatNumber(eligibleCount ?? 0)}
            detail="Contactable after eligibility rules"
            tone="engagement"
          />
          <MetricCard
            label="Excluded"
            value={formatNumber(excludedCount ?? 0)}
            detail="Blocked — open the Excluded tab for reasons"
            tone="default"
          />
          <MetricCard
            label="Sent"
            value={formatNumber(recipientSummaryQuery.data?.sent ?? 0)}
            detail="Contact events after launch (if any)"
            tone="financial"
          />
        </div>
        <p className="recipient-preview-section-hint">
          Prefer eligible counts over total matched audience for launch decisions. Total matched is
          criteria-only and is not the final contactable set (FR-054 / FR-055).
        </p>
      </section>

      <section className="panel" aria-labelledby="recipient-preview-tabs-heading">
        <div className="section-heading">
          <h2 id="recipient-preview-tabs-heading">Recipient review</h2>
          <span>Preview, eligible, and excluded recipient rows</span>
        </div>
        <div className="tab-list" role="tablist" aria-label="Campaign recipient review tabs">
          {recipientPreviewTabs.map((tab) => (
            <button
              type="button"
              className={activeTab === tab.value ? "tab-button active" : "tab-button"}
              role="tab"
              aria-selected={activeTab === tab.value}
              aria-controls={tab.panelId}
              id={tab.id}
              key={tab.value}
              tabIndex={activeTab === tab.value ? 0 : -1}
              onClick={() => setActiveTab(tab.value)}
              onKeyDown={(event) => handleRecipientPreviewTabKeyDown(event, tab.value)}
            >
              {tab.label}
            </button>
          ))}
        </div>
        <p className="recipient-preview-section-hint">
          Use arrow keys to move between tabs. Exclusion reason codes are stable for audit and
          compliance (BR-006).
        </p>
      </section>

      {activeTab === "preview" ? (
        <div id="recipient-preview-panel" role="tabpanel" aria-labelledby="recipient-preview-tab">
          <SegmentPreviewResults
            preview={previewQuery.data ?? null}
            sourceLabel={campaign?.segmentName ?? "Campaign audience"}
            isLoading={previewQuery.isLoading}
            errorMessage={errorMessage}
          />
        </div>
      ) : activeTab === "eligible" ? (
        <EligibleRecipientsTab
          recipients={eligibleRecipientsQuery.data ?? []}
          loading={eligibleRecipientsQuery.isLoading}
          error={eligibleRecipientsQuery.isError}
        />
      ) : (
        <ExcludedRecipientsTab
          recipients={excludedRecipientsQuery.data ?? []}
          loading={excludedRecipientsQuery.isLoading}
          error={excludedRecipientsQuery.isError}
        />
      )}
    </section>
  );
}

function LaunchResultPanel({
  summary,
  loading,
  error,
}: {
  summary: CampaignRecipientSummaryView | null;
  loading: boolean;
  error: boolean;
}) {
  return (
    <section className="panel" aria-labelledby="launch-result-heading">
      <div className="section-heading">
        <h2 id="launch-result-heading">{CAMPAIGN_LAUNCH_RESULT_HEADING}</h2>
        <span>{loading ? "Loading delivery summary" : "Campaign launch response summary"}</span>
      </div>
      {loading ? <p className="table-state">Loading launch result summary.</p> : null}
      {error ? (
        <p className="form-error" role="alert">
          Launch result summary could not be loaded.
        </p>
      ) : null}
      {!loading && !error && summary != null ? (
        <div className="metric-grid segment-preview-metrics" aria-label="Launch result metrics">
          <MetricCard
            label="Eligible"
            value={formatNumber(summary.eligible)}
            detail="Recipients available for launch"
            tone="engagement"
          />
          <MetricCard
            label="Excluded"
            value={formatNumber(summary.excluded)}
            detail="Recipients blocked by eligibility rules"
          />
          <MetricCard
            label="Sent"
            value={formatNumber(summary.sent)}
            detail="Contact events created"
            tone="financial"
          />
          <MetricCard
            label="Failed"
            value={formatNumber(summary.failed)}
            detail="Recipients that failed during launch"
          />
        </div>
      ) : null}
    </section>
  );
}

function LaunchConfirmationDialog({
  campaignName,
  eligibleCount,
  excludedCount,
  isLaunching,
  onCancel,
  onConfirm,
}: {
  campaignName: string;
  eligibleCount: number;
  excludedCount: number;
  isLaunching: boolean;
  onCancel: () => void;
  onConfirm: () => void;
}) {
  return (
    <ConfirmationDialog
      id="launch-confirmation"
      title={CAMPAIGN_LAUNCH_CONFIRM_TITLE}
      description={
        <>
          <p>
            <strong>{campaignName}</strong>
          </p>
          <p>
            Launching will contact <strong>{formatNumber(eligibleCount)}</strong> eligible
            recipients and move the campaign to <strong>ACTIVE</strong>.{" "}
            <strong>{formatNumber(excludedCount)}</strong> excluded recipients will not be
            contacted.
          </p>
          <p>This does not re-run eligibility from scratch; it uses the stored recipient snapshot.</p>
        </>
      }
      confirmLabel={CAMPAIGN_LAUNCH_CONFIRM_LABEL}
      confirmVariant="primary"
      busy={isLaunching}
      onCancel={onCancel}
      onConfirm={onConfirm}
    />
  );
}

function EligibleRecipientsTab({
  recipients,
  loading,
  error,
}: {
  recipients: CampaignRecipientView[];
  loading: boolean;
  error: boolean;
}) {
  return (
    <section
      className="panel"
      id="eligible-recipients-panel"
      role="tabpanel"
      aria-labelledby="eligible-recipients-tab"
    >
      <div className="section-heading">
        <h2>Eligible recipients</h2>
        <span>
          {loading ? "Loading stored recipients" : `${recipients.length} contactable rows`}
        </span>
      </div>
      <p className="recipient-preview-section-hint">
        These recipients passed eligibility checks for this campaign. After launch, status may show
        Sent when a contact event exists.
      </p>
      {loading ? <p className="table-state">Loading eligible recipients.</p> : null}
      {error ? (
        <p className="form-error" role="alert">
          Eligible recipients could not be loaded.
        </p>
      ) : null}
      {!loading && !error && recipients.length === 0 ? (
        <div className="recipient-preview-empty" role="status">
          <p>No eligible recipients have been generated for this campaign.</p>
          <p className="recipient-preview-section-hint">
            Run or refresh audience preview, then check the Excluded tab for blocking reasons.
          </p>
        </div>
      ) : null}
      {!loading && !error && recipients.length > 0 ? (
        <div className="table-scroll">
          <table
            className="recipient-preview-table"
            aria-label="Eligible campaign recipients table"
          >
            <caption className="sr-only">
              Eligible campaign recipients with status and delivery timestamps
            </caption>
            <thead>
              <tr>
                <th scope="col">Customer</th>
                <th scope="col">Status</th>
                <th scope="col">Explanation</th>
                <th scope="col">Sent</th>
                <th scope="col">Created</th>
              </tr>
            </thead>
            <tbody>
              {recipients.map((recipient) => (
                <tr key={recipient.id}>
                  <th scope="row">
                    <span className="table-primary-text">{recipient.customerFullName}</span>
                    <span className="table-secondary-text">{recipient.customerId}</span>
                  </th>
                  <td>
                    <StatusBadge value={formatCampaignEnum(recipient.eligibilityStatus)} />
                  </td>
                  <td>{recipient.eligibilityExplanation ?? "Eligible for campaign contact"}</td>
                  <td>{formatDateTime(recipient.sentAt)}</td>
                  <td>{formatDateTime(recipient.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </section>
  );
}

function ExcludedRecipientsTab({
  recipients,
  loading,
  error,
}: {
  recipients: CampaignRecipientView[];
  loading: boolean;
  error: boolean;
}) {
  return (
    <section
      className="panel"
      id="excluded-recipients-panel"
      role="tabpanel"
      aria-labelledby="excluded-recipients-tab"
    >
      <div className="section-heading">
        <h2>Excluded recipients</h2>
        <span>{loading ? "Loading exclusions" : `${recipients.length} excluded rows`}</span>
      </div>
      <p className="recipient-preview-section-hint">
        Each row includes a stable reason code and a plain-language explanation for compliance
        review (BR-001–003, BR-010–011).
      </p>
      {loading ? <p className="table-state">Loading excluded recipients.</p> : null}
      {error ? (
        <p className="form-error" role="alert">
          Excluded recipients could not be loaded.
        </p>
      ) : null}
      {!loading && !error && recipients.length === 0 ? (
        <div className="recipient-preview-empty" role="status">
          <p>No excluded recipients have been generated for this campaign.</p>
        </div>
      ) : null}
      {!loading && !error ? (
        <ExclusionReasonSummaryPanel
          reasons={summarizeExcludedRecipientReasons(recipients)}
          excludedCount={recipients.length}
          compactEmpty
        />
      ) : null}
      {!loading && !error && recipients.length > 0 ? (
        <div className="table-scroll">
          <table
            className="recipient-preview-table"
            aria-label="Excluded campaign recipients table"
          >
            <caption className="sr-only">
              Excluded campaign recipients with reason codes and explanations
            </caption>
            <thead>
              <tr>
                <th scope="col">Customer</th>
                <th scope="col">Status</th>
                <th scope="col">Reason</th>
                <th scope="col">Explanation</th>
                <th scope="col">Created</th>
              </tr>
            </thead>
            <tbody>
              {recipients.map((recipient) => {
                const reason = presentRecipientExclusionReason(recipient.exclusionReason);
                return (
                  <tr key={recipient.id}>
                    <th scope="row">
                      <span className="table-primary-text">{recipient.customerFullName}</span>
                      <span className="table-secondary-text">{recipient.customerId}</span>
                    </th>
                    <td>
                      <StatusBadge value={formatCampaignEnum(recipient.eligibilityStatus)} />
                    </td>
                    <td>
                      <span className="recipient-reason-cell">
                        <span className="recipient-reason-title">{reason.title}</span>
                        <span className="recipient-reason-code">{reason.code}</span>
                      </span>
                    </td>
                    <td>{recipient.eligibilityExplanation ?? "No explanation recorded"}</td>
                    <td>{formatDateTime(recipient.createdAt)}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      ) : null}
    </section>
  );
}

function summarizeExcludedRecipientReasons(
  recipients: CampaignRecipientView[],
): SegmentExclusionReasonSummary[] {
  const summaries = new Map<string, SegmentExclusionReasonSummary>();
  for (const recipient of recipients) {
    const code = recipient.exclusionReason?.trim() || "UNKNOWN";
    const current = summaries.get(code);
    summaries.set(code, {
      code,
      message:
        current?.message ?? recipient.eligibilityExplanation ?? "No exclusion explanation recorded",
      count: (current?.count ?? 0) + 1,
    });
  }
  return [...summaries.values()];
}

function formatDateTime(value: string | null) {
  if (value == null) {
    return "Not recorded";
  }
  return new Intl.DateTimeFormat("en", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}
