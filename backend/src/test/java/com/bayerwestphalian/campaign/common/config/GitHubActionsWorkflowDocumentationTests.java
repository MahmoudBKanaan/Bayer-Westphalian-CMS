package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Sprint 17 item <b>677</b>: Add GitHub Actions workflow.
 *
 * <p>KB: CI/CD via GitHub Actions for build, test, and package checks. Locks the primary workflow
 * file structure, triggers, and job foundations without executing the pipeline.
 *
 * <p>Item <b>678</b> specializes the backend packaging job — see {@link
 * BackendBuildJobDocumentationTests}. Item <b>679</b> specializes the backend test job — see
 * {@link BackendTestJobDocumentationTests}. Item <b>680</b> specializes the backend integration
 * test job — see {@link BackendIntegrationTestJobDocumentationTests}. Item <b>681</b>
 * specializes the frontend install job — see {@link FrontendInstallJobDocumentationTests}. Item
 * <b>682</b> specializes the frontend lint job — see {@link FrontendLintJobDocumentationTests}.
 * Item <b>683</b> specializes the frontend test job — see {@link
 * FrontendTestJobDocumentationTests}. Item <b>684</b> specializes the frontend build job — see
 * {@link FrontendBuildJobDocumentationTests}. Item <b>685</b> specializes the Docker backend image
 * build — see {@link DockerBackendImageBuildDocumentationTests}. Item <b>686</b> specializes the
 * Docker frontend image build — see {@link DockerFrontendImageBuildDocumentationTests}. Item
 * <b>687</b> specializes Docker Compose validation — see {@link
 * DockerComposeValidationDocumentationTests}. Item <b>690</b> specializes production config
 * validation — see {@link ProductionConfigValidationStepDocumentationTests}. Item <b>691</b>
 * specializes release artifact generation — see {@link
 * ReleaseArtifactGenerationDocumentationTests}. Item <b>692</b> specializes the README CI badge —
 * see {@link CiBadgeDocumentationTests}. Item <b>693</b> specializes fail-on-red verification —
 * see {@link PipelineFailsWhenTestsFailDocumentationTests}. Item <b>694</b> specializes
 * pass-on-green for clean {@code main} — see {@link
 * PipelinePassesOnCleanMainBranchDocumentationTests}. Item <b>695</b> specializes branch
 * protection recommendation — see {@link BranchProtectionRecommendationDocumentationTests}. Item
 * <b>696</b> specializes release tagging process — see {@link
 * ReleaseTaggingProcessDocumentationTests}. Item <b>697</b> specializes the deployment workflow
 * placeholder — see {@link DeploymentWorkflowPlaceholderDocumentationTests}. Item <b>698</b>
 * specializes CI on pull request — see {@link CiRunsOnPullRequestDocumentationTests}. Item
 * <b>699</b> specializes CI on main branch — see {@link CiRunsOnMainBranchDocumentationTests}.
 * Item <b>700</b> specializes backend build passes — see {@link
 * BackendBuildPassesDocumentationTests}. Item <b>701</b> specializes backend tests pass — see
 * {@link BackendTestsPassDocumentationTests}.
 */
@DisplayName("677 Add GitHub Actions workflow")
class GitHubActionsWorkflowDocumentationTests {

    private static final Path CI_WORKFLOW = Path.of("../.github/workflows/ci.yml");
    private static final Path GITHUB_README = Path.of("../.github/README.md");
    private static final Path CI_CD_DOC = Path.of("../docs/deployment/ci-cd.md");
    private static final Path DOCS_INDEX = Path.of("../docs/README.md");
    private static final Path ROOT_README = Path.of("../README.md");

    @Nested
    @DisplayName("Workflow file")
    class WorkflowFile {

        @Test
        void ciWorkflowExistsWithNameAndTriggers() throws Exception {
            assertThat(CI_WORKFLOW).exists();
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            assertThat(yaml)
                    .contains("677")
                    .contains("name: CI")
                    .contains("on:")
                    .contains("pull_request:")
                    .contains("push:")
                    .contains("main")
                    .contains("dev")
                    .contains("concurrency:")
                    .contains("permissions:")
                    .contains("contents: read");
        }

        @Test
        void ciWorkflowDefinesBackendBuildAndFrontendJobs() throws Exception {
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            assertThat(yaml)
                    .contains("jobs:")
                    .contains("backend-build:")
                    .contains("backend-test:")
                    .contains("backend-integration-test:")
                    .contains("frontend-install:")
                    .contains("frontend-lint:")
                    .contains("frontend-test:")
                    .contains("frontend-build:")
                    .contains("docker-backend:")
                    .contains("docker-frontend:")
                    .contains("docker-compose-validate:")
                    .contains("production-config-validate:")
                    .contains("runs-on: ubuntu-latest")
                    .contains("actions/checkout@v4")
                    .contains("actions/setup-java@v4")
                    .contains("actions/setup-node@v4")
                    .contains("java-version: \"21\"")
                    .contains("node-version: \"22\"")
                    .contains("mvn -B -DskipTests package")
                    .contains("mvn -B test")
                    .contains("*IntegrationTests")
                    .contains("npm ci")
                    .contains("npm run lint")
                    .contains("npm test")
                    .contains("npm run build")
                    .contains("docker build -t bwc-backend:ci -f backend/Dockerfile backend")
                    .contains("docker build -t bwc-frontend:ci -f frontend/Dockerfile frontend")
                    .contains("docker compose -f \"$compose_file\" config")
                    .contains("config --format json")
                    .contains("application-prod.yml")
                    .contains("production-config-validate")
                    .contains("working-directory: backend")
                    .contains("working-directory: frontend");
        }

        @Test
        void ciWorkflowDoesNotEmbedProductionSecrets() throws Exception {
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            assertThat(yaml)
                    .doesNotContain("JWT_SECRET:")
                    .doesNotContain("DB_PASSWORD:")
                    .doesNotContain("smtp-password")
                    .doesNotContain("-----BEGIN");
        }
    }

    @Nested
    @DisplayName("Documentation")
    class Documentation {

        @Test
        void ciCdDocumentDescribesWorkflowAndJobs() throws Exception {
            assertThat(CI_CD_DOC).exists();
            String doc = Files.readString(CI_CD_DOC, StandardCharsets.UTF_8);

            assertThat(doc)
                    .contains("677")
                    .contains("GitHub Actions")
                    .contains(".github/workflows/ci.yml")
                    .contains("backend-build")
                    .contains("backend-test")
                    .contains("backend-integration-test")
                    .contains("frontend-install")
                    .contains("frontend-lint")
                    .contains("frontend-test")
                    .contains("frontend-build")
                    .contains("docker-backend")
                    .contains("docker-frontend")
                    .contains("docker-compose-validate")
                    .contains("production-config-validate")
                    .contains("pull_request")
                    .contains("GitHubActionsWorkflowDocumentationTests");
        }

        @Test
        void githubReadmeAndDocsIndexLinkCiWorkflow() throws Exception {
            assertThat(GITHUB_README).exists();
            String githubReadme = Files.readString(GITHUB_README, StandardCharsets.UTF_8);
            assertThat(githubReadme)
                    .contains("677")
                    .contains("workflows/ci.yml")
                    .containsIgnoringCase("CI");

            assertThat(DOCS_INDEX).exists();
            String index = Files.readString(DOCS_INDEX, StandardCharsets.UTF_8);
            assertThat(index).contains("deployment/ci-cd.md");
        }

        @Test
        void rootReadmeMentionsGitHubActions() throws Exception {
            assertThat(ROOT_README).exists();
            String readme = Files.readString(ROOT_README, StandardCharsets.UTF_8);
            assertThat(readme).contains("GitHub Actions");
        }
    }
}
