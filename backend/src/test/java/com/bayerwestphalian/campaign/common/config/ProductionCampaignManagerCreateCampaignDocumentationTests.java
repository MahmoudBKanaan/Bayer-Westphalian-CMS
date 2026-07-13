package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProductionCampaignManagerCreateCampaignDocumentationTests {

    private static final Path SCRIPT =
            Path.of("../scripts/test-production-campaign-manager-create-campaign.ps1");
    private static final Path DOC =
            Path.of("../docs/deployment/campaign-manager-create-campaign-verification.md");

    @Test
    void verifierCreatesAndReadsDraftAsCampaignManager() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("CAMPAIGN_MANAGER")
                .contains("Post -Uri \"$origin/api/campaigns\"")
                .contains("Get -Uri \"$origin/api/campaigns/$campaignId\"")
                .contains("StatusCode -ne 201")
                .contains("Guid]::TryParse")
                .contains("ownerUserId")
                .contains("DRAFT");
    }

    @Test
    void verifierKeepsCampaignTargetlessAndNeverApprovesOrLaunches() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("productIds = @()")
                .contains("segmentId")
                .contains("no segment, products, schedule, recipients, approval, launch, or sending")
                .doesNotContain("/recipients/preview")
                .doesNotContain("/$campaignId/approve")
                .doesNotContain("/$campaignId/launch");
    }

    @Test
    void verifierUsesComplianceRejectionAndManagerArchiveCleanup() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("COMPLIANCE_OFFICER")
                .contains("/$campaignId/submit")
                .contains("/$campaignId/reject")
                .contains("rejectionReason")
                .contains("/$campaignId/archive")
                .contains("during failure cleanup")
                .contains("remains non-approved");
    }

    @Test
    void documentationRecordsBlockedRoleSeparatedAcceptance() throws Exception {
        String doc = Files.readString(DOC, StandardCharsets.UTF_8);

        assertThat(doc)
                .contains("Sprint 18 item 751")
                .contains("**BLOCKED**")
                .contains("No Campaign Manager or Compliance cleanup credential was requested")
                .contains("active `CAMPAIGN_MANAGER`, not Admin")
                .contains("valid UUID")
                .contains("only Compliance rejects")
                .contains("no approval or launch is allowed")
                .contains("do not prove this deployed role workflow");
    }
}
