package com.bayerwestphalian.campaign.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * KB item 506: AI feature documentation.
 *
 * <p>Asserts that {@code docs/modules/ai-features.md} describes the E21 AI decision-support module
 * (AI-001–AI-006, COMP-005 non-bypass guarantees, API surface, persistence, authorization, and
 * frontend) and is linked from the documentation index and package-info.
 */
@DisplayName("506 AI feature documentation")
class AiFeatureDocumentationTests {

    private static final Path AI_FEATURES_DOC = Path.of("../docs/modules/ai-features.md");
    private static final Path DOCS_INDEX = Path.of("../docs/README.md");
    private static final Path AI_PACKAGE_INFO =
            Path.of("src/main/java/com/bayerwestphalian/campaign/ai/package-info.java");

    @Test
    void documentsAiModuleBoundaryAndComponents() throws Exception {
        String documentation = Files.readString(AI_FEATURES_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("# AI Feature Documentation")
                .contains("## Package Boundary")
                .contains("com.bayerwestphalian.campaign.ai")
                .contains("AiController")
                .contains("AiSearchService")
                .contains("AiRecommendationService")
                .contains("CampaignCopyService")
                .contains("AiRecommendation")
                .contains("AiRecommendationRepository")
                .contains("AiRecommendationType")
                .contains("decision-support")
                .contains("COMP-005");
    }

    @Test
    void documentsKbTraceabilityAi001ThroughAi006() throws Exception {
        String documentation = Files.readString(AI_FEATURES_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## KB Traceability")
                .contains("E21")
                .contains("AI-001")
                .contains("AI-002")
                .contains("AI-003")
                .contains("AI-004")
                .contains("AI-005")
                .contains("AI-006")
                .contains("FR-015")
                .contains("COMP-005")
                .contains("Item **506**")
                .contains("Item **468**");
    }

    @Test
    void documentsNonBypassGuaranteesAndHumanApproval() throws Exception {
        String documentation = Files.readString(AI_FEATURES_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## Non-Bypass Guarantees (COMP-005)")
                .contains("EligibilityService")
                .contains("do-not-contact")
                .contains("consent")
                .contains("opt-out")
                .contains("Approve / reject / launch campaigns")
                .contains("## Human Approval Policy (summary)")
                .contains("requiresHumanApproval")
                .contains("true")
                .contains("never self-approves");
    }

    @Test
    void documentsFeaturesAndRestApiSurface() throws Exception {
        String documentation = Files.readString(AI_FEATURES_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## Features")
                .contains("### AI-001")
                .contains("explainScore")
                .contains("### AI-002")
                .contains("### AI-003")
                .contains("### AI-004")
                .contains("### AI-005")
                .contains("### AI-006")
                .contains("## REST API Surface")
                .contains("/api/ai")
                .contains("/api/ai/customer-search")
                .contains("/api/ai/segment-suggestions")
                .contains("/api/ai/product-recommendations")
                .contains("/api/ai/duplicate-contact-warning")
                .contains("/api/ai/campaign-copy")
                .contains("/api/ai/campaign-copy/{recommendationId}/approve");
    }

    @Test
    void documentsPersistenceAuthorizationAndFrontend() throws Exception {
        String documentation = Files.readString(AI_FEATURES_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## Persistence (`ai_recommendations`)")
                .contains("explanation")
                .contains("confidence_score")
                .contains("approved_by_user_id")
                .contains("findByTargetEntity")
                .contains("findByRecommendationType")
                .contains("## Authorization")
                .contains("BI_ANALYST")
                .contains("CAMPAIGN_MANAGER")
                .contains("401")
                .contains("403")
                .contains("## Frontend Surfaces")
                .contains("frontend/src/api/ai.ts")
                .contains("CustomersPage")
                .contains("CampaignBuilderPage");
    }

    @Test
    void documentsRelatedModulesAndImplementationEvidence() throws Exception {
        String documentation = Files.readString(AI_FEATURES_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## Related Documentation")
                .contains("eligibility-rules.md")
                .contains("consent-module.md")
                .contains("campaign-lifecycle.md")
                .contains("ai-limitations-and-human-approval.md")
                .contains("ai-decision-support-explanation.md")
                .contains("ai-test-evidence.md")
                .contains("## Implementation Evidence")
                .contains("AiSearchService.java")
                .contains("AiRecommendationService.java")
                .contains("CampaignCopyService.java")
                .contains("AiController.java")
                .contains("AiFeatureDocumentationTests")
                .contains("ai-features.md");
    }

    @Test
    void documentationIndexLinksAiFeatureDocumentation() throws Exception {
        String index = Files.readString(DOCS_INDEX, StandardCharsets.UTF_8);

        assertThat(index)
                .contains("modules/ai-features.md")
                .contains("AI Feature Documentation");
    }

    @Test
    void aiPackageInfoReferencesFeatureDocumentation() throws Exception {
        String packageInfo = Files.readString(AI_PACKAGE_INFO, StandardCharsets.UTF_8);

        assertThat(packageInfo)
                .contains("docs/modules/ai-features.md")
                .contains("item 506");
    }
}
