package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Sprint 17 item <b>708</b>: CI/CD documentation.
 *
 * <p>Locks the expanded CI/CD guide as the operational index for workflow structure, local parity,
 * evidence scripts, artifacts, security posture, and future maintenance.
 */
@DisplayName("708 CI/CD documentation")
class CiCdDocumentationExpansionTests {

    private static final Path CI_WORKFLOW = Path.of("../.github/workflows/ci.yml");
    private static final Path CI_CD_DOC = Path.of("../docs/deployment/ci-cd.md");
    private static final Path GITHUB_README = Path.of("../.github/README.md");
    private static final Path DOCS_INDEX = Path.of("../docs/README.md");

    @Test
    void workflowReferencesDocumentationItem708() throws Exception {
        assertThat(CI_WORKFLOW).exists();
        String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

        assertThat(yaml)
                .contains("708")
                .contains("CI/CD documentation");
    }

    @Test
    void ciCdGuideContainsRequiredOperationalSections() throws Exception {
        assertThat(CI_CD_DOC).exists();
        String doc = Files.readString(CI_CD_DOC, StandardCharsets.UTF_8);

        assertThat(doc)
                .contains("708")
                .contains("CI/CD documentation")
                .contains("Workflow file")
                .contains("Automated documentation evidence")
                .contains("Triggers")
                .contains("Jobs")
                .contains("Release artifact generation")
                .contains("Pipeline fails on intentionally broken test")
                .contains("Pipeline passes on clean main branch")
                .contains("Local parity")
                .contains("Security notes")
                .contains("Maintenance checklist")
                .contains("CiCdDocumentationExpansionTests");
    }

    @Test
    void ciCdGuideDocumentsBoundariesAndRelatedDocs() throws Exception {
        String doc = Files.readString(CI_CD_DOC, StandardCharsets.UTF_8);

        assertThat(doc)
                .contains("does not deploy")
                .contains("does not push images")
                .contains("does not embed production secrets")
                .contains("branch-protection.md")
                .contains("release-tagging.md")
                .contains("secrets.md")
                .contains("developer-setup.md");
    }

    @Test
    void documentationIndexesReferenceItem708() throws Exception {
        assertThat(GITHUB_README).exists();
        assertThat(DOCS_INDEX).exists();

        String githubReadme = Files.readString(GITHUB_README, StandardCharsets.UTF_8);
        String docsIndex = Files.readString(DOCS_INDEX, StandardCharsets.UTF_8);

        assertThat(githubReadme)
                .contains("708")
                .contains("CI/CD documentation")
                .contains("CiCdDocumentationExpansionTests");
        assertThat(docsIndex)
                .contains("CI/CD")
                .contains("708");
    }
}
