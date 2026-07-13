package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProductionCampaignManagerCreateSegmentDocumentationTests {

    private static final Path SCRIPT =
            Path.of("../scripts/test-production-campaign-manager-create-segment.ps1");
    private static final Path DOC =
            Path.of("../docs/deployment/campaign-manager-create-segment-verification.md");

    @Test
    void verifierUsesCampaignManagerForCompleteSegmentLifecycle() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("CAMPAIGN_MANAGER")
                .contains("Post -Uri \"$origin/api/segments\"")
                .contains("Get -Uri \"$origin/api/segments/$segmentId\"")
                .contains("Delete -Uri \"$origin/api/segments/$segmentId\"")
                .doesNotContain("AdminCredential")
                .doesNotContain("AdminCleanupCredential");
    }

    @Test
    void verifierValidatesAutomaticUuidOwnershipAndAudienceSafety() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("StatusCode -ne 201")
                .contains("Guid]::TryParse")
                .contains("ownerUserId")
                .contains("visibility = \"PRIVATE\"")
                .contains("criteria = @()")
                .contains("no audience criteria")
                .doesNotContain("/api/segments/preview");
    }

    @Test
    void verifierGuaranteesCleanupWithoutPrintingSensitiveMaterial() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("finally")
                .contains("deleted during failure cleanup")
                .contains("$password = $null")
                .contains("$accessToken = $null")
                .contains("$syntheticName = $null")
                .doesNotContain("Write-Host $syntheticName")
                .doesNotContain("Write-Host $accessToken")
                .doesNotContain("Set-Content");
    }

    @Test
    void documentationRecordsBlockedRoleSpecificUuidAcceptance() throws Exception {
        String doc = Files.readString(DOC, StandardCharsets.UTF_8);

        assertThat(doc)
                .contains("Sprint 18 item 750")
                .contains("**BLOCKED**")
                .contains("No Campaign Manager credential was requested")
                .contains("active `CAMPAIGN_MANAGER`, not Admin")
                .contains("valid-format UUID automatically")
                .contains("No customer preview is needed")
                .contains("do not prove the role-specific deployed workflow");
    }
}
