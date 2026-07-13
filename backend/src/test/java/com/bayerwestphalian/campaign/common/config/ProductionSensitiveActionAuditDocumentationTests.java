package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProductionSensitiveActionAuditDocumentationTests {

    private static final Path SCRIPT =
            Path.of("../scripts/test-production-sensitive-action-audit.ps1");
    private static final Path DOC =
            Path.of("../docs/deployment/sensitive-action-audit-verification.md");

    @Test
    void verifierRequiresExactActorLinkedCreateAndDisableEvents() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("ADMIN")
                .contains("/api/audit-logs/entities/users/$Id")
                .contains("$_.action -eq \"CREATE\"")
                .contains("$_.entityType -eq \"users\"")
                .contains("$_.actorUserId -eq $adminUserId")
                .contains("$createEvents.Count -ne 1")
                .contains("DISABLE_USER")
                .contains("$disableEvents.Count -ne 1");
    }

    @Test
    void verifierRejectsPasswordMaterialAndCleansSyntheticAccount() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("password|passwordHash|rawPassword")
                .contains("Audit payload contains prohibited password material")
                .contains("/disable")
                .contains("finally")
                .contains("$syntheticPassword = $null")
                .contains("$creationAuditJson = $null")
                .doesNotContain("Write-Host $syntheticPassword");
    }

    @Test
    void documentationRecordsBlockedTransactionalImmutableAcceptance() throws Exception {
        String doc = Files.readString(DOC, StandardCharsets.UTF_8);

        assertThat(doc)
                .contains("Sprint 18 item 756")
                .contains("**BLOCKED**")
                .contains("no sensitive action ran")
                .contains("exactly one entity-history `CREATE` event")
                .contains("exactly one `DISABLE_USER` event")
                .contains("normal application behavior offers no audit edit/delete path")
                .contains("Operational logs alone do not replace `AuditLog`")
                .contains("do not prove deployed transactional audit behavior");
    }
}
