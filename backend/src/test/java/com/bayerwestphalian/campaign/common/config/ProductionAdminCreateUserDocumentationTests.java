package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProductionAdminCreateUserDocumentationTests {

    private static final Path SCRIPT = Path.of("../scripts/test-production-admin-create-user.ps1");
    private static final Path DOC = Path.of("../docs/deployment/admin-create-user-verification.md");

    @Test
    void verifierCreatesReadsAndDisablesSyntheticUserAsAdmin() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("PSCredential")
                .contains("ADMIN")
                .contains("example.invalid")
                .contains("/api/users")
                .contains("StatusCode -ne 201")
                .contains("Guid]::TryParse")
                .contains("/disable")
                .contains("DISABLED");
    }

    @Test
    void verifierUsesGeneratedSecretsAndFailureCleanupWithoutPrintingThem() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("Guid]::NewGuid")
                .contains("finally")
                .contains("during failure cleanup")
                .contains("$adminPassword = $null")
                .contains("$syntheticPassword = $null")
                .contains("$accessToken = $null")
                .doesNotContain("Write-Host $syntheticEmail")
                .doesNotContain("Write-Host $syntheticPassword")
                .doesNotContain("Set-Content");
    }

    @Test
    void documentationRecordsBlockedExecutionAndAuditableAcceptance() throws Exception {
        String doc = Files.readString(DOC, StandardCharsets.UTF_8);

        assertThat(doc)
                .contains("Sprint 18 item 746")
                .contains("**BLOCKED**")
                .contains("no synthetic user was created")
                .contains("HTTP 201")
                .contains("valid UUID")
                .contains("user-creation audit event")
                .contains("synthetic account is `DISABLED`")
                .contains("direct database insert")
                .contains("does not prove this acceptance criterion");
    }
}
