import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  ADVISORY_AI_CAPABILITIES,
  AI_CONSENT_NON_BYPASS_UI_NOTICE,
  AI_FEATURES_DOC_PATH,
  AI_LIMITATIONS_DOC_PATH,
  AI_RECOMMENDATION_CANNOT_BYPASS_CONSENT_RULES_COMP,
  AI_RECOMMENDATION_CANNOT_BYPASS_CONSENT_RULES_FR,
  AI_RECOMMENDATION_CANNOT_BYPASS_CONSENT_RULES_ITEM,
  AI_RECOMMENDATION_CANNOT_BYPASS_CONSENT_RULES_NFR,
  AI_RECOMMENDATION_CANNOT_BYPASS_CONSENT_RULES_STATEMENT,
  BACKEND_CRITICAL_TEST_CLASS,
  COMPANION_HUMAN_DECISION_TEST_CLASS,
  FORBIDDEN_AI_CONSENT_OPERATIONS,
  aiOutputRequiresHumanAndConsentCompliance,
  aiRecommendationCanBypassConsentRules,
  isAuthoritativeForMarketingConsent,
  isForbiddenAiConsentOperation,
} from "@/features/ai/aiRecommendationCannotBypassConsentRules";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../../..");

function readRepoFile(relativeFromRepo: string): string {
  return readFileSync(path.join(repoRoot, relativeFromRepo), "utf8");
}

describe("aiRecommendationCannotBypassConsentRules (item 661)", () => {
  it("locks the critical KB rule identity", () => {
    expect(AI_RECOMMENDATION_CANNOT_BYPASS_CONSENT_RULES_ITEM).toBe(661);
    expect(AI_RECOMMENDATION_CANNOT_BYPASS_CONSENT_RULES_STATEMENT).toBe(
      "AI recommendation cannot bypass consent rules",
    );
    expect(AI_RECOMMENDATION_CANNOT_BYPASS_CONSENT_RULES_NFR).toEqual(["NFR-002"]);
    expect(AI_RECOMMENDATION_CANNOT_BYPASS_CONSENT_RULES_FR).toEqual(["FR-034"]);
    expect(AI_RECOMMENDATION_CANNOT_BYPASS_CONSENT_RULES_COMP).toEqual(["COMP-005"]);
    expect(ADVISORY_AI_CAPABILITIES).toEqual([
      "AI-001",
      "AI-002",
      "AI-003",
      "AI-004",
      "AI-005",
      "AI-006",
    ]);
    expect(FORBIDDEN_AI_CONSENT_OPERATIONS).toContain("overrideConsent");
    expect(FORBIDDEN_AI_CONSENT_OPERATIONS).toContain("bypassEligibility");
    expect(BACKEND_CRITICAL_TEST_CLASS).toContain(
      "AiRecommendationCannotBypassConsentRulesTests",
    );
    expect(COMPANION_HUMAN_DECISION_TEST_CLASS).toContain(
      "AiSupportsHumanDecisionMakingOnlyTests",
    );
    expect(AI_CONSENT_NON_BYPASS_UI_NOTICE).toMatch(/decision support/i);
  });

  it("treats AI as unable to bypass consent; eligibility remains required", () => {
    expect(isForbiddenAiConsentOperation("recordConsent")).toBe(true);
    expect(isForbiddenAiConsentOperation("recommendProducts")).toBe(false);

    expect(aiRecommendationCanBypassConsentRules()).toBe(false);
    expect(isAuthoritativeForMarketingConsent("ai-recommendation")).toBe(false);
    expect(isAuthoritativeForMarketingConsent("eligibility-service")).toBe(true);
    expect(isAuthoritativeForMarketingConsent("consent-service")).toBe(true);

    expect(
      aiOutputRequiresHumanAndConsentCompliance({
        capability: "AI-003",
        humanApproved: true,
        eligibilityEligible: false,
      }),
    ).toBe(true);
    expect(
      aiOutputRequiresHumanAndConsentCompliance({
        capability: "AI-005",
        humanApproved: false,
      }),
    ).toBe(true);
  });

  it("documents non-bypass consent rules in AI limitations and feature docs", () => {
    const limitationsPath = path.join(repoRoot, AI_LIMITATIONS_DOC_PATH);
    const featuresPath = path.join(repoRoot, AI_FEATURES_DOC_PATH);
    expect(existsSync(limitationsPath)).toBe(true);
    expect(existsSync(featuresPath)).toBe(true);

    const limitations = readRepoFile(AI_LIMITATIONS_DOC_PATH);
    expect(limitations).toContain("661");
    expect(limitations).toContain("AiRecommendationCannotBypassConsentRulesTests");
    expect(limitations).toMatch(/Cannot override or invent consent/i);
    expect(limitations).toMatch(/COMP-005|consent/i);

    const features = readRepoFile(AI_FEATURES_DOC_PATH);
    expect(features).toMatch(/consent|eligibility|human/i);
  });
});
