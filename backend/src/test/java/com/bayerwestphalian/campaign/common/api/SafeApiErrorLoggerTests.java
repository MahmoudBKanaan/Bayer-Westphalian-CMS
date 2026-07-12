package com.bayerwestphalian.campaign.common.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bayerwestphalian.campaign.common.exception.UnauthorizedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

@DisplayName("546 API error logging without leaking secrets")
class SafeApiErrorLoggerTests {

    @Nested
    class SanitizeForLog {

        @Test
        void redactsBearerTokens() {
            String input = "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.aaa.bbb";
            String sanitized = SafeApiErrorLogger.sanitizeForLog(input);
            assertThat(sanitized).contains("Bearer " + SafeApiErrorLogger.REDACTED);
            assertThat(sanitized).doesNotContain("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9");
        }

        @Test
        void redactsJsonPasswordFields() {
            String input = "{\"email\":\"a@b.c\",\"password\":\"SuperSecret1!\"}";
            String sanitized = SafeApiErrorLogger.sanitizeForLog(input);
            assertThat(sanitized).contains("\"password\":\"" + SafeApiErrorLogger.REDACTED + "\"");
            assertThat(sanitized).doesNotContain("SuperSecret1!");
            assertThat(sanitized).contains("a@b.c");
        }

        @Test
        void redactsFormPasswordFields() {
            String input = "email=a@b.c&password=SuperSecret1!";
            assertThat(SafeApiErrorLogger.sanitizeForLog(input))
                    .contains("password=" + SafeApiErrorLogger.REDACTED)
                    .doesNotContain("SuperSecret1!");
        }

        @Test
        void redactsJwtLikeStrings() {
            String jwt =
                    "prefix eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U suffix";
            assertThat(SafeApiErrorLogger.sanitizeForLog(jwt))
                    .contains(SafeApiErrorLogger.REDACTED)
                    .doesNotContain("eyJhbGciOiJIUzI1NiJ9");
        }

        @Test
        void handlesNullAndEmpty() {
            assertThat(SafeApiErrorLogger.sanitizeForLog(null)).isEmpty();
            assertThat(SafeApiErrorLogger.sanitizeForLog("")).isEmpty();
        }
    }

    @Nested
    class SensitiveHeaders {

        @Test
        void flagsAuthorizationAndCookieHeaders() {
            assertThat(SafeApiErrorLogger.isSensitiveHeader("Authorization")).isTrue();
            assertThat(SafeApiErrorLogger.isSensitiveHeader("Cookie")).isTrue();
            assertThat(SafeApiErrorLogger.isSensitiveHeader("X-Api-Key")).isTrue();
            assertThat(SafeApiErrorLogger.isSensitiveHeader("X-Request-Id")).isFalse();
            assertThat(SafeApiErrorLogger.isSensitiveHeader("Content-Type")).isFalse();
        }
    }

    @Nested
    class StructuredLogging {

        @Test
        void requestContextOmitsAuthorizationHeader() {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
            request.addHeader("Authorization", "Bearer super-secret-token");
            request.addHeader("X-Request-Id", "req-546");

            String context = SafeApiErrorLogger.requestContext(request);

            assertThat(context)
                    .contains("method=POST")
                    .contains("path=/api/auth/login")
                    .contains("requestId=req-546")
                    .doesNotContain("Bearer")
                    .doesNotContain("super-secret-token");
        }

        @Test
        void logApplicationErrorDoesNotPassRawPasswordInMessage() {
            Logger logger = mock(Logger.class);
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
            request.addHeader("X-Request-Id", "req-1");
            UnauthorizedException exception =
                    new UnauthorizedException("Invalid password SuperSecret1! for user");

            SafeApiErrorLogger.logApplicationError(logger, exception, request);

            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(logger)
                    .warn(
                            anyString(),
                            eq("UNAUTHORIZED"),
                            eq(401),
                            any(),
                            messageCaptor.capture());
            assertThat(messageCaptor.getValue()).doesNotContain("SuperSecret1!");
        }

        @Test
        void logUnexpectedErrorSanitizesMessageButKeepsThrowable() {
            Logger logger = mock(Logger.class);
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/customers");
            Exception exception =
                    new IllegalStateException(
                            "Failed with Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.aaa.bbb");

            SafeApiErrorLogger.logUnexpectedError(logger, exception, request);

            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(logger)
                    .error(
                            anyString(),
                            eq(IllegalStateException.class.getName()),
                            any(),
                            messageCaptor.capture(),
                            eq(exception));
            assertThat(messageCaptor.getValue())
                    .contains(SafeApiErrorLogger.REDACTED)
                    .doesNotContain("eyJhbGciOiJIUzI1NiJ9");
        }

        @Test
        void logValidationErrorDoesNotLogRejectedValues() {
            Logger logger = mock(Logger.class);
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users");

            SafeApiErrorLogger.logValidationError(logger, request, 2);

            verify(logger).info(anyString(), any(), eq(2));
            verify(logger, never()).info(contains("password"), any(), any());
        }

        @Test
        void logApplicationErrorUsesErrorLevelFor5xx() {
            Logger logger = mock(Logger.class);
            ApplicationExceptionLike fiveHundred =
                    new ApplicationExceptionLike(
                            HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "fail");

            SafeApiErrorLogger.logApplicationError(
                    logger, fiveHundred, new MockHttpServletRequest("GET", "/api/x"));

            verify(logger).error(anyString(), eq("INTERNAL_ERROR"), eq(500), any(), any());
        }
    }

    /** Minimal ApplicationException for level testing. */
    private static final class ApplicationExceptionLike
            extends com.bayerwestphalian.campaign.common.exception.ApplicationException {
        ApplicationExceptionLike(HttpStatus status, String code, String message) {
            super(status, code, message);
        }
    }
}
