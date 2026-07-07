package com.bayerwestphalian.campaign.common.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.common.exception.BusinessRuleException;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTests {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(new TestController())
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    @Test
    void mapsApplicationExceptionToErrorResponse() throws Exception {
        mockMvc.perform(get("/test/not-found").header("X-Request-Id", "request-123"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Customer was not found: customer-123"))
                .andExpect(jsonPath("$.path").value("/test/not-found"))
                .andExpect(jsonPath("$.requestId").value("request-123"));
    }

    @Test
    void mapsBusinessRuleExceptionToUnprocessableEntity() throws Exception {
        mockMvc.perform(get("/test/business-rule"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value("CONSENT_REQUIRED"))
                .andExpect(jsonPath("$.message").value("Consent is required before contact"));
    }

    @Test
    void mapsValidationFailureToErrorResponse() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/test/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0]").value("name: must not be blank"))
                .andExpect(jsonPath("$.validationErrors[0].field").value("name"))
                .andExpect(jsonPath("$.validationErrors[0].message").value("must not be blank"))
                .andExpect(jsonPath("$.validationErrors[0].objectName").value("testRequest"));
    }

    @Test
    void mapsAccessDeniedToForbiddenResponse() throws Exception {
        mockMvc.perform(get("/test/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(
                        jsonPath("$.message").value("Role is not allowed to perform this action"));
    }

    @Test
    void mapsMissingAuthenticationToUnauthorizedResponse() throws Exception {
        mockMvc.perform(get("/test/unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    @Test
    void mapsUnexpectedExceptionToInternalErrorResponse() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Unexpected server error"));
    }

    private record TestRequest(@NotBlank String name) {}

    @RestController
    private static final class TestController {

        @GetMapping("/test/not-found")
        String notFound() {
            throw new ResourceNotFoundException("Customer", "customer-123");
        }

        @GetMapping("/test/business-rule")
        String businessRule() {
            throw new BusinessRuleException(
                    "CONSENT_REQUIRED", "Consent is required before contact");
        }

        @PostMapping("/test/validate")
        String validate(@Valid @RequestBody TestRequest request) {
            return request.name();
        }

        @GetMapping("/test/forbidden")
        String forbidden() {
            throw new AccessDeniedException("denied");
        }

        @GetMapping("/test/unauthorized")
        String unauthorized() {
            throw new AuthenticationCredentialsNotFoundException("missing");
        }

        @GetMapping("/test/unexpected")
        String unexpected() {
            throw new IllegalStateException("boom");
        }
    }
}
