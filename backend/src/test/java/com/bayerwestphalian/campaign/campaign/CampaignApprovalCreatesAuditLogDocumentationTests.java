package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * KB item 529: campaign approval/rejection audit logging documentation remains available as KB
 * evidence (also items 235 / 236).
 */
@org.junit.jupiter.api.DisplayName("529 Campaign approval/rejection audit documentation")
class CampaignApprovalCreatesAuditLogDocumentationTests {

    private static final Path CAMPAIGN_AUDIT_DOC =
            Path.of("../docs/modules/campaign-audit-logging.md");

    @Test
    void documentsCampaignApprovalAuditActionEntityAndPayload() throws Exception {
        String documentation = Files.readString(CAMPAIGN_AUDIT_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Campaign Approval Audit (Item 529)")
                .contains("APPROVE")
                .contains("campaigns")
                .contains("approveCampaign")
                .contains("POST /api/campaigns/{id}/approve")
                .contains("status=SUBMITTED")
                .contains("status=APPROVED")
                .contains("approvedByUserId")
                .contains("approvedAt")
                .contains("complianceReviewNotes")
                .contains("Failed authorization");
    }

    @Test
    void documentsCampaignRejectionAuditActionEntityAndPayload() throws Exception {
        String documentation = Files.readString(CAMPAIGN_AUDIT_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Campaign Rejection Audit (Item 529)")
                .contains("REJECT")
                .contains("rejectCampaign")
                .contains("POST /api/campaigns/{id}/reject")
                .contains("status=REJECTED")
                .contains("rejectionReason")
                .contains("logRejection")
                .contains("Rejection reason is required");
    }

    @Test
    void documentsCampaignApprovalKbEvidence() throws Exception {
        String documentation = Files.readString(CAMPAIGN_AUDIT_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Campaign approval actions create audit logs")
                .contains("entity type `campaigns` and action `APPROVE`")
                .contains("SUBMITTED to APPROVED transition")
                .contains("Campaign rejection actions create audit logs")
                .contains("entity type `campaigns` and action `REJECT`")
                .contains("SUBMITTED to REJECTED")
                .contains("audit log API");
    }
}
