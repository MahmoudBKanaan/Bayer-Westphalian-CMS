package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Sprint 17 item <b>678</b>: Add backend build job.
 *
 * <p>KB: CI/CD must compile and package the Spring Boot backend (Java 21 / Maven) without requiring
 * a local developer machine. Locks the {@code backend-build} GitHub Actions job structure without
 * executing the pipeline.
 */
@DisplayName("678 Add backend build job")
class BackendBuildJobDocumentationTests {

    private static final Path CI_WORKFLOW = Path.of("../.github/workflows/ci.yml");
    private static final Path CI_CD_DOC = Path.of("../docs/deployment/ci-cd.md");
    private static final Path BACKEND_POM = Path.of("../backend/pom.xml");

    @Nested
    @DisplayName("CI workflow: backend-build job")
    class WorkflowJob {

        @Test
        void definesBackendBuildJobWithJava21AndMavenPackage() throws Exception {
            assertThat(CI_WORKFLOW).exists();
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            assertThat(yaml)
                    .contains("678")
                    .contains("backend-build:")
                    .contains("name: Backend build")
                    .contains("runs-on: ubuntu-latest")
                    .contains("working-directory: backend")
                    .contains("actions/setup-java@v4")
                    .contains("java-version: \"21\"")
                    .contains("distribution: temurin")
                    .contains("cache: maven")
                    .contains("cache-dependency-path: backend/pom.xml")
                    .contains("mvn -B -DskipTests package")
                    .contains("id: backend-build");
        }

        @Test
        void backendBuildJobSkipsTestsAndAssertsJarArtifact() throws Exception {
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            // Build job must package without running the full suite (item 679 owns tests).
            assertThat(yaml).contains("Build backend (Maven package, skip tests)");
            assertThat(yaml).contains("Assert backend JAR artifact was produced");
            assertThat(yaml).contains("*.jar");

            // The dedicated build job block should not run `mvn test` (test job is item 679).
            int buildJobIndex = yaml.indexOf("backend-build:");
            int nextJobIndex = yaml.indexOf("\n  backend-test:");
            if (nextJobIndex < 0) {
                nextJobIndex = yaml.indexOf("\n  frontend:");
            }
            assertThat(buildJobIndex).isGreaterThanOrEqualTo(0);
            assertThat(nextJobIndex).isGreaterThan(buildJobIndex);
            String buildJobBlock = yaml.substring(buildJobIndex, nextJobIndex);
            assertThat(buildJobBlock).contains("mvn -B -DskipTests package");
            assertThat(buildJobBlock).doesNotContain("mvn -B test");
        }

        @Test
        void backendPomIsJava21SpringBootProject() throws Exception {
            assertThat(BACKEND_POM).exists();
            String pom = Files.readString(BACKEND_POM, StandardCharsets.UTF_8);

            assertThat(pom)
                    .contains("<java.version>21</java.version>")
                    .contains("spring-boot-starter-parent")
                    .contains("spring-boot-maven-plugin")
                    .contains("<artifactId>campaign</artifactId>");
        }
    }

    @Nested
    @DisplayName("Documentation")
    class Documentation {

        @Test
        void ciCdDocDocumentsBackendBuildJobForItem678() throws Exception {
            assertThat(CI_CD_DOC).exists();
            String doc = Files.readString(CI_CD_DOC, StandardCharsets.UTF_8);

            assertThat(doc)
                    .contains("678")
                    .contains("backend-build")
                    .contains("Backend build")
                    .contains("mvn -B -DskipTests package")
                    .contains("BackendBuildJobDocumentationTests")
                    .contains("Java 21");
        }
    }
}
