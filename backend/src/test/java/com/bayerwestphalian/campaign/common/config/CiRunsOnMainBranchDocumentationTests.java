package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Sprint 17 item <b>699</b>: CI runs on main branch.
 *
 * <p>KB epic E25: the releasable {@code main} branch must run continuous integration on push so
 * pass-on-green (**694**), the CI badge (**692**), and production gate (**714**) stay meaningful.
 * Locks the {@code push} trigger including {@code main} without executing GitHub Actions.
 */
@DisplayName("699 CI runs on main branch")
class CiRunsOnMainBranchDocumentationTests {

    private static final Path CI_WORKFLOW = Path.of("../.github/workflows/ci.yml");
    private static final Path CI_CD_DOC = Path.of("../docs/deployment/ci-cd.md");
    private static final Path GITHUB_README = Path.of("../.github/README.md");
    private static final Path ROOT_README = Path.of("../README.md");

    @Nested
    @DisplayName("CI workflow: push to main")
    class PushMainTrigger {

        @Test
        void workflowDocumentsCiOnMainBranchForItem699() throws Exception {
            assertThat(CI_WORKFLOW).exists();
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            assertThat(yaml)
                    .contains("699")
                    .contains("push:")
                    .contains("name: CI")
                    .contains("- main")
                    .contains("- dev");
        }

        @Test
        void pushTriggerIncludesMainWithoutPathFilters() throws Exception {
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            int pushIndex = yaml.indexOf("push:");
            assertThat(pushIndex).isGreaterThanOrEqualTo(0);
            int prIndex = yaml.indexOf("\npull_request:", pushIndex);
            if (prIndex < 0) {
                prIndex = yaml.indexOf("\n  pull_request:", pushIndex);
            }
            assertThat(prIndex).isGreaterThan(pushIndex);
            String pushBlock = yaml.substring(pushIndex, prIndex);

            assertThat(pushBlock).contains("branches:");
            assertThat(pushBlock).contains("- main");
            assertThat(pushBlock).contains("- dev");
            assertThat(pushBlock).contains("699");
            assertThat(pushBlock).doesNotContain("paths:");
            assertThat(pushBlock).doesNotContain("paths-ignore:");
            assertThat(pushBlock).doesNotContain("tags:");
        }

        @Test
        void mainPushRunsFullQualityJobSetAndBadgeTracksMain() throws Exception {
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            assertThat(yaml).contains("backend-test:");
            assertThat(yaml).contains("frontend-test:");
            assertThat(yaml).contains("frontend-lint:");
            assertThat(yaml).contains("backend-build:");
            assertThat(yaml).contains("production-config-validate:");

            assertThat(ROOT_README).exists();
            String readme = Files.readString(ROOT_README, StandardCharsets.UTF_8);
            assertThat(readme).contains("badge.svg?branch=main");
        }
    }

    @Nested
    @DisplayName("Documentation")
    class Documentation {

        @Test
        void ciCdDocDocumentsCiOnMainBranchForItem699() throws Exception {
            assertThat(CI_CD_DOC).exists();
            String doc = Files.readString(CI_CD_DOC, StandardCharsets.UTF_8);

            assertThat(doc)
                    .contains("699")
                    .contains("push")
                    .containsIgnoringCase("CI runs on main")
                    .contains("CiRunsOnMainBranchDocumentationTests")
                    .contains("main");
        }

        @Test
        void githubReadmeReferencesItem699() throws Exception {
            assertThat(GITHUB_README).exists();
            String readme = Files.readString(GITHUB_README, StandardCharsets.UTF_8);

            assertThat(readme)
                    .contains("699")
                    .contains("CiRunsOnMainBranchDocumentationTests")
                    .containsIgnoringCase("main");
        }
    }
}
