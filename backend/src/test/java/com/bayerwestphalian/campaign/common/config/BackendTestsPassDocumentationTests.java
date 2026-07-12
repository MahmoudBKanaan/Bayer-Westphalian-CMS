package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Sprint 17 item <b>701</b>: Backend tests pass.
 *
 * <p>KB epic E25 acceptance: the {@code backend-test} job is the signal that the full Maven
 * Surefire suite succeeds. Locks real {@code mvn test} without soft-fail and without package-only
 * skip. Does not execute Maven or GitHub Actions.
 */
@DisplayName("701 Backend tests pass")
class BackendTestsPassDocumentationTests {

    private static final Path CI_WORKFLOW = Path.of("../.github/workflows/ci.yml");
    private static final Path CI_CD_DOC = Path.of("../docs/deployment/ci-cd.md");
    private static final Path GITHUB_README = Path.of("../.github/README.md");
    private static final Path BACKEND_POM = Path.of("../backend/pom.xml");

    @Nested
    @DisplayName("CI: backend-test pass criteria")
    class BackendTestPassCriteria {

        @Test
        void workflowDocumentsBackendTestsPassForItem701() throws Exception {
            assertThat(CI_WORKFLOW).exists();
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            assertThat(yaml)
                    .contains("701")
                    .contains("backend-test:")
                    .contains("name: Backend test")
                    .contains("mvn -B test")
                    .contains("id: backend-test")
                    .contains("Run backend tests (Maven test)");
        }

        @Test
        void backendTestJobPassesOnlyWhenMavenTestSucceeds() throws Exception {
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            int testJobIndex = indexOfJob(yaml, "backend-test:");
            int nextJobIndex = yaml.indexOf("\n  backend-integration-test:");
            assertThat(testJobIndex).isGreaterThanOrEqualTo(0);
            assertThat(nextJobIndex).isGreaterThan(testJobIndex);
            String block = yaml.substring(testJobIndex, nextJobIndex);

            assertThat(block).contains("mvn -B test");
            assertThat(block).contains("id: backend-test");
            assertThat(block).contains("701");
            assertThat(block).contains("693");
            assertThat(block).doesNotContain("continue-on-error: true");
            assertThat(block).doesNotContain("continue-on-error:true");
            assertThat(block).doesNotContain("mvn -B test || true");
            assertThat(block).doesNotContain("mvn -B -DskipTests package");
            assertThat(block).doesNotContain("-DskipTests");
            assertThat(block).doesNotContain("*IntegrationTests");
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
        void ciCdDocDocumentsBackendTestsPassForItem701() throws Exception {
            assertThat(CI_CD_DOC).exists();
            String doc = Files.readString(CI_CD_DOC, StandardCharsets.UTF_8);

            assertThat(doc)
                    .contains("701")
                    .containsIgnoringCase("Backend tests pass")
                    .contains("backend-test")
                    .contains("mvn -B test")
                    .contains("BackendTestsPassDocumentationTests");
        }

        @Test
        void githubReadmeReferencesItem701() throws Exception {
            assertThat(GITHUB_README).exists();
            String readme = Files.readString(GITHUB_README, StandardCharsets.UTF_8);

            assertThat(readme)
                    .contains("701")
                    .contains("BackendTestsPassDocumentationTests")
                    .contains("backend-test");
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
