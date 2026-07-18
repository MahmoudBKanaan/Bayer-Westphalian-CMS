package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProductionAdminLoginDocumentationTests {

    private static final Path SCRIPT = Path.of("../scripts/test-production-admin-login.ps1");
    private static final Path DOC = Path.of("../docs/deployment/admin-login-verification.md");

    @Test
    void verifierRequiresActiveAdminAndMatchingCurrentSession() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("PSCredential")
                .contains("/api/auth/login")
                .contains("/api/auth/me")
                .contains("ACTIVE")
                .contains("ADMIN")
                .contains("/api/auth/logout")
                .contains("Access token is not a JWT");
    }

    @Test
    void verifierDoesNotPrintOrPersistCredentialAndTokenMaterial() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("GetNetworkCredential().Password")
                .contains("$plainPassword = $null")
                .contains("$accessToken = $null")
                .doesNotContain("Write-Host $plainPassword")
                .doesNotContain("Write-Host $accessToken")
                .doesNotContain("Set-Content")
                .doesNotContain("Out-File");
    }

    @Test
    void documentationRecordsBlockedExecutionAndStrictAcceptance() throws Exception {
        String doc = DocumentationTestText.normalize(Files.readString(DOC, StandardCharsets.UTF_8));

        assertThat(doc)
                .contains("Sprint 18 item 745")
                .contains("**BLOCKED**")
                .contains("No production administrator credential was requested or used")
                .contains("Seeded `.test` credentials")
                .contains("item 744 has passed")
                .contains("menu visibility alone does not prove an Admin can log in");
    }
}
