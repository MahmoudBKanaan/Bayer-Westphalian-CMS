package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProductionLogsAccessibleDocumentationTests {

    private static final Path SCRIPT = Path.of("../scripts/test-production-logs-accessible.ps1");
    private static final Path DOC = Path.of("../docs/deployment/production-logging.md");

    @Test
    void verifierReadsBoundedLogsForEveryProductionServiceWithoutPrintingLines() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("postgres")
                .contains("database-backup")
                .contains("backend")
                .contains("frontend")
                .contains("reverse-proxy")
                .contains("docker logs --tail")
                .contains("Lines = $lines.Count")
                .doesNotContain("Write-Host $lines");
    }

    @Test
    void verifierScansAndDiscardsPotentialSensitiveOutput() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("secretPattern")
                .contains("Potential secret-bearing log output detected")
                .contains("$lines = $null")
                .contains("TailLines must be between 1 and 500");
    }

    @Test
    void documentationRecordsBlockedProductionOnlyAcceptance() throws Exception {
        String doc = Files.readString(DOC, StandardCharsets.UTF_8);

        assertThat(doc)
                .contains("Production Log Accessibility Verification (Item 758)")
                .contains("2026-07-13T00:29:40+03:00")
                .contains("**BLOCKED**")
                .contains("Only development PostgreSQL is running")
                .contains("Development database logs are not release evidence")
                .contains("all five deployed services")
                .contains("access is denied to normal users");
    }
}
