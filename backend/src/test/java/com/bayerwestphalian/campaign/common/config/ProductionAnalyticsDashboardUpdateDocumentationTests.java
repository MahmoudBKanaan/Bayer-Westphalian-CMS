package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProductionAnalyticsDashboardUpdateDocumentationTests {

    private static final Path SCRIPT =
            Path.of("../scripts/test-production-analytics-dashboard-updates.ps1");
    private static final Path DOC =
            Path.of("../docs/deployment/analytics-dashboard-update-verification.md");

    @Test
    void verifierRequiresProviderDisableAndZeroRecipients() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("ProviderSendingConfirmedDisabled")
                .contains("/recipients/eligible")
                .contains("/recipients/excluded")
                .contains("recipient rows exist")
                .contains("productIds = @()");
    }

    @Test
    void verifierChecksDeterministicDashboardAndDetailDeltas() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("/api/analytics/dashboard")
                .contains("/api/analytics/executive")
                .contains("campaignTotal + 1")
                .contains("activeCampaigns + 1")
                .contains("messagesSent")
                .contains("audienceSize")
                .contains("/api/analytics/campaigns/$campaignId")
                .contains("completedCampaigns + 1");
    }

    @Test
    void verifierCompletesAndArchivesSyntheticLifecycle() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("/$campaignId/launch")
                .contains("/$campaignId/complete")
                .contains("/$campaignId/archive")
                .contains("$campaignStatus = \"ARCHIVED\"")
                .contains("finally");
    }

    @Test
    void documentationRecordsBlockedFailClosedAcceptance() throws Exception {
        String doc = Files.readString(DOC, StandardCharsets.UTF_8);

        assertThat(doc)
                .contains("Sprint 18 item 755")
                .contains("**BLOCKED**")
                .contains("no dashboard was queried")
                .contains("campaign total `+1`")
                .contains("active campaigns `+1`")
                .contains("completed campaigns increases by one")
                .contains("fails closed")
                .contains("do not prove deployed aggregation updates");
    }
}
