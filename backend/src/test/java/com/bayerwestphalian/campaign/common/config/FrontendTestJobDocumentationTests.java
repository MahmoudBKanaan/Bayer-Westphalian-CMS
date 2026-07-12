package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Sprint 17 item <b>683</b>: Add frontend test job.
 *
 * <p>KB epic E25: CI must run the frontend unit/component suite (Vitest) on a clean agent. Locks
 * the {@code frontend-test} GitHub Actions job without executing the pipeline or {@code npm test}
 * from this class.
 */
@DisplayName("683 Add frontend test job")
class FrontendTestJobDocumentationTests {

    private static final Path CI_WORKFLOW = Path.of("../.github/workflows/ci.yml");
    private static final Path CI_CD_DOC = Path.of("../docs/deployment/ci-cd.md");
    private static final Path GITHUB_README = Path.of("../.github/README.md");
    private static final Path FRONTEND_PACKAGE_JSON = Path.of("../frontend/package.json");

    @Nested
    @DisplayName("CI workflow: frontend-test job")
    class WorkflowJob {

        @Test
        void definesFrontendTestJobWithNode22AndNpmTest() throws Exception {
            assertThat(CI_WORKFLOW).exists();
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            assertThat(yaml)
                    .contains("683")
                    .contains("frontend-test:")
                    .contains("name: Frontend test")
                    .contains("runs-on: ubuntu-latest")
                    .contains("working-directory: frontend")
                    .contains("actions/setup-node@v4")
                    .contains("node-version: \"22\"")
                    .contains("cache: npm")
                    .contains("cache-dependency-path: frontend/package-lock.json")
                    .contains("id: frontend-test")
                    .contains("Run frontend unit tests (npm test)");
        }

        @Test
        void frontendTestJobIsTestOnlySeparateFromLintAndBuild() throws Exception {
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            int testJobIndex = indexOfJob(yaml, "frontend-test:");
            // Next job is frontend-build (684) when present; else end of file slice not used.
            int nextJobIndex = yaml.indexOf("\n  frontend-build:");
            if (nextJobIndex < 0) {
                nextJobIndex = yaml.indexOf("\n  frontend:");
            }
            assertThat(testJobIndex).isGreaterThanOrEqualTo(0);
            assertThat(nextJobIndex).isGreaterThan(testJobIndex);

            String testBlock = yaml.substring(testJobIndex, nextJobIndex);
            assertThat(testBlock).contains("npm test");
            assertThat(testBlock).contains("npm ci");
            assertThat(testBlock).doesNotContain("npm run lint");
            assertThat(testBlock).doesNotContain("npm run build");
        }

        @Test
        void frontendPackageJsonDefinesTestScript() throws Exception {
            assertThat(FRONTEND_PACKAGE_JSON).exists();
            String packageJson = Files.readString(FRONTEND_PACKAGE_JSON, StandardCharsets.UTF_8);

            assertThat(packageJson).contains("\"test\"").contains("vitest");
        }
    }

    @Nested
    @DisplayName("Documentation")
    class Documentation {

        @Test
        void ciCdDocDocumentsFrontendTestJobForItem683() throws Exception {
            assertThat(CI_CD_DOC).exists();
            String doc = Files.readString(CI_CD_DOC, StandardCharsets.UTF_8);

            assertThat(doc)
                    .contains("683")
                    .contains("frontend-test")
                    .contains("Frontend test")
                    .contains("npm test")
                    .contains("FrontendTestJobDocumentationTests");
        }

        @Test
        void githubReadmeListsFrontendTestJob() throws Exception {
            assertThat(GITHUB_README).exists();
            String readme = Files.readString(GITHUB_README, StandardCharsets.UTF_8);

            assertThat(readme)
                    .contains("683")
                    .contains("frontend-test")
                    .contains("FrontendTestJobDocumentationTests");
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
