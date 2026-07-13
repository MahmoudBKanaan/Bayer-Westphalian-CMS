package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("781 Prepare final demo dataset")
class FinalDemoDatasetDocumentationTests {

    private static final Path SEED =
            Path.of("src/main/resources/db/demo/R__controlled_demo_data.sql");
    private static final Path MANIFEST = Path.of("../config/final-demo-dataset.json");
    private static final Path GUIDE = Path.of("../docs/demo/final-demo-dataset.md");

    @Test
    void datasetIsSyntheticDeterministicAndCoversFinalDemoWorkflows() throws Exception {
        String seed = Files.readString(SEED, StandardCharsets.UTF_8);
        String manifest = Files.readString(MANIFEST, StandardCharsets.UTF_8);

        assertThat(seed)
                .contains("Final controlled demo dataset (item 781)")
                .contains("example.test")
                .contains("on conflict")
                .contains("20000000-0000-0000-0000-000000000101")
                .contains("40000000-0000-0000-0000-000000000101")
                .contains("50000000-0000-0000-0000-000000000101")
                .contains("ai_recommendations")
                .contains("audit_logs");
        assertThat(manifest)
                .contains("\"backlogItem\": 781")
                .contains("\"scope\": \"dev-test-only\"")
                .contains("\"providerSendingRequired\": false")
                .contains("segment-preview-with-eligibility-exclusions")
                .doesNotContain("password");
    }

    @Test
    void guidePreventsProductionLoadingAndDocumentsReadOnlyVerification() throws Exception {
        String guide = Files.readString(GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("Item 781")
                .contains("Production configuration must not include `classpath:db/demo`")
                .contains("Real provider sending is not required")
                .contains("verify-final-demo-dataset.ps1")
                .contains("The verifier is read-only")
                .contains("never a production command")
                .contains("Do not present local/dev data");
    }
}
