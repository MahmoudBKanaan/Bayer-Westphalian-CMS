package com.bayerwestphalian.campaign.consent;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.actuate.health.Status;

class ConsentEvidenceStorageHealthIndicatorTests {

    @TempDir Path temporaryDirectory;

    @Test
    void reportsUpForWritableStorageDirectory() {
        Path root = temporaryDirectory.resolve("consent-evidence");
        ConsentEvidenceStorageHealthIndicator indicator =
                new ConsentEvidenceStorageHealthIndicator(root.toString());

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
        assertThat(Files.isDirectory(root)).isTrue();
    }

    @Test
    void reportsDownWhenConfiguredRootIsAFile() throws Exception {
        Path root = temporaryDirectory.resolve("not-a-directory");
        Files.writeString(root, "occupied");
        ConsentEvidenceStorageHealthIndicator indicator =
                new ConsentEvidenceStorageHealthIndicator(root.toString());

        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
        assertThat(indicator.health().getDetails().get("reason"))
                .isEqualTo("storage_unavailable");
    }
}
