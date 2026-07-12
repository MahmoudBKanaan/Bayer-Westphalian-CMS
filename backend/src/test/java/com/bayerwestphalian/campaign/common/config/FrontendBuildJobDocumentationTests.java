package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Sprint 17 item <b>684</b>: Add frontend build job.
 *
 * <p>KB epic E25: CI must produce a production frontend build on a clean agent. Locks the {@code
 * frontend-build} GitHub Actions job without executing the pipeline or {@code npm run build} from
 * this class.
 */
@DisplayName("684 Add frontend build job")
class FrontendBuildJobDocumentationTests {

    private static final Path CI_WORKFLOW = Path.of("../.github/workflows/ci.yml");
    private static final Path CI_CD_DOC = Path.of("../docs/deployment/ci-cd.md");
    private static final Path GITHUB_README = Path.of("../.github/README.md");
    private static final Path FRONTEND_PACKAGE_JSON = Path.of("../frontend/package.json");

    @Nested
    @DisplayName("CI workflow: frontend-build job")
    class WorkflowJob {

        @Test
        void definesFrontendBuildJobWithNode22AndNpmRunBuild() throws Exception {
            assertThat(CI_WORKFLOW).exists();
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            assertThat(yaml)
                    .contains("684")
                    .contains("frontend-build:")
                    .contains("name: Frontend build")
                    .contains("runs-on: ubuntu-latest")
                    .contains("working-directory: frontend")
                    .contains("actions/setup-node@v4")
                    .contains("node-version: \"22\"")
                    .contains("cache: npm")
                    .contains("cache-dependency-path: frontend/package-lock.json")
                    .contains("id: frontend-build")
                    .contains("Build frontend (npm run build)")
                    .contains("Assert frontend dist was produced");
        }

        @Test
        void frontendBuildJobIsBuildOnlySeparateFromLintAndTest() throws Exception {
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            int buildJobIndex = indexOfJob(yaml, "frontend-build:");
            assertThat(buildJobIndex).isGreaterThanOrEqualTo(0);

            // Frontend build ends at Docker backend job (685) when present.
            int nextJobIndex = yaml.indexOf("\n  docker-backend:");
            String buildBlock =
                    nextJobIndex > buildJobIndex
                            ? yaml.substring(buildJobIndex, nextJobIndex)
                            : yaml.substring(buildJobIndex);
            assertThat(buildBlock).contains("npm run build");
            assertThat(buildBlock).contains("npm ci");
            assertThat(buildBlock).contains("Assert frontend dist was produced");
            assertThat(buildBlock).doesNotContain("npm run lint");
            assertThat(buildBlock).doesNotContain("npm test");
        }

        @Test
        void frontendPackageJsonDefinesBuildScript() throws Exception {
            assertThat(FRONTEND_PACKAGE_JSON).exists();
            String packageJson = Files.readString(FRONTEND_PACKAGE_JSON, StandardCharsets.UTF_8);

            assertThat(packageJson).contains("\"build\"").contains("vite");
        }

        @Test
        void combinedMonolithicFrontendJobIsReplacedByDedicatedJobs() throws Exception {
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            // Dedicated install/lint/test/build jobs exist; no leftover combined job id "frontend:".
            assertThat(yaml).contains("frontend-install:");
            assertThat(yaml).contains("frontend-lint:");
            assertThat(yaml).contains("frontend-test:");
            assertThat(yaml).contains("frontend-build:");
            assertThat(yaml).doesNotContain("\n  frontend:\n");
        }
    }

    @Nested
    @DisplayName("Documentation")
    class Documentation {

        @Test
        void ciCdDocDocumentsFrontendBuildJobForItem684() throws Exception {
            assertThat(CI_CD_DOC).exists();
            String doc = Files.readString(CI_CD_DOC, StandardCharsets.UTF_8);

            assertThat(doc)
                    .contains("684")
                    .contains("frontend-build")
                    .contains("Frontend build")
                    .contains("npm run build")
                    .contains("FrontendBuildJobDocumentationTests");
        }

        @Test
        void githubReadmeListsFrontendBuildJob() throws Exception {
            assertThat(GITHUB_README).exists();
            String readme = Files.readString(GITHUB_README, StandardCharsets.UTF_8);

            assertThat(readme)
                    .contains("684")
                    .contains("frontend-build")
                    .contains("FrontendBuildJobDocumentationTests");
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
