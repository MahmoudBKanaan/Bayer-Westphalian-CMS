package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProductionContactEventRecordingDocumentationTests {

    private static final Path SCRIPT = Path.of("../scripts/test-production-record-contact-event.ps1");
    private static final Path DOC = Path.of("../docs/deployment/contact-event-recording-verification.md");

    @Test
    void verifierRecordsProviderFreeNoteAsCustomerServiceAgent() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("CUSTOMER_SERVICE_AGENT")
                .contains("Post -Uri \"$origin/api/contact-events\"")
                .contains("channel = \"PHONE\"")
                .contains("eventType = \"NOTE\"")
                .contains("NO_RESPONSE")
                .contains("StatusCode -ne 201")
                .contains("Guid]::TryParse")
                .contains("createdByUserId");
    }

    @Test
    void verifierReadsExactTimelineEventAndCleansCustomer() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("/api/contact-events/timeline?customerId=$customerId&eventType=NOTE")
                .contains("$matching.Count -ne 1")
                .contains("RequiredRole \"ADMIN\"")
                .contains("Delete -Uri \"$origin/api/customers/$customerId\"")
                .contains("immutable event retained")
                .contains("during failure cleanup");
    }

    @Test
    void verifierDoesNotPrintOrPersistSensitiveContactMaterial() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("$agentToken = $null")
                .contains("$adminToken = $null")
                .contains("$syntheticEmail = $null")
                .contains("$eventNotes = $null")
                .doesNotContain("Write-Host $eventNotes")
                .doesNotContain("Write-Host $syntheticEmail")
                .doesNotContain("Set-Content");
    }

    @Test
    void documentationRecordsBlockedImmutableContactAcceptance() throws Exception {
        String doc = Files.readString(DOC, StandardCharsets.UTF_8);

        assertThat(doc)
                .contains("Sprint 18 item 754")
                .contains("**BLOCKED**")
                .contains("No Agent or Admin cleanup credential was requested")
                .contains("provider-free `PHONE`/`NOTE` event")
                .contains("exactly one matching event")
                .contains("immutable history")
                .contains("does not call an email/SMS provider")
                .contains("do not prove the deployed write/read workflow");
    }
}
