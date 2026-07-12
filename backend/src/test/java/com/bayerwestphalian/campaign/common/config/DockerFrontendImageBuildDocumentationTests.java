package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Sprint 17 item <b>686</b>: Add Docker frontend image build.
 *
 * <p>KB epic E25 / DevOps: Docker packaging for deployment preparation. Locks the frontend {@code
 * Dockerfile} and the {@code docker-frontend} GitHub Actions job without executing Docker or the
 * pipeline from this class.
 */
@DisplayName("686 Add Docker frontend image build")
class DockerFrontendImageBuildDocumentationTests {

    private static final Path CI_WORKFLOW = Path.of("../.github/workflows/ci.yml");
    private static final Path CI_CD_DOC = Path.of("../docs/deployment/ci-cd.md");
    private static final Path GITHUB_README = Path.of("../.github/README.md");
    private static final Path DOCKER_README = Path.of("../docker/README.md");
    private static final Path FRONTEND_DOCKERFILE = Path.of("../frontend/Dockerfile");
    private static final Path FRONTEND_DOCKERIGNORE = Path.of("../frontend/.dockerignore");
    private static final Path FRONTEND_NGINX_CONF = Path.of("../frontend/nginx.docker.conf");
    private static final Path FRONTEND_PACKAGE_LOCK = Path.of("../frontend/package-lock.json");

    @Nested
    @DisplayName("Dockerfile")
    class Dockerfile {

        @Test
        void frontendDockerfileExistsAsMultiStageNodeNginxImage() throws Exception {
            assertThat(FRONTEND_DOCKERFILE).exists();
            String dockerfile = Files.readString(FRONTEND_DOCKERFILE, StandardCharsets.UTF_8);

            assertThat(dockerfile)
                    .contains("686")
                    .contains("FROM node:22")
                    .contains("npm ci")
                    .contains("npm run build")
                    .contains("FROM nginx:")
                    .contains("nginx.docker.conf")
                    .contains("/usr/share/nginx/html")
                    .contains("EXPOSE 80");
        }

        @Test
        void frontendDockerfileDoesNotEmbedProductionSecrets() throws Exception {
            String dockerfile = Files.readString(FRONTEND_DOCKERFILE, StandardCharsets.UTF_8);

            assertThat(dockerfile)
                    .doesNotContain("JWT_SECRET=")
                    .doesNotContain("VITE_API_KEY=")
                    .doesNotContain("-----BEGIN");
        }

        @Test
        void frontendDockerSupportFilesExist() throws Exception {
            assertThat(FRONTEND_DOCKERIGNORE).exists();
            String dockerignore = Files.readString(FRONTEND_DOCKERIGNORE, StandardCharsets.UTF_8);
            assertThat(dockerignore).contains("node_modules/").contains("dist/");

            assertThat(FRONTEND_NGINX_CONF).exists();
            String nginx = Files.readString(FRONTEND_NGINX_CONF, StandardCharsets.UTF_8);
            assertThat(nginx).contains("try_files").contains("index.html");

            assertThat(FRONTEND_PACKAGE_LOCK).exists();
        }
    }

    @Nested
    @DisplayName("CI workflow: docker-frontend job")
    class WorkflowJob {

        @Test
        void definesDockerFrontendJobThatBuildsFrontendDockerfile() throws Exception {
            assertThat(CI_WORKFLOW).exists();
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            assertThat(yaml)
                    .contains("686")
                    .contains("docker-frontend:")
                    .contains("name: Docker frontend image")
                    .contains("runs-on: ubuntu-latest")
                    .contains("id: docker-frontend")
                    .contains("Build frontend Docker image")
                    .contains("docker build -t bwc-frontend:ci -f frontend/Dockerfile frontend")
                    .contains("Assert frontend Docker image exists")
                    .contains("docker image inspect bwc-frontend:ci");
        }

        @Test
        void dockerFrontendJobIsSeparateFromBackendImageAndDoesNotPush() throws Exception {
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            int backendJob = indexOfJob(yaml, "docker-backend:");
            int frontendJob = indexOfJob(yaml, "docker-frontend:");
            assertThat(backendJob).isNonNegative();
            assertThat(frontendJob).isGreaterThan(backendJob);

            int nextJobIndex = yaml.indexOf("\n  docker-compose-validate:");
            String frontendBlock =
                    nextJobIndex > frontendJob
                            ? yaml.substring(frontendJob, nextJobIndex)
                            : yaml.substring(frontendJob);
            assertThat(frontendBlock).contains("docker build -t bwc-frontend:ci");
            assertThat(frontendBlock).doesNotContain("docker push");
            assertThat(frontendBlock).doesNotContain("JWT_SECRET:");
        }
    }

    @Nested
    @DisplayName("Documentation")
    class Documentation {

        @Test
        void ciCdDocDocumentsDockerFrontendImageForItem686() throws Exception {
            assertThat(CI_CD_DOC).exists();
            String doc = Files.readString(CI_CD_DOC, StandardCharsets.UTF_8);

            assertThat(doc)
                    .contains("686")
                    .contains("docker-frontend")
                    .contains("Docker frontend")
                    .contains("frontend/Dockerfile")
                    .contains("DockerFrontendImageBuildDocumentationTests")
                    .contains("bwc-frontend:ci");
        }

        @Test
        void githubAndDockerReadmeReferenceFrontendImage() throws Exception {
            assertThat(GITHUB_README).exists();
            String githubReadme = Files.readString(GITHUB_README, StandardCharsets.UTF_8);
            assertThat(githubReadme)
                    .contains("686")
                    .contains("docker-frontend")
                    .contains("DockerFrontendImageBuildDocumentationTests");

            assertThat(DOCKER_README).exists();
            String dockerReadme = Files.readString(DOCKER_README, StandardCharsets.UTF_8);
            assertThat(dockerReadme).contains("frontend/Dockerfile").contains("686");
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
