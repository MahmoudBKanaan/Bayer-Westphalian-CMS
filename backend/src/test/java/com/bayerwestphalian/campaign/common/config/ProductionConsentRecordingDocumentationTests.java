package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProductionConsentRecordingDocumentationTests {

    private static final Path SCRIPT = Path.of("../scripts/test-production-record-consent.ps1");
    private static final Path DOC = Path.of("../docs/deployment/consent-recording-verification.md");

    @Test
    void verifierRecordsAndReadsConsentAsAuthorizedAgent() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("CUSTOMER_SERVICE_AGENT")
                .contains("Post -Uri \"$origin/api/consents\"")
                .contains("MARKETING_EMAIL")
                .contains("GIVEN")
                .contains("StatusCode -ne 201")
                .contains("Guid]::TryParse")
                .contains("/api/consents/status")
                .contains("Recorded consent could not be read back");
    }

    @Test
    void verifierUsesNonContactableSyntheticDataAndTwoStageCleanup() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("example.invalid")
                .contains("INACTIVE")
                .contains("doNotContact = $true")
                .contains("/api/consents/withdraw")
                .contains("WITHDRAWN")
                .contains("RequiredRole \"ADMIN\"")
                .contains("Delete -Uri \"$origin/api/customers/$customerId\"")
                .contains("finally");
    }

    @Test
    void verifierDoesNotPersistOrPrintSensitiveMaterial() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("$agentToken = $null")
                .contains("$adminToken = $null")
                .contains("$syntheticEmail = $null")
                .doesNotContain("Write-Host $syntheticEmail")
                .doesNotContain("Write-Host $agentToken")
                .doesNotContain("Set-Content")
                .doesNotContain("Out-File");
    }

    @Test
    void documentationRecordsBlockedAuditableConsentAcceptance() throws Exception {
        String doc = Files.readString(DOC, StandardCharsets.UTF_8);

        assertThat(doc)
                .contains("Sprint 18 item 748")
                .contains("**BLOCKED**")
                .contains("no customer or consent record was created")
                .contains("valid consent UUID")
                .contains("immutable consent-creation audit evidence")
                .contains("withdrawal succeeds and is audited")
                .contains("must remain ineligible for communication")
                .contains("Direct SQL")
                .contains("do not prove deployed consent recording");
    }
}
