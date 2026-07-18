package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("762 Restore guide")
class ProductionRestoreGuideDocumentationTests {

    private static final Path GUIDE = Path.of("../docs/deployment/restore-guide.md");

    @Test
    void guideDefinesApprovedVerifiedAndRehearsedRestore() throws Exception {
        String guide = DocumentationTestText.normalize(Files.readString(GUIDE, StandardCharsets.UTF_8));

        assertThat(guide)
                .contains("Sprint 18 item 762")
                .contains("NFR-013")
                .contains("expected RPO")
                .contains("maintenance mode")
                .contains("sha256sum -c")
                .contains("pg_restore --list")
                .contains("test-production-restore.ps1")
                .contains("--exit-on-error --no-owner --no-privileges")
                .contains("matching consent-evidence snapshot");
    }

    @Test
    void guideFailsClosedAndRequiresBusinessValidation() throws Exception {
        String guide = DocumentationTestText.normalize(Files.readString(GUIDE, StandardCharsets.UTF_8));

        assertThat(guide)
                .contains("flyway_schema_history")
                .contains("Never run `flyway clean`")
                .contains("unauthorized page/API denial")
                .contains("Never automatically replay campaign sends")
                .contains("Abort criteria")
                .contains("End maintenance mode only after explicit approval")
                .contains("new post-restore recovery point");
    }
}
