package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Sprint 17 item <b>692</b>: Add CI badge to README.
 *
 * <p>KB epic E25 / Sprint 17: pipeline status should be visible on the project README. Locks the
 * GitHub Actions status badge markdown for workflow {@code ci.yml} without executing the pipeline.
 */
@DisplayName("692 Add CI badge to README")
class CiBadgeDocumentationTests {

    private static final Path ROOT_README = Path.of("../README.md");
    private static final Path CI_WORKFLOW = Path.of("../.github/workflows/ci.yml");
    private static final Path CI_CD_DOC = Path.of("../docs/deployment/ci-cd.md");
    private static final Path GITHUB_README = Path.of("../.github/README.md");

    private static final String BADGE_IMAGE =
            "https://github.com/MahmoudBKanaan/Bayer-Westphalian-CMS/actions/workflows/ci.yml/badge.svg?branch=main";
    private static final String BADGE_LINK =
            "https://github.com/MahmoudBKanaan/Bayer-Westphalian-CMS/actions/workflows/ci.yml";

    @Nested
    @DisplayName("Root README badge")
    class RootReadme {

        @Test
        void rootReadmeContainsCiStatusBadgeForWorkflow() throws Exception {
            assertThat(ROOT_README).exists();
            String readme = Files.readString(ROOT_README, StandardCharsets.UTF_8);

            assertThat(readme)
                    .contains("692")
                    .contains("[![CI](")
                    .contains(BADGE_IMAGE)
                    .contains(BADGE_LINK)
                    .contains("actions/workflows/ci.yml")
                    .contains("badge.svg")
                    .contains("branch=main");
        }

        @Test
        void badgeAppearsNearTopOfReadmeBeforeProjectIdentity() throws Exception {
            String readme = Files.readString(ROOT_README, StandardCharsets.UTF_8);

            int titleIndex = readme.indexOf("# Bayer-Westphalian Campaign Management Platform");
            int badgeIndex = readme.indexOf("[![CI](");
            int identityIndex = readme.indexOf("## Project Identity");

            assertThat(titleIndex).isGreaterThanOrEqualTo(0);
            assertThat(badgeIndex).isGreaterThan(titleIndex);
            assertThat(identityIndex).isGreaterThan(badgeIndex);
        }
    }

    @Nested
    @DisplayName("Workflow and documentation consistency")
    class Consistency {

        @Test
        void workflowFileAndNameMatchBadgeTarget() throws Exception {
            assertThat(CI_WORKFLOW).exists();
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            assertThat(yaml).contains("name: CI");
            assertThat(CI_WORKFLOW.getFileName().toString()).isEqualTo("ci.yml");
        }

        @Test
        void ciCdDocDocumentsCiBadgeForItem692() throws Exception {
            assertThat(CI_CD_DOC).exists();
            String doc = Files.readString(CI_CD_DOC, StandardCharsets.UTF_8);

            assertThat(doc)
                    .contains("692")
                    .containsIgnoringCase("CI badge")
                    .contains("badge.svg")
                    .contains("README.md")
                    .contains("CiBadgeDocumentationTests");
        }

        @Test
        void githubReadmeReferencesItem692() throws Exception {
            assertThat(GITHUB_README).exists();
            String readme = Files.readString(GITHUB_README, StandardCharsets.UTF_8);

            assertThat(readme)
                    .contains("692")
                    .contains("CiBadgeDocumentationTests")
                    .containsIgnoringCase("CI badge");
        }
    }
}
