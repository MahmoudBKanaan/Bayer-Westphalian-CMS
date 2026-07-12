package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Sprint 17 item <b>688</b>: Add environment variable template.
 *
 * <p>KB: deployment configuration is driven by environment variables. Locks the checked-in {@code
 * .env.example} templates and catalog documentation without loading real secrets or starting the
 * application.
 */
@DisplayName("688 Add environment variable template")
class EnvironmentVariableTemplateDocumentationTests {

    private static final Path ROOT_ENV_EXAMPLE = Path.of("../.env.example");
    private static final Path BACKEND_ENV_EXAMPLE = Path.of("../backend/.env.example");
    private static final Path FRONTEND_ENV_EXAMPLE = Path.of("../frontend/.env.example");
    private static final Path ENV_DOC = Path.of("../docs/deployment/environment-variables.md");
    private static final Path DOCS_INDEX = Path.of("../docs/README.md");
    private static final Path DEVELOPER_SETUP = Path.of("../docs/development/developer-setup.md");
    private static final Path CI_CD_DOC = Path.of("../docs/deployment/ci-cd.md");

    @Nested
    @DisplayName("Template files")
    class TemplateFiles {

        @Test
        void rootEnvExampleDocumentsFullStackVariables() throws Exception {
            assertThat(ROOT_ENV_EXAMPLE).exists();
            String content = Files.readString(ROOT_ENV_EXAMPLE, StandardCharsets.UTF_8);

            assertThat(content)
                    .contains("688")
                    .contains("SPRING_PROFILES_ACTIVE=")
                    .contains("DB_URL=")
                    .contains("DB_USERNAME=")
                    .contains("DB_PASSWORD=")
                    .contains("JWT_SECRET=")
                    .contains("CORS_ALLOWED_ORIGINS=")
                    .contains("POSTGRES_DB=")
                    .contains("VITE_API_BASE_URL=")
                    .contains("HTTPS_REQUIRED=")
                    .contains("PROVIDER_REAL_SENDING_ENABLED=");
        }

        @Test
        void backendEnvExampleCoversRuntimeKeys() throws Exception {
            assertThat(BACKEND_ENV_EXAMPLE).exists();
            String content = Files.readString(BACKEND_ENV_EXAMPLE, StandardCharsets.UTF_8);

            assertThat(content)
                    .contains("688")
                    .contains("SPRING_PROFILES_ACTIVE=")
                    .contains("DB_URL=")
                    .contains("JWT_SECRET=")
                    .contains("CORS_ALLOWED_ORIGINS=")
                    .contains("SMTP_PASSWORD=")
                    .contains("SMS_API_KEY=")
                    .contains("FILE_STORAGE_LOCAL_PATH=");
        }

        @Test
        void frontendEnvExampleCoversViteKeys() throws Exception {
            assertThat(FRONTEND_ENV_EXAMPLE).exists();
            String content = Files.readString(FRONTEND_ENV_EXAMPLE, StandardCharsets.UTF_8);

            assertThat(content)
                    .contains("688")
                    .contains("VITE_API_BASE_URL=")
                    .contains("VITE_APP_ENV=");
        }

        @Test
        void templatesDoNotEmbedProductionSecretValues() throws Exception {
            String root = Files.readString(ROOT_ENV_EXAMPLE, StandardCharsets.UTF_8);
            String backend = Files.readString(BACKEND_ENV_EXAMPLE, StandardCharsets.UTF_8);

            // Templates may document placeholder names, not real production material.
            assertThat(root).doesNotContain("-----BEGIN");
            assertThat(backend).doesNotContain("-----BEGIN");
            assertThat(root).doesNotContain("production-jwt-secret-32chars-min!!");
            assertThat(backend).doesNotContain("production-jwt-secret-32chars-min!!");
        }
    }

    @Nested
    @DisplayName("Documentation")
    class Documentation {

        @Test
        void environmentVariablesDocCatalogsTemplatesForItem688() throws Exception {
            assertThat(ENV_DOC).exists();
            String doc = Files.readString(ENV_DOC, StandardCharsets.UTF_8);

            assertThat(doc)
                    .contains("688")
                    .contains("environment variable")
                    .contains(".env.example")
                    .contains("backend/.env.example")
                    .contains("frontend/.env.example")
                    .contains("JWT_SECRET")
                    .contains("DB_URL")
                    .contains("VITE_API_BASE_URL")
                    .contains("EnvironmentVariableTemplateDocumentationTests");
        }

        @Test
        void docsIndexAndSetupLinkEnvironmentTemplate() throws Exception {
            assertThat(DOCS_INDEX).exists();
            String index = Files.readString(DOCS_INDEX, StandardCharsets.UTF_8);
            assertThat(index).contains("deployment/environment-variables.md");

            assertThat(DEVELOPER_SETUP).exists();
            String setup = Files.readString(DEVELOPER_SETUP, StandardCharsets.UTF_8);
            assertThat(setup).contains(".env.example");

            assertThat(CI_CD_DOC).exists();
            String ci = Files.readString(CI_CD_DOC, StandardCharsets.UTF_8);
            assertThat(ci).contains("688").contains("environment-variables.md");
            assertThat(ci).contains("689").contains("secrets.md");
        }
    }
}
