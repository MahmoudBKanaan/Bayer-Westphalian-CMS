package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DeploymentScreenshotEvidenceDocumentationTests {

    private static final Path EVIDENCE =
            Path.of("../docs/deployment/deployment-screenshot-evidence.md");

    @Test
    void evidenceRecordHonestlyDocumentsBlockedCaptureAttempt() throws Exception {
        String notes = Files.readString(EVIDENCE, StandardCharsets.UTF_8);

        assertThat(notes)
                .contains("Sprint 18 item 743")
                .contains("2026-07-12T20:59:29Z")
                .contains("BLOCKED - no production deployment available")
                .contains("Valid deployment screenshots captured | **0**")
                .contains("Vite development server and is not release evidence")
                .contains("would misrepresent");
    }

    @Test
    void evidencePlanDefinesCompleteDeploymentScreenshotSet() throws Exception {
        String notes = Files.readString(EVIDENCE, StandardCharsets.UTF_8);

        assertThat(notes)
                .contains("DEP-01")
                .contains("DEP-02")
                .contains("DEP-03")
                .contains("DEP-04")
                .contains("DEP-05")
                .contains("DEP-06")
                .contains("DEP-07")
                .contains("DEP-08")
                .contains("DEP-09")
                .contains("743-<NN>-<short-description>-<UTC>.png");
    }

    @Test
    void evidencePlanPreventsSecretAndCustomerDataCapture() throws Exception {
        String notes = Files.readString(EVIDENCE, StandardCharsets.UTF_8);

        assertThat(notes)
                .contains("Prohibited content")
                .contains("Never open `.env.production`")
                .contains("Use synthetic data")
                .contains("second person approves every image")
                .contains("production screenshots stay outside Git")
                .contains("privacy/security review");
    }

    @Test
    void screenshotEvidenceIsIndexedAndBlocksReleaseDraft() throws Exception {
        String index = Files.readString(Path.of("../docs/README.md"), StandardCharsets.UTF_8);
        String release =
                Files.readString(
                        Path.of("../docs/releases/v1.0-draft.md"), StandardCharsets.UTF_8);

        assertThat(index)
                .contains("deployment/deployment-screenshot-evidence.md")
                .contains("item **743**");
        assertThat(release)
                .contains("Deployment screenshots")
                .contains("Item 743 DEP-01 through DEP-09")
                .contains("**BLOCKED**");
    }
}
