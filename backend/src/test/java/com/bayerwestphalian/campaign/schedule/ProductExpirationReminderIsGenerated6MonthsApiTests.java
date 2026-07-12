package com.bayerwestphalian.campaign.schedule;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.web.servlet.MockMvc;

/**
 * API coverage for KB item 399 / BR-023: product-expiration reminder is generated 6 months before
 * expiration via {@code POST /api/reminders/expiration/6-month/generate}.
 */
@WebMvcTest(controllers = ReminderController.class)
@Import({
    SecurityConfiguration.class,
    JwtAuthenticationFilter.class,
    AuthorizationExpressions.class,
    GlobalExceptionHandler.class
})
class ProductExpirationReminderIsGenerated6MonthsApiTests {

    private static final UUID REMINDER_ID = UUID.fromString("90000000-0000-0000-0000-000000000399");
    private static final UUID CUSTOMER_ID = UUID.fromString("20000000-0000-0000-0000-000000000399");
    private static final UUID PRODUCT_ID = UUID.fromString("30000000-0000-0000-0000-000000000399");
    private static final LocalDate AS_OF_DATE = LocalDate.of(2026, 7, 11);
    private static final Instant CREATED_AT = Instant.parse("2026-07-11T10:00:00Z");

    @Autowired private MockMvc mockMvc;

    @MockBean private ReminderService reminderService;

    @MockBean private ReminderProcessingScheduler reminderProcessingScheduler;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void campaignManagerCanGenerateSixMonthProductExpirationReminderViaPostApi() throws Exception {
        when(jwtService.validateToken("campaign-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CAMPAIGN_MANAGER));
        when(reminderService.generateSixMonthExpirationReminders(eq(AS_OF_DATE)))
                .thenReturn(List.of(sixMonthReminderView()));

        mockMvc.perform(
                        post("/api/reminders/expiration/6-month/generate")
                                .header("Authorization", "Bearer campaign-token")
                                .param("asOfDate", AS_OF_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(
                        jsonPath("$.message")
                                .value("Six-month product-expiration reminders generated"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(REMINDER_ID.toString()))
                .andExpect(jsonPath("$.data[0].customerId").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.data[0].productId").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.data[0].reminderType").value("PRODUCT_EXPIRATION"))
                .andExpect(jsonPath("$.data[0].reminderLevel").value("YELLOW"))
                .andExpect(jsonPath("$.data[0].scheduledDate").value(AS_OF_DATE.toString()))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));

        verify(reminderService).generateSixMonthExpirationReminders(AS_OF_DATE);
    }

    @Test
    void adminCanGenerateSixMonthProductExpirationReminder() throws Exception {
        when(jwtService.validateToken("admin-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.ADMIN));
        when(reminderService.generateSixMonthExpirationReminders(eq(AS_OF_DATE)))
                .thenReturn(List.of(sixMonthReminderView()));

        mockMvc.perform(
                        post("/api/reminders/expiration/6-month/generate")
                                .header("Authorization", "Bearer admin-token")
                                .param("asOfDate", AS_OF_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].reminderType").value("PRODUCT_EXPIRATION"))
                .andExpect(jsonPath("$.data[0].reminderLevel").value("YELLOW"));

        verify(reminderService).generateSixMonthExpirationReminders(AS_OF_DATE);
    }

    @Test
    void customerServiceAgentCanGenerateSixMonthProductExpirationReminder() throws Exception {
        when(jwtService.validateToken("csa-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CUSTOMER_SERVICE_AGENT));
        when(reminderService.generateSixMonthExpirationReminders(eq(AS_OF_DATE)))
                .thenReturn(List.of(sixMonthReminderView()));

        mockMvc.perform(
                        post("/api/reminders/expiration/6-month/generate")
                                .header("Authorization", "Bearer csa-token")
                                .param("asOfDate", AS_OF_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.message")
                                .value("Six-month product-expiration reminders generated"))
                .andExpect(jsonPath("$.data[0].reminderLevel").value("YELLOW"));

        verify(reminderService).generateSixMonthExpirationReminders(AS_OF_DATE);
    }

    @Test
    void sixMonthGenerateAllowsNullAsOfDate() throws Exception {
        when(jwtService.validateToken("campaign-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CAMPAIGN_MANAGER));
        when(reminderService.generateSixMonthExpirationReminders(isNull()))
                .thenReturn(List.of(sixMonthReminderView()));

        mockMvc.perform(
                        post("/api/reminders/expiration/6-month/generate")
                                .header("Authorization", "Bearer campaign-token"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.message")
                                .value("Six-month product-expiration reminders generated"));

        verify(reminderService).generateSixMonthExpirationReminders(isNull());
    }

    @Test
    void sixMonthGenerateReturnsEmptyListWhenNoCandidates() throws Exception {
        when(jwtService.validateToken("campaign-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CAMPAIGN_MANAGER));
        when(reminderService.generateSixMonthExpirationReminders(eq(AS_OF_DATE)))
                .thenReturn(List.of());

        mockMvc.perform(
                        post("/api/reminders/expiration/6-month/generate")
                                .header("Authorization", "Bearer campaign-token")
                                .param("asOfDate", AS_OF_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @ParameterizedTest
    @MethodSource("unauthorizedRoles")
    void unauthorizedRolesCannotGenerateSixMonthReminders(SystemRoleName role) throws Exception {
        when(jwtService.validateToken("denied-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(role));

        mockMvc.perform(
                        post("/api/reminders/expiration/6-month/generate")
                                .header("Authorization", "Bearer denied-token")
                                .param("asOfDate", AS_OF_DATE.toString()))
                .andExpect(status().isForbidden())
                .andExpect(
                        content()
                                .string(
                                        not(
                                                containsString(
                                                        "Six-month product-expiration reminders generated"))));

        verify(reminderService, never()).generateSixMonthExpirationReminders(any());
    }

    static Stream<SystemRoleName> unauthorizedRoles() {
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
                UUID.fromString("10000000-0000-0000-0000-000000000399"),
                "test.user@bayer-westphalian.test",
                List.of(role));
    }

    private static ReminderScheduleView sixMonthReminderView() {
        return new ReminderScheduleView(
                REMINDER_ID,
                CUSTOMER_ID,
                "Ada SixMonth",
                PRODUCT_ID,
                "Life Protection",
                ProductType.LIFE_INSURANCE,
                ReminderType.PRODUCT_EXPIRATION,
                ReminderLevel.YELLOW,
                AS_OF_DATE,
                ReminderStatus.PENDING,
                CREATED_AT,
                null,
                true);
    }
}
