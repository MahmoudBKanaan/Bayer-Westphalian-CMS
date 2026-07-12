import type { SegmentPreviewView } from "@/api/segments";
import { ExclusionReasonSummaryPanel } from "@/components/ExclusionReasonSummaryPanel";
import { MetricCard } from "@/components/MetricCard";
import { StatusBadge } from "@/components/StatusBadge";
import { formatNumber } from "@/utils/format";

export type SegmentPreviewResultsProps = {
  preview: SegmentPreviewView | null;
  sourceLabel?: string;
  isLoading?: boolean;
  errorMessage?: string;
};

/**
 * Eligibility-aware segment audience preview results (KB FR-079, FR-054, FR-055).
 *
 * Production gate (item 208): displays backend preview after EligibilityService — never treat
 * total/criteria-only matches as a final campaign audience; use eligible counts/customers.
 */
export function SegmentPreviewResults({
  preview,
  sourceLabel,
  isLoading = false,
  errorMessage,
}: SegmentPreviewResultsProps) {
  if (isLoading) {
    return (
      <section className="panel" aria-labelledby="segment-preview-heading" aria-busy="true">
        <div className="section-heading">
          <h2 id="segment-preview-heading">Audience preview</h2>
          <span>Running eligibility checks…</span>
        </div>
        <p className="table-state">Loading audience preview…</p>
      </section>
    );
  }

  if (errorMessage) {
    return (
      <section className="panel" aria-labelledby="segment-preview-heading">
        <div className="section-heading">
          <h2 id="segment-preview-heading">Audience preview</h2>
          <span>Preview failed</span>
        </div>
        <p className="form-error" role="alert">
          {errorMessage}
        </p>
      </section>
    );
  }

  if (preview == null) {
    return (
      <section className="panel" aria-labelledby="segment-preview-heading">
        <div className="section-heading">
          <h2 id="segment-preview-heading">Audience preview</h2>
          <span>Eligibility-aware audience size</span>
        </div>
        <div className="state-panel" role="status">
          <strong>No preview yet</strong>
          <p>
            Run preview on draft criteria or a saved segment to see total audience, eligible and
            excluded counts, and matching customers.
          </p>
        </div>
      </section>
    );
  }

  const eligibilityRate =
    preview.totalAudienceCount === 0
      ? 0
      : (preview.eligibleCount / preview.totalAudienceCount) * 100;

  return (
    <section className="panel" aria-labelledby="segment-preview-heading">
      <div className="section-heading">
        <h2 id="segment-preview-heading">Audience preview</h2>
        <span>
          {sourceLabel
            ? `Source: ${sourceLabel}`
            : "Criteria matches filtered by EligibilityService"}
        </span>
      </div>

      <div className="metric-grid segment-preview-metrics" aria-label="Audience size metrics">
        <MetricCard
          label="Total audience"
          value={formatNumber(preview.totalAudienceCount)}
          detail="Criteria matches before eligibility"
        />
        <MetricCard
          label="Eligible"
          value={formatNumber(preview.eligibleCount)}
          detail="Contactable after eligibility checks"
        />
        <MetricCard
          label="Excluded"
          value={formatNumber(preview.excludedCount)}
          detail="Blocked by DNC, consent, limits, etc."
        />
        <MetricCard
          label="Eligibility rate"
          value={`${eligibilityRate.toFixed(0)}%`}
          detail={
            preview.totalAudienceCount === 0
              ? "No criteria matches"
              : `${preview.eligibleCount} of ${preview.totalAudienceCount} contactable`
          }
        />
      </div>

      <div className="section-heading">
        <h3>Eligible customers ({preview.matchingCustomers.length})</h3>
        <span>Shown for campaign targeting preview</span>
      </div>

      {preview.matchingCustomers.length === 0 ? (
        <p className="table-state">
          {preview.totalAudienceCount === 0
            ? "No customers matched the current criteria."
            : "All matched customers were excluded by eligibility rules."}
        </p>
      ) : (
        <div className="table-scroll">
          <table aria-label="Eligible customers preview table">
            <thead>
              <tr>
                <th scope="col">Name</th>
                <th scope="col">Type</th>
                <th scope="col">Location</th>
                <th scope="col">Status</th>
                <th scope="col">Contact</th>
              </tr>
            </thead>
            <tbody>
              {preview.matchingCustomers.map((customer) => (
                <tr key={customer.id}>
                  <th scope="row">
                    <span className="table-primary-text">{customer.fullName}</span>
                    <span className="table-secondary-text">
                      {customer.email ?? "No email on file"}
                    </span>
                  </th>
                  <td>{formatEnumLabel(customer.customerType)}</td>
                  <td>{formatLocation(customer.city, customer.country)}</td>
                  <td>
                    <StatusBadge value={formatEnumLabel(customer.status)} />
                  </td>
                  <td>
                    <StatusBadge value={customer.doNotContact ? "Do not contact" : "Allowed"} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <ExclusionReasonSummaryPanel
        reasons={preview.exclusionReasonSummary}
        excludedCount={preview.excludedCount}
        compactEmpty
      />
    </section>
  );
}

function formatLocation(city: string | null, country: string | null) {
  if (city && country) {
    return `${city}, ${country}`;
  }
  return city ?? country ?? "—";
}

function formatEnumLabel(value: string) {
  return value
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}
