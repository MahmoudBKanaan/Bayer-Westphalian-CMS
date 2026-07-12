package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** KB item 254: campaign lifecycle documentation remains available as implementation evidence. */
class CampaignLifecycleDocumentationTests {

    private static final Path CAMPAIGN_LIFECYCLE_DOC =
            Path.of("../docs/modules/campaign-lifecycle.md");
    private static final Path DOCUMENTATION_INDEX = Path.of("../docs/README.md");

    @Test
    void documentsCampaignLifecycleStatusesAndAllowedTransitions() throws Exception {
        String documentation = Files.readString(CAMPAIGN_LIFECYCLE_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Campaign Lifecycle Documentation")
                .contains("DRAFT")
                .contains("SUBMITTED")
                .contains("APPROVED")
                .contains("REJECTED")
                .contains("ACTIVE")
                .contains("PAUSED")
                .contains("COMPLETED")
                .contains("ARCHIVED")
                .contains("DRAFT` | submit | `SUBMITTED")
                .contains("SUBMITTED` | approve | `APPROVED")
                .contains("SUBMITTED` | reject with reason | `REJECTED")
                .contains("APPROVED` | launch | `ACTIVE")
                .contains("ACTIVE` | pause | `PAUSED")
                .contains("PAUSED` | resume | `ACTIVE")
                .contains("ACTIVE` | complete | `COMPLETED")
                .contains("COMPLETED` | archive | `ARCHIVED");
    }

    @Test
    void documentsLifecycleRolesEndpointsAndAuditEvidence() throws Exception {
        String documentation = Files.readString(CAMPAIGN_LIFECYCLE_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("FR-050")
                .contains("FR-057")
                .contains("FR-058")
                .contains("FR-059")
                .contains("FR-060")
                .contains("FR-061")
                .contains("FR-062")
                .contains("BR-005")
                .contains("POST /api/campaigns/{id}/submit")
                .contains("POST /api/campaigns/{id}/approve")
                .contains("POST /api/campaigns/{id}/reject")
                .contains("POST /api/campaigns/{id}/launch")
                .contains("POST /api/campaigns/{id}/pause")
                .contains("POST /api/campaigns/{id}/complete")
                .contains("POST /api/campaigns/{id}/archive")
                .contains("CAMPAIGN_MANAGER")
                .contains("COMPLIANCE_OFFICER")
                .contains("PRODUCT_MANAGER")
                .contains("A campaign owner cannot approve or reject their own campaign")
                .contains("SUBMIT")
                .contains("APPROVE")
                .contains("REJECT")
                .contains("UPDATE");
    }

    @Test
    void documentsCampaignCreationAndApprovalActivityDiagram() throws Exception {
        String documentation = Files.readString(CAMPAIGN_LIFECYCLE_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Creation And Approval Activity Diagram")
                .contains("```mermaid")
                .contains("flowchart TD")
                .contains("Campaign Manager creates campaign draft")
                .contains("Required campaign fields valid?")
                .contains("Persist campaign as DRAFT")
                .contains("Write CREATE audit log")
                .contains("Submit for compliance review?")
                .contains("Submission fields complete?")
                .contains("Set status to SUBMITTED")
                .contains("Write SUBMIT audit log")
                .contains(
                        "Compliance Officer reviews campaign, recipient eligibility, consent, and audit evidence")
                .contains("Compliance decision")
                .contains("Rejection reason provided?")
                .contains(
                        "Set status to REJECTED with rejectionReason and optional complianceReviewNotes")
                .contains("Write REJECT audit log")
                .contains("Campaign Manager revises rejected campaign")
                .contains(
                        "Set status to APPROVED with approvedByUserId, approvedAt, and optional complianceReviewNotes")
                .contains("Write APPROVE audit log")
                .contains("Block launch until approval")
                .contains("Campaign ready for launch");
    }

    @Test
    void documentationIndexLinksCampaignLifecycleDocumentation() throws Exception {
        String index = Files.readString(DOCUMENTATION_INDEX, StandardCharsets.UTF_8);

        assertThat(index)
                .contains("[Campaign Lifecycle Documentation](modules/campaign-lifecycle.md)");
    }
}
