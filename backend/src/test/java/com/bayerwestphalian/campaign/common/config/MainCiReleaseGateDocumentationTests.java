package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("714 Main is not releasable unless CI passes")
class MainCiReleaseGateDocumentationTests {

    private static final Path GATE = Path.of("../scripts/assert-main-ci-success.sh");
    private static final Path DEPLOY = Path.of("../.github/workflows/deploy-placeholder.yml");
    private static final Path GUIDE = Path.of("../docs/deployment/release-tagging.md");

    @Test
    void releaseGateRequiresSuccessfulCiForExactMainCommit() throws Exception {
        String gate = Files.readString(GATE, StandardCharsets.UTF_8);

        assertThat(gate)
                .contains("714")
                .contains("release_branch\" != \"main")
                .contains("actions/workflows/ci.yml/runs")
                .contains("head_sha=${GITHUB_SHA}")
                .contains(".status == \"completed\"")
                .contains(".conclusion == \"success\"")
                .contains(".head_sha == env.GITHUB_SHA")
                .contains("no completed successful CI push run exists");
    }

    @Test
    void deploymentRunsGateBeforeRecordingIntent() throws Exception {
        String workflow = Files.readString(DEPLOY, StandardCharsets.UTF_8);

        assertThat(workflow).contains("actions: read").contains("scripts/assert-main-ci-success.sh");
        assertThat(workflow.indexOf("scripts/assert-main-ci-success.sh"))
                .isLessThan(workflow.indexOf("Record placeholder deployment intent"));
    }

    @Test
    void guideDefinesFailClosedReleaseReadiness() throws Exception {
        String guide = Files.readString(GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("Mandatory main CI release gate (item 714)")
                .contains("exact commit SHA")
                .contains("completed")
                .contains("success")
                .contains("fails closed")
                .contains("local test results do not make");
    }
}
