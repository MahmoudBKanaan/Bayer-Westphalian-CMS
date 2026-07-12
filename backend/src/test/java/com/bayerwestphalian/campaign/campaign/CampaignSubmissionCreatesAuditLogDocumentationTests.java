package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * KB item 528: campaign submission audit logging documentation remains available as KB evidence.
 */
@DisplayName("528 Campaign submission audit documentation")
class CampaignSubmissionCreatesAuditLogDocumentationTests {

    private static final Path CAMPAIGN_AUDIT_DOC =
            Path.of("../docs/modules/campaign-audit-logging.md");

    @Test
    void documentsCampaignSubmissionAuditActionEntityAndPayload() throws Exception {
        String documentation = Files.readString(CAMPAIGN_AUDIT_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Campaign Submission Audit (Item 528)")
                .contains("SUBMIT")
                .contains("campaigns")
                .contains("submitCampaign")
                .contains("POST /api/campaigns/{id}/submit")
                .contains("status=DRAFT")
                .contains("status=SUBMITTED")
                .contains("logSubmission")
                .contains("Validation failures");
    }

    @Test
    void documentsCampaignSubmissionKbEvidence() throws Exception {
        String documentation = Files.readString(CAMPAIGN_AUDIT_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Campaign submission actions create audit logs")
                .contains("entity type `campaigns` and action `SUBMIT`")
                .contains("DRAFT to SUBMITTED")
                .contains("audit log API");
    }
}
