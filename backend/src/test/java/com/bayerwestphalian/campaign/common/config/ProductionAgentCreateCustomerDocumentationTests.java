package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProductionAgentCreateCustomerDocumentationTests {

    private static final Path SCRIPT =
            Path.of("../scripts/test-production-agent-create-customer.ps1");
    private static final Path DOC =
            Path.of("../docs/deployment/agent-create-customer-verification.md");

    @Test
    void verifierUsesAgentForCreationAndAdminOnlyForCleanup() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("CUSTOMER_SERVICE_AGENT")
                .contains("RequiredRole \"ADMIN\"")
                .contains("Post -Uri \"$origin/api/customers\"")
                .contains("Get -Uri \"$origin/api/customers/$customerId\"")
                .contains("Delete -Uri \"$origin/api/customers/$customerId\"")
                .contains("Admin soft-delete succeeded");
    }

    @Test
    void verifierCreatesSafeSyntheticCustomerAndValidatesPersistence() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("example.invalid")
                .contains("PROSPECT")
                .contains("INACTIVE")
                .contains("doNotContact = $true")
                .contains("PRODUCTION_SMOKE_TEST")
                .contains("StatusCode -ne 201")
                .contains("Guid]::TryParse")
                .contains("deletedAt");
    }

    @Test
    void verifierCleansUpAndDoesNotPrintSensitiveIdentityMaterial() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("finally")
                .contains("soft-deleted during failure cleanup")
                .contains("$agentToken = $null")
                .contains("$adminToken = $null")
                .contains("$syntheticEmail = $null")
                .doesNotContain("Write-Host $syntheticEmail")
                .doesNotContain("Write-Host $agentToken")
                .doesNotContain("Set-Content");
    }

    @Test
    void documentationRecordsBlockedRoleSpecificAcceptance() throws Exception {
        String doc = DocumentationTestText.normalize(Files.readString(DOC, StandardCharsets.UTF_8));

        assertThat(doc)
                .contains("Sprint 18 item 747")
                .contains("**BLOCKED**")
                .contains("No Customer Service Agent or Admin cleanup credential was requested")
                .contains("An active `CUSTOMER_SERVICE_AGENT`, not Admin")
                .contains("Customer Service Agent still cannot use Admin-only delete behavior")
                .contains("soft-deletes the synthetic customer")
                .contains("does not prove this role-specific workflow");
    }
}
