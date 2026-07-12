export type AiScoreFactor = {
  fieldName: string;
  weight: number | string;
  contribution: number | string;
  reason: string;
};

export type AiExplanationDisplayProps = {
  explanation: string;
  confidenceScore?: number | string | null;
  storedRecommendationId?: string | null;
  factors?: AiScoreFactor[];
};

/**
 * Shared AI explanation display for KB E21 recommendation payloads.
 *
 * Backend AI responses expose required explanation text, optional confidence, optional stored
 * recommendation ids, and score factors for search/default-risk scoring.
 */
export function AiExplanationDisplay({
  explanation,
  confidenceScore,
  storedRecommendationId,
  factors = [],
}: AiExplanationDisplayProps) {
  const normalizedExplanation = explanation.trim();

  if (normalizedExplanation === "") {
    return null;
  }

  return (
    <div className="ai-explanation" aria-label="AI explanation">
      <p>{normalizedExplanation}</p>
      {confidenceScore != null || storedRecommendationId != null ? (
        <dl className="ai-explanation-meta">
          {confidenceScore != null ? (
            <div>
              <dt>Confidence</dt>
              <dd>{formatConfidence(confidenceScore)}</dd>
            </div>
          ) : null}
          {storedRecommendationId != null ? (
            <div>
              <dt>Recommendation ID</dt>
              <dd>{storedRecommendationId}</dd>
            </div>
          ) : null}
        </dl>
      ) : null}
      {factors.length > 0 ? (
        <ul className="ai-explanation-factors" aria-label="AI score factors">
          {factors.map((factor) => (
            <li key={`${factor.fieldName}-${factor.reason}`}>
              <strong>{factor.fieldName}</strong>
              <span>
                {formatContribution(factor.contribution)} / {formatContribution(factor.weight)}
              </span>
              <p>{factor.reason}</p>
            </li>
          ))}
        </ul>
      ) : null}
    </div>
  );
}

function formatConfidence(value: number | string) {
  const numeric = typeof value === "number" ? value : Number(value);
  if (!Number.isFinite(numeric)) {
    return String(value);
  }
  return `${numeric.toFixed(0)}%`;
}

function formatContribution(value: number | string) {
  const numeric = typeof value === "number" ? value : Number(value);
  if (!Number.isFinite(numeric)) {
    return String(value);
  }
  return numeric.toFixed(0);
}
