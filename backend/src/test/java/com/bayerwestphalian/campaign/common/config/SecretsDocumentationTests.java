package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Sprint 17 item <b>689</b>: Add secrets documentation.
 *
 * <p>KB: production secrets must be managed outside Git and validated at startup. Locks the secrets
 * ops guide and cross-links without loading real secret values.
 */
@DisplayName("689 Add secrets documentation")
class SecretsDocumentationTests {

    private static final Path SECRETS_DOC = Path.of("../docs/deployment/secrets.md");
    private static final Path ENV_DOC = Path.of("../docs/deployment/environment-variables.md");
    private static final Path SECURITY_HARDENING =
            Path.of("../docs/architecture/security-hardening.md");
    private static final Path PROD_CHECKLIST =
            Path.of("../docs/deployment/production-security-checklist.md");
    private static final Path CI_CD_DOC = Path.of("../docs/deployment/ci-cd.md");
    private static final Path DOCS_INDEX = Path.of("../docs/README.md");
    private static final Path GITIGNORE = Path.of("../.gitignore");
    private static final Path CI_WORKFLOW = Path.of("../.github/workflows/ci.yml");

    @Nested
    @DisplayName("Secrets guide")
    class SecretsGuide {

        @Test
        void secretsDocumentExistsWithItem689Scope() throws Exception {
            assertThat(SECRETS_DOC).exists();
            String doc = Files.readString(SECRETS_DOC, StandardCharsets.UTF_8);

            assertThat(doc)
                    .contains("689")
                    .contains("JWT_SECRET")
                    .contains("DB_PASSWORD")
                    .contains("SecretPresenceValidator")
                    .contains("MissingSecretsAreDetectedTests")
                    .contains("665")
                    .contains("secret manager")
                    .contains("Never commit")
                    .contains("SecretsDocumentationTests");
        }

        @Test
        void secretsDocumentCoversCiAndDockerRules() throws Exception {
            String doc = Files.readString(SECRETS_DOC, StandardCharsets.UTF_8);

            assertThat(doc)
                    .contains("GitHub Actions")
                    .contains("Docker")
                    .contains("SMTP_PASSWORD")
                    .contains("SMS_API_KEY")
                    .contains("rotation");
        }

        @Test
        void secretsDocumentDoesNotEmbedRealSecretMaterial() throws Exception {
            String doc = Files.readString(SECRETS_DOC, StandardCharsets.UTF_8);

            assertThat(doc).doesNotContain("-----BEGIN");
            assertThat(doc).doesNotContain("production-jwt-secret-32chars-min!!");
        }
    }

    @Nested
    @DisplayName("Cross-links and repo hygiene")
    class CrossLinks {

        @Test
        void relatedDocsAndIndexLinkSecretsGuide() throws Exception {
            assertThat(ENV_DOC).exists();
            String envDoc = Files.readString(ENV_DOC, StandardCharsets.UTF_8);
            assertThat(envDoc).contains("689").contains("secrets");

            assertThat(SECURITY_HARDENING).exists();
            String hardening = Files.readString(SECURITY_HARDENING, StandardCharsets.UTF_8);
            assertThat(hardening).contains("SecretPresenceValidator").contains("JWT_SECRET");

            assertThat(PROD_CHECKLIST).exists();
            String checklist = Files.readString(PROD_CHECKLIST, StandardCharsets.UTF_8);
            assertThat(checklist).contains("JWT_SECRET").contains("SecretPresenceValidator");

            assertThat(DOCS_INDEX).exists();
            String index = Files.readString(DOCS_INDEX, StandardCharsets.UTF_8);
            assertThat(index).contains("deployment/secrets.md");

            assertThat(CI_CD_DOC).exists();
            String ci = Files.readString(CI_CD_DOC, StandardCharsets.UTF_8);
            assertThat(ci).contains("689").contains("secrets.md");
        }

        @Test
        void gitignoreExcludesEnvAndKeyMaterial() throws Exception {
            assertThat(GITIGNORE).exists();
            String gitignore = Files.readString(GITIGNORE, StandardCharsets.UTF_8);

            assertThat(gitignore)
                    .contains(".env")
                    .contains("!.env.example")
                    .contains("*.pem")
                    .contains("*.key")
                    .contains("secrets/");
        }

        @Test
        void ciWorkflowDoesNotEmbedSecretAssignments() throws Exception {
            assertThat(CI_WORKFLOW).exists();
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            assertThat(yaml)
                    .doesNotContain("JWT_SECRET:")
                    .doesNotContain("DB_PASSWORD:")
                    .doesNotContain("SMTP_PASSWORD:")
                    .doesNotContain("SMS_API_KEY:");
        }
    }
}
