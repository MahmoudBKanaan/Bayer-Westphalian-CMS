package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Sprint 17 item <b>706</b>: Pipeline fails on intentionally broken test.
 *
 * <p>KB epic E25 / Sprint 17: intentional red evidence must be repeatable without committing a
 * broken test. The verification script creates a temporary failing Vitest test, expects the test
 * command to return non-zero, and deletes the probe in a finally block.
 */
@DisplayName("706 Pipeline fails on intentionally broken test")
class PipelineFailsOnIntentionallyBrokenTestDocumentationTests {

    private static final Path CI_WORKFLOW = Path.of("../.github/workflows/ci.yml");
    private static final Path CI_CD_DOC = Path.of("../docs/deployment/ci-cd.md");
    private static final Path GITHUB_README = Path.of("../.github/README.md");
    private static final Path FAILURE_SCRIPT =
            Path.of("../scripts/verify-pipeline-fails-on-broken-test.ps1");

    @Test
    void workflowReferencesIntentionalBrokenTestEvidence() throws Exception {
        assertThat(CI_WORKFLOW).exists();
        String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

        assertThat(yaml)
                .contains("706")
                .containsIgnoringCase("intentionally broken test")
                .contains("fail-on-red");
    }

    @Test
    void verificationScriptCreatesBrokenTestExpectsFailureAndCleansUp() throws Exception {
        assertThat(FAILURE_SCRIPT).exists();
        String script = Files.readString(FAILURE_SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("706")
                .contains("__pipeline_broken__.test.ts")
                .contains("expect(\"pipeline\").toBe(\"red\")")
                .contains("npm test")
                .contains("$LASTEXITCODE")
                .contains("non-zero")
                .contains("finally")
                .contains("Remove-Item")
                .contains("exit 0");
    }

    @Test
    void ciCdDocumentationExplainsRuntimeEvidenceAndNoCommittedBrokenTest() throws Exception {
        assertThat(CI_CD_DOC).exists();
        String doc = Files.readString(CI_CD_DOC, StandardCharsets.UTF_8);

        assertThat(doc)
                .contains("706")
                .contains("Pipeline fails on intentionally broken test")
                .contains("verify-pipeline-fails-on-broken-test.ps1")
                .contains("__pipeline_broken__.test.ts")
                .contains("intentionally broken test is")
                .contains("committed")
                .contains("PipelineFailsOnIntentionallyBrokenTestDocumentationTests");
    }

    @Test
    void githubReadmeReferencesItem706RuntimeEvidence() throws Exception {
        assertThat(GITHUB_README).exists();
        String readme = Files.readString(GITHUB_README, StandardCharsets.UTF_8);

        assertThat(readme)
                .contains("706")
                .contains("verify-pipeline-fails-on-broken-test.ps1")
                .contains("PipelineFailsOnIntentionallyBrokenTestDocumentationTests");
    }
}
