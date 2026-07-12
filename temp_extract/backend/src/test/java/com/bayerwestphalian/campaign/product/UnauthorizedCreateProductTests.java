package com.bayerwestphalian.campaign.product;

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

@WebMvcTest(controllers = ProductController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class UnauthorizedCreateProductTests {

    @Autowired private MockMvc mockMvc;

    @MockBean private ProductService productService;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void unauthenticatedUserCannotCreateProduct() throws Exception {
        mockMvc.perform(
                        post("/api/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createProductPayload()))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(not(containsString("Product created"))));

        verifyNoInteractions(productService);
    }

    @Test
    void biAnalystCannotCreateProduct() throws Exception {
        assertUnauthorizedRoleCannotCreateProduct(SystemRoleName.BI_ANALYST, "bi-analyst-token");
    }

    @Test
    void campaignManagerCannotCreateProduct() throws Exception {
        assertUnauthorizedRoleCannotCreateProduct(
                SystemRoleName.CAMPAIGN_MANAGER, "campaign-manager-token");
    }

    @Test
    void customerServiceAgentCannotCreateProduct() throws Exception {
        assertUnauthorizedRoleCannotCreateProduct(
                SystemRoleName.CUSTOMER_SERVICE_AGENT, "customer-service-token");
    }

    @Test
    void complianceOfficerCannotCreateProduct() throws Exception {
        assertUnauthorizedRoleCannotCreateProduct(
                SystemRoleName.COMPLIANCE_OFFICER, "compliance-token");
    }

    private void assertUnauthorizedRoleCannotCreateProduct(
            SystemRoleName role, String token) throws Exception {
        when(jwtService.validateToken(token, JwtTokenType.ACCESS)).thenReturn(roleClaims(role));

        mockMvc.perform(
                        post("/api/products")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createProductPayload()))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("Product created"))));

        verify(productService, never()).createProduct(org.mockito.ArgumentMatchers.any());
    }

    private static JwtTokenClaims roleClaims(SystemRoleName role) {
        return new JwtTokenClaims(
                UUID.fromString("10000000-0000-0000-0000-000000009903"),
                "unauthorized.user@bayer-westphalian.test",
                List.of(role));
    }

    private static String createProductPayload() {
        return """
                {
                  "name": "Unauthorized Product",
                  "productType": "LIFE_INSURANCE",
                  "description": "Should not be created",
                  "price": 99.99,
                  "durationMonths": 12,
                  "expirationPolicy": "Annual renewal"
                }
                """;
    }
}