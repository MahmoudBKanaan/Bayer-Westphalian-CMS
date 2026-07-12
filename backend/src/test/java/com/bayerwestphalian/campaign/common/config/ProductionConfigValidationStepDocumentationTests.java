package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Sprint 17 item <b>690</b>: Add production config validation step.
 *
 * <p>KB epic E25: CI must validate production configuration artifacts before release preparation.
 * Locks the {@code production-config-validate} GitHub Actions job and prod config files without
 * starting Spring Boot or using real secrets.
 */
@DisplayName("690 Add production config validation step")
class ProductionConfigValidationStepDocumentationTests {

    private static final Path CI_WORKFLOW = Path.of("../.github/workflows/ci.yml");
    private static final Path CI_CD_DOC = Path.of("../docs/deployment/ci-cd.md");
    private static final Path GITHUB_README = Path.of("../.github/README.md");
    private static final Path PROD_YML =
            Path.of("../backend/src/main/resources/application-prod.yml");
    private static final Path SECRETS_DOC = Path.of("../docs/deployment/secrets.md");

    @Nested
    @DisplayName("CI workflow: production-config-validate job")
    class WorkflowJob {

        @Test
        void definesProductionConfigValidateJob() throws Exception {
            assertThat(CI_WORKFLOW).exists();
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            assertThat(yaml)
                    .contains("690")
                    .contains("production-config-validate:")
                    .contains("name: Production config validation")
                    .contains("runs-on: ubuntu-latest")
                    .contains("id: production-config-validate")
                    .contains("Validate production configuration artifacts")
                    .contains("application-prod.yml")
                    .contains("EnvironmentVariableValidator")
                    .contains("SecretPresenceValidator")
                    .contains("MIN_JWT_SECRET_LENGTH = 32");
        }

        @Test
        void productionConfigJobIsStaticOnlyWithoutSecretValues() throws Exception {
            String yaml = Files.readString(CI_WORKFLOW, StandardCharsets.UTF_8);

            int jobIndex = indexOfJob(yaml, "production-config-validate:");
            assertThat(jobIndex).isNonNegative();
            String jobBlock = yaml.substring(jobIndex);

            assertThat(jobBlock).contains("application-prod.yml");
            assertThat(jobBlock).containsIgnoringCase("static checks");
            assertThat(jobBlock).doesNotContain("SPRING_PROFILES_ACTIVE=prod");
            assertThat(jobBlock).doesNotContain("mvn spring-boot:run");
            assertThat(jobBlock).doesNotContain("JWT_SECRET: ");
            assertThat(jobBlock).doesNotContain("DB_PASSWORD: ");
            assertThat(jobBlock).doesNotContain("docker push");
        }
    }

    @Nested
    @DisplayName("Production artifacts referenced by CI")
    class Artifacts {

        @Test
        void applicationProdYmlHardensErrorsAndRequiresEnvInjection() throws Exception {
            assertThat(PROD_YML).exists();
            String prod = Files.readString(PROD_YML, StandardCharsets.UTF_8);

            assertThat(prod)
                    .contains("include-stacktrace: never")
                    .contains("include-exception: false")
                    .contains("${DB_URL}")
                    .contains("${DB_USERNAME}")
                    .contains("${DB_PASSWORD}")
                    .contains("${JWT_SECRET}")
                    .contains("${CORS_ALLOWED_ORIGINS}");
            assertThat(prod.toLowerCase()).doesNotContain("localhost");
        }

        @Test
        void secretAndEnvironmentValidatorsExist() throws Exception {
            Path envValidator =
                    Path.of(
                            "../backend/src/main/java/com/bayerwestphalian/campaign/common/config/EnvironmentVariableValidator.java");
            Path secretValidator =
                    Path.of(
                            "../backend/src/main/java/com/bayerwestphalian/campaign/common/config/SecretPresenceValidator.java");
            Path postProcessor =
                    Path.of(
                            "../backend/src/main/java/com/bayerwestphalian/campaign/common/config/ProductionEnvironmentPostProcessor.java");

            assertThat(envValidator).exists();
            assertThat(secretValidator).exists();
            assertThat(postProcessor).exists();

            String secrets = Files.readString(secretValidator, StandardCharsets.UTF_8);
            assertThat(secrets)
                    .contains("MIN_JWT_SECRET_LENGTH = 32")
                    .contains("MIN_DB_PASSWORD_LENGTH = 8");
        }
    }

    @Nested
    @DisplayName("Documentation")
    class Documentation {

        @Test
        void ciCdDocDocumentsProductionConfigValidationForItem690() throws Exception {
            assertThat(CI_CD_DOC).exists();
            String doc = Files.readString(CI_CD_DOC, StandardCharsets.UTF_8);

            assertThat(doc)
                    .contains("690")
                    .contains("production-config-validate")
                    .contains("Production config validation")
                    .contains("ProductionConfigValidationStepDocumentationTests")
                    .contains("application-prod.yml");
        }

        @Test
        void githubReadmeAndSecretsDocReferenceItem690() throws Exception {
            assertThat(GITHUB_README).exists();
            String readme = Files.readString(GITHUB_README, StandardCharsets.UTF_8);
            assertThat(readme)
                    .contains("690")
                    .contains("production-config-validate")
                    .contains("ProductionConfigValidationStepDocumentationTests");

            assertThat(SECRETS_DOC).exists();
            String secrets = Files.readString(SECRETS_DOC, StandardCharsets.UTF_8);
            assertThat(secrets).contains("690");
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
