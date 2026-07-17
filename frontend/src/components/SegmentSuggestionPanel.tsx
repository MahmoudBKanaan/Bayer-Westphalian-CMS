import { useMutation } from "@tanstack/react-query";
import {
  generateSegmentSuggestions,
  mapSuggestedCriteriaToSegmentPayload,
  type SegmentSuggestionView,
} from "@/api/ai";
import { isAuthorizationError } from "@/api/client";
import { AiExplanationDisplay } from "@/components/AiExplanationDisplay";
import { usePermissions } from "@/features/auth/usePermissions";
import type { SegmentCriteriaPayload } from "@/api/segments";

/**
 * Segments page AI-002 suggestions (Sprint 13 evidence).
 * Applying criteria only fills a draft form — never auto-saves a segment.
 */
export function SegmentSuggestionPanel({
  onApplyToDraft,
}: {
  onApplyToDraft: (input: {
    name: string;
    description: string;
    criteria: SegmentCriteriaPayload[];
    sourceLabel: string;
  }) => void;
}) {
  const permissions = usePermissions();
  const canUse = permissions.canUseAiSegmentSuggestions();
  const canSaveOperational = permissions.canCreateSegments();

  const mutation = useMutation({
    mutationFn: () => generateSegmentSuggestions({ expirationWithinMonths: 3 }),
  });

  if (!canUse) {
    return null;
  }

  const suggestions = mutation.data?.suggestions ?? [];
  const errorMessage = mutation.isError
    ? isAuthorizationError(mutation.error)
      ? "You are not authorized to generate segment suggestions."
      : "Segment suggestions could not be generated."
    : "";

  return (
    <section className="panel" aria-labelledby="ai-segment-suggestions-heading">
      <div className="section-heading">
        <h2 id="ai-segment-suggestions-heading">AI segment suggestions</h2>
        <span>Rule-based audience ideas (AI-002)</span>
      </div>
      <p className="table-secondary-text">
        Structured criteria for human review. Suggestions do not create or save segments until you
        explicitly apply and save a draft.
        {!canSaveOperational
          ? " Your role can preview suggestions but may not save operational segments alone."
          : ""}
      </p>
      <div className="form-actions">
        <button
          type="button"
          onClick={() => mutation.mutate()}
          disabled={mutation.isPending}
          aria-label="Generate segment suggestions"
        >
          {mutation.isPending ? "Generating…" : "Generate segment suggestions"}
        </button>
      </div>
      {errorMessage ? (
        <p className="form-error" role="alert">
          {errorMessage}
        </p>
      ) : null}
      {mutation.isSuccess && suggestions.length === 0 ? (
        <p className="table-state">No segment suggestions available for the current signals.</p>
      ) : null}
      {suggestions.length > 0 ? (
        <ul className="ai-recommendation-grid" aria-label="Segment suggestion results">
          {suggestions.map((suggestion) => (
            <SegmentSuggestionCard
              key={suggestion.storedRecommendationId ?? suggestion.suggestedName}
              suggestion={suggestion}
              canApply={canSaveOperational || canUse}
              onApply={() =>
                onApplyToDraft({
                  name: suggestion.suggestedName,
                  description: suggestion.description ?? suggestion.explanation,
                  criteria: mapSuggestedCriteriaToSegmentPayload(
                    suggestion.suggestedCriteria ?? [],
                  ),
                  sourceLabel: `AI suggestion: ${suggestion.suggestedName}`,
                })
              }
              onPreview={() =>
                onApplyToDraft({
                  name: suggestion.suggestedName,
                  description: suggestion.description ?? suggestion.explanation,
                  criteria: mapSuggestedCriteriaToSegmentPayload(
                    suggestion.suggestedCriteria ?? [],
                  ),
                  sourceLabel: `AI preview: ${suggestion.suggestedName}`,
                })
              }
            />
          ))}
        </ul>
      ) : null}
    </section>
  );
}

function SegmentSuggestionCard({
  suggestion,
  canApply,
  onApply,
  onPreview,
}: {
  suggestion: SegmentSuggestionView;
  canApply: boolean;
  onApply: () => void;
  onPreview: () => void;
}) {
  const summary =
    suggestion.suggestedCriteriaSummary?.length > 0
      ? suggestion.suggestedCriteriaSummary
      : (suggestion.suggestedCriteria ?? []).map(
          (criterion) => `${criterion.fieldName} ${criterion.operator} ${criterion.value}`,
        );

  return (
    <li className="ai-recommendation-card">
      <div>
        <strong>{suggestion.suggestedName}</strong>
        <span>
          AI-assisted segment idea
          {suggestion.confidenceScore != null
            ? ` · Confidence ${formatConfidence(suggestion.confidenceScore)}`
            : ""}
        </span>
      </div>
      {suggestion.description ? <p>{suggestion.description}</p> : null}
      <AiExplanationDisplay
        explanation={suggestion.explanation}
        confidenceScore={
          suggestion.confidenceScore == null
            ? undefined
            : Number(suggestion.confidenceScore)
        }
      />
      {summary.length > 0 ? (
        <ul aria-label={`Criteria for ${suggestion.suggestedName}`}>
          {summary.map((line) => (
            <li key={line}>
              <code>{line}</code>
            </li>
          ))}
        </ul>
      ) : null}
      <div className="form-actions">
        <button type="button" className="secondary-button" onClick={onPreview}>
          Preview criteria
        </button>
        {canApply ? (
          <button type="button" onClick={onApply}>
            Apply to draft
          </button>
        ) : null}
      </div>
    </li>
  );
}

function formatConfidence(value: number | string) {
  const numeric = typeof value === "number" ? value : Number(value);
  if (!Number.isFinite(numeric)) {
    return String(value);
  }
  return `${Math.round(numeric)}%`;
}
