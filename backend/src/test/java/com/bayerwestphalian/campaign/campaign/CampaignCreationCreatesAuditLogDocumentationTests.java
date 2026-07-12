package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** KB item 233: campaign creation audit logging documentation remains available as KB evidence. */
class CampaignCreationCreatesAuditLogDocumentationTests {

    private static final Path CAMPAIGN_AUDIT_DOC =
            Path.of("../docs/modules/campaign-audit-logging.md");

    @Test
    void documentsCampaignAuditBoundaryAndServices() throws Exception {
        String documentation = Files.readString(CAMPAIGN_AUDIT_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Campaign Audit Logging Documentation")
                .contains("com.bayerwestphalian.campaign.campaign")
                .contains("com.bayerwestphalian.campaign.audit")
                .contains("CampaignService")
                .contains("AuditService")
                .contains("AuditLog")
                .contains("AuditLogRepository")
                .contains("AuditController")
                .contains("/api/audit-logs");
    }

    @Test
    void documentsCampaignCreationAuditActionEntityAndPayload() throws Exception {
        String documentation = Files.readString(CAMPAIGN_AUDIT_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Campaign Creation Audit (Item 233)")
                .contains("CREATE")
                .contains("campaigns")
                .contains("createCampaign")
                .contains("name")
                .contains("objective")
                .contains("status")
                .contains("ownerUserId")
                .contains("channel")
                .contains("productIds")
                .contains("Failed validation before persist does not write an audit row");
    }

    @Test
    void documentsCampaignAuditAuthorizationAndKbEvidence() throws Exception {
        String documentation = Files.readString(CAMPAIGN_AUDIT_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Spring Security")
                .contains("method-level authorization")
                .contains("ADMIN")
                .contains("CAMPAIGN_MANAGER")
                .contains("COMPLIANCE_OFFICER")
                .contains("SYSTEM_AUDITOR")
                .contains("Campaign creation actions create audit logs")
                .contains("entity type `campaigns` and action `CREATE`")
                .contains("Campaign approval actions create audit logs")
                .contains("entity type `campaigns` and action `APPROVE`")
                .contains("audit log API");
    }

    @Test
    void documentationIndexLinksCampaignAuditLoggingDocumentation() throws Exception {
        String index = Files.readString(Path.of("../docs/README.md"), StandardCharsets.UTF_8);

        assertThat(index).contains("modules/campaign-audit-logging.md");
    }
}
