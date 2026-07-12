package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Sprint 17 item <b>691</b>: Add release artifact generation.
 *
 * <p>KB epic E25 / Sprint 17: packaging and release preparation. Locks GitHub Actions
 * {@code actions/upload-artifact} steps that publish the backend JAR and frontend production
 * {@code dist/} for download from CI runs (no registry push, no secrets in artifacts).
 */
@DisplayName("691 Add release artifact generation")
class ReleaseArtifactGenerationDocumentationTests {

    private static final Path CI_WORKFLOW = Path.of("../.github/workflows/ci.yml");
    private static final Path CI_CD_DOC = Path.of("../docs/deployment/ci-cd.md");
    private static final Path GITHUB_README = Path.of("../.github/README.md");

    @Nested
    @DisplayName("CI workflow: upload-artifact steps")
    class WorkflowArtifacts {

        @Test
        void definesUploadArtifactForBackendJarAndFrontendDist() throws Exception {
            assertThat(CI_WORKFLOW).exists();
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            assertThat(yaml)
                    .contains("691")
                    .contains("actions/upload-artifact@v4")
                    .contains("Upload backend release JAR")
                    .contains("name: bwc-backend-jar")
                    .contains("backend/target/*.jar")
                    .contains("!backend/target/*.jar.original")
                    .contains("Upload frontend release dist")
                    .contains("name: bwc-frontend-dist")
                    .contains("path: frontend/dist")
                    .contains("if-no-files-found: error")
                    .contains("retention-days: 14");
        }

        @Test
        void backendBuildJobUploadsJarWithoutPublishingSecretsOrImages() throws Exception {
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            int buildJobIndex = indexOfJob(yaml, "backend-build:");
            int nextJobIndex = yaml.indexOf("\n  backend-test:");
            assertThat(buildJobIndex).isNonNegative();
            assertThat(nextJobIndex).isGreaterThan(buildJobIndex);
            String buildJobBlock = yaml.substring(buildJobIndex, nextJobIndex);

            assertThat(buildJobBlock).contains("actions/upload-artifact@v4");
            assertThat(buildJobBlock).contains("bwc-backend-jar");
            assertThat(buildJobBlock).contains("mvn -B -DskipTests package");
            assertThat(buildJobBlock).doesNotContain("mvn -B test");
            assertThat(buildJobBlock).doesNotContain("docker push");
            assertThat(buildJobBlock).doesNotContain("JWT_SECRET:");
            assertThat(buildJobBlock).doesNotContain("DB_PASSWORD:");
        }

        @Test
        void frontendBuildJobUploadsDistWithoutPublishingSecretsOrImages() throws Exception {
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            int buildJobIndex = indexOfJob(yaml, "frontend-build:");
            int nextJobIndex = yaml.indexOf("\n  docker-backend:");
            assertThat(buildJobIndex).isNonNegative();
            assertThat(nextJobIndex).isGreaterThan(buildJobIndex);
            String buildJobBlock = yaml.substring(buildJobIndex, nextJobIndex);

            assertThat(buildJobBlock).contains("actions/upload-artifact@v4");
            assertThat(buildJobBlock).contains("bwc-frontend-dist");
            assertThat(buildJobBlock).contains("npm run build");
            assertThat(buildJobBlock).contains("path: frontend/dist");
            assertThat(buildJobBlock).doesNotContain("docker push");
            assertThat(buildJobBlock).doesNotContain("JWT_SECRET:");
            assertThat(buildJobBlock).doesNotContain("DB_PASSWORD:");
        }
    }

    @Nested
    @DisplayName("Documentation")
    class Documentation {

        @Test
        void ciCdDocDocumentsReleaseArtifactGenerationForItem691() throws Exception {
            assertThat(CI_CD_DOC).exists();
            String doc = Files.readString(CI_CD_DOC, StandardCharsets.UTF_8);

            assertThat(doc)
                    .contains("691")
                    .containsIgnoringCase("release artifact")
                    .contains("bwc-backend-jar")
                    .contains("bwc-frontend-dist")
                    .contains("upload-artifact")
                    .contains("ReleaseArtifactGenerationDocumentationTests");
        }

        @Test
        void githubReadmeReferencesItem691AndArtifactNames() throws Exception {
            assertThat(GITHUB_README).exists();
            String readme = Files.readString(GITHUB_README, StandardCharsets.UTF_8);

            assertThat(readme)
                    .contains("691")
                    .contains("bwc-backend-jar")
                    .contains("bwc-frontend-dist")
                    .contains("ReleaseArtifactGenerationDocumentationTests");
        }
    }

    private static int indexOfJob(String yaml, String jobKey) {
        int withNewline = yaml.indexOf("\n  " + jobKey);
        if (withNewline >= 0) {
            return withNewline;
        }
        return yaml.indexOf(jobKey);
    }
}
