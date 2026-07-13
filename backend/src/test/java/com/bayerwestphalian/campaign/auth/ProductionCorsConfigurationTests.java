package com.bayerwestphalian.campaign.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Item 540: production CORS is restricted to explicit configured origins (no wildcards, no
 * localhost defaults).
 */
@DisplayName("540 Production CORS configuration")
class ProductionCorsConfigurationTests {

    @Nested
    @DisplayName("validateProductionOrigins")
    class Validation {

        @Test
        void acceptsExplicitHttpsOrigins() {
            List<String> origins =
                    SecurityConfiguration.validateProductionOrigins(
                            List.of(
                                    "https://campaign.example.com",
                                    "https://www.example.com"));

            assertThat(origins)
                    .containsExactly(
                            "https://campaign.example.com", "https://www.example.com");
        }

        @Test
        void rejectsEmptyOrigins() {
            assertThatThrownBy(() -> SecurityConfiguration.validateProductionOrigins(List.of()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("CORS_ALLOWED_ORIGINS");
        }

        @Test
        void rejectsWildcardOrigin() {
            assertThatThrownBy(
                            () ->
                                    SecurityConfiguration.validateProductionOrigins(
                                            List.of("*")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("wildcard");
        }

        @Test
        void rejectsWildcardSubdomainPattern() {
            assertThatThrownBy(
                            () ->
                                    SecurityConfiguration.validateProductionOrigins(
                                            List.of("https://*.example.com")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("wildcard");
        }

        @Test
        void rejectsLocalhostOrigins() {
            assertThatThrownBy(
                            () ->
                                    SecurityConfiguration.validateProductionOrigins(
                                            List.of("http://localhost:5173")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("localhost");
            assertThatThrownBy(
                            () ->
                                    SecurityConfiguration.validateProductionOrigins(
                                            List.of("http://127.0.0.1:5173")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("localhost");
        }

        @Test
        void rejectsNonHttpsOrigins() {
            assertThatThrownBy(
                            () ->
                                    SecurityConfiguration.validateProductionOrigins(
                                            List.of("campaign.example.com")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("HTTPS");
            assertThatThrownBy(
                            () ->
                                    SecurityConfiguration.validateProductionOrigins(
                                            List.of("http://campaign.example.com")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("HTTPS");
        }

        @Test
        void rejectsOriginsWithPathsQueriesFragmentsOrUserInfo() {
            assertThatThrownBy(
                            () ->
                                    SecurityConfiguration.validateProductionOrigins(
                                            List.of("https://campaign.example.com/api")))
                    .hasMessageContaining("origins only");
            assertThatThrownBy(
                            () ->
                                    SecurityConfiguration.validateProductionOrigins(
                                            List.of("https://campaign.example.com?tenant=one")))
                    .hasMessageContaining("origins only");
            assertThatThrownBy(
                            () ->
                                    SecurityConfiguration.validateProductionOrigins(
                                            List.of("https://user@campaign.example.com")))
                    .hasMessageContaining("user info");
        }
    }

    @Nested
    @DisplayName("resolveAllowedOrigins under prod profile")
    class ResolveUnderProd {

        @Test
        void failsStartupWhenProductionOriginsMissing() {
            SecurityConfiguration configuration = new SecurityConfiguration();
            MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", "prod");

            assertThatThrownBy(() -> configuration.resolveAllowedOrigins(environment))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("CORS_ALLOWED_ORIGINS");
        }

        @Test
        void failsWhenProductionOriginsBlank() {
            SecurityConfiguration configuration = new SecurityConfiguration();
            MockEnvironment environment =
                    new MockEnvironment()
                            .withProperty("spring.profiles.active", "prod")
                            .withProperty("app.cors.allowed-origins", "  ,  ");

            assertThatThrownBy(() -> configuration.resolveAllowedOrigins(environment))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void buildsRestrictedCorsSourceForProduction() {
            SecurityConfiguration configuration = new SecurityConfiguration();
            MockEnvironment environment =
                    new MockEnvironment()
                            .withProperty("spring.profiles.active", "prod")
                            .withProperty(
                                    "app.cors.allowed-origins",
                                    "https://app.bayer-westphalian.example");

            CorsConfigurationSource source = configuration.corsConfigurationSource(environment);
            CorsConfiguration cors = source.getCorsConfiguration(new MockHttpServletRequest());

            assertThat(cors).isNotNull();
            assertThat(cors.getAllowedOrigins())
                    .containsExactly("https://app.bayer-westphalian.example");
            assertThat(cors.getAllowCredentials()).isTrue();
            assertThat(cors.getMaxAge()).isEqualTo(3600L);
            assertThat(cors.getAllowedMethods()).contains("OPTIONS", "GET", "POST");
        }

        @Test
        void isProductionProfileDetectsProd() {
            assertThat(
                            SecurityConfiguration.isProductionProfile(
                                    new MockEnvironment().withProperty("spring.profiles.active", "prod")))
                    .isTrue();
            assertThat(
                            SecurityConfiguration.isProductionProfile(
                                    new MockEnvironment().withProperty("spring.profiles.active", "dev")))
                    .isFalse();
            assertThat(SecurityConfiguration.isProductionProfile(new MockEnvironment()))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("configuration and documentation")
    class ConfigAndDocs {

        @Test
        void productionYmlRequiresCorsAllowedOriginsEnvironmentVariable() throws Exception {
            String yaml =
                    Files.readString(
                            Path.of("src/main/resources/application-prod.yml"),
                            StandardCharsets.UTF_8);

            assertThat(yaml)
                    .contains("cors:")
                    .contains("allowed-origins: ${CORS_ALLOWED_ORIGINS}")
                    .doesNotContain("localhost:5173");
            // No embedded localhost production default for CORS.
            assertThat(yaml).doesNotContain("CORS_ALLOWED_ORIGINS:http://localhost");
        }

        @Test
        void baseYmlKeepsDevLocalDefaults() throws Exception {
            String yaml =
                    Files.readString(
                            Path.of("src/main/resources/application.yml"), StandardCharsets.UTF_8);

            assertThat(yaml)
                    .contains("cors:")
                    .contains("CORS_ALLOWED_ORIGINS")
                    .contains("localhost:5173");
        }

        @Test
        void securityHardeningDocDescribesProductionCors() throws Exception {
            Path doc = Path.of("..", "docs", "architecture", "security-hardening.md");
            assertThat(doc).exists();
            String content = Files.readString(doc, StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("540")
                    .contains("CORS")
                    .contains("CORS_ALLOWED_ORIGINS")
                    .containsIgnoringCase("production");
        }
    }
}
