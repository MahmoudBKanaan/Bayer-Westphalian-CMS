package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProductionCampaignManagerLaunchApprovedDocumentationTests {

    private static final Path SCRIPT =
            Path.of("../scripts/test-production-campaign-manager-launch-approved.ps1");
    private static final Path DOC =
            Path.of("../docs/deployment/campaign-manager-launch-approved-verification.md");

    @Test
    void verifierRequiresProviderDisableAndSeparateHumanApproval() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("ProviderSendingConfirmedDisabled")
                .contains("Provider sending must be confirmed disabled")
                .contains("CAMPAIGN_MANAGER")
                .contains("COMPLIANCE_OFFICER")
                .contains("/$campaignId/submit")
                .contains("/$campaignId/approve");
    }

    @Test
    void verifierFailsClosedUnlessRecipientRowsRemainZero() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("/recipients/eligible")
                .contains("/recipients/excluded")
                .contains("unexpectedly has recipient rows")
                .contains("unexpected eligible/sent recipient")
                .contains("productIds = @()")
                .contains("zero recipients and zero contact generation");
    }

    @Test
    void verifierRequiresManagerLaunchAuditAndLifecycleCleanup() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("/$campaignId/launch")
                .contains("ACTIVE")
                .contains("$_.action -eq \"LAUNCH\"")
                .contains("$_.actorUserId -eq $manager.UserId")
                .contains("/$campaignId/complete")
                .contains("/$campaignId/archive")
                .contains("during failure cleanup");
    }

    @Test
    void documentationRecordsBlockedSafetyCriticalAcceptance() throws Exception {
        String doc = Files.readString(DOC, StandardCharsets.UTF_8);

        assertThat(doc)
                .contains("Sprint 18 item 753")
                .contains("**BLOCKED**")
                .contains("No Manager or Compliance credential was requested")
                .contains("Zero recipient rows are therefore a mandatory fail-closed precondition")
                .contains("`CAMPAIGN_MANAGER` changes status to `ACTIVE`")
                .contains("ProviderSendingConfirmedDisabled")
                .contains("cannot satisfy this production acceptance check");
    }
}
