package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Sprint 17 item <b>694</b>: Verify pipeline passes on clean main branch.
 *
 * <p>KB epic E25 / Sprint 17: a clean, green codebase on {@code main} must be able to produce a
 * green CI run (pass-on-green). Locks that the CI workflow triggers on push to {@code main}, runs
 * the full job set without path filters or job-level {@code if:} skips, and that the README badge
 * reports {@code main} status. Does not execute GitHub Actions or claim a remote green check.
 */
@DisplayName("694 Verify pipeline passes on clean main branch")
class PipelinePassesOnCleanMainBranchDocumentationTests {

    private static final Path CI_WORKFLOW = Path.of("../.github/workflows/ci.yml");
    private static final Path CI_CD_DOC = Path.of("../docs/deployment/ci-cd.md");
    private static final Path GITHUB_README = Path.of("../.github/README.md");
    private static final Path ROOT_README = Path.of("../README.md");

    private static final List<String> REQUIRED_JOBS =
            List.of(
                    "backend-build:",
                    "backend-test:",
                    "backend-integration-test:",
                    "frontend-install:",
                    "frontend-lint:",
                    "frontend-test:",
                    "frontend-build:",
                    "docker-backend:",
                    "docker-frontend:",
                    "docker-compose-validate:",
                    "production-config-validate:");

    @Nested
    @DisplayName("CI workflow: pass-on-green for main")
    class PassOnGreenMain {

        @Test
        void workflowDocumentsPassOnGreenForItem694() throws Exception {
            assertThat(CI_WORKFLOW).exists();
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            assertThat(yaml)
                    .contains("694")
                    .containsIgnoringCase("pass-on-green")
                    .containsIgnoringCase("clean main");
        }

        @Test
        void pushToMainTriggersCiWithoutPathFilters() throws Exception {
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            assertThat(yaml).contains("push:");
            assertThat(yaml).contains("pull_request:");
            assertThat(yaml).contains("- main");
            assertThat(yaml).contains("- dev");
            assertThat(yaml).doesNotContain("paths:");
            assertThat(yaml).doesNotContain("paths-ignore:");
            assertThat(yaml).doesNotContain("tags-ignore:");
        }

        @Test
        void fullJobSetDefinedWithoutJobLevelIfSkips() throws Exception {
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            for (String job : REQUIRED_JOBS) {
                assertThat(yaml).as(job).contains(job);
            }

            // No job/step if: that would skip the matrix on main (workflow stays unconditional).
            assertThat(yaml.lines().anyMatch(line -> {
                        String trimmed = line.strip();
                        return trimmed.startsWith("if:")
                                && (trimmed.contains("github.ref")
                                        || trimmed.contains("false")
                                        || trimmed.contains("github.event"));
                    }))
                    .as("no job/step if: conditions that skip CI on main")
                    .isFalse();
        }

        @Test
        void qualityAndPackageJobsPresentForCleanMainGreenPath() throws Exception {
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            assertThat(yaml).contains("mvn -B -DskipTests package");
            assertThat(yaml).contains("mvn -B test");
            assertThat(yaml).contains("npm test");
            assertThat(yaml).contains("npm run build");
            assertThat(yaml).contains("actions/upload-artifact@v4");
            assertThat(yaml).contains("name: CI");
        }
    }

    @Nested
    @DisplayName("README badge and documentation")
    class Documentation {

        @Test
        void readmeBadgeTracksMainBranchStatus() throws Exception {
            assertThat(ROOT_README).exists();
            String readme = Files.readString(ROOT_README, StandardCharsets.UTF_8);

            assertThat(readme)
                    .contains("badge.svg?branch=main")
                    .contains("actions/workflows/ci.yml");
        }

        @Test
        void ciCdDocDocumentsPassOnGreenForItem694() throws Exception {
            assertThat(CI_CD_DOC).exists();
            String doc = Files.readString(CI_CD_DOC, StandardCharsets.UTF_8);

            assertThat(doc)
                    .contains("694")
                    .containsIgnoringCase("passes on clean main")
                    .containsIgnoringCase("pass-on-green")
                    .contains("push")
                    .contains("main")
                    .contains("PipelinePassesOnCleanMainBranchDocumentationTests");
        }

        @Test
        void githubReadmeReferencesItem694() throws Exception {
            assertThat(GITHUB_README).exists();
            String readme = Files.readString(GITHUB_README, StandardCharsets.UTF_8);

            assertThat(readme)
                    .contains("694")
                    .contains("PipelinePassesOnCleanMainBranchDocumentationTests");
        }
    }
}
