package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** KB item 530: campaign launch audit logging documentation remains available as KB evidence. */
@DisplayName("530 Campaign launch audit documentation")
class CampaignLaunchCreatesAuditLogDocumentationTests {

    private static final Path CAMPAIGN_AUDIT_DOC =
            Path.of("../docs/modules/campaign-audit-logging.md");

    @Test
    void documentsCampaignLaunchAuditActionEntityAndPayload() throws Exception {
        String documentation = Files.readString(CAMPAIGN_AUDIT_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Campaign Launch Audit (Item 530)")
                .contains("LAUNCH")
                .contains("campaigns")
                .contains("launchCampaign")
                .contains("POST /api/campaigns/{id}/launch")
                .contains("status=APPROVED")
                .contains("status=ACTIVE")
                .contains("logLaunch")
                .contains("Only APPROVED");
    }

    @Test
    void documentsCampaignLaunchKbEvidence() throws Exception {
        String documentation = Files.readString(CAMPAIGN_AUDIT_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Campaign launch actions create audit logs")
                .contains("entity type `campaigns` and action `LAUNCH`")
                .contains("APPROVED to ACTIVE")
                .contains("audit log API");
    }
}
