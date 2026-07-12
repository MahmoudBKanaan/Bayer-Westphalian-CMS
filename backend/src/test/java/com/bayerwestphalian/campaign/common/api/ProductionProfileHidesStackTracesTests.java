package com.bayerwestphalian.campaign.common.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * Sprint 16 critical test item <b>664</b>: Production profile hides stack traces.
 *
 * <p>KB rules:
 *
 * <ul>
 *   <li>{@code NFR-001} / {@code NFR-014} — secure errors; observability without client leakage
 *   <li>Items 539 / 554 — production never exposes stack traces or exception details to clients
 *   <li>{@code application-prod.yml} forces {@code server.error.include-stacktrace: never}
 *   <li>{@link ProductionErrorSafetyConfiguration} strips forbidden keys under {@code prod}
 *   <li>{@link GlobalExceptionHandler} returns safe {@code INTERNAL_ERROR} JSON for unexpected
 *       failures
 * </ul>
 *
 * <p>Companion: {@link ProductionStackTraceHiddenTests} (items 539 / 554).
 */
@DisplayName("664 Production profile hides stack traces")
class ProductionProfileHidesStackTracesTests {

    private static final Path PROD_YAML = Path.of("src/main/resources/application-prod.yml");
    private static final Path SECURITY_HARDENING_DOC =
            Path.of("../docs/architecture/security-hardening.md");
    private static final Path PRODUCTION_CHECKLIST_DOC =
            Path.of("../docs/deployment/production-security-checklist.md");

    @Nested
    @DisplayName("application-prod.yml")
    class ProductionYaml {

        @Test
        void productionProfileNeverIncludesStackTracesOrExceptionDetails() throws Exception {
            String yaml = Files.readString(PROD_YAML, StandardCharsets.UTF_8);

            assertThat(yaml)
                    .contains("include-stacktrace: never")
                    .contains("include-message: never")
                    .contains("include-binding-errors: never")
                    .contains("include-exception: false");
            assertThat(yaml).doesNotContain("include-stacktrace: always");
            assertThat(yaml).doesNotContain("include-stacktrace: on_param");
            assertThat(yaml).doesNotContain("include-exception: true");
        }
    }

    @Nested
    @DisplayName("ProductionErrorSafetyConfiguration (prod profile only)")
    class ProductionErrorAttributes {

        @Test
        void errorAttributesBeanIsActiveOnlyUnderProdProfile() {
            try (AnnotationConfigApplicationContext context =
                    new AnnotationConfigApplicationContext()) {
                ConfigurableEnvironment environment = new StandardEnvironment();
                environment.setActiveProfiles("prod");
                context.setEnvironment(environment);
                context.register(ProductionErrorSafetyConfiguration.class);
                context.refresh();

                assertThat(context.getBeansOfType(ErrorAttributes.class)).isNotEmpty();
            }

            try (AnnotationConfigApplicationContext context =
                    new AnnotationConfigApplicationContext()) {
                ConfigurableEnvironment environment = new StandardEnvironment();
                environment.setActiveProfiles("dev");
                context.setEnvironment(environment);
                context.register(ProductionErrorSafetyConfiguration.class);
                context.refresh();

                assertThat(context.getBeansOfType(ErrorAttributes.class)).isEmpty();
            }
        }

        @Test
        void sanitizeForProductionRemovesStackTraceAndExceptionKeys() {
            Map<String, Object> raw = new LinkedHashMap<>();
            raw.put("timestamp", "2026-07-12T12:00:00Z");
            raw.put("status", 500);
            raw.put("error", "Internal Server Error");
            raw.put("message", "NullPointerException at com.example.Foo");
            raw.put("exception", "java.lang.NullPointerException");
            raw.put("trace", "java.lang.NullPointerException\n\tat com.example.Foo.bar(Foo.java:1)");
            raw.put("stackTrace", "should-not-leak");
            raw.put("path", "/api/boom");
            raw.put("errors", "binding dump");

            Map<String, Object> sanitized =
                    ProductionErrorSafetyConfiguration.sanitizeForProduction(raw);

            assertThat(sanitized)
                    .containsKeys("timestamp", "status", "error")
                    .doesNotContainKeys(
                            "message", "exception", "trace", "stackTrace", "path", "errors");
            assertThat(sanitized.values().stream().map(String::valueOf).toList())
                    .noneMatch(value -> value.contains("NullPointerException"))
                    .noneMatch(value -> value.contains("com.example.Foo"));
        }

        @Test
        void productionErrorAttributesOmitTraceEvenWhenOptionsWouldAllowIt() {
            ProductionErrorSafetyConfiguration configuration =
                    new ProductionErrorSafetyConfiguration();
            ErrorAttributes errorAttributes = configuration.productionSafeErrorAttributes();

            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/error");
            request.setAttribute(
                    "jakarta.servlet.error.exception",
                    new IllegalStateException("secret boom details for item 664"));
            request.setAttribute("jakarta.servlet.error.status_code", 500);

            Map<String, Object> attrs =
                    errorAttributes.getErrorAttributes(
                            new ServletWebRequest(request),
                            ErrorAttributeOptions.of(
                                    ErrorAttributeOptions.Include.STACK_TRACE,
                                    ErrorAttributeOptions.Include.EXCEPTION,
                                    ErrorAttributeOptions.Include.MESSAGE));

            assertThat(attrs).doesNotContainKeys("trace", "exception", "message", "stackTrace");
            assertThat(attrs.toString()).doesNotContain("secret boom details");
            assertThat(attrs.toString()).doesNotContain("IllegalStateException");
        }

        @Test
        void forbiddenClientKeysIncludeStackTraceMarkers() {
            assertThat(ProductionErrorSafetyConfiguration.isForbiddenClientErrorKey("trace"))
                    .isTrue();
            assertThat(ProductionErrorSafetyConfiguration.isForbiddenClientErrorKey("stackTrace"))
                    .isTrue();
            assertThat(ProductionErrorSafetyConfiguration.isForbiddenClientErrorKey("exception"))
                    .isTrue();
            assertThat(ProductionErrorSafetyConfiguration.isForbiddenClientErrorKey("status"))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("API ErrorResponse JSON never leaks stack traces")
    class ApiJsonNeverContainsStackTrace {

        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
            mockMvc =
                    MockMvcBuilders.standaloneSetup(new BoomController())
                            .setControllerAdvice(new GlobalExceptionHandler())
                            .build();
        }

        @Test
        void unexpectedExceptionBodyHasNoStackTraceFieldsOrInternalMessage() throws Exception {
            mockMvc.perform(get("/test/boom-664"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                    .andExpect(jsonPath("$.message").value("Unexpected server error"))
                    .andExpect(jsonPath("$.stackTrace").doesNotExist())
                    .andExpect(jsonPath("$.trace").doesNotExist())
                    .andExpect(jsonPath("$.exception").doesNotExist());

            String body =
                    mockMvc.perform(get("/test/boom-664"))
                            .andReturn()
                            .getResponse()
                            .getContentAsString();
            assertThat(body)
                    .doesNotContain("at com.")
                    .doesNotContain("IllegalStateException")
                    .doesNotContain("production-must-not-see-this-664");
        }

        @Test
        void productionErrorDoesNotExposeStackTraceEvenWhenTraceIsRequested() throws Exception {
            var result =
                    mockMvc.perform(
                                    get("/test/boom-664")
                                            .param("trace", "true")
                                            .header("X-Request-Id", "req-664"))
                            .andExpect(status().isInternalServerError())
                            .andExpect(jsonPath("$.status").value(500))
                            .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                            .andExpect(jsonPath("$.message").value("Unexpected server error"))
                            .andExpect(jsonPath("$.requestId").value("req-664"))
                            .andExpect(jsonPath("$.details").isEmpty())
                            .andExpect(jsonPath("$.stackTrace").doesNotExist())
                            .andExpect(jsonPath("$.trace").doesNotExist())
                            .andExpect(jsonPath("$.exception").doesNotExist())
                            .andReturn();

            assertThat(result.getResponse().getContentAsString())
                    .doesNotContain("java.lang")
                    .doesNotContain("IllegalStateException")
                    .doesNotContain("production-must-not-see-this-664")
                    .doesNotContain("\tat ");
        }

        @RestController
        private static final class BoomController {
            @GetMapping("/test/boom-664")
            String boom() {
                throw new IllegalStateException("production-must-not-see-this-664");
            }
        }
    }

    @Nested
    @DisplayName("Documentation")
    class Docs {

        @Test
        void securityHardeningAndChecklistDocumentProductionStackTraceHiding() throws Exception {
            assertThat(SECURITY_HARDENING_DOC).exists();
            String hardening = Files.readString(SECURITY_HARDENING_DOC, StandardCharsets.UTF_8);
            assertThat(hardening)
                    .contains("664")
                    .contains("ProductionProfileHidesStackTracesTests")
                    .containsIgnoringCase("stack")
                    .contains("prod")
                    .contains("include-stacktrace: never");

            assertThat(PRODUCTION_CHECKLIST_DOC).exists();
            String checklist =
                    Files.readString(PRODUCTION_CHECKLIST_DOC, StandardCharsets.UTF_8);
            assertThat(checklist)
                    .contains("include-stacktrace: never")
                    .containsIgnoringCase("stack");
        }
    }
}
