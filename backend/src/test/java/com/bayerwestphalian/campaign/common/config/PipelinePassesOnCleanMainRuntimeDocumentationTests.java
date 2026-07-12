package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Sprint 17 item <b>707</b>: Pipeline passes on clean main branch.
 *
 * <p>KB epic E25 / Sprint 17: runtime pass evidence must be repeatable and must not be recorded
 * from a dirty development tree. The verification script requires branch {@code main}, a clean
 * worktree, then runs the local CI parity commands that mirror the workflow quality gates.
 */
@DisplayName("707 Pipeline passes on clean main branch")
class PipelinePassesOnCleanMainRuntimeDocumentationTests {

    private static final Path CI_WORKFLOW = Path.of("../.github/workflows/ci.yml");
    private static final Path CI_CD_DOC = Path.of("../docs/deployment/ci-cd.md");
    private static final Path GITHUB_README = Path.of("../.github/README.md");
    private static final Path CLEAN_MAIN_SCRIPT =
            Path.of("../scripts/verify-pipeline-passes-on-clean-main.ps1");

    @Test
    void workflowReferencesCleanMainRuntimeEvidence() throws Exception {
        assertThat(CI_WORKFLOW).exists();
        String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

        assertThat(yaml)
                .contains("707")
                .containsIgnoringCase("clean main")
                .containsIgnoringCase("pass-on-green");
    }

    @Test
    void verificationScriptRequiresCleanMainAndRunsCiParityCommands() throws Exception {
        assertThat(CLEAN_MAIN_SCRIPT).exists();
        String script = Files.readString(CLEAN_MAIN_SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("707")
                .contains("Pipeline passes on clean main branch")
                .contains("git branch --show-current")
                .contains("git status --porcelain")
                .contains("mvn.cmd")
                .contains("-DskipTests")
                .contains("npm.cmd")
                .contains("npm test")
                .contains("npm run build")
                .contains("docker")
                .contains("test-docker-compose-config.ps1");
    }

    @Test
    void ciCdDocumentationExplainsCleanMainRuntimeEvidence() throws Exception {
        assertThat(CI_CD_DOC).exists();
        String doc = Files.readString(CI_CD_DOC, StandardCharsets.UTF_8);

        assertThat(doc)
                .contains("707")
                .contains("Pipeline passes on clean main branch")
                .contains("verify-pipeline-passes-on-clean-main.ps1")
                .contains("clean worktree")
                .contains("PipelinePassesOnCleanMainRuntimeDocumentationTests");
    }

    @Test
    void githubReadmeReferencesItem707RuntimeEvidence() throws Exception {
        assertThat(GITHUB_README).exists();
        String readme = Files.readString(GITHUB_README, StandardCharsets.UTF_8);

        assertThat(readme)
                .contains("707")
                .contains("verify-pipeline-passes-on-clean-main.ps1")
                .contains("PipelinePassesOnCleanMainRuntimeDocumentationTests");
    }
}
