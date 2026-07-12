package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.auth.JwtAuthenticationFilter;
import com.bayerwestphalian.campaign.auth.JwtService;
import com.bayerwestphalian.campaign.auth.JwtTokenClaims;
import com.bayerwestphalian.campaign.auth.JwtTokenType;
import com.bayerwestphalian.campaign.auth.SecurityConfiguration;
import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import com.bayerwestphalian.campaign.common.exception.BusinessRuleException;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * KB item 221 / FR-052: campaign product selection via {@code PUT/GET /api/campaigns/{id}/products}.
 */
@WebMvcTest(controllers = CampaignController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class CampaignProductSelectionTests {

    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID OWNER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000101");
    private static final UUID PRODUCT_ID =
            UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID PRODUCT_ID_2 =
            UUID.fromString("40000000-0000-0000-0000-000000000002");
    private static final Instant CREATED_AT = Instant.parse("2026-07-09T10:15:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-07-09T11:00:00Z");

    @Autowired private MockMvc mockMvc;

    @MockBean private CampaignService campaignService;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void campaignManagerCanSelectPromotedProducts() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(campaignService.selectProducts(eq(CAMPAIGN_ID), any(SelectCampaignProductsCommand.class)))
                .thenReturn(viewWithProducts(List.of(PRODUCT_ID, PRODUCT_ID_2)));

        mockMvc.perform(
                        put("/api/campaigns/{id}/products", CAMPAIGN_ID)
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "productIds": ["%s", "%s"]
                                        }
                                        """
                                                .formatted(PRODUCT_ID, PRODUCT_ID_2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Campaign products updated"))
                .andExpect(jsonPath("$.data.productIds.length()").value(2))
                .andExpect(jsonPath("$.data.productIds[0]").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.data.productIds[1]").value(PRODUCT_ID_2.toString()));

        ArgumentCaptor<SelectCampaignProductsCommand> commandCaptor =
                ArgumentCaptor.forClass(SelectCampaignProductsCommand.class);
        verify(campaignService).selectProducts(eq(CAMPAIGN_ID), commandCaptor.capture());
        assertThat(commandCaptor.getValue().productIds())
                .containsExactly(PRODUCT_ID, PRODUCT_ID_2);
    }

    @Test
    void campaignManagerCanClearProductSelectionWithEmptyList() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(campaignService.selectProducts(eq(CAMPAIGN_ID), any(SelectCampaignProductsCommand.class)))
                .thenReturn(viewWithProducts(List.of()));

        mockMvc.perform(
                        put("/api/campaigns/{id}/products", CAMPAIGN_ID)
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "productIds": []
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Campaign products updated"))
                .andExpect(jsonPath("$.data.productIds.length()").value(0));

        ArgumentCaptor<SelectCampaignProductsCommand> commandCaptor =
                ArgumentCaptor.forClass(SelectCampaignProductsCommand.class);
        verify(campaignService).selectProducts(eq(CAMPAIGN_ID), commandCaptor.capture());
        assertThat(commandCaptor.getValue().productIds()).isEmpty();
    }

    @Test
    void campaignManagerCanListSelectedProducts() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(campaignService.listSelectedProductIds(CAMPAIGN_ID))
                .thenReturn(List.of(PRODUCT_ID, PRODUCT_ID_2));

        mockMvc.perform(
                        get("/api/campaigns/{id}/products", CAMPAIGN_ID)
                                .header("Authorization", "Bearer campaign-manager-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Campaign products loaded"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0]").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.data[1]").value(PRODUCT_ID_2.toString()));

        verify(campaignService).listSelectedProductIds(CAMPAIGN_ID);
    }

    @Test
    void adminCanSelectCampaignProducts() throws Exception {
        when(jwtService.validateToken("admin-token", JwtTokenType.ACCESS)).thenReturn(adminClaims());
        when(campaignService.selectProducts(eq(CAMPAIGN_ID), any(SelectCampaignProductsCommand.class)))
                .thenReturn(viewWithProducts(List.of(PRODUCT_ID)));

        mockMvc.perform(
                        put("/api/campaigns/{id}/products", CAMPAIGN_ID)
                                .header("Authorization", "Bearer admin-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "productIds": ["%s"]
                                        }
                                        """
                                                .formatted(PRODUCT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Campaign products updated"));

        verify(campaignService)
                .selectProducts(eq(CAMPAIGN_ID), any(SelectCampaignProductsCommand.class));
    }

    @Test
    void biAnalystCannotSelectCampaignProducts() throws Exception {
        when(jwtService.validateToken("bi-analyst-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.BI_ANALYST));

        mockMvc.perform(
                        put("/api/campaigns/{id}/products", CAMPAIGN_ID)
                                .header("Authorization", "Bearer bi-analyst-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "productIds": ["%s"]
                                        }
                                        """
                                                .formatted(PRODUCT_ID)))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("Campaign products updated"))));

        verify(campaignService, never())
                .selectProducts(any(UUID.class), any(SelectCampaignProductsCommand.class));
    }

    @Test
    void productManagerCannotSelectCampaignProducts() throws Exception {
        when(jwtService.validateToken("product-manager-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.PRODUCT_MANAGER));

        mockMvc.perform(
                        put("/api/campaigns/{id}/products", CAMPAIGN_ID)
                                .header("Authorization", "Bearer product-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "productIds": ["%s"]
                                        }
                                        """
                                                .formatted(PRODUCT_ID)))
                .andExpect(status().isForbidden());

        verify(campaignService, never())
                .selectProducts(any(UUID.class), any(SelectCampaignProductsCommand.class));
    }

    @Test
    void rejectsMissingProductIdsBody() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());

        mockMvc.perform(
                        put("/api/campaigns/{id}/products", CAMPAIGN_ID)
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verify(campaignService, never())
                .selectProducts(any(UUID.class), any(SelectCampaignProductsCommand.class));
    }

    @Test
    void returnsNotFoundWhenCampaignMissingForProductSelection() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(campaignService.selectProducts(eq(CAMPAIGN_ID), any(SelectCampaignProductsCommand.class)))
                .thenThrow(new ResourceNotFoundException("Campaign", CAMPAIGN_ID));

        mockMvc.perform(
                        put("/api/campaigns/{id}/products", CAMPAIGN_ID)
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "productIds": ["%s"]
                                        }
                                        """
                                                .formatted(PRODUCT_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void returnsBusinessRuleWhenProductsChangedOnSubmittedCampaign() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(campaignService.selectProducts(eq(CAMPAIGN_ID), any(SelectCampaignProductsCommand.class)))
                .thenThrow(
                        new BusinessRuleException(
                                "CAMPAIGN_LIFECYCLE",
                                "Campaign targeting (segment/products) cannot be changed in status SUBMITTED; only DRAFT or REJECTED"));

        mockMvc.perform(
                        put("/api/campaigns/{id}/products", CAMPAIGN_ID)
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "productIds": ["%s"]
                                        }
                                        """
                                                .formatted(PRODUCT_ID)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CAMPAIGN_LIFECYCLE"));
    }

    @Test
    void returnsValidationErrorWhenProductInactive() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(campaignService.selectProducts(eq(CAMPAIGN_ID), any(SelectCampaignProductsCommand.class)))
                .thenThrow(
                        new ValidationException(
                                "Campaign product validation failed",
                                List.of(
                                        "productIds: product "
                                                + PRODUCT_ID
                                                + " is not active or is deleted")));

        mockMvc.perform(
                        put("/api/campaigns/{id}/products", CAMPAIGN_ID)
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "productIds": ["%s"]
                                        }
                                        """
                                                .formatted(PRODUCT_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createAndUpdatePayloadsSupportProductSelectionFields() {
        CreateCampaignCommand create =
                new CreateCampaignRequest(
                                "Name",
                                "Objective",
                                null,
                                CampaignChannel.EMAIL,
                                null,
                                null,
                                null,
                                null,
                                List.of(PRODUCT_ID, PRODUCT_ID_2))
                        .toCommand();
        UpdateCampaignCommand update =
                new UpdateCampaignRequest(
                                "Name",
                                "Objective",
                                null,
                                CampaignChannel.EMAIL,
                                null,
                                null,
                                null,
                                null,
                                List.of(PRODUCT_ID))
                        .toCommand();

        assertThat(create.productIds()).containsExactly(PRODUCT_ID, PRODUCT_ID_2);
        assertThat(update.productIds()).containsExactly(PRODUCT_ID);
    }

    private static JwtTokenClaims campaignManagerClaims() {
        return new JwtTokenClaims(
                OWNER_ID,
                "campaign.manager@bayer-westphalian.test",
                List.of(SystemRoleName.CAMPAIGN_MANAGER));
    }

    private static JwtTokenClaims adminClaims() {
        return new JwtTokenClaims(
                UUID.fromString("10000000-0000-0000-0000-000000009901"),
                "admin@bayer-westphalian.test",
                List.of(SystemRoleName.ADMIN));
    }

    private static JwtTokenClaims roleClaims(SystemRoleName role) {
        return new JwtTokenClaims(
                UUID.fromString("10000000-0000-0000-0000-000000009902"),
                role.name().toLowerCase().replace('_', '.') + "@bayer-westphalian.test",
                List.of(role));
    }

    private static CampaignView viewWithProducts(List<UUID> productIds) {
        return new CampaignView(
                CAMPAIGN_ID,
                "Life renewal outreach",
                "Promote life insurance renewals",
                CampaignStatus.DRAFT,
                OWNER_ID,
                "Campaign Manager",
                null,
                null,
                CampaignChannel.EMAIL,
                "Renew your cover",
                "Dear customer, ...",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                null,
                null,
                null,
                null,
                productIds,
                CREATED_AT,
                UPDATED_AT);
    }
}
