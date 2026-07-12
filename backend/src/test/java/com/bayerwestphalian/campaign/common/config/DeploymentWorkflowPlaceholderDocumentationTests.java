package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Sprint 17 item <b>697</b>: Add deployment workflow placeholder.
 *
 * <p>KB epic E25: packaging is automated; production deploy is prepared later (E26 / Sprint 18).
 * Locks a manual {@code workflow_dispatch} GitHub Actions workflow that does not push images,
 * SSH, or apply cloud manifests.
 */
@DisplayName("697 Add deployment workflow placeholder")
class DeploymentWorkflowPlaceholderDocumentationTests {

    private static final Path DEPLOY_WORKFLOW =
            Path.of("../.github/workflows/deploy-placeholder.yml");
    private static final Path CI_WORKFLOW = Path.of("../.github/workflows/ci.yml");
    private static final Path CI_CD_DOC = Path.of("../docs/deployment/ci-cd.md");
    private static final Path GITHUB_README = Path.of("../.github/README.md");
    private static final Path DOCS_INDEX = Path.of("../docs/README.md");

    @Nested
    @DisplayName("Deploy placeholder workflow")
    class WorkflowFile {

        @Test
        void definesManualDispatchPlaceholderWorkflow() throws Exception {
            assertThat(DEPLOY_WORKFLOW).exists();
            String yaml = Files.readString(DEPLOY_WORKFLOW, StandardCharsets.UTF_8);

            assertThat(yaml)
                    .contains("697")
                    .contains("name: Deploy (placeholder)")
                    .contains("workflow_dispatch:")
                    .contains("deploy-placeholder:")
                    .contains("name: Deployment placeholder")
                    .contains("id: deploy-placeholder")
                    .contains("PLACEHOLDER")
                    .contains("contents: read")
                    .contains("actions/checkout@v4");
        }

        @Test
        void placeholderDoesNotPerformLiveDeploymentActions() throws Exception {
            String yaml = Files.readString(DEPLOY_WORKFLOW, StandardCharsets.UTF_8);

            // No automatic push/PR deploy; manual only.
            assertThat(yaml).contains("workflow_dispatch:");
            assertThat(yaml).doesNotContain("pull_request:");
            // Avoid false positives from comments: forbid active-looking deploy verbs as steps.
            assertThat(yaml).doesNotContain("docker push");
            assertThat(yaml).doesNotContain("kubectl ");
            assertThat(yaml).doesNotContain("helm ");
            assertThat(yaml).doesNotContain("ssh ");
            assertThat(yaml).doesNotContain("aws ");
            assertThat(yaml).doesNotContain("az ");
            assertThat(yaml).doesNotContain("gcloud ");
            assertThat(yaml).doesNotContain("JWT_SECRET:");
            assertThat(yaml).doesNotContain("DB_PASSWORD:");
            assertThat(yaml).doesNotContain("secrets.");
        }

        @Test
        void placeholderDocumentsFutureSprint18Gate() throws Exception {
            String yaml = Files.readString(DEPLOY_WORKFLOW, StandardCharsets.UTF_8);

            assertThat(yaml)
                    .contains("Sprint 18")
                    .contains("691")
                    .contains("696")
                    .containsIgnoringCase("does NOT deploy")
                    .contains("staging-placeholder")
                    .contains("production-placeholder");
        }
    }

    @Nested
    @DisplayName("Documentation")
    class Documentation {

        @Test
        void ciCdDocDocumentsDeploymentPlaceholderForItem697() throws Exception {
            assertThat(CI_CD_DOC).exists();
            String doc = Files.readString(CI_CD_DOC, StandardCharsets.UTF_8);

            assertThat(doc)
                    .contains("697")
                    .contains("deploy-placeholder.yml")
                    .contains("Deploy (placeholder)")
                    .contains("DeploymentWorkflowPlaceholderDocumentationTests")
                    .containsIgnoringCase("workflow_dispatch");
        }

        @Test
        void githubReadmeAndDocsIndexReferenceItem697() throws Exception {
            assertThat(GITHUB_README).exists();
            String readme = Files.readString(GITHUB_README, StandardCharsets.UTF_8);
            assertThat(readme)
                    .contains("697")
                    .contains("deploy-placeholder.yml")
                    .contains("DeploymentWorkflowPlaceholderDocumentationTests");

            assertThat(DOCS_INDEX).exists();
            String index = Files.readString(DOCS_INDEX, StandardCharsets.UTF_8);
            assertThat(index).contains("697");
            assertThat(index).containsIgnoringCase("deploy");
        }

        @Test
        void ciWorkflowRemainsSeparateFromDeployPlaceholder() throws Exception {
            assertThat(CI_WORKFLOW).exists();
            String ci = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);
            assertThat(ci).contains("name: CI");
            assertThat(ci).contains("697");
            assertThat(ci).doesNotContain("name: Deploy (placeholder)");
        }
    }
}
