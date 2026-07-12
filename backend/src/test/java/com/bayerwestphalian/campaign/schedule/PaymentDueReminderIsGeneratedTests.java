package com.bayerwestphalian.campaign.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.auth.JwtAuthenticationFilter;
import com.bayerwestphalian.campaign.auth.JwtService;
import com.bayerwestphalian.campaign.auth.JwtTokenClaims;
import com.bayerwestphalian.campaign.auth.JwtTokenType;
import com.bayerwestphalian.campaign.auth.SecurityConfiguration;
import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import com.bayerwestphalian.campaign.product.ProductType;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.web.servlet.MockMvc;

/**
 * KB item 393 / E18 Reminder scheduling / BR-020–BR-024: payment due reminder is generated via
 * {@code POST /api/reminders/payment/generate}.
 *
 * <p>Generation creates {@code PAYMENT_DUE} reminder schedules for unpaid due/overdue payments,
 * with Green/Yellow/Red levels from payment reminder history, while excluding paid payments and
 * ineligible recipients.
 */
@WebMvcTest(controllers = ReminderController.class)
@Import({
    SecurityConfiguration.class,
    JwtAuthenticationFilter.class,
    AuthorizationExpressions.class,
    GlobalExceptionHandler.class
})
class PaymentDueReminderIsGeneratedTests {

    private static final UUID REMINDER_ID = UUID.fromString("90000000-0000-0000-0000-000000000393");
    private static final UUID CUSTOMER_ID = UUID.fromString("20000000-0000-0000-0000-000000000393");
    private static final UUID PRODUCT_ID = UUID.fromString("30000000-0000-0000-0000-000000000393");
    private static final LocalDate AS_OF_DATE = LocalDate.of(2026, 7, 11);
    private static final Instant CREATED_AT = Instant.parse("2026-07-11T10:00:00Z");

    @Autowired private MockMvc mockMvc;

    @MockBean private ReminderService reminderService;

    @MockBean private ReminderProcessingScheduler reminderProcessingScheduler;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void campaignManagerCanGeneratePaymentDueReminderViaPostApi() throws Exception {
        when(jwtService.validateToken("campaign-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CAMPAIGN_MANAGER));
        when(reminderService.generatePaymentDueReminders(eq(AS_OF_DATE)))
                .thenReturn(List.of(generatedPaymentDueReminder(ReminderLevel.GREEN)));

        mockMvc.perform(
                        post("/api/reminders/payment/generate")
                                .header("Authorization", "Bearer campaign-token")
                                .param("asOfDate", AS_OF_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Payment due reminders generated"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(REMINDER_ID.toString()))
                .andExpect(jsonPath("$.data[0].customerId").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.data[0].customerFullName").value("Ada Payer"))
                .andExpect(jsonPath("$.data[0].productId").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.data[0].productName").value("Life Protection"))
                .andExpect(jsonPath("$.data[0].reminderType").value("PAYMENT_DUE"))
                .andExpect(jsonPath("$.data[0].reminderLevel").value("GREEN"))
                .andExpect(jsonPath("$.data[0].scheduledDate").value(AS_OF_DATE.toString()))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));

        ArgumentCaptor<LocalDate> asOfDateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(reminderService).generatePaymentDueReminders(asOfDateCaptor.capture());
        assertThat(asOfDateCaptor.getValue()).isEqualTo(AS_OF_DATE);
    }

    @Test
    void adminCanGeneratePaymentDueReminder() throws Exception {
        when(jwtService.validateToken("admin-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.ADMIN));
        when(reminderService.generatePaymentDueReminders(eq(AS_OF_DATE)))
                .thenReturn(List.of(generatedPaymentDueReminder(ReminderLevel.GREEN)));

        mockMvc.perform(
                        post("/api/reminders/payment/generate")
                                .header("Authorization", "Bearer admin-token")
                                .param("asOfDate", AS_OF_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Payment due reminders generated"))
                .andExpect(jsonPath("$.data[0].reminderType").value("PAYMENT_DUE"))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));

        verify(reminderService).generatePaymentDueReminders(AS_OF_DATE);
    }

    @Test
    void customerServiceAgentCanGeneratePaymentDueReminder() throws Exception {
        when(jwtService.validateToken("csa-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CUSTOMER_SERVICE_AGENT));
        when(reminderService.generatePaymentDueReminders(eq(AS_OF_DATE)))
                .thenReturn(List.of(generatedPaymentDueReminder(ReminderLevel.YELLOW)));

        mockMvc.perform(
                        post("/api/reminders/payment/generate")
                                .header("Authorization", "Bearer csa-token")
                                .param("asOfDate", AS_OF_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Payment due reminders generated"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].reminderType").value("PAYMENT_DUE"))
                .andExpect(jsonPath("$.data[0].reminderLevel").value("YELLOW"))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));

        verify(reminderService).generatePaymentDueReminders(AS_OF_DATE);
    }

    @Test
    void paymentDueReminderIsGeneratedWithGreenYellowAndRedLevels() throws Exception {
        when(jwtService.validateToken("campaign-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CAMPAIGN_MANAGER));
        when(reminderService.generatePaymentDueReminders(eq(AS_OF_DATE)))
                .thenReturn(
                        List.of(
                                generatedPaymentDueReminder(ReminderLevel.GREEN),
                                generatedPaymentDueReminder(
                                        UUID.fromString("90000000-0000-0000-0000-000000000394"),
                                        ReminderLevel.YELLOW),
                                generatedPaymentDueReminder(
                                        UUID.fromString("90000000-0000-0000-0000-000000000395"),
                                        ReminderLevel.RED)));

        mockMvc.perform(
                        post("/api/reminders/payment/generate")
                                .header("Authorization", "Bearer campaign-token")
                                .param("asOfDate", AS_OF_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Payment due reminders generated"))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].reminderLevel").value("GREEN"))
                .andExpect(jsonPath("$.data[1].reminderLevel").value("YELLOW"))
                .andExpect(jsonPath("$.data[2].reminderLevel").value("RED"))
                .andExpect(jsonPath("$.data[0].reminderType").value("PAYMENT_DUE"))
                .andExpect(jsonPath("$.data[1].reminderType").value("PAYMENT_DUE"))
                .andExpect(jsonPath("$.data[2].reminderType").value("PAYMENT_DUE"));

        verify(reminderService).generatePaymentDueReminders(AS_OF_DATE);
    }

    @Test
    void generatePaymentDueRemindersAllowsNullAsOfDate() throws Exception {
        when(jwtService.validateToken("campaign-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CAMPAIGN_MANAGER));
        when(reminderService.generatePaymentDueReminders(isNull()))
                .thenReturn(List.of(generatedPaymentDueReminder(ReminderLevel.GREEN)));

        mockMvc.perform(
                        post("/api/reminders/payment/generate")
                                .header("Authorization", "Bearer campaign-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Payment due reminders generated"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].reminderType").value("PAYMENT_DUE"));

        verify(reminderService).generatePaymentDueReminders(isNull());
    }

    @Test
    void generatePaymentDueRemindersReturnsEmptyListWhenNoCandidates() throws Exception {
        when(jwtService.validateToken("campaign-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CAMPAIGN_MANAGER));
        when(reminderService.generatePaymentDueReminders(eq(AS_OF_DATE))).thenReturn(List.of());

        mockMvc.perform(
                        post("/api/reminders/payment/generate")
                                .header("Authorization", "Bearer campaign-token")
                                .param("asOfDate", AS_OF_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Payment due reminders generated"))
                .andExpect(jsonPath("$.data", hasSize(0)));

        verify(reminderService).generatePaymentDueReminders(AS_OF_DATE);
    }

    @ParameterizedTest
    @MethodSource("unauthorizedGenerateRoles")
    void unauthorizedRolesCannotGeneratePaymentDueReminders(SystemRoleName role) throws Exception {
        when(jwtService.validateToken("denied-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(role));

        mockMvc.perform(
                        post("/api/reminders/payment/generate")
                                .header("Authorization", "Bearer denied-token")
                                .param("asOfDate", AS_OF_DATE.toString()))
                .andExpect(status().isForbidden())
                .andExpect(
                        content().string(not(containsString("Payment due reminders generated"))));

        verify(reminderService, never()).generatePaymentDueReminders(any());
    }

    @Test
    void unauthenticatedRequestCannotGeneratePaymentDueReminders() throws Exception {
        mockMvc.perform(
                        post("/api/reminders/payment/generate")
                                .param("asOfDate", AS_OF_DATE.toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(
                        content().string(not(containsString("Payment due reminders generated"))));

        verify(reminderService, never()).generatePaymentDueReminders(any());
    }

    static Stream<SystemRoleName> unauthorizedGenerateRoles() {
        return Stream.of(
                SystemRoleName.BI_ANALYST,
                SystemRoleName.PRODUCT_MANAGER,
                SystemRoleName.COMPLIANCE_OFFICER,
                SystemRoleName.SALES_AGENT,
                SystemRoleName.MARKETING_ANALYST,
                SystemRoleName.EXECUTIVE_VIEWER,
                SystemRoleName.SYSTEM_AUDITOR);
    }

    private static JwtTokenClaims roleClaims(SystemRoleName role) {
        return new JwtTokenClaims(
                UUID.fromString("10000000-0000-0000-0000-000000000393"),
                "test.user@bayer-westphalian.test",
                List.of(role));
    }

    private static ReminderScheduleView generatedPaymentDueReminder(ReminderLevel level) {
        return generatedPaymentDueReminder(REMINDER_ID, level);
    }

    private static ReminderScheduleView generatedPaymentDueReminder(
            UUID reminderId, ReminderLevel level) {
        return new ReminderScheduleView(
                reminderId,
                CUSTOMER_ID,
                "Ada Payer",
                PRODUCT_ID,
                "Life Protection",
                ProductType.LIFE_INSURANCE,
                ReminderType.PAYMENT_DUE,
                level,
                AS_OF_DATE,
                ReminderStatus.PENDING,
                CREATED_AT,
                null,
                true);
    }
}
