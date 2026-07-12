package com.bayerwestphalian.campaign.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.bayerwestphalian.campaign.common.exception.BusinessRuleException;
import com.bayerwestphalian.campaign.product.ProductType;
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
 * API coverage for KB item 397 / BR-024: payment reminder is not sent if payment is completed.
 *
 * <p>Sprint 16 critical restatement: item <b>660</b> — {@link
 * PaymentReminderIsNotSentIfPaymentIsCompletedTests}.
 */
@WebMvcTest(controllers = ReminderController.class)
@Import({
    SecurityConfiguration.class,
    JwtAuthenticationFilter.class,
    AuthorizationExpressions.class,
    GlobalExceptionHandler.class
})
class PaymentReminderNotSentIfPaymentCompletedApiTests {

    private static final UUID CUSTOMER_ID = UUID.fromString("20000000-0000-0000-0000-000000000397");
    private static final UUID PRODUCT_ID = UUID.fromString("30000000-0000-0000-0000-000000000397");
    private static final UUID REMINDER_ID = UUID.fromString("90000000-0000-0000-0000-000000000397");
    private static final LocalDate AS_OF_DATE = LocalDate.of(2026, 7, 11);

    @Autowired private MockMvc mockMvc;

    @MockBean private ReminderService reminderService;

    @MockBean private ReminderProcessingScheduler reminderProcessingScheduler;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void sendDueApiReturnsCancelledNotSentWhenPaymentCompleted() throws Exception {
        when(jwtService.validateToken("manager-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CAMPAIGN_MANAGER));
        when(reminderService.sendDueReminders(eq(AS_OF_DATE)))
                .thenReturn(List.of(cancelledPaymentReminderView()));

        mockMvc.perform(
                        post("/api/reminders/due/send")
                                .header("Authorization", "Bearer manager-token")
                                .param("asOfDate", AS_OF_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Due reminders processed"))
                .andExpect(jsonPath("$.data[0].reminderType").value("PAYMENT_DUE"))
                .andExpect(jsonPath("$.data[0].status").value("CANCELLED"))
                .andExpect(jsonPath("$.data[0].sentAt").doesNotExist());

        ArgumentCaptor<LocalDate> asOfCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(reminderService).sendDueReminders(asOfCaptor.capture());
        assertThat(asOfCaptor.getValue()).isEqualTo(AS_OF_DATE);
    }

    @Test
    void markSentApiReturnsCancelledNotSentWhenPaymentCompleted() throws Exception {
        when(jwtService.validateToken("manager-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CAMPAIGN_MANAGER));
        when(reminderService.markSent(eq(REMINDER_ID))).thenReturn(cancelledPaymentReminderView());

        mockMvc.perform(
                        put("/api/reminders/{id}/sent", REMINDER_ID)
                                .header("Authorization", "Bearer manager-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reminder marked sent"))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.sentAt").doesNotExist());

        verify(reminderService).markSent(REMINDER_ID);
    }

    @Test
    void createPaymentReminderApiRejectsCompletedPayment() throws Exception {
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
                  "scheduledDate": "2026-07-11"
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
                        content()
                                .string(
                                        containsString(
                                                "Payment reminder must not be scheduled after the payment is completed")));
    }

    private static JwtTokenClaims roleClaims(SystemRoleName role) {
        return new JwtTokenClaims(
                UUID.fromString("10000000-0000-0000-0000-000000000397"),
                "test.user@bayer-westphalian.test",
                List.of(role));
    }

    private static ReminderScheduleView cancelledPaymentReminderView() {
        return new ReminderScheduleView(
                REMINDER_ID,
                CUSTOMER_ID,
                "Ada Paid",
                PRODUCT_ID,
                "Life Protection",
                ProductType.LIFE_INSURANCE,
                ReminderType.PAYMENT_DUE,
                ReminderLevel.GREEN,
                AS_OF_DATE,
                ReminderStatus.CANCELLED,
                Instant.parse("2026-07-10T10:00:00Z"),
                null,
                false);
    }
}
