package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("761 Backup guide")
class ProductionBackupGuideDocumentationTests {

    private static final Path GUIDE = Path.of("../docs/deployment/backup-guide.md");

    @Test
    void guideDefinesScheduledVerifiedAndOffHostBackups() throws Exception {
        String guide = Files.readString(GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("Sprint 18 item 761")
                .contains("NFR-013")
                .contains("RPO")
                .contains("RTO")
                .contains("database-backup")
                .contains("pg_dump --format=custom")
                .contains("SHA-256")
                .contains("encrypted off-host storage")
                .contains("test-production-backup.ps1")
                .contains("test-production-backup-exists.ps1");
    }

    @Test
    void guideCoversCompleteRecoveryScopeAndReleaseGate() throws Exception {
        String guide = Files.readString(GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("bwc_consent_evidence")
                .contains("matching recovery point")
                .contains("test-production-restore.ps1")
                .contains("A release is blocked")
                .contains("Never commit dumps")
                .contains("must not appear in Git, CI artifacts, screenshots, or tickets");
    }
}
