package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Sprint 17 item <b>700</b>: Backend build passes.
 *
 * <p>KB epic E25 acceptance: the {@code backend-build} job is the signal that packaging succeeds.
 * Locks that CI runs real Maven package + JAR assertion without soft-fail, and that local parity
 * is documented. Does not execute Maven or GitHub Actions.
 */
@DisplayName("700 Backend build passes")
class BackendBuildPassesDocumentationTests {

    private static final Path CI_WORKFLOW = Path.of("../.github/workflows/ci.yml");
    private static final Path CI_CD_DOC = Path.of("../docs/deployment/ci-cd.md");
    private static final Path GITHUB_README = Path.of("../.github/README.md");
    private static final Path BACKEND_POM = Path.of("../backend/pom.xml");

    @Nested
    @DisplayName("CI: backend-build pass criteria")
    class BackendBuildPassCriteria {

        @Test
        void workflowDocumentsBackendBuildPassesForItem700() throws Exception {
            assertThat(CI_WORKFLOW).exists();
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            assertThat(yaml)
                    .contains("700")
                    .contains("backend-build:")
                    .contains("name: Backend build")
                    .contains("mvn -B -DskipTests package")
                    .contains("Assert backend JAR artifact was produced");
        }

        @Test
        void backendBuildJobPassesOnlyWhenPackageAndJarSucceed() throws Exception {
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            int buildJobIndex = indexOfJob(yaml, "backend-build:");
            int nextJobIndex = yaml.indexOf("\n  backend-test:");
            assertThat(buildJobIndex).isGreaterThanOrEqualTo(0);
            assertThat(nextJobIndex).isGreaterThan(buildJobIndex);
            String block = yaml.substring(buildJobIndex, nextJobIndex);

            assertThat(block).contains("mvn -B -DskipTests package");
            assertThat(block).contains("id: backend-build");
            assertThat(block).contains("Assert backend JAR artifact was produced");
            assertThat(block).contains("*.jar");
            assertThat(block).contains("700");
            assertThat(block).doesNotContain("continue-on-error: true");
            assertThat(block).doesNotContain("continue-on-error:true");
            assertThat(block).doesNotContain("mvn -B -DskipTests package || true");
            assertThat(block).doesNotContain("mvn -B test");
        }

        @Test
        void backendPomSupportsPackagedSpringBootJar() throws Exception {
            assertThat(BACKEND_POM).exists();
            String pom = Files.readString(BACKEND_POM, StandardCharsets.UTF_8);

            assertThat(pom)
                    .contains("<java.version>21</java.version>")
                    .contains("spring-boot-maven-plugin")
                    .contains("spring-boot-starter-parent");
        }
    }

    @Nested
    @DisplayName("Documentation")
    class Documentation {

        @Test
        void ciCdDocDocumentsBackendBuildPassesForItem700() throws Exception {
            assertThat(CI_CD_DOC).exists();
            String doc = Files.readString(CI_CD_DOC, StandardCharsets.UTF_8);

            assertThat(doc)
                    .contains("700")
                    .containsIgnoringCase("Backend build passes")
                    .contains("backend-build")
                    .contains("mvn -B -DskipTests package")
                    .contains("BackendBuildPassesDocumentationTests");
        }

        @Test
        void githubReadmeReferencesItem700() throws Exception {
            assertThat(GITHUB_README).exists();
            String readme = Files.readString(GITHUB_README, StandardCharsets.UTF_8);

            assertThat(readme)
                    .contains("700")
                    .contains("BackendBuildPassesDocumentationTests")
                    .contains("backend-build");
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
