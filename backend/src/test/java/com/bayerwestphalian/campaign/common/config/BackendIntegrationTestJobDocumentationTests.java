package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Sprint 17 item <b>680</b>: Add backend integration test job if feasible.
 *
 * <p>KB epic E25: CI should exercise integration checks when practical. Feasibility is established
 * by the project's {@code *IntegrationTests} naming convention and Testcontainers PostgreSQL
 * usage. Locks the {@code backend-integration-test} job without executing the pipeline or Maven
 * suite from this class.
 */
@DisplayName("680 Add backend integration test job if feasible")
class BackendIntegrationTestJobDocumentationTests {

    private static final Path CI_WORKFLOW = Path.of("../.github/workflows/ci.yml");
    private static final Path CI_CD_DOC = Path.of("../docs/deployment/ci-cd.md");
    private static final Path GITHUB_README = Path.of("../.github/README.md");
    private static final Path BACKEND_POM = Path.of("../backend/pom.xml");
    private static final Path BACKEND_TEST_ROOT =
            Path.of("../backend/src/test/java/com/bayerwestphalian/campaign");

    @Nested
    @DisplayName("Feasibility")
    class Feasibility {

        @Test
        void integrationTestClassesExistUnderNamingConvention() throws Exception {
            assertThat(BACKEND_TEST_ROOT).exists();

            long integrationClassCount;
            try (Stream<Path> paths = Files.walk(BACKEND_TEST_ROOT)) {
                integrationClassCount =
                        paths.filter(Files::isRegularFile)
                                .map(path -> path.getFileName().toString())
                                .filter(name -> name.endsWith("IntegrationTests.java"))
                                .count();
            }

            // Project already has a non-trivial integration suite — job is feasible.
            assertThat(integrationClassCount).isGreaterThanOrEqualTo(5);
        }

        @Test
        void backendPomDeclaresTestcontainersForIntegrationRuntime() throws Exception {
            assertThat(BACKEND_POM).exists();
            String pom = Files.readString(BACKEND_POM, StandardCharsets.UTF_8);

            assertThat(pom)
                    .contains("testcontainers")
                    .contains("postgresql")
                    .contains("spring-boot-starter-test");
        }
    }

    @Nested
    @DisplayName("CI workflow: backend-integration-test job")
    class WorkflowJob {

        @Test
        void definesBackendIntegrationTestJobWithFilteredSurefire() throws Exception {
            assertThat(CI_WORKFLOW).exists();
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            assertThat(yaml)
                    .contains("680")
                    .contains("backend-integration-test:")
                    .contains("name: Backend integration test")
                    .contains("runs-on: ubuntu-latest")
                    .contains("working-directory: backend")
                    .contains("actions/setup-java@v4")
                    .contains("java-version: \"21\"")
                    .contains("distribution: temurin")
                    .contains("cache: maven")
                    .contains("cache-dependency-path: backend/pom.xml")
                    .contains("id: backend-integration-test")
                    .contains("*IntegrationTests");
        }

        @Test
        void backendIntegrationTestJobIsSeparateFromFullSuiteAndBuild() throws Exception {
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            int buildJobIndex = indexOfJob(yaml, "backend-build:");
            int testJobIndex = indexOfJob(yaml, "backend-test:");
            int integrationJobIndex = indexOfJob(yaml, "backend-integration-test:");
            // Prefer dedicated frontend-install (681) when present; else combined frontend job.
            int nextAfterIntegration = yaml.indexOf("\n  frontend-install:");
            if (nextAfterIntegration < 0) {
                nextAfterIntegration = yaml.indexOf("\n  frontend:");
            }

            assertThat(buildJobIndex).isGreaterThanOrEqualTo(0);
            assertThat(testJobIndex).isGreaterThan(buildJobIndex);
            assertThat(integrationJobIndex).isGreaterThan(testJobIndex);
            assertThat(nextAfterIntegration).isGreaterThan(integrationJobIndex);

            String integrationBlock = yaml.substring(integrationJobIndex, nextAfterIntegration);
            assertThat(integrationBlock).contains("mvn -B test -Dtest='*IntegrationTests'");
            assertThat(integrationBlock)
                    .contains("Run backend integration tests (Maven, *IntegrationTests)");
            assertThat(integrationBlock).doesNotContain("mvn -B -DskipTests package");

            String fullSuiteBlock = yaml.substring(testJobIndex, integrationJobIndex);
            assertThat(fullSuiteBlock).contains("Run backend tests (Maven test)");
            assertThat(fullSuiteBlock).contains("mvn -B test");
            assertThat(fullSuiteBlock).doesNotContain("*IntegrationTests");

            String buildBlock = yaml.substring(buildJobIndex, testJobIndex);
            assertThat(buildBlock).contains("mvn -B -DskipTests package");
            assertThat(buildBlock).doesNotContain("mvn -B test");
        }
    }

    @Nested
    @DisplayName("Documentation")
    class Documentation {

        @Test
        void ciCdDocDocumentsBackendIntegrationTestJobForItem680() throws Exception {
            assertThat(CI_CD_DOC).exists();
            String doc = Files.readString(CI_CD_DOC, StandardCharsets.UTF_8);

            assertThat(doc)
                    .contains("680")
                    .contains("backend-integration-test")
                    .contains("Backend integration test")
                    .contains("*IntegrationTests")
                    .contains("BackendIntegrationTestJobDocumentationTests")
                    .containsIgnoringCase("feasible");
        }

        @Test
        void githubReadmeListsBackendIntegrationTestJob() throws Exception {
            assertThat(GITHUB_README).exists();
            String readme = Files.readString(GITHUB_README, StandardCharsets.UTF_8);

            assertThat(readme)
                    .contains("680")
                    .contains("backend-integration-test")
                    .contains("BackendIntegrationTestJobDocumentationTests");
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
