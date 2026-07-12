package com.bayerwestphalian.campaign.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class PaymentRecordCanBeCreatedTests {

    private static final UUID PAYMENT_ID = UUID.fromString("42000000-0000-0000-0000-000000000001");
    private static final UUID OWNERSHIP_ID =
            UUID.fromString("41000000-0000-0000-0000-000000000001");
    private static final UUID CUSTOMER_ID = UUID.fromString("41000000-0000-0000-0000-000000000101");
    private static final UUID PRODUCT_ID = UUID.fromString("41000000-0000-0000-0000-000000000201");
    private static final LocalDate DUE_DATE = LocalDate.parse("2026-08-01");

    @Autowired private MockMvc mockMvc;

    @MockBean private PaymentRecordService paymentRecordService;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void customerServiceAgentCanCreatePaymentRecord() throws Exception {
        when(jwtService.validateToken("agent-token", JwtTokenType.ACCESS))
                .thenReturn(customerServiceAgentClaims());
        when(paymentRecordService.createPaymentRecord(any(CreatePaymentRecordCommand.class)))
                .thenReturn(duePaymentView());

        mockMvc.perform(
                        post("/api/payment-records")
                                .header("Authorization", "Bearer agent-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createPaymentPayload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Payment record created"))
                .andExpect(jsonPath("$.data.id").value(PAYMENT_ID.toString()))
                .andExpect(jsonPath("$.data.customerId").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.data.productOwnershipId").value(OWNERSHIP_ID.toString()))
                .andExpect(jsonPath("$.data.dueDate").value(DUE_DATE.toString()))
                .andExpect(jsonPath("$.data.amountDue").value(89.50))
                .andExpect(jsonPath("$.data.status").value("DUE"));

        ArgumentCaptor<CreatePaymentRecordCommand> commandCaptor =
                ArgumentCaptor.forClass(CreatePaymentRecordCommand.class);
        verify(paymentRecordService).createPaymentRecord(commandCaptor.capture());
        assertThat(commandCaptor.getValue().customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(commandCaptor.getValue().productOwnershipId()).isEqualTo(OWNERSHIP_ID);
        assertThat(commandCaptor.getValue().dueDate()).isEqualTo(DUE_DATE);
        assertThat(commandCaptor.getValue().amountDue()).isEqualByComparingTo("89.50");
    }

    @Test
    void adminCanCreatePaymentRecord() throws Exception {
        when(jwtService.validateToken("admin-token", JwtTokenType.ACCESS))
                .thenReturn(adminClaims());
        when(paymentRecordService.createPaymentRecord(any(CreatePaymentRecordCommand.class)))
                .thenReturn(duePaymentView());

        mockMvc.perform(
                        post("/api/payment-records")
                                .header("Authorization", "Bearer admin-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createPaymentPayload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Payment record created"))
                .andExpect(jsonPath("$.data.customerFullName").value("Ada Payer"))
                .andExpect(jsonPath("$.data.productName").value("Life Protection"))
                .andExpect(jsonPath("$.data.reminderCount").value(0))
                .andExpect(jsonPath("$.data.defaultRisk").value(false));

        verify(paymentRecordService).createPaymentRecord(any(CreatePaymentRecordCommand.class));
    }

    @Test
    void productManagerCannotCreatePaymentRecord() throws Exception {
        when(jwtService.validateToken("product-manager-token", JwtTokenType.ACCESS))
                .thenReturn(productManagerClaims());

        mockMvc.perform(
                        post("/api/payment-records")
                                .header("Authorization", "Bearer product-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createPaymentPayload()))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("Payment record created"))));
    }

    @Test
    void savedPaymentRecordIsReturnedWhenListingCustomerPayments() throws Exception {
        when(jwtService.validateToken("agent-token", JwtTokenType.ACCESS))
                .thenReturn(customerServiceAgentClaims());
        when(paymentRecordService.searchPayments(any(PaymentRecordSearchCriteria.class)))
                .thenReturn(List.of(duePaymentView()));

        mockMvc.perform(
                        get("/api/payment-records")
                                .header("Authorization", "Bearer agent-token")
                                .param("customerId", CUSTOMER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Payment records loaded"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(PAYMENT_ID.toString()))
                .andExpect(jsonPath("$.data[0].dueDate").value(DUE_DATE.toString()))
                .andExpect(jsonPath("$.data[0].amountDue").value(89.50))
                .andExpect(jsonPath("$.data[0].status").value("DUE"));

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

    private static JwtTokenClaims productManagerClaims() {
        return new JwtTokenClaims(
                UUID.fromString("10000000-0000-0000-0000-000000009904"),
                "product.manager@bayer-westphalian.test",
                List.of(SystemRoleName.PRODUCT_MANAGER));
    }

    private static String createPaymentPayload() {
        return """
                {
                  "customerId": "%s",
                  "productOwnershipId": "%s",
                  "dueDate": "2026-08-01",
                  "amountDue": 89.50
                }
                """
                .formatted(CUSTOMER_ID, OWNERSHIP_ID);
    }

    private static PaymentRecordView duePaymentView() {
        return new PaymentRecordView(
                PAYMENT_ID,
                CUSTOMER_ID,
                "Ada Payer",
                OWNERSHIP_ID,
                PRODUCT_ID,
                "Life Protection",
                ProductType.LIFE_INSURANCE,
                DUE_DATE,
                null,
                new BigDecimal("89.50"),
                null,
                PaymentStatus.DUE,
                0,
                0,
                false);
    }
}
