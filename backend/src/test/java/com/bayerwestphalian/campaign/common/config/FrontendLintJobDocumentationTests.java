package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Sprint 17 item <b>682</b>: Add frontend lint job.
 *
 * <p>KB epic E25: CI must run frontend static analysis (ESLint) on a clean agent. Locks the {@code
 * frontend-lint} GitHub Actions job without executing the pipeline or {@code npm run lint} from this
 * class.
 */
@DisplayName("682 Add frontend lint job")
class FrontendLintJobDocumentationTests {

    private static final Path CI_WORKFLOW = Path.of("../.github/workflows/ci.yml");
    private static final Path CI_CD_DOC = Path.of("../docs/deployment/ci-cd.md");
    private static final Path GITHUB_README = Path.of("../.github/README.md");
    private static final Path FRONTEND_PACKAGE_JSON = Path.of("../frontend/package.json");

    @Nested
    @DisplayName("CI workflow: frontend-lint job")
    class WorkflowJob {

        @Test
        void definesFrontendLintJobWithNode22AndNpmRunLint() throws Exception {
            assertThat(CI_WORKFLOW).exists();
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            assertThat(yaml)
                    .contains("682")
                    .contains("frontend-lint:")
                    .contains("name: Frontend lint")
                    .contains("runs-on: ubuntu-latest")
                    .contains("working-directory: frontend")
                    .contains("actions/setup-node@v4")
                    .contains("node-version: \"22\"")
                    .contains("cache: npm")
                    .contains("cache-dependency-path: frontend/package-lock.json")
                    .contains("id: frontend-lint")
                    .contains("Lint frontend (npm run lint)")
                    .contains("npm run lint");
        }

        @Test
        void frontendLintJobIsLintOnlySeparateFromTestAndBuild() throws Exception {
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            int lintJobIndex = indexOfJob(yaml, "frontend-lint:");
            // Next job is frontend-test (683) when present; else combined frontend job.
            int nextJobIndex = yaml.indexOf("\n  frontend-test:");
            if (nextJobIndex < 0) {
                nextJobIndex = yaml.indexOf("\n  frontend:");
            }
            assertThat(lintJobIndex).isGreaterThanOrEqualTo(0);
            assertThat(nextJobIndex).isGreaterThan(lintJobIndex);

            String lintBlock = yaml.substring(lintJobIndex, nextJobIndex);
            assertThat(lintBlock).contains("npm run lint");
            assertThat(lintBlock).contains("npm ci");
            assertThat(lintBlock).doesNotContain("npm test");
            assertThat(lintBlock).doesNotContain("npm run build");
        }

        @Test
        void frontendPackageJsonDefinesLintScript() throws Exception {
            assertThat(FRONTEND_PACKAGE_JSON).exists();
            String packageJson = Files.readString(FRONTEND_PACKAGE_JSON, StandardCharsets.UTF_8);

            assertThat(packageJson).contains("\"lint\"").contains("eslint");
        }
    }

    @Nested
    @DisplayName("Documentation")
    class Documentation {

        @Test
        void ciCdDocDocumentsFrontendLintJobForItem682() throws Exception {
            assertThat(CI_CD_DOC).exists();
            String doc = Files.readString(CI_CD_DOC, StandardCharsets.UTF_8);

            assertThat(doc)
                    .contains("682")
                    .contains("frontend-lint")
                    .contains("Frontend lint")
                    .contains("npm run lint")
                    .contains("FrontendLintJobDocumentationTests");
        }

        @Test
        void githubReadmeListsFrontendLintJob() throws Exception {
            assertThat(GITHUB_README).exists();
            String readme = Files.readString(GITHUB_README, StandardCharsets.UTF_8);

            assertThat(readme)
                    .contains("682")
                    .contains("frontend-lint")
                    .contains("FrontendLintJobDocumentationTests");
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
