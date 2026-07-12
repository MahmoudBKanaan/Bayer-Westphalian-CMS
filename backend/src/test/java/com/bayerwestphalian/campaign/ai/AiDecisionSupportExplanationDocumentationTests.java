package com.bayerwestphalian.campaign.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * KB item 508: AI decision-support explanation.
 *
 * <p>Asserts that {@code docs/modules/ai-decision-support-explanation.md} describes explainScore
 * factors, required narrative explanations, per-feature guidance (AI-001–AI-006), UI/storage rules,
 * and is linked from the docs index and related AI docs.
 */
@DisplayName("508 AI decision-support explanation")
class AiDecisionSupportExplanationDocumentationTests {

    private static final Path EXPLANATION_DOC =
            Path.of("../docs/modules/ai-decision-support-explanation.md");
    private static final Path AI_FEATURES_DOC = Path.of("../docs/modules/ai-features.md");
    private static final Path POLICY_DOC =
            Path.of("../docs/modules/ai-limitations-and-human-approval.md");
    private static final Path DOCS_INDEX = Path.of("../docs/README.md");
    private static final Path AI_PACKAGE_INFO =
            Path.of("src/main/java/com/bayerwestphalian/campaign/ai/package-info.java");

    @Test
    void documentsTitlePurposeAndKbTraceability() throws Exception {
        String documentation = Files.readString(EXPLANATION_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("# AI Decision-Support Explanation")
                .contains("## Purpose")
                .contains("Transparency")
                .contains("COMP-005")
                .contains("## KB Traceability")
                .contains("explainScore")
                .contains("Item **474**")
                .contains("Item **483**")
                .contains("Item **508**")
                .contains("AI-001");
    }

    @Test
    void documentsExplanationFormsFactorListAndNarrative() throws Exception {
        String documentation = Files.readString(EXPLANATION_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## Explanation Forms")
                .contains("ScoreExplanationView")
                .contains("factor")
                .contains("weight")
                .contains("contribution")
                .contains("detail")
                .contains("explainScore")
                .contains("AiCustomerSearchHitView")
                .contains("ai_recommendations.explanation")
                .contains("input_summary")
                .contains("recommendation");
    }

    @Test
    void documentsFeatureByFeatureExplanationGuide() throws Exception {
        String documentation = Files.readString(EXPLANATION_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## Feature-by-Feature Explanation Guide")
                .contains("### AI-001")
                .contains("### AI-002")
                .contains("### AI-003")
                .contains("### AI-004")
                .contains("### AI-005")
                .contains("### AI-006")
                .contains("doNotContact")
                .contains("requiresHumanApproval")
                .contains("true")
                .contains("BR-010")
                .contains("BR-011");
    }

    @Test
    void documentsStoragePresentationAndNonGoals() throws Exception {
        String documentation = Files.readString(EXPLANATION_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## Storage and Audit Rules")
                .contains("mandatory")
                .contains("## Presentation Principles (Operators & UI)")
                .contains("Explain before act")
                .contains("High search/risk score")
                .contains("eligible")
                .contains("frontend/src/api/ai.ts")
                .contains("## What Explanations Are Not")
                .contains("EligibilityService")
                .contains("legal advice")
                .contains("## Engineering Checklist");
    }

    @Test
    void documentsRelatedModulesAndEvidence() throws Exception {
        String documentation = Files.readString(EXPLANATION_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## Related Documentation")
                .contains("ai-features.md")
                .contains("ai-limitations-and-human-approval.md")
                .contains("ai-test-evidence.md")
                .contains("## Implementation Evidence")
                .contains("ScoreExplanationView.java")
                .contains("AiSearchService")
                .contains("AiDecisionSupportExplanationDocumentationTests")
                .contains("ai-decision-support-explanation.md");
    }

    @Test
    void documentationIndexLinksExplanationDocument() throws Exception {
        String index = Files.readString(DOCS_INDEX, StandardCharsets.UTF_8);

        assertThat(index)
                .contains("modules/ai-decision-support-explanation.md")
                .contains("AI Decision-Support Explanation");
    }

    @Test
    void relatedAiDocsLinkExplanationDocument() throws Exception {
        String features = Files.readString(AI_FEATURES_DOC, StandardCharsets.UTF_8);
        String policy = Files.readString(POLICY_DOC, StandardCharsets.UTF_8);

        assertThat(features)
                .contains("ai-decision-support-explanation.md")
                .contains("AI Decision-Support Explanation");
        assertThat(policy)
                .contains("ai-decision-support-explanation.md")
                .contains("AI Decision-Support Explanation");
    }

    @Test
    void aiPackageInfoReferencesExplanationDocument() throws Exception {
        String packageInfo = Files.readString(AI_PACKAGE_INFO, StandardCharsets.UTF_8);

        assertThat(packageInfo)
                .contains("docs/modules/ai-decision-support-explanation.md")
                .contains("item 508");
    }
}
