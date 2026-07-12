package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Sprint 17 item <b>693</b>: Verify pipeline fails when tests fail.
 *
 * <p>KB epic E25 / Sprint 17: broken tests must fail CI (fail-on-red). Locks that quality-gate jobs
 * run real test/lint commands without {@code continue-on-error} or shell swallow ({@code || true}).
 * Does not execute the pipeline or inject a deliberately broken test (see item <b>706</b> for
 * intentional break evidence).
 */
@DisplayName("693 Verify pipeline fails when tests fail")
class PipelineFailsWhenTestsFailDocumentationTests {

    private static final Path CI_WORKFLOW = Path.of("../.github/workflows/ci.yml");
    private static final Path CI_CD_DOC = Path.of("../docs/deployment/ci-cd.md");
    private static final Path GITHUB_README = Path.of("../.github/README.md");

    @Nested
    @DisplayName("CI workflow: fail-on-red quality gates")
    class FailOnRedJobs {

        @Test
        void workflowDocumentsFailOnRedForItem693() throws Exception {
            assertThat(CI_WORKFLOW).exists();
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            assertThat(yaml)
                    .contains("693")
                    .containsIgnoringCase("fail-on-red")
                    .contains("continue-on-error");
        }

        @Test
        void backendTestJobFailsHardOnMavenTestFailure() throws Exception {
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);
            String block = jobBlock(yaml, "backend-test:", "backend-integration-test:");

            assertThat(block).contains("mvn -B test");
            assertThat(block).contains("id: backend-test");
            assertThat(block).contains("693");
            assertFailOnRed(block);
            assertThat(block).doesNotContain("mvn -B -DskipTests");
            assertThat(block).doesNotContain("-DskipTests");
            assertThat(block).doesNotContain("|| true");
        }

        @Test
        void backendIntegrationTestJobFailsHardOnMavenFailure() throws Exception {
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);
            String block = jobBlock(yaml, "backend-integration-test:", "frontend-install:");

            assertThat(block).contains("mvn -B test -Dtest='*IntegrationTests'");
            assertThat(block).contains("id: backend-integration-test");
            assertThat(block).contains("693");
            assertFailOnRed(block);
            assertThat(block).doesNotContain("|| true");
        }

        @Test
        void frontendTestJobFailsHardOnNpmTestFailure() throws Exception {
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);
            String block = jobBlock(yaml, "frontend-test:", "frontend-build:");

            assertThat(block).contains("npm test");
            assertThat(block).contains("id: frontend-test");
            assertThat(block).contains("693");
            assertFailOnRed(block);
            assertThat(block).doesNotContain("|| true");
            assertThat(block).doesNotContain("--passWithNoTests");
        }

        @Test
        void frontendLintJobFailsHardOnLintFailure() throws Exception {
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);
            String block = jobBlock(yaml, "frontend-lint:", "frontend-test:");

            assertThat(block).contains("npm run lint");
            assertThat(block).contains("id: frontend-lint");
            assertThat(block).contains("693");
            assertFailOnRed(block);
            assertThat(block).doesNotContain("|| true");
        }

        @Test
        void qualityGateJobsDoNotSetContinueOnError() throws Exception {
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            assertFailOnRed(jobBlock(yaml, "backend-test:", "backend-integration-test:"));
            assertFailOnRed(jobBlock(yaml, "backend-integration-test:", "frontend-install:"));
            assertFailOnRed(jobBlock(yaml, "frontend-lint:", "frontend-test:"));
            assertFailOnRed(jobBlock(yaml, "frontend-test:", "frontend-build:"));
        }
    }

    @Nested
    @DisplayName("Documentation")
    class Documentation {

        @Test
        void ciCdDocDocumentsFailOnRedVerificationForItem693() throws Exception {
            assertThat(CI_CD_DOC).exists();
            String doc = Files.readString(CI_CD_DOC, StandardCharsets.UTF_8);

            assertThat(doc)
                    .contains("693")
                    .containsIgnoringCase("fails when tests fail")
                    .containsIgnoringCase("continue-on-error")
                    .contains("backend-test")
                    .contains("frontend-test")
                    .contains("PipelineFailsWhenTestsFailDocumentationTests");
        }

        @Test
        void githubReadmeReferencesItem693() throws Exception {
            assertThat(GITHUB_README).exists();
            String readme = Files.readString(GITHUB_README, StandardCharsets.UTF_8);

            assertThat(readme)
                    .contains("693")
                    .contains("PipelineFailsWhenTestsFailDocumentationTests");
        }
    }

    private static void assertFailOnRed(String jobBlock) {
        assertThat(jobBlock).doesNotContain("continue-on-error: true");
        assertThat(jobBlock).doesNotContain("continue-on-error:true");
        // Comments may mention continue-on-error; forbid the YAML key enabling it.
        assertThat(jobBlock.lines().anyMatch(line -> {
                    String trimmed = line.strip();
                    return trimmed.startsWith("continue-on-error:")
                            && trimmed.contains("true");
                }))
                .as("job must not enable continue-on-error: true")
                .isFalse();
    }

    private static String jobBlock(String yaml, String jobKey, String nextJobKey) {
        int start = indexOfJob(yaml, jobKey);
        int end = indexOfJob(yaml, nextJobKey);
        assertThat(start).as(jobKey).isNonNegative();
        assertThat(end).as(nextJobKey).isGreaterThan(start);
        return yaml.substring(start, end);
    }

    private static int indexOfJob(String yaml, String jobKey) {
        int withNewline = yaml.indexOf("\n  " + jobKey);
        if (withNewline >= 0) {
            return withNewline;
        }
        return yaml.indexOf(jobKey);
    }
}
