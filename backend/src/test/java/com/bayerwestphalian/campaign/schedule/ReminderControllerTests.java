package com.bayerwestphalian.campaign.schedule;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.auth.JwtAuthenticationFilter;
import com.bayerwestphalian.campaign.auth.JwtService;
import com.bayerwestphalian.campaign.auth.JwtTokenClaims;
import com.bayerwestphalian.campaign.auth.JwtTokenType;
import com.bayerwestphalian.campaign.auth.SecurityConfiguration;
import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import com.bayerwestphalian.campaign.common.exception.BusinessRuleException;
import com.bayerwestphalian.campaign.product.ProductType;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import java.time.Instant;
import java.time.LocalDate;
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

@WebMvcTest(controllers = ReminderController.class)
@Import({
    SecurityConfiguration.class,
    JwtAuthenticationFilter.class,
    AuthorizationExpressions.class,
    GlobalExceptionHandler.class
})
class ReminderControllerTests {

    private static final UUID CUSTOMER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID PRODUCT_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID REMINDER_ID = UUID.fromString("90000000-0000-0000-0000-000000000001");

    @Autowired private MockMvc mockMvc;

    @MockBean private ReminderService reminderService;
    @MockBean private ReminderProcessingScheduler reminderProcessingScheduler;
    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void campaignManagerCanCreatePaymentReminder() throws Exception {
        when(jwtService.validateToken("manager-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CAMPAIGN_MANAGER));
        when(reminderService.createPaymentReminders(any(ReminderScheduleCommand.class)))
                .thenReturn(paymentReminder(ReminderStatus.PENDING));

        String payload =
                """
                {
                  "customerId": "%s",
                  "productId": "%s",
                  "reminderLevel": "GREEN",
                  "scheduledDate": "2026-08-01"
                }
                """
                        .formatted(CUSTOMER_ID, PRODUCT_ID);

        mockMvc.perform(
                        post("/api/reminders/payment")
                                .header("Authorization", "Bearer manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Payment reminder scheduled"))
                .andExpect(jsonPath("$.data.id").value(REMINDER_ID.toString()))
                .andExpect(jsonPath("$.data.reminderType").value("PAYMENT_DUE"))
                .andExpect(jsonPath("$.data.reminderLevel").value("GREEN"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        verify(reminderService)
                .createPaymentReminders(
                        argThat(
                                command ->
                                        command.customerId().equals(CUSTOMER_ID)
                                                && command.productId().equals(PRODUCT_ID)
                                                && command.reminderType() == ReminderType.PAYMENT_DUE
                                                && command.reminderLevel() == ReminderLevel.GREEN
                                                && command.scheduledDate()
                                                        .equals(LocalDate.of(2026, 8, 1))));
    }

    @Test
    void campaignManagerCannotCreatePaymentReminderForCompletedPayment() throws Exception {
        when(jwtService.validateToken("manager-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CAMPAIGN_MANAGER));
        when(reminderService.createPaymentReminders(any(ReminderScheduleCommand.class)))
                .thenThrow(
                        new BusinessRuleException(
                                "PAYMENT_REMINDER_PAYMENT_COMPLETED",
                                "Payment reminder must not be scheduled after the payment is completed"));

        String payload =
                """
                {
                  "customerId": "%s",
                  "productId": "%s",
                  "reminderLevel": "GREEN",
                  "scheduledDate": "2026-08-01"
                }
                """
                        .formatted(CUSTOMER_ID, PRODUCT_ID);

        mockMvc.perform(
                        post("/api/reminders/payment")
                                .header("Authorization", "Bearer manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PAYMENT_REMINDER_PAYMENT_COMPLETED"))
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Payment reminder must not be scheduled after the payment is completed"))
                .andExpect(jsonPath("$.path").value("/api/reminders/payment"));
    }

    @Test
    void campaignManagerCanCreateExpirationReminder() throws Exception {
        when(jwtService.validateToken("manager-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CAMPAIGN_MANAGER));
        when(reminderService.createExpirationReminders(any(ReminderScheduleCommand.class)))
                .thenReturn(expirationReminder(ReminderStatus.PENDING));

        String payload =
                """
                {
                  "customerId": "%s",
                  "productId": "%s",
                  "reminderLevel": "YELLOW",
                  "scheduledDate": "2026-10-01"
                }
                """
                        .formatted(CUSTOMER_ID, PRODUCT_ID);

        mockMvc.perform(
                        post("/api/reminders/expiration")
                                .header("Authorization", "Bearer manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product-expiration reminder scheduled"))
                .andExpect(jsonPath("$.data.reminderType").value("PRODUCT_EXPIRATION"))
                .andExpect(jsonPath("$.data.reminderLevel").value("YELLOW"));

        verify(reminderService)
                .createExpirationReminders(
                        argThat(
                                command ->
                                        command.customerId().equals(CUSTOMER_ID)
                                                && command.productId().equals(PRODUCT_ID)
                                                && command.reminderType()
                                                        == ReminderType.PRODUCT_EXPIRATION
                                                && command.reminderLevel()
                                                        == ReminderLevel.YELLOW
                        && command.scheduledDate()
                                .equals(LocalDate.of(2026, 10, 1))));
    }

    @Test
    void customerServiceAgentCanGeneratePaymentDueReminders() throws Exception {
        when(jwtService.validateToken("csa-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CUSTOMER_SERVICE_AGENT));
        when(reminderService.generatePaymentDueReminders(LocalDate.of(2026, 8, 1)))
                .thenReturn(List.of(paymentReminder(ReminderStatus.PENDING)));

        mockMvc.perform(
                        post("/api/reminders/payment/generate")
                                .header("Authorization", "Bearer csa-token")
                                .param("asOfDate", "2026-08-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Payment due reminders generated"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].reminderType").value("PAYMENT_DUE"));

        verify(reminderService).generatePaymentDueReminders(LocalDate.of(2026, 8, 1));
    }

    @Test
    void customerServiceAgentCanGenerateThreeMonthExpirationReminders() throws Exception {
        when(jwtService.validateToken("csa-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CUSTOMER_SERVICE_AGENT));
        when(reminderService.generateThreeMonthExpirationReminders(LocalDate.of(2026, 7, 11)))
                .thenReturn(List.of(threeMonthExpirationReminder(ReminderStatus.PENDING)));

        mockMvc.perform(
                        post("/api/reminders/expiration/3-month/generate")
                                .header("Authorization", "Bearer csa-token")
                                .param("asOfDate", "2026-07-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(
                        jsonPath("$.message")
                                .value("Three-month product-expiration reminders generated"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].reminderType").value("PRODUCT_EXPIRATION"))
                .andExpect(jsonPath("$.data[0].reminderLevel").value("RED"));

        verify(reminderService).generateThreeMonthExpirationReminders(LocalDate.of(2026, 7, 11));
    }

    @Test
    void customerServiceAgentCanGenerateSixMonthExpirationReminders() throws Exception {
        when(jwtService.validateToken("csa-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CUSTOMER_SERVICE_AGENT));
        when(reminderService.generateSixMonthExpirationReminders(LocalDate.of(2026, 7, 11)))
                .thenReturn(List.of(sixMonthExpirationReminder(ReminderStatus.PENDING)));

        mockMvc.perform(
                        post("/api/reminders/expiration/6-month/generate")
                                .header("Authorization", "Bearer csa-token")
                                .param("asOfDate", "2026-07-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(
                        jsonPath("$.message")
                                .value("Six-month product-expiration reminders generated"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].reminderType").value("PRODUCT_EXPIRATION"))
                .andExpect(jsonPath("$.data[0].reminderLevel").value("YELLOW"));

        verify(reminderService).generateSixMonthExpirationReminders(LocalDate.of(2026, 7, 11));
    }

    @Test
    void customerServiceAgentCanGenerateTwelveMonthExpirationReminders() throws Exception {
        when(jwtService.validateToken("csa-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CUSTOMER_SERVICE_AGENT));
        when(reminderService.generateTwelveMonthExpirationReminders(LocalDate.of(2026, 7, 11)))
                .thenReturn(List.of(twelveMonthExpirationReminder(ReminderStatus.PENDING)));

        mockMvc.perform(
                        post("/api/reminders/expiration/12-month/generate")
                                .header("Authorization", "Bearer csa-token")
                                .param("asOfDate", "2026-07-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(
                        jsonPath("$.message")
                                .value("Twelve-month product-expiration reminders generated"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].reminderType").value("PRODUCT_EXPIRATION"))
                .andExpect(jsonPath("$.data[0].reminderLevel").value("GREEN"));

        verify(reminderService).generateTwelveMonthExpirationReminders(LocalDate.of(2026, 7, 11));
    }

    @Test
    void campaignManagerCanSendDueReminders() throws Exception {
        when(jwtService.validateToken("manager-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CAMPAIGN_MANAGER));
        when(reminderService.sendDueReminders(LocalDate.of(2026, 8, 1)))
                .thenReturn(List.of(paymentReminder(ReminderStatus.SENT)));

        mockMvc.perform(
                        post("/api/reminders/due/send")
                                .header("Authorization", "Bearer manager-token")
                                .param("asOfDate", "2026-08-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Due reminders processed"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].status").value("SENT"));

        verify(reminderService).sendDueReminders(LocalDate.of(2026, 8, 1));
    }

    @Test
    void adminCanManuallyTriggerReminderProcessingForTestEnvironment() throws Exception {
        when(jwtService.validateToken("admin-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.ADMIN));
        when(reminderProcessingScheduler.triggerManualProcessing())
                .thenReturn(List.of(paymentReminder(ReminderStatus.SENT)));

        mockMvc.perform(
                        post("/api/reminders/due/manual-trigger")
                                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Manual reminder processing triggered"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].status").value("SENT"));

        verify(reminderProcessingScheduler).triggerManualProcessing();
    }

    @Test
    void campaignManagerCannotManuallyTriggerReminderProcessing() throws Exception {
        when(jwtService.validateToken("manager-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CAMPAIGN_MANAGER));

        mockMvc.perform(
                        post("/api/reminders/due/manual-trigger")
                                .header("Authorization", "Bearer manager-token"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(reminderProcessingScheduler);
    }

    @Test
    void campaignManagerCanMarkReminderSent() throws Exception {
        when(jwtService.validateToken("manager-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CAMPAIGN_MANAGER));
        when(reminderService.markSent(REMINDER_ID)).thenReturn(paymentReminder(ReminderStatus.SENT));

        mockMvc.perform(
                        put("/api/reminders/{id}/sent", REMINDER_ID)
                                .header("Authorization", "Bearer manager-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Reminder marked sent"))
                .andExpect(jsonPath("$.data.status").value("SENT"));

        verify(reminderService).markSent(REMINDER_ID);
    }

    @Test
    void campaignManagerCanCancelReminder() throws Exception {
        when(jwtService.validateToken("manager-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CAMPAIGN_MANAGER));
        when(reminderService.cancelReminder(REMINDER_ID))
                .thenReturn(paymentReminder(ReminderStatus.CANCELLED));

        mockMvc.perform(
                        put("/api/reminders/{id}/cancel", REMINDER_ID)
                                .header("Authorization", "Bearer manager-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Reminder cancelled"))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        verify(reminderService).cancelReminder(REMINDER_ID);
    }

    @Test
    void salesAgentCanListReminders() throws Exception {
        when(jwtService.validateToken("agent-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.SALES_AGENT));
        when(reminderService.searchReminders(any(ReminderScheduleSearchCriteria.class)))
                .thenReturn(List.of(paymentReminder(ReminderStatus.PENDING)));

        mockMvc.perform(
                        get("/api/reminders")
                                .header("Authorization", "Bearer agent-token")
                                .param("customerId", CUSTOMER_ID.toString())
                                .param("status", "PENDING")
                                .param("dueOnOrBefore", "2026-08-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Reminders loaded"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(REMINDER_ID.toString()));

        verify(reminderService)
                .searchReminders(
                        argThat(
                                criteria ->
                                        criteria.customerId().equals(CUSTOMER_ID)
                                                && criteria.status() == ReminderStatus.PENDING
                                                && criteria.dueOnOrBefore()
                                                        .equals(LocalDate.of(2026, 8, 31))));
    }

    @Test
    void productManagerCannotCreatePaymentReminder() throws Exception {
        when(jwtService.validateToken("product-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.PRODUCT_MANAGER));

        String payload =
                """
                {
                  "customerId": "%s",
                  "productId": "%s",
                  "reminderLevel": "GREEN",
                  "scheduledDate": "2026-08-01"
                }
                """
                        .formatted(CUSTOMER_ID, PRODUCT_ID);

        mockMvc.perform(
                        post("/api/reminders/payment")
                                .header("Authorization", "Bearer product-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserCannotListReminders() throws Exception {
        mockMvc.perform(get("/api/reminders")).andExpect(status().isUnauthorized());
    }

    private static ReminderScheduleView paymentReminder(ReminderStatus status) {
        return new ReminderScheduleView(
                REMINDER_ID,
                CUSTOMER_ID,
                "Ada Lovelace",
                PRODUCT_ID,
                "Car Insurance",
                ProductType.AUTO_INSURANCE,
                ReminderType.PAYMENT_DUE,
                ReminderLevel.GREEN,
                LocalDate.of(2026, 8, 1),
                status,
                Instant.parse("2026-07-11T10:00:00Z"),
                status == ReminderStatus.SENT ? Instant.parse("2026-08-01T10:00:00Z") : null,
                status == ReminderStatus.PENDING);
    }

    private static ReminderScheduleView expirationReminder(ReminderStatus status) {
        return new ReminderScheduleView(
                REMINDER_ID,
                CUSTOMER_ID,
                "Ada Lovelace",
                PRODUCT_ID,
                "Car Insurance",
                ProductType.AUTO_INSURANCE,
                ReminderType.PRODUCT_EXPIRATION,
                ReminderLevel.YELLOW,
                LocalDate.of(2026, 10, 1),
                status,
                Instant.parse("2026-07-11T10:00:00Z"),
                null,
                status == ReminderStatus.PENDING);
    }

    private static ReminderScheduleView threeMonthExpirationReminder(ReminderStatus status) {
        return new ReminderScheduleView(
                REMINDER_ID,
                CUSTOMER_ID,
                "Ada Lovelace",
                PRODUCT_ID,
                "Car Insurance",
                ProductType.AUTO_INSURANCE,
                ReminderType.PRODUCT_EXPIRATION,
                ReminderLevel.RED,
                LocalDate.of(2026, 7, 11),
                status,
                Instant.parse("2026-07-11T10:00:00Z"),
                null,
                status == ReminderStatus.PENDING);
    }

    private static ReminderScheduleView sixMonthExpirationReminder(ReminderStatus status) {
        return new ReminderScheduleView(
                REMINDER_ID,
                CUSTOMER_ID,
                "Ada Lovelace",
                PRODUCT_ID,
                "Car Insurance",
                ProductType.AUTO_INSURANCE,
                ReminderType.PRODUCT_EXPIRATION,
                ReminderLevel.YELLOW,
                LocalDate.of(2026, 7, 11),
                status,
                Instant.parse("2026-07-11T10:00:00Z"),
                null,
                status == ReminderStatus.PENDING);
    }

    private static ReminderScheduleView twelveMonthExpirationReminder(ReminderStatus status) {
        return new ReminderScheduleView(
                REMINDER_ID,
                CUSTOMER_ID,
                "Ada Lovelace",
                PRODUCT_ID,
                "Car Insurance",
                ProductType.AUTO_INSURANCE,
                ReminderType.PRODUCT_EXPIRATION,
                ReminderLevel.GREEN,
                LocalDate.of(2026, 7, 11),
                status,
                Instant.parse("2026-07-11T10:00:00Z"),
                null,
                status == ReminderStatus.PENDING);
    }

    private static JwtTokenClaims roleClaims(SystemRoleName role) {
        return new JwtTokenClaims(
                UUID.fromString("10000000-0000-0000-0000-000000000002"),
                "test.user@bayer-westphalian.test",
                List.of(role));
    }
}
