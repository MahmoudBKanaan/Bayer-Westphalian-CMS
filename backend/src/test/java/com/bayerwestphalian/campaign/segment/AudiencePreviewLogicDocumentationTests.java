package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * KB item 204: Audience preview logic documentation covers criteria matching, EligibilityService
 * gate, counts, exclusion summary, API/UI boundary, and production gate evidence.
 */
class AudiencePreviewLogicDocumentationTests {

    private static final Path AUDIENCE_PREVIEW_DOC =
            Path.of("../docs/modules/audience-preview-logic.md");

    @Test
    void documentsPreviewArchitectureAndEntryPoints() throws Exception {
        String documentation = Files.readString(AUDIENCE_PREVIEW_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Audience Preview Logic Documentation")
                .contains("POST /api/segments/preview")
                .contains("SegmentController.previewSegment")
                .contains("SegmentService.previewSegment")
                .contains("findMatchingCustomers")
                .contains("EligibilityService.evaluateForSegmentPreview")
                .contains("SegmentExclusionReasonSummarySupport")
                .contains("SegmentPreviewView")
                .contains("com.bayerwestphalian.campaign.segment")
                .contains("com.bayerwestphalian.campaign.campaign");
    }

    @Test
    void documentsKbRequirementsAndBacklogItems() throws Exception {
        String documentation = Files.readString(AUDIENCE_PREVIEW_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("FR-054")
                .contains("FR-055")
                .contains("FR-079")
                .contains("BR-001")
                .contains("BR-002")
                .contains("BR-003")
                .contains("BR-006")
                .contains("BR-011")
                .contains("178")
                .contains("198")
                .contains("199")
                .contains("208");
    }

    @Test
    void documentsCriteriaPhaseVersusEligibilityPhase() throws Exception {
        String documentation = Files.readString(AUDIENCE_PREVIEW_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Step 1 — Criteria Matching")
                .contains("Step 2 — Eligibility Gate")
                .contains("criteria-only")
                .contains("No eligibility")
                .contains("totalAudienceCount")
                .contains("MARKETING_EMAIL")
                .contains("never skips")
                .contains("UNKNOWN")
                .contains("Same-campaign duplicate")
                .contains("**No**")
                .contains("Monthly contact limit")
                .contains("Do-not-contact")
                .contains("Marketing opt-out")
                .contains("Guardian consent");
    }

    @Test
    void documentsCountsEligibleListAndExclusionSummary() throws Exception {
        String documentation = Files.readString(AUDIENCE_PREVIEW_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("eligibleCount")
                .contains("excludedCount")
                .contains("eligibleCount + excludedCount == totalAudienceCount")
                .contains("matchingCustomers")
                .contains("only eligible")
                .contains("exclusionReasonSummary")
                .contains("DO_NOT_CONTACT")
                .contains("MARKETING_OPT_OUT")
                .contains("INVALID_CONSENT")
                .contains("MONTHLY_CONTACT_LIMIT")
                .contains("descending by `count`")
                .contains("SegmentPreviewView` enforces");
    }

    @Test
    void documentsApiAuthorizationFrontendAndPathComparison() throws Exception {
        String documentation = Files.readString(AUDIENCE_PREVIEW_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("API Contract")
                .contains("canPreviewSegments")
                .contains("BI_ANALYST")
                .contains("CAMPAIGN_MANAGER")
                .contains("ADMIN")
                .contains("SegmentPreviewResults")
                .contains("ExclusionReasonSummaryPanel")
                .contains("SegmentInsightPanel")
                .contains("frontend/src/api/segments.ts")
                .contains("Criteria Only vs Preview vs Campaign Recipients")
                .contains("Campaign recipient generation")
                .contains("Production gate")
                .contains("final campaign audience");
    }

    @Test
    void documentsWorkedExampleCommonMistakesAndEvidence() throws Exception {
        String documentation = Files.readString(AUDIENCE_PREVIEW_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Worked Example")
                .contains("city = Munich")
                .contains("PROSPECT")
                .contains("Common Mistakes")
                .contains("Showing criteria matches as “ready to contact”")
                .contains("Skipping eligibility for performance")
                .contains("Evidence")
                .contains("Audience size is previewed as criteria match count")
                .contains("Preview always applies eligibility to every criteria match")
                .contains("Criteria-only matching is not a final contactable audience");
    }

    @Test
    void documentationIndexLinksAudiencePreviewLogic() throws Exception {
        String index = Files.readString(Path.of("../docs/README.md"), StandardCharsets.UTF_8);

        assertThat(index).contains("modules/audience-preview-logic.md");
    }

    @Test
    void relatedModuleDocsLinkAudiencePreviewLogic() throws Exception {
        String segmentation =
                Files.readString(
                        Path.of("../docs/modules/segmentation-module.md"), StandardCharsets.UTF_8);
        String eligibility =
                Files.readString(
                        Path.of("../docs/architecture/eligibility-rules.md"),
                        StandardCharsets.UTF_8);
        String criteriaGuide =
                Files.readString(
                        Path.of("../docs/modules/segment-criteria-guide.md"),
                        StandardCharsets.UTF_8);

        assertThat(segmentation).contains("audience-preview-logic.md");
        assertThat(eligibility).contains("audience-preview-logic.md");
        assertThat(criteriaGuide).contains("audience-preview-logic.md");
    }
}
