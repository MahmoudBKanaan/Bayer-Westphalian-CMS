package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Sprint 17 item <b>696</b>: Add release tagging process.
 *
 * <p>KB epic E25 / release strategy: versions {@code v0.1}–{@code v1.0} are recorded as Git tags on
 * green {@code main}. Locks the in-repo process guide without creating tags or pushing remotes.
 */
@DisplayName("696 Add release tagging process")
class ReleaseTaggingProcessDocumentationTests {

    private static final Path RELEASE_TAGGING_DOC =
            Path.of("../docs/deployment/release-tagging.md");
    private static final Path CI_CD_DOC = Path.of("../docs/deployment/ci-cd.md");
    private static final Path BRANCH_PROTECTION_DOC =
            Path.of("../docs/deployment/branch-protection.md");
    private static final Path GITHUB_README = Path.of("../.github/README.md");
    private static final Path DOCS_INDEX = Path.of("../docs/README.md");
    private static final Path ROOT_README = Path.of("../README.md");

    @Nested
    @DisplayName("Release tagging guide")
    class Guide {

        @Test
        void releaseTaggingDocumentExistsWithItem696Scope() throws Exception {
            assertThat(RELEASE_TAGGING_DOC).exists();
            String doc = Files.readString(RELEASE_TAGGING_DOC, StandardCharsets.UTF_8);

            assertThat(doc)
                    .contains("696")
                    .containsIgnoringCase("release tagging")
                    .contains("main")
                    .contains("git tag")
                    .contains("v0.1")
                    .contains("v0.9")
                    .contains("v1.0")
                    .contains("annotated")
                    .contains("CI")
                    .contains("ReleaseTaggingProcessDocumentationTests");
        }

        @Test
        void guideCoversKbVersionsPreconditionsAndAntiPatterns() throws Exception {
            String doc = Files.readString(RELEASE_TAGGING_DOC, StandardCharsets.UTF_8);

            assertThat(doc)
                    .contains("Production candidate")
                    .contains("Production-ready MVP")
                    .contains("git push origin")
                    .contains("force")
                    .contains("secrets")
                    .contains("bwc-backend-jar")
                    .contains("branch-protection.md")
                    .contains("714");
        }

        @Test
        void guideDoesNotEmbedSecretsOrForceTagRewriteInstructionsAsPolicy() throws Exception {
            String doc = Files.readString(RELEASE_TAGGING_DOC, StandardCharsets.UTF_8);

            assertThat(doc).doesNotContain("-----BEGIN");
            assertThat(doc).doesNotContain("JWT_SECRET=");
            assertThat(doc).contains("do not move");
            assertThat(doc.toLowerCase()).contains("immutable");
        }
    }

    @Nested
    @DisplayName("Cross-links and KB alignment")
    class CrossLinks {

        @Test
        void ciCdDocsIndexAndBranchProtectionLinkReleaseTagging() throws Exception {
            assertThat(CI_CD_DOC).exists();
            String ciCd = Files.readString(CI_CD_DOC, StandardCharsets.UTF_8);
            assertThat(ciCd)
                    .contains("696")
                    .contains("release-tagging.md")
                    .contains("ReleaseTaggingProcessDocumentationTests");

            assertThat(DOCS_INDEX).exists();
            String index = Files.readString(DOCS_INDEX, StandardCharsets.UTF_8);
            assertThat(index).contains("deployment/release-tagging.md");
            assertThat(index).contains("696");

            assertThat(BRANCH_PROTECTION_DOC).exists();
            String protection = Files.readString(BRANCH_PROTECTION_DOC, StandardCharsets.UTF_8);
            assertThat(protection).contains("696");
        }

        @Test
        void githubReadmeReferencesItem696() throws Exception {
            assertThat(GITHUB_README).exists();
            String readme = Files.readString(GITHUB_README, StandardCharsets.UTF_8);

            assertThat(readme)
                    .contains("696")
                    .contains("ReleaseTaggingProcessDocumentationTests")
                    .containsIgnoringCase("release tagging");
        }

        @Test
        void rootReadmeListsKbReleaseVersionsAlignedWithTagScheme() throws Exception {
            assertThat(ROOT_README).exists();
            String readme = Files.readString(ROOT_README, StandardCharsets.UTF_8);

            assertThat(readme)
                    .contains("v0.1")
                    .contains("v0.9")
                    .contains("v1.0")
                    .contains("Production candidate")
                    .contains("Production-ready MVP");
        }
    }
}
