package com.bayerwestphalian.campaign.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.auth.JwtAuthenticationFilter;
import com.bayerwestphalian.campaign.auth.JwtService;
import com.bayerwestphalian.campaign.auth.JwtTokenClaims;
import com.bayerwestphalian.campaign.auth.JwtTokenType;
import com.bayerwestphalian.campaign.auth.SecurityConfiguration;
import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import java.math.BigDecimal;
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

@WebMvcTest(controllers = PaymentRecordController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class PaymentRecordCanBeMarkedPaidTests {

    private static final UUID PAYMENT_ID = UUID.fromString("42000000-0000-0000-0000-000000000001");
    private static final UUID OWNERSHIP_ID =
            UUID.fromString("41000000-0000-0000-0000-000000000001");
    private static final UUID CUSTOMER_ID = UUID.fromString("41000000-0000-0000-0000-000000000101");
    private static final UUID PRODUCT_ID = UUID.fromString("41000000-0000-0000-0000-000000000201");
    private static final LocalDate DUE_DATE = LocalDate.parse("2026-07-15");
    private static final Instant PAID_AT = Instant.parse("2026-07-10T09:30:00Z");

    @Autowired private MockMvc mockMvc;

    @MockBean private PaymentRecordService paymentRecordService;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void customerServiceAgentCanMarkPaymentRecordPaid() throws Exception {
        when(jwtService.validateToken("agent-token", JwtTokenType.ACCESS))
                .thenReturn(customerServiceAgentClaims());
        when(paymentRecordService.markPaid(eq(PAYMENT_ID), any(MarkPaymentPaidCommand.class)))
                .thenReturn(paidPaymentView());

        mockMvc.perform(
                        patch("/api/payment-records/{id}/mark-paid", PAYMENT_ID)
                                .header("Authorization", "Bearer agent-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(markPaidPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Payment record marked paid"))
                .andExpect(jsonPath("$.data.id").value(PAYMENT_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.amountPaid").value(129.99))
                .andExpect(jsonPath("$.data.paidAt").value("2026-07-10T09:30:00Z"));

        ArgumentCaptor<MarkPaymentPaidCommand> commandCaptor =
                ArgumentCaptor.forClass(MarkPaymentPaidCommand.class);
        verify(paymentRecordService).markPaid(eq(PAYMENT_ID), commandCaptor.capture());
        assertThat(commandCaptor.getValue().amountPaid()).isEqualByComparingTo("129.99");
        assertThat(commandCaptor.getValue().paidAt()).isEqualTo(PAID_AT);
    }

    @Test
    void adminCanMarkPaymentRecordPaid() throws Exception {
        when(jwtService.validateToken("admin-token", JwtTokenType.ACCESS))
                .thenReturn(adminClaims());
        when(paymentRecordService.markPaid(eq(PAYMENT_ID), any(MarkPaymentPaidCommand.class)))
                .thenReturn(paidPaymentView());

        mockMvc.perform(
                        patch("/api/payment-records/{id}/mark-paid", PAYMENT_ID)
                                .header("Authorization", "Bearer admin-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(markPaidPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Payment record marked paid"))
                .andExpect(jsonPath("$.data.customerFullName").value("Ada Payer"))
                .andExpect(jsonPath("$.data.productName").value("Life Protection"))
                .andExpect(jsonPath("$.data.dueDate").value(DUE_DATE.toString()))
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.defaultRisk").value(false));

        verify(paymentRecordService).markPaid(eq(PAYMENT_ID), any(MarkPaymentPaidCommand.class));
    }

    @Test
    void campaignManagerCannotMarkPaymentRecordPaid() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());

        mockMvc.perform(
                        patch("/api/payment-records/{id}/mark-paid", PAYMENT_ID)
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(markPaidPayload()))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("Payment record marked paid"))));
    }

    @Test
    void productManagerCannotMarkPaymentRecordPaid() throws Exception {
        when(jwtService.validateToken("product-manager-token", JwtTokenType.ACCESS))
                .thenReturn(productManagerClaims());

        mockMvc.perform(
                        patch("/api/payment-records/{id}/mark-paid", PAYMENT_ID)
                                .header("Authorization", "Bearer product-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(markPaidPayload()))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("Payment record marked paid"))));
    }

    @Test
    void savedPaidPaymentRecordIsReturnedWhenListingCustomerPayments() throws Exception {
        when(jwtService.validateToken("agent-token", JwtTokenType.ACCESS))
                .thenReturn(customerServiceAgentClaims());
        when(paymentRecordService.searchPayments(any(PaymentRecordSearchCriteria.class)))
                .thenReturn(List.of(paidPaymentView()));

        mockMvc.perform(
                        get("/api/payment-records")
                                .header("Authorization", "Bearer agent-token")
                                .param("customerId", CUSTOMER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Payment records loaded"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(PAYMENT_ID.toString()))
                .andExpect(jsonPath("$.data[0].status").value("PAID"))
                .andExpect(jsonPath("$.data[0].amountPaid").value(129.99))
                .andExpect(jsonPath("$.data[0].paidAt").value("2026-07-10T09:30:00Z"));

        verify(paymentRecordService).searchPayments(any(PaymentRecordSearchCriteria.class));
    }

    private static JwtTokenClaims customerServiceAgentClaims() {
        return new JwtTokenClaims(
                UUID.fromString("10000000-0000-0000-0000-000000009905"),
                "customer.service@bayer-westphalian.test",
                List.of(SystemRoleName.CUSTOMER_SERVICE_AGENT));
    }

    private static JwtTokenClaims adminClaims() {
        return new JwtTokenClaims(
                UUID.fromString("10000000-0000-0000-0000-000000009901"),
                "admin@bayer-westphalian.test",
                List.of(SystemRoleName.ADMIN));
    }

    private static JwtTokenClaims campaignManagerClaims() {
        return new JwtTokenClaims(
                UUID.fromString("10000000-0000-0000-0000-000000009902"),
                "campaign.manager@bayer-westphalian.test",
                List.of(SystemRoleName.CAMPAIGN_MANAGER));
    }

    private static JwtTokenClaims productManagerClaims() {
        return new JwtTokenClaims(
                UUID.fromString("10000000-0000-0000-0000-000000009904"),
                "product.manager@bayer-westphalian.test",
                List.of(SystemRoleName.PRODUCT_MANAGER));
    }

    private static String markPaidPayload() {
        return """
                {
                  "amountPaid": 129.99,
                  "paidAt": "2026-07-10T09:30:00Z"
                }
                """;
    }

    private static PaymentRecordView paidPaymentView() {
        return new PaymentRecordView(
                PAYMENT_ID,
                CUSTOMER_ID,
                "Ada Payer",
                OWNERSHIP_ID,
                PRODUCT_ID,
                "Life Protection",
                ProductType.LIFE_INSURANCE,
                DUE_DATE,
                PAID_AT,
                new BigDecimal("129.99"),
                new BigDecimal("129.99"),
                PaymentStatus.PAID,
                0,
                0,
                false);
    }
}
