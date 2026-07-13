package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProductionComplianceApproveCampaignDocumentationTests {

    private static final Path SCRIPT =
            Path.of("../scripts/test-production-compliance-approve-campaign.ps1");
    private static final Path DOC =
            Path.of("../docs/deployment/compliance-approve-campaign-verification.md");

    @Test
    void verifierSeparatesManagerSubmissionFromComplianceApproval() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("CAMPAIGN_MANAGER")
                .contains("COMPLIANCE_OFFICER")
                .contains("/$campaignId/submit")
                .contains("/$campaignId/approve")
                .contains("complianceReviewNotes")
                .contains("APPROVED")
                .contains("approvedByUserId")
                .contains("approvedAt");
    }

    @Test
    void verifierRequiresPersistedHumanApprovalAuditEvidence() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("/api/audit-logs/entities/campaigns/$campaignId")
                .contains("$_.action -eq \"APPROVE\"")
                .contains("$_.actorUserId")
                .contains("Immutable APPROVE audit event")
                .contains("Retained synthetic campaign UUID");
    }

    @Test
    void verifierKeepsApprovedCampaignTargetlessAndNeverLaunches() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("productIds = @()")
                .contains("No segment, products, recipients, schedule, or sending")
                .contains("never preview, launch, or send")
                .doesNotContain("/$campaignId/launch")
                .doesNotContain("/recipients/preview");
    }

    @Test
    void documentationRecordsBlockedHumanOnlyAcceptanceAndRetention() throws Exception {
        String doc = Files.readString(DOC, StandardCharsets.UTF_8);

        assertThat(doc)
                .contains("Sprint 18 item 752")
                .contains("**BLOCKED**")
                .contains("No Campaign Manager or Compliance Officer credential was requested")
                .contains("real human using the `COMPLIANCE_OFFICER` role, not Admin or AI")
                .contains("immutable `APPROVE` audit event")
                .contains("intentionally retained")
                .contains("must never be launched casually")
                .contains("do not prove this role-specific human gate");
    }
}
