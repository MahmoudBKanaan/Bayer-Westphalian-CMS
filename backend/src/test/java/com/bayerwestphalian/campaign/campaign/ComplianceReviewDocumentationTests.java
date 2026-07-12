package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** KB item 255: compliance review documentation remains available as implementation evidence. */
class ComplianceReviewDocumentationTests {

    private static final Path COMPLIANCE_REVIEW_DOC =
            Path.of("../docs/modules/compliance-review.md");
    private static final Path DOCUMENTATION_INDEX = Path.of("../docs/README.md");

    @Test
    void documentsComplianceReviewTraceabilityInputsAndDecisions() throws Exception {
        String documentation = Files.readString(COMPLIANCE_REVIEW_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Compliance Review Documentation")
                .contains("FR-054")
                .contains("FR-055")
                .contains("FR-058")
                .contains("FR-059")
                .contains("BR-005")
                .contains("COMP-006")
                .contains("TC-011")
                .contains("Campaign name, objective, owner, channel")
                .contains("Recipient preview totals")
                .contains("eligible recipient count")
                .contains("excluded recipient count")
                .contains("exclusion reasons")
                .contains("Approve")
                .contains("Reject")
                .contains("Record notes");
    }

    @Test
    void documentsComplianceReviewEndpointsRolesAndFields() throws Exception {
        String documentation = Files.readString(COMPLIANCE_REVIEW_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("POST /api/campaigns/{id}/approve")
                .contains("POST /api/campaigns/{id}/reject")
                .contains("PUT /api/campaigns/{id}/compliance-review-notes")
                .contains("COMPLIANCE_OFFICER")
                .contains("ADMIN")
                .contains("CAMPAIGN_MANAGER")
                .contains("PRODUCT_MANAGER")
                .contains("The campaign owner cannot approve or reject their own campaign")
                .contains("approvedByUserId")
                .contains("approvedByFullName")
                .contains("approvedAt")
                .contains("rejectionReason")
                .contains("campaigns.rejection_reason")
                .contains("complianceReviewNotes");
    }

    @Test
    void documentsComplianceReviewAuditAndUiEvidence() throws Exception {
        String documentation = Files.readString(COMPLIANCE_REVIEW_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("SUBMIT")
                .contains("APPROVE")
                .contains("REJECT")
                .contains("UPDATE")
                .contains("Compliance Review page")
                .contains("requires a rejection reason")
                .contains("Campaigns page")
                .contains("Campaign Builder page")
                .contains("CampaignController")
                .contains("CampaignService")
                .contains("CampaignComplianceReviewNotesTests")
                .contains("CompliancePage.test.tsx");
    }

    @Test
    void documentationIndexLinksComplianceReviewDocumentation() throws Exception {
        String index = Files.readString(DOCUMENTATION_INDEX, StandardCharsets.UTF_8);

        assertThat(index)
                .contains("[Compliance Review Documentation](modules/compliance-review.md)");
    }
}
