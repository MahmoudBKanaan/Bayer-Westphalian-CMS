package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CampaignLaunchDocumentationTests {

    private static final Path CAMPAIGN_LAUNCH_DOC = Path.of("../docs/modules/campaign-launch.md");

    @Test
    void documentsCampaignLaunchRulesAndAuthorization() throws Exception {
        String documentation = Files.readString(CAMPAIGN_LAUNCH_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Campaign Launch Documentation")
                .contains("POST /api/campaigns/{id}/launch")
                .contains("APPROVED")
                .contains("ACTIVE")
                .contains("CAMPAIGN_MANAGER")
                .contains("ADMIN")
                .contains("PRODUCT_MANAGER")
                .contains("cannot launch campaigns")
                .contains("BR-005")
                .contains("TC-001")
                .contains("TC-012")
                .contains("TC-013");
    }

    @Test
    void documentsCampaignLaunchSideEffects() throws Exception {
        String documentation = Files.readString(CAMPAIGN_LAUNCH_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("CampaignService.launchCampaign")
                .contains("campaign_recipients")
                .contains("ELIGIBLE")
                .contains("contact_events")
                .contains("SENT")
                .contains("campaign_metrics")
                .contains("LAUNCH")
                .contains("old status `APPROVED`")
                .contains("new status `ACTIVE`")
                .contains("Excluded recipients do not create contact events");
    }

    @Test
    void documentsCampaignLaunchNoBypassGuarantee() throws Exception {
        String documentation = Files.readString(CAMPAIGN_LAUNCH_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("No-Bypass Guarantee")
                .contains("never builds a fresh audience directly from segment criteria")
                .contains("stored recipient snapshot")
                .contains("Missing or invalid customer consent")
                .contains("Minor beneficiary rows without guardian consent")
                .contains("MARKETING_OPT_OUT")
                .contains("DO_NOT_CONTACT")
                .contains("DUPLICATE_CAMPAIGN_RECIPIENT")
                .contains("MONTHLY_CONTACT_LIMIT")
                .contains("APPROVED")
                .contains("Only rows still marked `ELIGIBLE` can create `SENT` contact events");
    }

    @Test
    void documentsCampaignLaunchSequenceDiagram() throws Exception {
        String documentation = Files.readString(CAMPAIGN_LAUNCH_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Launch Sequence Diagram")
                .contains("```mermaid")
                .contains("sequenceDiagram")
                .contains("actor Manager as Campaign Manager")
                .contains("participant UI as Recipient Preview UI")
                .contains("participant API as CampaignController")
                .contains("participant Service as CampaignService")
                .contains("participant RecipientRepo as CampaignRecipientRepository")
                .contains("participant ContactRepo as ContactEventRepository")
                .contains("participant MetricsRepo as CampaignMetricsRepository")
                .contains("participant Audit as AuditService")
                .contains("Confirm launch")
                .contains("POST /api/campaigns/{id}/launch")
                .contains("transition APPROVED to ACTIVE")
                .contains("save SENT contact_events")
                .contains("logLaunch(APPROVED, ACTIVE)")
                .contains("Show launch result");
    }

    @Test
    void documentationIndexLinksCampaignLaunchDocumentation() throws Exception {
        String index = Files.readString(Path.of("../docs/README.md"), StandardCharsets.UTF_8);

        assertThat(index).contains("[Campaign Launch Documentation](modules/campaign-launch.md)");
    }
}
