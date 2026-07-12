import type { SegmentExclusionReasonSummary } from "@/api/segments";
import {
  presentExclusionReasons,
  summarizeExclusionTotals,
} from "@/features/segments/exclusionReasons";
import { formatNumber, formatPercent } from "@/utils/format";

export type ExclusionReasonSummaryPanelProps = {
  reasons: SegmentExclusionReasonSummary[];
  excludedCount: number;
  /** When true, show compact empty copy for previews with no exclusions. */
  compactEmpty?: boolean;
};

/**
 * Visual summary of eligibility exclusion reasons for segment/campaign previews (KB BR-006, FR-055).
 */
export function ExclusionReasonSummaryPanel({
  reasons,
  excludedCount,
  compactEmpty = false,
}: ExclusionReasonSummaryPanelProps) {
  const presented = presentExclusionReasons(reasons, excludedCount);
  const totals = summarizeExclusionTotals(reasons, excludedCount);

  return (
    <section className="exclusion-summary-panel" aria-labelledby="exclusion-reason-summary-heading">
      <div className="section-heading">
        <h3 id="exclusion-reason-summary-heading">Exclusion reason summary</h3>
        <span>
          {totals.excludedCount === 0
            ? "No exclusions"
            : `${formatNumber(totals.excludedCount)} excluded · ${totals.reasonGroups} reason group${totals.reasonGroups === 1 ? "" : "s"}`}
        </span>
      </div>

      <p className="table-state">
        Stable eligibility reason codes used for audience preview and campaign recipient review
        (BR-001–003, BR-010–011, FR-034/055).
      </p>

      {presented.length === 0 ? (
        <div className="state-panel" role="status">
          <strong>{excludedCount === 0 ? "No customers excluded" : "Reasons unavailable"}</strong>
          <p>
            {excludedCount === 0
              ? compactEmpty
                ? "All matched customers remain eligible for contact."
                : "No customers were excluded for this preview."
              : "Customers were excluded, but no detailed reason breakdown was returned."}
          </p>
        </div>
      ) : (
        <>
          <ul className="exclusion-reason-cards" aria-label="Exclusion reason cards">
            {presented.map((reason) => (
              <li
                key={reason.code}
                className={`exclusion-reason-card exclusion-reason-card--${reason.severity}`}
              >
                <div className="exclusion-reason-card-header">
                  <div>
                    <span className="eyebrow">{reason.code}</span>
                    <strong>{reason.title}</strong>
                  </div>
                  <div className="exclusion-reason-count" aria-label={`${reason.title} count`}>
                    <span className="exclusion-reason-count-value">
                      {formatNumber(reason.count)}
                    </span>
                    <span className="exclusion-reason-count-label">excluded</span>
                  </div>
                </div>
                <p className="exclusion-reason-message">{reason.message}</p>
                <p className="exclusion-reason-hint">{reason.ruleHint}</p>
                <div
                  className="exclusion-reason-bar"
                  role="meter"
                  aria-label={`Share of exclusions for ${reason.title}`}
                  aria-valuemin={0}
                  aria-valuemax={100}
                  aria-valuenow={Math.round(reason.shareOfExcluded)}
                  aria-valuetext={`${formatPercent(reason.shareOfExcluded)} of exclusions`}
                >
                  <div
                    className="exclusion-reason-bar-fill"
                    style={{ width: `${Math.min(100, Math.max(0, reason.shareOfExcluded))}%` }}
                  />
                </div>
                <span className="table-secondary-text">
                  {formatPercent(reason.shareOfExcluded)} of excluded audience
                </span>
              </li>
            ))}
          </ul>

          <div className="table-scroll">
            <table aria-label="Exclusion reason summary table">
              <thead>
                <tr>
                  <th scope="col">Reason</th>
                  <th scope="col">Code</th>
                  <th scope="col">Explanation</th>
                  <th scope="col">Count</th>
                  <th scope="col">Share</th>
                </tr>
              </thead>
              <tbody>
                {presented.map((reason) => (
                  <tr key={`row-${reason.code}`}>
                    <th scope="row">{reason.title}</th>
                    <td>
                      <code>{reason.code}</code>
                    </td>
                    <td>{reason.message}</td>
                    <td>{formatNumber(reason.count)}</td>
                    <td>{formatPercent(reason.shareOfExcluded)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </section>
  );
}
