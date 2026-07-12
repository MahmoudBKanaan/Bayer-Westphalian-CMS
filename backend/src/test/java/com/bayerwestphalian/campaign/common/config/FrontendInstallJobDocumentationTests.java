package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Sprint 17 item <b>681</b>: Add frontend install job.
 *
 * <p>KB epic E25: CI must install frontend dependencies from the lockfile on a clean agent. Locks
 * the {@code frontend-install} GitHub Actions job without executing the pipeline or {@code npm ci}
 * from this class.
 */
@DisplayName("681 Add frontend install job")
class FrontendInstallJobDocumentationTests {

    private static final Path CI_WORKFLOW = Path.of("../.github/workflows/ci.yml");
    private static final Path CI_CD_DOC = Path.of("../docs/deployment/ci-cd.md");
    private static final Path GITHUB_README = Path.of("../.github/README.md");
    private static final Path FRONTEND_PACKAGE_JSON = Path.of("../frontend/package.json");
    private static final Path FRONTEND_PACKAGE_LOCK = Path.of("../frontend/package-lock.json");

    @Nested
    @DisplayName("CI workflow: frontend-install job")
    class WorkflowJob {

        @Test
        void definesFrontendInstallJobWithNode22AndNpmCi() throws Exception {
            assertThat(CI_WORKFLOW).exists();
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            assertThat(yaml)
                    .contains("681")
                    .contains("frontend-install:")
                    .contains("name: Frontend install")
                    .contains("runs-on: ubuntu-latest")
                    .contains("working-directory: frontend")
                    .contains("actions/setup-node@v4")
                    .contains("node-version: \"22\"")
                    .contains("cache: npm")
                    .contains("cache-dependency-path: frontend/package-lock.json")
                    .contains("id: frontend-install")
                    .contains("Install frontend dependencies (npm ci)");
        }

        @Test
        void frontendInstallJobIsInstallOnlySeparateFromLintTestBuild() throws Exception {
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            int installJobIndex = indexOfJob(yaml, "frontend-install:");
            // Next job is frontend-lint (682) when present; else combined frontend job.
            int nextJobIndex = yaml.indexOf("\n  frontend-lint:");
            if (nextJobIndex < 0) {
                nextJobIndex = yaml.indexOf("\n  frontend:");
            }
            assertThat(installJobIndex).isNonNegative();
            assertThat(nextJobIndex).isGreaterThan(installJobIndex);

            String installBlock = yaml.substring(installJobIndex, nextJobIndex);
            assertThat(installBlock).contains("npm ci");
            assertThat(installBlock).contains("Assert node_modules was installed");
            assertThat(installBlock).doesNotContain("npm run lint");
            assertThat(installBlock).doesNotContain("npm test");
            assertThat(installBlock).doesNotContain("npm run build");
        }

        @Test
        void frontendPackageManifestAndLockfileExist() throws Exception {
            assertThat(FRONTEND_PACKAGE_JSON).exists();
            assertThat(FRONTEND_PACKAGE_LOCK).exists();

            String packageJson = Files.readString(FRONTEND_PACKAGE_JSON, StandardCharsets.UTF_8);
            assertThat(packageJson)
                    .contains("\"name\"")
                    .contains("bayer-westphalian-campaign-platform-frontend")
                    .contains("\"scripts\"");
        }
    }

    @Nested
    @DisplayName("Documentation")
    class Documentation {

        @Test
        void ciCdDocDocumentsFrontendInstallJobForItem681() throws Exception {
            assertThat(CI_CD_DOC).exists();
            String doc = Files.readString(CI_CD_DOC, StandardCharsets.UTF_8);

            assertThat(doc)
                    .contains("681")
                    .contains("frontend-install")
                    .contains("Frontend install")
                    .contains("npm ci")
                    .contains("FrontendInstallJobDocumentationTests")
                    .contains("Node.js")
                    .contains("22");
        }

        @Test
        void githubReadmeListsFrontendInstallJob() throws Exception {
            assertThat(GITHUB_README).exists();
            String readme = Files.readString(GITHUB_README, StandardCharsets.UTF_8);

            assertThat(readme)
                    .contains("681")
                    .contains("frontend-install")
                    .contains("FrontendInstallJobDocumentationTests");
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
