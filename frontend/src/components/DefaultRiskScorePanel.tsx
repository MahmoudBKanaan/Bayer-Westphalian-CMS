import { useMutation } from "@tanstack/react-query";
import {
  generateDefaultRiskScore,
  type DefaultRiskScoreView,
} from "@/api/ai";
import { isAuthorizationError } from "@/api/client";
import { AiExplanationDisplay } from "@/components/AiExplanationDisplay";
import { usePermissions } from "@/features/auth/usePermissions";

/**
 * Customer Details AI-004 default-risk score (Sprint 13 evidence).
 * Advisory only — never auto-blocks marketing or changes payment status.
 */
export function DefaultRiskScorePanel({
  customerId,
  customerName,
}: {
  customerId: string;
  customerName: string;
}) {
  const permissions = usePermissions();
  // Same audience as product AI + CSA (payment-aware roles already on customer details).
  const canUse =
    permissions.canUseAiProductRecommendations() || permissions.canReadPaymentRecords();

  const mutation = useMutation({
    mutationFn: () => generateDefaultRiskScore(customerId),
  });

  if (!canUse) {
    return null;
  }

  const data: DefaultRiskScoreView | undefined = mutation.data;
  const errorMessage = mutation.isError
    ? isAuthorizationError(mutation.error)
      ? "You are not authorized to generate default-risk scores."
      : "Default-risk score could not be generated."
    : "";

  return (
    <section className="panel" aria-labelledby="ai-default-risk-heading">
      <div className="section-heading">
        <h2 id="ai-default-risk-heading">AI default-risk score</h2>
        <span>Rule-based advisory score (AI-004)</span>
      </div>
      <p className="table-secondary-text">
        Advisory decision support for <strong>{customerName}</strong>. Does not mark payments,
        block campaigns, or change consent. Human operators remain responsible.
      </p>
      <div className="form-actions">
        <button
          type="button"
          onClick={() => mutation.mutate()}
          disabled={mutation.isPending}
          aria-label="Generate default-risk score"
        >
          {mutation.isPending ? "Scoring…" : "Generate default-risk score"}
        </button>
      </div>
      {errorMessage ? (
        <p className="form-error" role="alert">
          {errorMessage}
        </p>
      ) : null}
      {data != null ? (
        <div className="ai-recommendation-card" aria-label="Default-risk score result">
          <div>
            <strong>
              Score {formatScore(data.riskScore)} · Risk {data.riskLevel}
            </strong>
            <span>AI-assisted risk signal</span>
          </div>
          <AiExplanationDisplay
            explanation={data.explanation}
            confidenceScore={
              typeof data.riskScore === "number"
                ? data.riskScore
                : Number(data.riskScore)
            }
            factors={data.factors.map((factor) => ({
              fieldName: factor.factor,
              weight: factor.weight,
              contribution: factor.contribution,
              reason: factor.detail ?? "Contributed to the default-risk score.",
            }))}
          />
        </div>
      ) : null}
    </section>
  );
}

function formatScore(value: number | string) {
  const numeric = typeof value === "number" ? value : Number(value);
  if (!Number.isFinite(numeric)) {
    return String(value);
  }
  return String(Math.round(numeric));
}
