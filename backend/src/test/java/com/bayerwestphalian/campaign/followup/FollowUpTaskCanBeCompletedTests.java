package com.bayerwestphalian.campaign.followup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
import com.bayerwestphalian.campaign.campaign.Campaign;
import com.bayerwestphalian.campaign.campaign.CampaignChannel;
import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import com.bayerwestphalian.campaign.user.User;
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
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

/**
 * KB item 392 / E17 Follow-up management: follow-up task can be completed via {@code PUT
 * /api/follow-up-tasks/{id}/complete}.
 *
 * <p>The assignee or a manager (Admin, Campaign Manager) may complete a task. Completion marks the
 * task {@code COMPLETED} and records {@code completed_at}.
 */
@WebMvcTest(controllers = FollowUpController.class)
@Import({
    SecurityConfiguration.class,
    JwtAuthenticationFilter.class,
    AuthorizationExpressions.class,
    GlobalExceptionHandler.class
})
class FollowUpTaskCanBeCompletedTests {

    private static final UUID TASK_ID = UUID.fromString("70000000-0000-0000-0000-000000000392");
    private static final UUID CUSTOMER_ID = UUID.fromString("20000000-0000-0000-0000-000000000392");
    private static final UUID ASSIGNEE_ID = UUID.fromString("10000000-0000-0000-0000-000000000007");
    private static final UUID CAMPAIGN_ID = UUID.fromString("50000000-0000-0000-0000-000000000392");
    private static final Instant CREATED_AT = Instant.parse("2026-07-10T12:00:00Z");
    private static final Instant COMPLETED_AT = Instant.parse("2026-07-11T09:30:00Z");
    private static final LocalDate DUE_DATE = LocalDate.of(2026, 9, 15);

    @Autowired private MockMvc mockMvc;

    @MockBean private FollowUpService followUpService;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void salesAgentCanCompleteFollowUpTaskViaPutApi() throws Exception {
        when(jwtService.validateToken("sales-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.SALES_AGENT));
        when(followUpService.completeTaskView(eq(TASK_ID))).thenReturn(completedTaskView());

        mockMvc.perform(
                        put("/api/follow-up-tasks/{id}/complete", TASK_ID)
                                .header("Authorization", "Bearer sales-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Follow-up task completed"))
                .andExpect(jsonPath("$.data.id").value(TASK_ID.toString()))
                .andExpect(jsonPath("$.data.customerId").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.data.assignedToUserId").value(ASSIGNEE_ID.toString()))
                .andExpect(jsonPath("$.data.assignedToFullName").value("Test Sales Agent"))
                .andExpect(jsonPath("$.data.title").value("Call interested prospect"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.priority").value("HIGH"))
                .andExpect(jsonPath("$.data.dueDate").value(DUE_DATE.toString()))
                .andExpect(jsonPath("$.data.completedAt").value(COMPLETED_AT.toString()));

        ArgumentCaptor<UUID> taskIdCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(followUpService).completeTaskView(taskIdCaptor.capture());
        assertThat(taskIdCaptor.getValue()).isEqualTo(TASK_ID);
    }

    @Test
    void customerServiceAgentCanCompleteFollowUpTask() throws Exception {
        when(jwtService.validateToken("csa-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CUSTOMER_SERVICE_AGENT));
        when(followUpService.completeTaskView(eq(TASK_ID))).thenReturn(completedTaskView());

        mockMvc.perform(
                        put("/api/follow-up-tasks/{id}/complete", TASK_ID)
                                .header("Authorization", "Bearer csa-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Follow-up task completed"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.completedAt").value(COMPLETED_AT.toString()));

        verify(followUpService).completeTaskView(TASK_ID);
    }

    @Test
    void adminCanCompleteFollowUpTask() throws Exception {
        when(jwtService.validateToken("admin-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.ADMIN));
        when(followUpService.completeTaskView(eq(TASK_ID))).thenReturn(completedTaskView());

        mockMvc.perform(
                        put("/api/follow-up-tasks/{id}/complete", TASK_ID)
                                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Follow-up task completed"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.completedAt").exists());

        verify(followUpService).completeTaskView(TASK_ID);
    }

    @Test
    void campaignManagerCanCompleteFollowUpTask() throws Exception {
        when(jwtService.validateToken("campaign-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CAMPAIGN_MANAGER));
        when(followUpService.completeTaskView(eq(TASK_ID))).thenReturn(completedTaskView());

        mockMvc.perform(
                        put("/api/follow-up-tasks/{id}/complete", TASK_ID)
                                .header("Authorization", "Bearer campaign-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Follow-up task completed"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        verify(followUpService).completeTaskView(TASK_ID);
    }

    @Test
    void salesAgentCanCompleteFollowUpTaskViaStatusEndpoint() throws Exception {
        when(jwtService.validateToken("sales-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.SALES_AGENT));
        when(followUpService.updateTaskStatus(eq(TASK_ID), eq(FollowUpTaskStatus.COMPLETED)))
                .thenReturn(completedTask());

        mockMvc.perform(
                        put("/api/follow-up-tasks/{id}/status", TASK_ID)
                                .header("Authorization", "Bearer sales-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Follow-up task status updated"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.completedAt").value(COMPLETED_AT.toString()));

        verify(followUpService).updateTaskStatus(TASK_ID, FollowUpTaskStatus.COMPLETED);
    }

    @ParameterizedTest
    @MethodSource("unauthorizedCompleteRoles")
    void unauthorizedRolesCannotCompleteFollowUpTask(SystemRoleName role) throws Exception {
        when(jwtService.validateToken("denied-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(role));

        mockMvc.perform(
                        put("/api/follow-up-tasks/{id}/complete", TASK_ID)
                                .header("Authorization", "Bearer denied-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("Follow-up task completed"))));

        verify(followUpService, never()).completeTaskView(eq(TASK_ID));
    }

    @Test
    void unauthenticatedRequestCannotCompleteFollowUpTask() throws Exception {
        mockMvc.perform(put("/api/follow-up-tasks/{id}/complete", TASK_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(not(containsString("Follow-up task completed"))));

        verify(followUpService, never()).completeTaskView(eq(TASK_ID));
    }

    @Test
    void completeReturnsNotFoundWhenTaskDoesNotExist() throws Exception {
        when(jwtService.validateToken("sales-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.SALES_AGENT));
        when(followUpService.completeTaskView(eq(TASK_ID)))
                .thenThrow(new ResourceNotFoundException("FollowUpTask", TASK_ID));

        mockMvc.perform(
                        put("/api/follow-up-tasks/{id}/complete", TASK_ID)
                                .header("Authorization", "Bearer sales-token"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("Follow-up task completed"))));
    }

    static Stream<SystemRoleName> unauthorizedCompleteRoles() {
        return Stream.of(
                SystemRoleName.BI_ANALYST,
                SystemRoleName.PRODUCT_MANAGER,
                SystemRoleName.COMPLIANCE_OFFICER,
                SystemRoleName.MARKETING_ANALYST,
                SystemRoleName.EXECUTIVE_VIEWER,
                SystemRoleName.SYSTEM_AUDITOR);
    }

    private static JwtTokenClaims roleClaims(SystemRoleName role) {
        return new JwtTokenClaims(
                UUID.fromString("10000000-0000-0000-0000-000000000392"),
                "test.user@bayer-westphalian.test",
                List.of(role));
    }

    private static FollowUpTask completedTask() {
        Customer customer = Customer.create(CustomerType.PROSPECT, "Ada", "Prospect");
        ReflectionTestUtils.setField(customer, "id", CUSTOMER_ID);

        User assignee =
                User.create("sales@bayer-westphalian.test", "{noop}x", "Test Sales Agent");
        ReflectionTestUtils.setField(assignee, "id", ASSIGNEE_ID);

        User owner = User.create("owner@bayer-westphalian.test", "{noop}x", "Owner");
        Campaign campaign =
                Campaign.create(
                        "Renewal outreach", "Renew", owner, null, CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);

        FollowUpTask task =
                new FollowUpTask(customer, assignee, "Call interested prospect", DUE_DATE);
        ReflectionTestUtils.setField(task, "id", TASK_ID);
        ReflectionTestUtils.setField(task, "createdAt", CREATED_AT);
        task.setCampaign(campaign);
        task.setDescription("Prospect requested a callback");
        task.updatePriority(FollowUpTaskPriority.HIGH);
        ReflectionTestUtils.setField(task, "status", FollowUpTaskStatus.COMPLETED);
        ReflectionTestUtils.setField(task, "completedAt", COMPLETED_AT);
        return task;
    }

    private static FollowUpTaskView completedTaskView() {
        return FollowUpTaskView.from(completedTask());
    }
}
