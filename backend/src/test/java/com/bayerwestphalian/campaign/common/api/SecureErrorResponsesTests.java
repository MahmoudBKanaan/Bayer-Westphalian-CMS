package com.bayerwestphalian.campaign.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("538 Secure error responses")
class SecureErrorResponsesTests {

    private SecureErrorResponses secureErrorResponses;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        secureErrorResponses = new SecureErrorResponses(objectMapper);
    }

    @Test
    void writesStructuredJsonWithoutStackTraceFields() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/customers");
        request.addHeader("X-Request-Id", "req-538");
        MockHttpServletResponse response = new MockHttpServletResponse();

        secureErrorResponses.write(
                request,
                response,
                HttpStatus.FORBIDDEN,
                "ACCESS_DENIED",
                "Role is not allowed to perform this action");

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).contains("application/json");
        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(body.get("status").asInt()).isEqualTo(403);
        assertThat(body.get("code").asText()).isEqualTo("ACCESS_DENIED");
        assertThat(body.get("message").asText())
                .isEqualTo("Role is not allowed to perform this action");
        assertThat(body.get("path").asText()).isEqualTo("/api/customers");
        assertThat(body.get("requestId").asText()).isEqualTo("req-538");
        assertThat(body.has("stackTrace")).isFalse();
        assertThat(body.has("exception")).isFalse();
        assertThat(body.has("trace")).isFalse();
    }

    @Test
    void redactsSensitiveValidationRejectedValues() {
        assertThat(SecureErrorResponses.sanitizeRejectedValue("password", "Secret123!"))
                .isEqualTo(SecureErrorResponses.REDACTED_VALUE);
        assertThat(SecureErrorResponses.sanitizeRejectedValue("refreshToken", "jwt.payload"))
                .isEqualTo(SecureErrorResponses.REDACTED_VALUE);
        assertThat(SecureErrorResponses.sanitizeRejectedValue("apiKey", "key-value"))
                .isEqualTo(SecureErrorResponses.REDACTED_VALUE);
        assertThat(SecureErrorResponses.sanitizeRejectedValue("email", "user@example.test"))
                .isEqualTo("user@example.test");
        assertThat(SecureErrorResponses.isSensitiveField("temporaryPassword")).isTrue();
        assertThat(SecureErrorResponses.isSensitiveField("fullName")).isFalse();
    }
}
