package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Sprint 17 item <b>709</b>: Environment variable documentation.
 *
 * <p>Locks the environment-variable guide as the operational catalog for required production
 * keys, secret/non-secret classification, validation behavior, and change management.
 */
@DisplayName("709 Environment variable documentation")
class EnvironmentVariableDocumentationTests {

    private static final Path ENV_DOC = Path.of("../docs/deployment/environment-variables.md");
    private static final Path DOCS_INDEX = Path.of("../docs/README.md");
    private static final Path SECRETS_DOC = Path.of("../docs/deployment/secrets.md");
    private static final Path CI_CD_DOC = Path.of("../docs/deployment/ci-cd.md");
    private static final Path CI_WORKFLOW = Path.of("../.github/workflows/ci.yml");

    @Test
    void environmentVariableGuideContainsRequiredOperationalSections() throws Exception {
        assertThat(ENV_DOC).exists();
        String doc = Files.readString(ENV_DOC, StandardCharsets.UTF_8);

        assertThat(doc)
                .contains("709")
                .contains("Environment variable documentation")
                .contains("Variable catalog")
                .contains("Classification")
                .contains("Required production variables")
                .contains("Secret variables")
                .contains("Validation and startup behavior")
                .contains("Change management")
                .contains("Rotation notes")
                .contains("Troubleshooting")
                .contains("EnvironmentVariableDocumentationTests");
    }

    @Test
    void guideClassifiesRequiredProductionAndSecretVariables() throws Exception {
        String doc = Files.readString(ENV_DOC, StandardCharsets.UTF_8);

        assertThat(doc)
                .contains("DB_URL")
                .contains("DB_USERNAME")
                .contains("DB_PASSWORD")
                .contains("JWT_SECRET")
                .contains("CORS_ALLOWED_ORIGINS")
                .contains("SMTP_PASSWORD")
                .contains("SMS_API_KEY")
                .contains("secret manager")
                .contains("required in prod");
    }

    @Test
    void guideLinksValidationSecretsAndCiDocumentation() throws Exception {
        String doc = Files.readString(ENV_DOC, StandardCharsets.UTF_8);

        assertThat(doc)
                .contains("EnvironmentVariableValidator")
                .contains("SecretPresenceValidator")
                .contains("ProductionEnvironmentPostProcessor")
                .contains("secrets.md")
                .contains("ci-cd.md")
                .contains("production-security-checklist.md");

        assertThat(SECRETS_DOC).exists();
        assertThat(CI_CD_DOC).exists();
        assertThat(CI_WORKFLOW).exists();
        assertThat(Files.readString(CI_CD_DOC, StandardCharsets.UTF_8))
                .contains("709")
                .contains("environment-variables.md");
        assertThat(Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8))
                .contains("709")
                .contains("environment-variables.md");
    }

    @Test
    void docsIndexReferencesEnvironmentVariableDocumentation() throws Exception {
        assertThat(DOCS_INDEX).exists();
        String index = Files.readString(DOCS_INDEX, StandardCharsets.UTF_8);

        assertThat(index)
                .contains("Environment Variable")
                .contains("709")
                .contains("deployment/environment-variables.md");
    }
}
