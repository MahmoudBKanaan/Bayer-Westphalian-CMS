import { useMutation } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import {
  generateProductRecommendations,
  type ProductRecommendationListResponse,
  type ProductRecommendationView,
} from "@/api/ai";
import { isAuthorizationError } from "@/api/client";
import { AiExplanationDisplay } from "@/components/AiExplanationDisplay";
import { usePermissions } from "@/features/auth/usePermissions";

/**
 * Customer Details AI-003 product recommendations (Sprint 13 evidence).
 * Decision support only — never auto-assigns products or creates ownerships.
 */
export function ProductRecommendationPanel({
  customerId,
  customerName,
}: {
  customerId: string;
  customerName: string;
}) {
  const permissions = usePermissions();
  const canUse = permissions.canUseAiProductRecommendations();
  const canDraftCampaigns = permissions.canManageCampaigns();

  const mutation = useMutation({
    mutationFn: () => generateProductRecommendations(customerId),
  });

  if (!canUse) {
    return null;
  }

  const data: ProductRecommendationListResponse | undefined = mutation.data;
  const errorMessage = mutation.isError
    ? isAuthorizationError(mutation.error)
      ? "You are not authorized to generate product recommendations."
      : "Product recommendations could not be generated."
    : "";

  return (
    <section className="panel" aria-labelledby="ai-product-recommendations-heading">
      <div className="section-heading">
        <h2 id="ai-product-recommendations-heading">AI product recommendations</h2>
        <span>Rule-based decision support (AI-003)</span>
      </div>
      <p className="table-secondary-text" data-testid="ai-product-advisory-label">
        AI-assisted recommendation for <strong>{customerName}</strong>. Does not assign products,
        create ownerships, or change marketing eligibility. Consent and eligibility remain
        authoritative.
      </p>
      <div className="form-actions">
        <button
          type="button"
          onClick={() => mutation.mutate()}
          disabled={mutation.isPending}
          aria-label="Generate product recommendations"
        >
          {mutation.isPending ? "Generating…" : "Generate recommendations"}
        </button>
      </div>
      {errorMessage ? (
        <p className="form-error" role="alert">
          {errorMessage}
        </p>
      ) : null}
      {mutation.isSuccess && (data?.recommendations.length ?? 0) === 0 ? (
        <p className="table-state">No product recommendations for this customer.</p>
      ) : null}
      {data != null && data.recommendations.length > 0 ? (
        <ul className="ai-recommendation-grid" aria-label="Product recommendation results">
          {data.recommendations.map((row) => (
            <ProductRecommendationCard
              key={row.productId}
              row={row}
              canDraftCampaigns={canDraftCampaigns}
            />
          ))}
        </ul>
      ) : null}
    </section>
  );
}

function ProductRecommendationCard({
  row,
  canDraftCampaigns,
}: {
  row: ProductRecommendationView;
  canDraftCampaigns: boolean;
}) {
  return (
    <li className="ai-recommendation-card">
      <div>
        <strong>{row.productName ?? row.productId}</strong>
        <span>
          {row.productType ?? "Product"} · AI-assisted recommendation
          {row.confidenceScore != null
            ? ` · Confidence ${formatConfidence(row.confidenceScore)}`
            : ""}
        </span>
      </div>
      <p>{row.recommendation}</p>
      <AiExplanationDisplay
        explanation={row.explanation}
        confidenceScore={
          row.confidenceScore == null ? undefined : Number(row.confidenceScore)
        }
      />
      <div className="form-actions">
        <Link to={`/products/${row.productId}`}>Review product</Link>
        {canDraftCampaigns ? (
          <Link to={`/campaign-builder?productId=${encodeURIComponent(row.productId)}`}>
            Use in campaign draft
          </Link>
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
