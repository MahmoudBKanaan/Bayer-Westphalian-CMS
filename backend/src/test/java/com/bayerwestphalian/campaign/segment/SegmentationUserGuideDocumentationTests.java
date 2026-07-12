package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * KB item 205: Segmentation user guide section documents CM/BI workflows, criteria builder, preview
 * interpretation, access rules, and FR-070–079 traceability.
 */
class SegmentationUserGuideDocumentationTests {

    private static final Path SEGMENTATION_USER_GUIDE =
            Path.of("../docs/user-guides/segmentation-user-guide.md");

    @Test
    void documentsScopeAndRoleMatrix() throws Exception {
        String guide = Files.readString(SEGMENTATION_USER_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("Segmentation User Guide")
                .contains("CAMPAIGN_MANAGER")
                .contains("BI_ANALYST")
                .contains("ADMIN")
                .contains("COMPLIANCE_OFFICER")
                .contains("reusable audience segments")
                .contains("AND")
                .contains("OR")
                .contains("Preview")
                .contains("does **not** launch campaigns")
                .contains("BI Analyst cannot edit segments unless allowed")
                .contains("Campaign Manager can create reusable segments");
    }

    @Test
    void documentsCampaignManagerWorkflows() throws Exception {
        String guide = Files.readString(SEGMENTATION_USER_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("Campaign Manager Workflows")
                .contains("Saved segments")
                .contains("PRIVATE")
                .contains("TEAM")
                .contains("GLOBAL")
                .contains("Create a reusable segment")
                .contains("Create segment")
                .contains("Edit or delete a segment")
                .contains("Save changes")
                .contains("Delete segment")
                .contains("Preview audience")
                .contains("Total audience count")
                .contains("Eligible count")
                .contains("Excluded count")
                .contains("Exclusion reason summary")
                .contains("Recommended Campaign Manager practice");
    }

    @Test
    void documentsBiAnalystWorkflows() throws Exception {
        String guide = Files.readString(SEGMENTATION_USER_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("BI Analyst Workflows")
                .contains("Read-only access")
                .contains("Segmentation insights")
                .contains("What BI Analysts cannot do alone")
                .contains("Create, edit, or delete reusable segments")
                .contains("item 200")
                .contains("Analytical tips")
                .contains("totalAudienceCount")
                .contains("eligibleCount")
                .contains("DO_NOT_CONTACT")
                .contains("MARKETING_OPT_OUT")
                .contains("INVALID_CONSENT")
                .contains("MONTHLY_CONTACT_LIMIT");
    }

    @Test
    void documentsCriteriaBuilderAndPreviewInterpretation() throws Exception {
        String guide = Files.readString(SEGMENTATION_USER_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("Criteria Builder")
                .contains("Add criterion")
                .contains("age group")
                .contains("city")
                .contains("product type")
                .contains("DUE")
                .contains("PAID")
                .contains("OVERDUE")
                .contains("DEFAULT_RISK")
                .contains("expiring within 3 / 6 / 12 months")
                .contains("segment-criteria-guide.md")
                .contains("Understanding Preview Results")
                .contains("Eligible + Excluded = Total")
                .contains("audience-preview-logic.md")
                .contains("eligibility is always applied");
    }

    @Test
    void documentsAccessAuditAndRelatedDocs() throws Exception {
        String guide = Files.readString(SEGMENTATION_USER_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("Access And Error Handling")
                .contains("Backend authorization is authoritative")
                .contains("403 Forbidden")
                .contains("validation errors")
                .contains("Audit Expectations")
                .contains("segments")
                .contains("segmentation-module.md")
                .contains("eligibility-rules.md")
                .contains("role-based-access.md");
    }

    @Test
    void documentsKbTraceability() throws Exception {
        String guide = Files.readString(SEGMENTATION_USER_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("KB Traceability")
                .contains("Campaign Manager defines segments")
                .contains("BI Analyst views segmentation insights")
                .contains("FR-070")
                .contains("FR-076")
                .contains("FR-077")
                .contains("FR-078")
                .contains("FR-079")
                .contains("FR-054")
                .contains("FR-055")
                .contains("Item **200**")
                .contains("Item **201**");
    }

    @Test
    void documentationIndexLinksSegmentationUserGuide() throws Exception {
        String index = Files.readString(Path.of("../docs/README.md"), StandardCharsets.UTF_8);

        assertThat(index).contains("user-guides/segmentation-user-guide.md");
    }

    @Test
    void segmentationModuleLinksUserGuide() throws Exception {
        String moduleDoc =
                Files.readString(
                        Path.of("../docs/modules/segmentation-module.md"), StandardCharsets.UTF_8);

        assertThat(moduleDoc).contains("segmentation-user-guide.md");
    }
}
