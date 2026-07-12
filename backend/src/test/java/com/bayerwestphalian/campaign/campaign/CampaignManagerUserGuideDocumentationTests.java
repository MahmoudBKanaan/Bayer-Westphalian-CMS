package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** KB item 257: Campaign Manager user guide remains available as workflow evidence. */
class CampaignManagerUserGuideDocumentationTests {

    private static final Path CAMPAIGN_MANAGER_GUIDE =
            Path.of("../docs/user-guides/campaign-manager-guide.md");

    @Test
    void documentsCampaignManagerScopeAndDashboardWorkflow() throws Exception {
        String guide = Files.readString(CAMPAIGN_MANAGER_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("Campaign Manager User Guide")
                .contains("CAMPAIGN_MANAGER")
                .contains("plan campaigns")
                .contains("define audiences")
                .contains("select promoted products")
                .contains("draft compliant messages")
                .contains("submit campaigns for compliance review")
                .contains("revise rejected campaigns")
                .contains("cannot approve or reject campaigns")
                .contains("cannot approve their own campaigns")
                .contains(
                        "draft, submitted, approved, rejected, active, paused, completed, and archived campaigns")
                .contains("Campaigns")
                .contains("Campaign Builder")
                .contains("Segmentation");
    }

    @Test
    void documentsCampaignCreationEditingAndSubmissionWorkflow() throws Exception {
        String guide = Files.readString(CAMPAIGN_MANAGER_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("Campaign Creation And Draft Editing")
                .contains("Campaign name and objective")
                .contains("EMAIL")
                .contains("SMS")
                .contains("PHONE")
                .contains("Message subject and message body")
                .contains("Start date and end date")
                .contains("Draft and rejected campaigns can be edited")
                .contains(
                        "Submitted, approved, active, paused, completed, and archived campaigns cannot be edited")
                .contains("POST /api/campaigns")
                .contains("PUT /api/campaigns/{id}")
                .contains("PUT /api/campaigns/{id}/segment")
                .contains("PUT /api/campaigns/{id}/products")
                .contains("POST /api/campaigns/{id}/submit")
                .contains("SUBMIT")
                .contains("rejectionReason")
                .contains("complianceReviewNotes");
    }

    @Test
    void documentsAudienceProductLifecycleAccessAndAuditEvidence() throws Exception {
        String guide = Files.readString(CAMPAIGN_MANAGER_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("Audience And Product Selection")
                .contains("age group")
                .contains("location")
                .contains("product ownership")
                .contains("payment history")
                .contains("AND/OR logic")
                .contains("Approved campaigns can be launched")
                .contains("BR-005")
                .contains("POST /api/campaigns/{id}/launch")
                .contains("POST /api/campaigns/{id}/pause")
                .contains("POST /api/campaigns/{id}/complete")
                .contains("POST /api/campaigns/{id}/archive")
                .contains("Backend authorization is authoritative")
                .contains("403 Forbidden")
                .contains("business-rule errors")
                .contains("CREATE")
                .contains("UPDATE")
                .contains("APPROVE")
                .contains("REJECT");
    }

    @Test
    void documentsCampaignManagerKbTraceability() throws Exception {
        String guide = Files.readString(CAMPAIGN_MANAGER_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("KB Traceability")
                .contains("Role description")
                .contains("Allowed functions")
                .contains("Screens")
                .contains("FR-050")
                .contains("FR-057")
                .contains("FR-052")
                .contains("FR-053")
                .contains("FR-054")
                .contains("FR-055")
                .contains("FR-058")
                .contains("FR-060")
                .contains("FR-061")
                .contains("FR-062")
                .contains("FR-077")
                .contains("FR-078")
                .contains("FR-079");
    }

    @Test
    void documentationIndexLinksCampaignManagerGuide() throws Exception {
        String index = Files.readString(Path.of("../docs/README.md"), StandardCharsets.UTF_8);

        assertThat(index).contains("user-guides/campaign-manager-guide.md");
    }
}
