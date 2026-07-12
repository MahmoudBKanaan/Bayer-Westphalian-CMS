package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Sprint 17 item <b>679</b>: Add backend test job.
 *
 * <p>KB: CI/CD must run the backend unit/integration suite on pull requests and protected branches
 * (epic E25). Locks the {@code backend-test} GitHub Actions job structure without executing the
 * pipeline or the Maven suite from this class.
 */
@DisplayName("679 Add backend test job")
class BackendTestJobDocumentationTests {

    private static final Path CI_WORKFLOW = Path.of("../.github/workflows/ci.yml");
    private static final Path CI_CD_DOC = Path.of("../docs/deployment/ci-cd.md");
    private static final Path GITHUB_README = Path.of("../.github/README.md");
    private static final Path BACKEND_POM = Path.of("../backend/pom.xml");

    @Nested
    @DisplayName("CI workflow: backend-test job")
    class WorkflowJob {

        @Test
        void definesBackendTestJobWithJava21AndMavenTest() throws Exception {
            assertThat(CI_WORKFLOW).exists();
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            assertThat(yaml)
                    .contains("679")
                    .contains("backend-test:")
                    .contains("name: Backend test")
                    .contains("runs-on: ubuntu-latest")
                    .contains("working-directory: backend")
                    .contains("actions/setup-java@v4")
                    .contains("java-version: \"21\"")
                    .contains("distribution: temurin")
                    .contains("cache: maven")
                    .contains("cache-dependency-path: backend/pom.xml")
                    .contains("mvn -B test")
                    .contains("id: backend-test");
        }

        @Test
        void backendTestJobIsSeparateFromBackendBuildJob() throws Exception {
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            int buildJobIndex = yaml.indexOf("\n  backend-build:");
            if (buildJobIndex < 0) {
                buildJobIndex = yaml.indexOf("backend-build:");
            }
            int testJobIndex = yaml.indexOf("\n  backend-test:");
            if (testJobIndex < 0) {
                testJobIndex = yaml.indexOf("backend-test:");
            }
            // Full-suite job ends at integration job (680) when present; else frontend.
            int nextJobIndex = yaml.indexOf("\n  backend-integration-test:");
            if (nextJobIndex < 0) {
                nextJobIndex = yaml.indexOf("\n  frontend:");
            }

            assertThat(buildJobIndex).isGreaterThanOrEqualTo(0);
            assertThat(testJobIndex).isGreaterThan(buildJobIndex);
            assertThat(nextJobIndex).isGreaterThan(testJobIndex);

            String testJobBlock = yaml.substring(testJobIndex, nextJobIndex);
            assertThat(testJobBlock).contains("mvn -B test");
            assertThat(testJobBlock).contains("Run backend tests (Maven test)");
            // Test job must actually execute tests (not package-only).
            assertThat(testJobBlock).doesNotContain("mvn -B -DskipTests package");
            // Full suite is unfiltered; integration-only filter belongs to item 680.
            assertThat(testJobBlock).doesNotContain("*IntegrationTests");

            String buildJobBlock = yaml.substring(buildJobIndex, testJobIndex);
            assertThat(buildJobBlock).contains("mvn -B -DskipTests package");
            assertThat(buildJobBlock).doesNotContain("mvn -B test");
        }

        @Test
        void backendPomDeclaresTestStackForSurefire() throws Exception {
            assertThat(BACKEND_POM).exists();
            String pom = Files.readString(BACKEND_POM, StandardCharsets.UTF_8);

            assertThat(pom)
                    .contains("spring-boot-starter-test")
                    .contains("spring-security-test")
                    .contains("testcontainers");
        }
    }

    @Nested
    @DisplayName("Documentation")
    class Documentation {

        @Test
        void ciCdDocDocumentsBackendTestJobForItem679() throws Exception {
            assertThat(CI_CD_DOC).exists();
            String doc = Files.readString(CI_CD_DOC, StandardCharsets.UTF_8);

            assertThat(doc)
                    .contains("679")
                    .contains("backend-test")
                    .contains("Backend test")
                    .contains("mvn -B test")
                    .contains("BackendTestJobDocumentationTests")
                    .contains("Java 21");
        }

        @Test
        void githubReadmeListsBackendTestJob() throws Exception {
            assertThat(GITHUB_README).exists();
            String readme = Files.readString(GITHUB_README, StandardCharsets.UTF_8);

            assertThat(readme)
                    .contains("679")
                    .contains("backend-test")
                    .contains("BackendTestJobDocumentationTests");
        }
    }
}
