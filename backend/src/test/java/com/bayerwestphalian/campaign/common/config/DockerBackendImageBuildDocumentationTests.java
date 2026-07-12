package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Sprint 17 item <b>685</b>: Add Docker backend image build.
 *
 * <p>KB epic E25 / DevOps: Docker packaging for deployment preparation. Locks the backend {@code
 * Dockerfile} and the {@code docker-backend} GitHub Actions job without executing Docker or the
 * pipeline from this class.
 */
@DisplayName("685 Add Docker backend image build")
class DockerBackendImageBuildDocumentationTests {

    private static final Path CI_WORKFLOW = Path.of("../.github/workflows/ci.yml");
    private static final Path CI_CD_DOC = Path.of("../docs/deployment/ci-cd.md");
    private static final Path GITHUB_README = Path.of("../.github/README.md");
    private static final Path DOCKER_README = Path.of("../docker/README.md");
    private static final Path BACKEND_DOCKERFILE = Path.of("../backend/Dockerfile");
    private static final Path BACKEND_DOCKERIGNORE = Path.of("../backend/.dockerignore");
    private static final Path BACKEND_POM = Path.of("../backend/pom.xml");

    @Nested
    @DisplayName("Dockerfile")
    class Dockerfile {

        @Test
        void backendDockerfileExistsAsMultiStageJava21Image() throws Exception {
            assertThat(BACKEND_DOCKERFILE).exists();
            String dockerfile = Files.readString(BACKEND_DOCKERFILE, StandardCharsets.UTF_8);

            assertThat(dockerfile)
                    .contains("685")
                    .contains("FROM maven:")
                    .contains("eclipse-temurin")
                    .contains("21")
                    .contains("mvn -B -DskipTests package")
                    .contains("bayer-westphalian-campaign-platform.jar")
                    .contains("EXPOSE 8080")
                    .contains("ENTRYPOINT");
        }

        @Test
        void backendDockerfileDoesNotEmbedProductionSecrets() throws Exception {
            String dockerfile = Files.readString(BACKEND_DOCKERFILE, StandardCharsets.UTF_8);

            assertThat(dockerfile)
                    .doesNotContain("JWT_SECRET=")
                    .doesNotContain("DB_PASSWORD=")
                    .doesNotContain("-----BEGIN");
        }

        @Test
        void backendDockerignoreAndPomSupportImageBuild() throws Exception {
            assertThat(BACKEND_DOCKERIGNORE).exists();
            String dockerignore = Files.readString(BACKEND_DOCKERIGNORE, StandardCharsets.UTF_8);
            assertThat(dockerignore).contains("target/");

            assertThat(BACKEND_POM).exists();
            String pom = Files.readString(BACKEND_POM, StandardCharsets.UTF_8);
            assertThat(pom)
                    .contains("<finalName>bayer-westphalian-campaign-platform</finalName>")
                    .contains("<java.version>21</java.version>");
        }
    }

    @Nested
    @DisplayName("CI workflow: docker-backend job")
    class WorkflowJob {

        @Test
        void definesDockerBackendJobThatBuildsBackendDockerfile() throws Exception {
            assertThat(CI_WORKFLOW).exists();
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            assertThat(yaml)
                    .contains("685")
                    .contains("docker-backend:")
                    .contains("name: Docker backend image")
                    .contains("runs-on: ubuntu-latest")
                    .contains("id: docker-backend")
                    .contains("Build backend Docker image")
                    .contains("docker build -t bwc-backend:ci -f backend/Dockerfile backend")
                    .contains("Assert backend Docker image exists")
                    .contains("docker image inspect bwc-backend:ci");
        }

        @Test
        void dockerBackendJobDoesNotPushOrEmbedSecrets() throws Exception {
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            int jobIndex = indexOfJob(yaml, "docker-backend:");
            assertThat(jobIndex).isGreaterThanOrEqualTo(0);
            int nextJobIndex = yaml.indexOf("\n  docker-frontend:");
            String jobBlock =
                    nextJobIndex > jobIndex
                            ? yaml.substring(jobIndex, nextJobIndex)
                            : yaml.substring(jobIndex);

            assertThat(jobBlock).contains("docker build");
            assertThat(jobBlock).doesNotContain("docker push");
            assertThat(jobBlock).doesNotContain("JWT_SECRET:");
            assertThat(jobBlock).doesNotContain("DB_PASSWORD:");
        }
    }

    @Nested
    @DisplayName("Documentation")
    class Documentation {

        @Test
        void ciCdDocDocumentsDockerBackendImageForItem685() throws Exception {
            assertThat(CI_CD_DOC).exists();
            String doc = Files.readString(CI_CD_DOC, StandardCharsets.UTF_8);

            assertThat(doc)
                    .contains("685")
                    .contains("docker-backend")
                    .contains("Docker backend")
                    .contains("backend/Dockerfile")
                    .contains("DockerBackendImageBuildDocumentationTests")
                    .contains("bwc-backend:ci");
        }

        @Test
        void githubAndDockerReadmeReferenceBackendImage() throws Exception {
            assertThat(GITHUB_README).exists();
            String githubReadme = Files.readString(GITHUB_README, StandardCharsets.UTF_8);
            assertThat(githubReadme)
                    .contains("685")
                    .contains("docker-backend")
                    .contains("DockerBackendImageBuildDocumentationTests");

            assertThat(DOCKER_README).exists();
            String dockerReadme = Files.readString(DOCKER_README, StandardCharsets.UTF_8);
            assertThat(dockerReadme).contains("backend/Dockerfile").contains("685");
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
