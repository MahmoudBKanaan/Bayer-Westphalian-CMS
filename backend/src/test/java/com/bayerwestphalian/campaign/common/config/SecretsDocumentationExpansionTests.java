package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Sprint 17 item <b>710</b>: Secrets documentation.
 *
 * <p>Locks the operational secrets guide: ownership, provisioning, rotation, leak response,
 * backup/restore handling, audit evidence, and safe cross-links.
 */
@DisplayName("710 Secrets documentation")
class SecretsDocumentationExpansionTests {

    private static final Path SECRETS_DOC = Path.of("../docs/deployment/secrets.md");
    private static final Path ENV_DOC = Path.of("../docs/deployment/environment-variables.md");
    private static final Path CI_CD_DOC = Path.of("../docs/deployment/ci-cd.md");
    private static final Path BACKUP_RESTORE = Path.of("../docs/deployment/backup-and-restore.md");
    private static final Path DOCS_INDEX = Path.of("../docs/README.md");
    private static final Path CI_WORKFLOW = Path.of("../.github/workflows/ci.yml");

    @Test
    void secretsGuideContainsOperationalSectionsForItem710() throws Exception {
        assertThat(SECRETS_DOC).exists();
        String doc = Files.readString(SECRETS_DOC, StandardCharsets.UTF_8);

        assertThat(doc)
                .contains("710")
                .contains("Secrets documentation")
                .contains("Secret ownership and access")
                .contains("Provisioning checklist")
                .contains("Rotation schedule")
                .contains("Leak response runbook")
                .contains("Backup and restore handling")
                .contains("Audit evidence")
                .contains("SecretsDocumentationExpansionTests");
    }

    @Test
    void secretsGuideDocumentsConcreteSecretNamesWithoutValues() throws Exception {
        String doc = Files.readString(SECRETS_DOC, StandardCharsets.UTF_8);

        assertThat(doc)
                .contains("JWT_SECRET")
                .contains("DB_PASSWORD")
                .contains("SMTP_PASSWORD")
                .contains("SMS_API_KEY")
                .contains("secret manager")
                .contains("least privilege")
                .contains("break-glass");
        assertThat(doc)
                .doesNotContain("-----BEGIN")
                .doesNotContain("production-jwt-secret-32chars-min!!");
    }

    @Test
    void relatedDocsAndCiReferenceExpandedSecretsDocumentation() throws Exception {
        assertThat(ENV_DOC).exists();
        assertThat(CI_CD_DOC).exists();
        assertThat(BACKUP_RESTORE).exists();
        assertThat(DOCS_INDEX).exists();
        assertThat(CI_WORKFLOW).exists();

        assertThat(Files.readString(ENV_DOC, StandardCharsets.UTF_8))
                .contains("710")
                .contains("secrets.md");
        assertThat(Files.readString(CI_CD_DOC, StandardCharsets.UTF_8))
                .contains("710")
                .contains("secrets.md");
        assertThat(Files.readString(BACKUP_RESTORE, StandardCharsets.UTF_8))
                .contains("secrets.md");
        assertThat(Files.readString(DOCS_INDEX, StandardCharsets.UTF_8))
                .contains("710")
                .contains("deployment/secrets.md");
        assertThat(Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8))
                .contains("710")
                .contains("secrets.md");
    }

    @Test
    void ciWorkflowDoesNotExposeSecretAssignments() throws Exception {
        String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

        assertThat(yaml)
                .doesNotContain("JWT_SECRET:")
                .doesNotContain("DB_PASSWORD:")
                .doesNotContain("SMTP_PASSWORD:")
                .doesNotContain("SMS_API_KEY:");
    }
}
