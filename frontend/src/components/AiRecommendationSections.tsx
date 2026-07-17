import { Link } from "react-router-dom";
import { AiExplanationDisplay } from "@/components/AiExplanationDisplay";
import type { PermissionChecks } from "@/features/auth/permissions";

export type AiRecommendationSection = {
  id: string;
  title: string;
  signal: string;
  explanation: string;
  confidenceScore?: number;
  destination: string;
  cta: string;
};

export function buildAiRecommendationSections(
  permissions: PermissionChecks,
): AiRecommendationSection[] {
  const sections: AiRecommendationSection[] = [];

  if (permissions.canUseAiCustomerSignals()) {
    sections.push({
      id: "customer-search",
      title: "Customer search",
      signal: "Fuzzy profile matching",
      explanation: "Ranks customer matches using name, email, city, profile, and status signals.",
      confidenceScore: 80,
      destination: "/customers",
      cta: "Open customers",
    });
    sections.push({
      id: "duplicate-contact",
      title: "Duplicate-contact warning",
      signal: "Navigation only — run check in recipient preview",
      explanation:
        "Open a campaign, open Recipient Preview, select a recipient, and run Check "
        + "Duplicate-Contact Risk. No score is calculated until that action.",
      destination: "/campaigns",
      cta: "Open campaigns",
    });
    sections.push({
      id: "default-risk",
      title: "Default-risk scoring",
      signal: "Missed payments and reminder escalation",
      explanation:
        "Explains risk using missed payments, overdue days, reminder level, and payment history.",
      confidenceScore: 85,
      destination: "/customers",
      cta: "Open customers",
    });
  }

  if (permissions.canUseAiSegmentSuggestions()) {
    sections.push({
      id: "segment-suggestions",
      title: "Segment suggestions",
      signal: "Ownership, payment, location, expiration",
      explanation:
        "Suggests audience criteria from product ownership, payment history, location, "
        + "and expiration rules.",
      confidenceScore: 78,
      destination: "/segments",
      cta: "Open segments",
    });
  }

  if (permissions.canUseAiProductRecommendations()) {
    sections.push({
      id: "product-recommendations",
      title: "Product recommendations",
      signal: "Profile and owned-product rules",
      explanation:
        "Recommends products from customer profile fit, active catalog rules, "
        + "and owned-product gaps.",
      confidenceScore: 75,
      destination: "/products",
      cta: "Open products",
    });
  }

  if (permissions.canUseAiCampaignCopy()) {
    sections.push({
      id: "campaign-copy",
      title: "Campaign copy",
      signal: "Human-approved subject, body, CTA",
      explanation:
        "Drafts campaign copy from objective, product, audience, and channel while keeping "
        + "human approval required.",
      confidenceScore: 70,
      destination: "/campaign-builder",
      cta: "Open builder",
    });
  }

  return sections;
}

export function AiRecommendationSections({
  sections,
}: {
  sections: AiRecommendationSection[];
}) {
  if (sections.length === 0) {
    return null;
  }

  return (
    <section className="panel" aria-labelledby="ai-recommendations-heading">
      <div className="section-heading">
        <h2 id="ai-recommendations-heading">AI recommendations</h2>
        <span>E21 decision support</span>
      </div>
      <ul className="ai-recommendation-grid" aria-label="AI recommendation sections">
        {sections.map((section) => (
          <li className="ai-recommendation-card" key={section.id}>
            <div>
              <strong>{section.title}</strong>
              <span>{section.signal}</span>
            </div>
            <AiExplanationDisplay
              explanation={section.explanation}
              confidenceScore={section.confidenceScore}
            />
            <Link to={section.destination}>{section.cta}</Link>
          </li>
        ))}
      </ul>
    </section>
  );
}
