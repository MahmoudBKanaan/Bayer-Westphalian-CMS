package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Sprint 17 item <b>695</b>: Add branch protection recommendation.
 *
 * <p>KB epic E25: releasable {@code main} should not accept unreviewed red merges. Locks the
 * in-repo branch protection recommendation document and cross-links without applying GitHub
 * settings via API.
 */
@DisplayName("695 Add branch protection recommendation")
class BranchProtectionRecommendationDocumentationTests {

    private static final Path BRANCH_PROTECTION_DOC =
            Path.of("../docs/deployment/branch-protection.md");
    private static final Path CI_CD_DOC = Path.of("../docs/deployment/ci-cd.md");
    private static final Path GITHUB_README = Path.of("../.github/README.md");
    private static final Path DOCS_INDEX = Path.of("../docs/README.md");
    private static final Path CI_WORKFLOW = Path.of("../.github/workflows/ci.yml");

    @Nested
    @DisplayName("Branch protection guide")
    class Guide {

        @Test
        void branchProtectionDocumentExistsWithItem695Scope() throws Exception {
            assertThat(BRANCH_PROTECTION_DOC).exists();
            String doc = Files.readString(BRANCH_PROTECTION_DOC, StandardCharsets.UTF_8);

            assertThat(doc)
                    .contains("695")
                    .containsIgnoringCase("branch protection")
                    .contains("main")
                    .contains("Require status checks")
                    .contains("Allow force pushes")
                    .contains("Backend test")
                    .contains("Frontend test")
                    .contains("Production config validation")
                    .contains("solo")
                    .contains("BranchProtectionRecommendationDocumentationTests");
        }

        @Test
        void guideListsRequiredCiCheckNamesAndForbidsForcePushOnMain() throws Exception {
            String doc = Files.readString(BRANCH_PROTECTION_DOC, StandardCharsets.UTF_8);

            assertThat(doc)
                    .contains("Backend build")
                    .contains("Backend integration test")
                    .contains("Frontend lint")
                    .contains("Frontend build")
                    .contains("Docker backend image")
                    .contains("Docker frontend image")
                    .contains("Docker Compose validation")
                    .contains("Off")
                    .contains("pull request")
                    .contains("ci.yml");
        }

        @Test
        void guideIsRecommendationNotRuntimeEnforcementCode() throws Exception {
            String doc = Files.readString(BRANCH_PROTECTION_DOC, StandardCharsets.UTF_8);

            assertThat(doc).containsIgnoringCase("recommendation");
            assertThat(doc).containsIgnoringCase("Settings");
            assertThat(doc).containsIgnoringCase("not");
            assertThat(doc).doesNotContain("gh api");
            assertThat(doc).doesNotContain("GITHUB_TOKEN:");
        }
    }

    @Nested
    @DisplayName("Cross-links and CI alignment")
    class CrossLinks {

        @Test
        void ciCdAndDocsIndexLinkBranchProtectionGuide() throws Exception {
            assertThat(CI_CD_DOC).exists();
            String ciCd = Files.readString(CI_CD_DOC, StandardCharsets.UTF_8);
            assertThat(ciCd)
                    .contains("695")
                    .contains("branch-protection.md")
                    .contains("BranchProtectionRecommendationDocumentationTests");

            assertThat(DOCS_INDEX).exists();
            String index = Files.readString(DOCS_INDEX, StandardCharsets.UTF_8);
            assertThat(index).contains("deployment/branch-protection.md");
            assertThat(index).contains("695");
        }

        @Test
        void githubReadmeReferencesItem695() throws Exception {
            assertThat(GITHUB_README).exists();
            String readme = Files.readString(GITHUB_README, StandardCharsets.UTF_8);

            assertThat(readme)
                    .contains("695")
                    .contains("BranchProtectionRecommendationDocumentationTests")
                    .containsIgnoringCase("branch protection");
        }

        @Test
        void workflowStillTriggersOnMainForProtectedReleasableLine() throws Exception {
            assertThat(CI_WORKFLOW).exists();
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            assertThat(yaml).contains("name: CI");
            assertThat(yaml).contains("- main");
            assertThat(yaml).contains("pull_request:");
            assertThat(yaml).contains("backend-test:");
            assertThat(yaml).contains("frontend-test:");
        }
    }
}
