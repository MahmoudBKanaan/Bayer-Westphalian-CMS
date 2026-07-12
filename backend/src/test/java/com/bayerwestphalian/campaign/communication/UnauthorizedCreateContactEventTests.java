package com.bayerwestphalian.campaign.communication;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.auth.JwtAuthenticationFilter;
import com.bayerwestphalian.campaign.auth.JwtService;
import com.bayerwestphalian.campaign.auth.JwtTokenClaims;
import com.bayerwestphalian.campaign.auth.JwtTokenType;
import com.bayerwestphalian.campaign.auth.SecurityConfiguration;
import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CommunicationController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class UnauthorizedCreateContactEventTests {

    @Autowired private MockMvc mockMvc;

    @MockBean private CommunicationService communicationService;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void unauthenticatedUserCannotCreateContactEvent() throws Exception {
        mockMvc.perform(
                        post("/api/contact-events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createContactEventPayload()))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(not(containsString("Contact event recorded"))));

        verifyNoInteractions(communicationService);
    }

    @Test
    void biAnalystCannotCreateContactEvent() throws Exception {
        assertUnauthorizedRoleCannotCreateContactEvent(
                SystemRoleName.BI_ANALYST, "bi-analyst-token");
    }

    @Test
    void complianceOfficerCannotCreateContactEvent() throws Exception {
        assertUnauthorizedRoleCannotCreateContactEvent(
                SystemRoleName.COMPLIANCE_OFFICER, "compliance-token");
    }

    @Test
    void productManagerCannotCreateContactEvent() throws Exception {
        assertUnauthorizedRoleCannotCreateContactEvent(
                SystemRoleName.PRODUCT_MANAGER, "product-manager-token");
    }

    private void assertUnauthorizedRoleCannotCreateContactEvent(SystemRoleName role, String token)
            throws Exception {
        when(jwtService.validateToken(token, JwtTokenType.ACCESS)).thenReturn(roleClaims(role));

        mockMvc.perform(
                        post("/api/contact-events")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createContactEventPayload()))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("Contact event recorded"))));

        verify(communicationService, never())
                .recordContactEvent(org.mockito.ArgumentMatchers.any());
    }

    private static JwtTokenClaims roleClaims(SystemRoleName role) {
        return new JwtTokenClaims(
                UUID.fromString("10000000-0000-0000-0000-000000009903"),
                "unauthorized.user@bayer-westphalian.test",
                List.of(role));
    }

    private static String createContactEventPayload() {
        return """
                {
                  "customerId": "20000000-0000-0000-0000-000000000001",
                  "channel": "EMAIL",
                  "eventType": "SENT",
                  "notes": "Unauthorized note"
                }
                """;
    }
}
