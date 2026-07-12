package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Sprint 17 item <b>698</b>: CI runs on pull request.
 *
 * <p>KB epic E25: pull requests into protected branches must trigger continuous integration so
 * fail-on-red (**693**) and branch protection (**695**) can gate merges. Locks the {@code
 * pull_request} trigger on the CI workflow without executing GitHub Actions.
 */
@DisplayName("698 CI runs on pull request")
class CiRunsOnPullRequestDocumentationTests {

    private static final Path CI_WORKFLOW = Path.of("../.github/workflows/ci.yml");
    private static final Path CI_CD_DOC = Path.of("../docs/deployment/ci-cd.md");
    private static final Path GITHUB_README = Path.of("../.github/README.md");
    private static final Path BRANCH_PROTECTION_DOC =
            Path.of("../docs/deployment/branch-protection.md");

    @Nested
    @DisplayName("CI workflow: pull_request trigger")
    class PullRequestTrigger {

        @Test
        void workflowDocumentsCiOnPullRequestForItem698() throws Exception {
            assertThat(CI_WORKFLOW).exists();
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            assertThat(yaml)
                    .contains("698")
                    .contains("pull_request:")
                    .contains("name: CI")
                    .contains("- main")
                    .contains("- dev");
        }

        @Test
        void pullRequestTargetsMainAndDevWithoutPathFilters() throws Exception {
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            int prIndex = yaml.indexOf("pull_request:");
            assertThat(prIndex).isGreaterThanOrEqualTo(0);
            int concurrencyIndex = yaml.indexOf("\nconcurrency:", prIndex);
            assertThat(concurrencyIndex).isGreaterThan(prIndex);
            String prBlock = yaml.substring(prIndex, concurrencyIndex);

            assertThat(prBlock).contains("branches:");
            assertThat(prBlock).contains("- main");
            assertThat(prBlock).contains("- dev");
            assertThat(prBlock).doesNotContain("paths:");
            assertThat(prBlock).doesNotContain("paths-ignore:");
            assertThat(prBlock).doesNotContain("types:");
        }

        @Test
        void pullRequestCiRunsFullQualityJobSet() throws Exception {
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            assertThat(yaml).contains("backend-test:");
            assertThat(yaml).contains("frontend-test:");
            assertThat(yaml).contains("frontend-lint:");
            assertThat(yaml).contains("backend-build:");
            assertThat(yaml).contains("production-config-validate:");
            // No workflow-level if: that would skip PR runs.
            assertThat(yaml.lines().noneMatch(line -> {
                        String trimmed = line.strip();
                        return trimmed.startsWith("if:")
                                && trimmed.contains("pull_request");
                    }))
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("Documentation")
    class Documentation {

        @Test
        void ciCdDocDocumentsCiOnPullRequestForItem698() throws Exception {
            assertThat(CI_CD_DOC).exists();
            String doc = Files.readString(CI_CD_DOC, StandardCharsets.UTF_8);

            assertThat(doc)
                    .contains("698")
                    .contains("pull_request")
                    .containsIgnoringCase("CI runs on pull request")
                    .contains("CiRunsOnPullRequestDocumentationTests")
                    .contains("main")
                    .contains("dev");
        }

        @Test
        void githubReadmeAndBranchProtectionReferencePrCi() throws Exception {
            assertThat(GITHUB_README).exists();
            String readme = Files.readString(GITHUB_README, StandardCharsets.UTF_8);
            assertThat(readme)
                    .contains("698")
                    .contains("CiRunsOnPullRequestDocumentationTests")
                    .containsIgnoringCase("pull request");

            assertThat(BRANCH_PROTECTION_DOC).exists();
            String protection = Files.readString(BRANCH_PROTECTION_DOC, StandardCharsets.UTF_8);
            assertThat(protection).containsIgnoringCase("pull request");
            assertThat(protection).contains("CI");
        }
    }
}
