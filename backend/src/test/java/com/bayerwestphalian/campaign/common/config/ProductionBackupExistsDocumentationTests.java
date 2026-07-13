package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProductionBackupExistsDocumentationTests {

    private static final Path SCRIPT = Path.of("../scripts/test-production-backup-exists.ps1");
    private static final Path DOC = Path.of("../docs/deployment/backup-and-restore.md");

    @Test
    void verifierRequiresFreshCompletedValidArchiveReadOnly() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("docker volume inspect")
                .contains("dst=/backups,readonly")
                .contains("--network none")
                .contains("! -name '*.partial'")
                .contains("test -s")
                .contains("sha256sum -c")
                .contains("pg_restore --list")
                .contains("MAXIMUM_AGE_SECONDS");
    }

    @Test
    void documentationRecordsBlockedArtifactBasedAcceptance() throws Exception {
        String doc = Files.readString(DOC, StandardCharsets.UTF_8);

        assertThat(doc)
                .contains("Backup existence verification (Item 757)")
                .contains("2026-07-13T00:27:39+03:00")
                .contains("**BLOCKED**")
                .contains("no `bwc_postgres_backups`")
                .contains("Existing database data volumes are not backup evidence")
                .contains("not merely that backup configuration is present");
    }
}
