package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Sprint 17 item <b>687</b>: Add Docker Compose validation.
 *
 * <p>KB epic E25 / DevOps: CI must validate the Compose model (services, networks, volumes) without
 * requiring a full stack start. Locks {@code docker-compose.yml} and the {@code
 * docker-compose-validate} GitHub Actions job without executing Docker from this class.
 */
@DisplayName("687 Add Docker Compose validation")
class DockerComposeValidationDocumentationTests {

    private static final Path CI_WORKFLOW = Path.of("../.github/workflows/ci.yml");
    private static final Path CI_CD_DOC = Path.of("../docs/deployment/ci-cd.md");
    private static final Path GITHUB_README = Path.of("../.github/README.md");
    private static final Path DOCKER_README = Path.of("../docker/README.md");
    private static final Path COMPOSE_FILE = Path.of("../docker-compose.yml");
    private static final Path COMPOSE_CONFIG_SCRIPT =
            Path.of("../scripts/test-docker-compose-config.ps1");

    @Nested
    @DisplayName("Compose file")
    class ComposeFile {

        @Test
        void dockerComposeDefinesPostgresNetworkVolumeAndHealthcheck() throws Exception {
            assertThat(COMPOSE_FILE).exists();
            String compose = Files.readString(COMPOSE_FILE, StandardCharsets.UTF_8);

            assertThat(compose)
                    .contains("services:")
                    .contains("postgres:")
                    .contains("postgres:16-alpine")
                    .contains("bwc_postgres_data")
                    .contains("bwc_local")
                    .contains("healthcheck:")
                    .contains("pg_isready");
        }

        @Test
        void localPowerShellComposeConfigScriptExists() throws Exception {
            assertThat(COMPOSE_CONFIG_SCRIPT).exists();
            String script = Files.readString(COMPOSE_CONFIG_SCRIPT, StandardCharsets.UTF_8);

            assertThat(script)
                    .contains("docker compose")
                    .contains("config")
                    .contains("postgres")
                    .contains("bwc_local")
                    .contains("bwc_postgres_data");
        }
    }

    @Nested
    @DisplayName("CI workflow: docker-compose-validate job")
    class WorkflowJob {

        @Test
        void definesDockerComposeValidateJob() throws Exception {
            assertThat(CI_WORKFLOW).exists();
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            assertThat(yaml)
                    .contains("687")
                    .contains("docker-compose-validate:")
                    .contains("name: Docker Compose validation")
                    .contains("runs-on: ubuntu-latest")
                    .contains("id: docker-compose-validate")
                    .contains("Validate Docker Compose configuration")
                    .contains("docker compose -f \"$compose_file\" config")
                    .contains("config --format json")
                    .contains("postgres:16-alpine")
                    .contains("bwc_local")
                    .contains("bwc_postgres_data");
        }

        @Test
        void composeValidationDoesNotStartContainersOrPushImages() throws Exception {
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            int jobIndex = indexOfJob(yaml, "docker-compose-validate:");
            assertThat(jobIndex).isGreaterThanOrEqualTo(0);
            int nextJobIndex = yaml.indexOf("\n  production-config-validate:");
            String jobBlock =
                    nextJobIndex > jobIndex
                            ? yaml.substring(jobIndex, nextJobIndex)
                            : yaml.substring(jobIndex);

            assertThat(jobBlock).contains("docker compose");
            assertThat(jobBlock).contains("config");
            assertThat(jobBlock).doesNotContain("docker compose up");
            assertThat(jobBlock).doesNotContain("docker push");
            assertThat(jobBlock).doesNotContain("JWT_SECRET:");
        }
    }

    @Nested
    @DisplayName("Documentation")
    class Documentation {

        @Test
        void ciCdDocDocumentsComposeValidationForItem687() throws Exception {
            assertThat(CI_CD_DOC).exists();
            String doc = Files.readString(CI_CD_DOC, StandardCharsets.UTF_8);

            assertThat(doc)
                    .contains("687")
                    .contains("docker-compose-validate")
                    .contains("Docker Compose validation")
                    .contains("docker-compose.yml")
                    .contains("DockerComposeValidationDocumentationTests");
        }

        @Test
        void githubAndDockerReadmeReferenceComposeValidation() throws Exception {
            assertThat(GITHUB_README).exists();
            String githubReadme = Files.readString(GITHUB_README, StandardCharsets.UTF_8);
            assertThat(githubReadme)
                    .contains("687")
                    .contains("docker-compose-validate")
                    .contains("DockerComposeValidationDocumentationTests");

            assertThat(DOCKER_README).exists();
            String dockerReadme = Files.readString(DOCKER_README, StandardCharsets.UTF_8);
            assertThat(dockerReadme).contains("687").containsIgnoringCase("compose");
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
