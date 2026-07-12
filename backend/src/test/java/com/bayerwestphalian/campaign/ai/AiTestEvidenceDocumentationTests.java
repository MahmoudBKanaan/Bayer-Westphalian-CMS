package com.bayerwestphalian.campaign.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * KB item 509: AI test evidence.
 *
 * <p>Asserts that {@code docs/modules/ai-test-evidence.md} catalogs backend/frontend AI tests,
 * maps acceptance items (469–505, 494–501, 506–509), and is linked from the documentation index and
 * related AI docs.
 */
@DisplayName("509 AI test evidence")
class AiTestEvidenceDocumentationTests {

    private static final Path TEST_EVIDENCE_DOC = Path.of("../docs/modules/ai-test-evidence.md");
    private static final Path AI_FEATURES_DOC = Path.of("../docs/modules/ai-features.md");
    private static final Path POLICY_DOC =
            Path.of("../docs/modules/ai-limitations-and-human-approval.md");
    private static final Path EXPLANATION_DOC =
            Path.of("../docs/modules/ai-decision-support-explanation.md");
    private static final Path DOCS_INDEX = Path.of("../docs/README.md");
    private static final Path AI_PACKAGE_INFO =
            Path.of("src/main/java/com/bayerwestphalian/campaign/ai/package-info.java");

    @Test
    void documentsTitleSummaryAndLayers() throws Exception {
        String documentation = Files.readString(TEST_EVIDENCE_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("# AI Test Evidence")
                .contains("## Evidence Summary")
                .contains("item **509**")
                .contains("backend/src/test/java/.../ai/")
                .contains("AiRecommendationRepositoryIntegrationTests")
                .contains("frontend/src/api/ai.test.ts")
                .contains("COMP-005")
                .contains("does not require executing the suite");
    }

    @Test
    void documentsBackendTestInventory() throws Exception {
        String documentation = Files.readString(TEST_EVIDENCE_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## Backend Test Inventory")
                .contains("AiRecommendationTests")
                .contains("AiRecommendationRepositoryTests")
                .contains("AiDtoTests")
                .contains("AiSearchServiceTests")
                .contains("AiRecommendationServiceTests")
                .contains("CampaignCopyServiceTests")
                .contains("AiControllerTests")
                .contains("AiFeatureDocumentationTests")
                .contains("AiLimitationsAndHumanApprovalPolicyDocumentationTests")
                .contains("AiDecisionSupportExplanationDocumentationTests")
                .contains("AiTestEvidenceDocumentationTests")
                .contains("AiSupportsHumanDecisionMakingOnlyTests")
                .contains("FlywayMigrationResourceTests");
    }

    @Test
    void documentsAcceptanceMappingForBuildAndQualityItems() throws Exception {
        String documentation = Files.readString(TEST_EVIDENCE_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## Acceptance Mapping (Build Items)")
                .contains("**469**")
                .contains("**470**")
                .contains("**471**")
                .contains("**472**")
                .contains("**474**")
                .contains("**475**")
                .contains("**485**")
                .contains("**488**")
                .contains("**489**")
                .contains("## Acceptance Mapping (Quality / Safety Items)")
                .contains("**494**")
                .contains("**495**")
                .contains("**496**")
                .contains("**497**")
                .contains("**498**")
                .contains("**499**")
                .contains("**500**")
                .contains("**501**")
                .contains("**502**")
                .contains("**503**")
                .contains("**504**")
                .contains("**505**")
                .contains("**512**");
    }

    @Test
    void documentsFrontendEvidenceAndComp005Themes() throws Exception {
        String documentation = Files.readString(TEST_EVIDENCE_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## Frontend Test Evidence")
                .contains("AiExplanationDisplay.test.tsx")
                .contains("AiRecommendationSections.test.tsx")
                .contains("CustomersPage.test.tsx")
                .contains("CampaignBuilderPage.test.tsx")
                .contains("## COMP-005 Safety Themes Covered by Tests")
                .contains("requiresHumanApproval")
                .contains("explainScore")
                .contains("No campaign auto-approve")
                .contains("## How to Run (when requested)")
                .contains("## Related Documentation")
                .contains("## Implementation Evidence")
                .contains("ai-test-evidence.md");
    }

    @Test
    void documentsDocumentationEvidenceTableFor506Through509() throws Exception {
        String documentation = Files.readString(TEST_EVIDENCE_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## Documentation Evidence (Items 506–509)")
                .contains("ai-features.md")
                .contains("ai-limitations-and-human-approval.md")
                .contains("ai-decision-support-explanation.md")
                .contains("ai-test-evidence.md")
                .contains("**506**")
                .contains("**507**")
                .contains("**508**")
                .contains("**509**");
    }

    @Test
    void documentationIndexLinksAiTestEvidence() throws Exception {
        String index = Files.readString(DOCS_INDEX, StandardCharsets.UTF_8);

        assertThat(index)
                .contains("modules/ai-test-evidence.md")
                .contains("AI Test Evidence");
    }

    @Test
    void relatedAiDocsLinkTestEvidenceCatalog() throws Exception {
        String features = Files.readString(AI_FEATURES_DOC, StandardCharsets.UTF_8);
        String policy = Files.readString(POLICY_DOC, StandardCharsets.UTF_8);
        String explanation = Files.readString(EXPLANATION_DOC, StandardCharsets.UTF_8);

        assertThat(features)
                .contains("ai-test-evidence.md")
                .contains("AI Test Evidence");
        assertThat(policy)
                .contains("ai-test-evidence.md")
                .contains("AI Test Evidence");
        assertThat(explanation)
                .contains("ai-test-evidence.md")
                .contains("AI Test Evidence");
    }

    @Test
    void aiPackageInfoReferencesTestEvidenceDocument() throws Exception {
        String packageInfo = Files.readString(AI_PACKAGE_INFO, StandardCharsets.UTF_8);

        assertThat(packageInfo)
                .contains("docs/modules/ai-test-evidence.md")
                .contains("item 509");
    }
}
