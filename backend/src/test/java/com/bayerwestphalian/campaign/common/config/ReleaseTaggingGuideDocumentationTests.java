package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Sprint 17 item <b>711</b>: Release tagging guide.
 *
 * <p>Locks the expanded guide content for release ownership, verification, notes, evidence, and
 * troubleshooting without creating or pushing Git tags.
 */
@DisplayName("711 Release tagging guide")
class ReleaseTaggingGuideDocumentationTests {

    private static final Path RELEASE_TAGGING_DOC =
            Path.of("../docs/deployment/release-tagging.md");
    private static final Path CI_CD_DOC = Path.of("../docs/deployment/ci-cd.md");
    private static final Path DOCS_INDEX = Path.of("../docs/README.md");
    private static final Path GITHUB_README = Path.of("../.github/README.md");

    @Nested
    @DisplayName("Guide content")
    class GuideContent {

        @Test
        void releaseTaggingGuideDocumentsItem711Scope() throws Exception {
            assertThat(RELEASE_TAGGING_DOC).exists();
            String doc = Files.readString(RELEASE_TAGGING_DOC, StandardCharsets.UTF_8);

            assertThat(doc)
                    .contains("711")
                    .contains("Release tagging guide")
                    .contains("Release roles and ownership")
                    .contains("Release operator")
                    .contains("Reviewer / admin")
                    .contains("System Auditor")
                    .contains("Verification commands")
                    .contains("Release notes template")
                    .contains("Evidence capture")
                    .contains("Troubleshooting")
                    .contains("ReleaseTaggingGuideDocumentationTests");
        }

        @Test
        void guideCoversVerificationEvidenceAndSafeReleaseNotes() throws Exception {
            String doc = Files.readString(RELEASE_TAGGING_DOC, StandardCharsets.UTF_8);

            assertThat(doc)
                    .contains("git branch --show-current")
                    .contains("git show --no-patch --decorate")
                    .contains("git rev-list -n 1")
                    .contains("git ls-remote --tags origin")
                    .contains("Full SHA on `main`")
                    .contains("Green workflow **CI** run URL")
                    .contains("bwc-backend-jar")
                    .contains("bwc-frontend-dist")
                    .contains("no secrets, credentials, or environment values");
        }

        @Test
        void guideKeepsTagsHumanApprovedAndImmutable() throws Exception {
            String doc = Files.readString(RELEASE_TAGGING_DOC, StandardCharsets.UTF_8);

            assertThat(doc)
                    .contains("human release operator")
                    .contains("do not force-move")
                    .contains("corrected patch tag")
                    .contains("do not tag");

            assertThat(doc).doesNotContain("git tag -f v0.9");
            assertThat(doc).doesNotContain("git push --force");
            assertThat(doc).doesNotContain("JWT_SECRET=");
            assertThat(doc).doesNotContain("-----BEGIN");
        }
    }

    @Nested
    @DisplayName("Cross-links")
    class CrossLinks {

        @Test
        void relatedDocsReferenceReleaseTaggingGuideItem711() throws Exception {
            String ciCd = Files.readString(CI_CD_DOC, StandardCharsets.UTF_8);
            assertThat(ciCd)
                    .contains("711")
                    .contains("Release tagging guide")
                    .contains("release-tagging.md")
                    .contains("ReleaseTaggingGuideDocumentationTests");

            String docsIndex = Files.readString(DOCS_INDEX, StandardCharsets.UTF_8);
            assertThat(docsIndex)
                    .contains("deployment/release-tagging.md")
                    .contains("711");

            String githubReadme = Files.readString(GITHUB_README, StandardCharsets.UTF_8);
            assertThat(githubReadme)
                    .contains("711")
                    .contains("ReleaseTaggingGuideDocumentationTests")
                    .contains("release-tagging.md");
        }
    }
}
