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
import com.bayerwestphalian.campaign.common.exception.ValidationException;
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
 * KB item 391 / E17 Follow-up management: follow-up task can be assigned via {@code PUT
 * /api/follow-up-tasks/{id}/assign}.
 *
 * <p>Authorized assign roles per KB role matrix and screen access: Admin, Customer Service Agent,
 * Sales Agent, and Campaign Manager. Assignment stores {@code assigned_to} for lead ownership and
 * assignee-based worklist filtering.
 */
@WebMvcTest(controllers = FollowUpController.class)
@Import({
    SecurityConfiguration.class,
    JwtAuthenticationFilter.class,
    AuthorizationExpressions.class,
    GlobalExceptionHandler.class
})
class FollowUpTaskCanBeAssignedTests {

    private static final UUID TASK_ID = UUID.fromString("70000000-0000-0000-0000-000000000391");
    private static final UUID CUSTOMER_ID = UUID.fromString("20000000-0000-0000-0000-000000000391");
    private static final UUID ASSIGNEE_ID = UUID.fromString("10000000-0000-0000-0000-000000000007");
    private static final UUID CAMPAIGN_ID = UUID.fromString("50000000-0000-0000-0000-000000000391");
    private static final Instant CREATED_AT = Instant.parse("2026-07-10T12:00:00Z");
    private static final LocalDate DUE_DATE = LocalDate.of(2026, 9, 15);

    @Autowired private MockMvc mockMvc;

    @MockBean private FollowUpService followUpService;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void salesAgentCanAssignFollowUpTaskViaPutApi() throws Exception {
        when(jwtService.validateToken("sales-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.SALES_AGENT));
        when(followUpService.assignTask(eq(TASK_ID), eq(ASSIGNEE_ID)))
                .thenReturn(assignedTask(ASSIGNEE_ID, "Test Sales Agent"));

        mockMvc.perform(
                        put("/api/follow-up-tasks/{id}/assign", TASK_ID)
                                .header("Authorization", "Bearer sales-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(assignPayload(ASSIGNEE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Follow-up task assigned"))
                .andExpect(jsonPath("$.data.id").value(TASK_ID.toString()))
                .andExpect(jsonPath("$.data.customerId").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.data.assignedToUserId").value(ASSIGNEE_ID.toString()))
                .andExpect(jsonPath("$.data.assignedToFullName").value("Test Sales Agent"))
                .andExpect(jsonPath("$.data.title").value("Call interested prospect"))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.priority").value("HIGH"))
                .andExpect(jsonPath("$.data.dueDate").value(DUE_DATE.toString()));

        ArgumentCaptor<UUID> taskIdCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<UUID> assigneeCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(followUpService).assignTask(taskIdCaptor.capture(), assigneeCaptor.capture());
        assertThat(taskIdCaptor.getValue()).isEqualTo(TASK_ID);
        assertThat(assigneeCaptor.getValue()).isEqualTo(ASSIGNEE_ID);
    }

    @Test
    void customerServiceAgentCanAssignFollowUpTask() throws Exception {
        when(jwtService.validateToken("csa-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CUSTOMER_SERVICE_AGENT));
        when(followUpService.assignTask(eq(TASK_ID), eq(ASSIGNEE_ID)))
                .thenReturn(assignedTask(ASSIGNEE_ID, "Test Sales Agent"));

        mockMvc.perform(
                        put("/api/follow-up-tasks/{id}/assign", TASK_ID)
                                .header("Authorization", "Bearer csa-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(assignPayload(ASSIGNEE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Follow-up task assigned"));

        verify(followUpService).assignTask(TASK_ID, ASSIGNEE_ID);
    }

    @Test
    void campaignManagerCanAssignFollowUpTask() throws Exception {
        when(jwtService.validateToken("cm-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CAMPAIGN_MANAGER));
        when(followUpService.assignTask(eq(TASK_ID), eq(ASSIGNEE_ID)))
                .thenReturn(assignedTask(ASSIGNEE_ID, "Test Sales Agent"));

        mockMvc.perform(
                        put("/api/follow-up-tasks/{id}/assign", TASK_ID)
                                .header("Authorization", "Bearer cm-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(assignPayload(ASSIGNEE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Follow-up task assigned"));

        verify(followUpService).assignTask(TASK_ID, ASSIGNEE_ID);
    }

    @Test
    void adminCanAssignFollowUpTask() throws Exception {
        when(jwtService.validateToken("admin-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.ADMIN));
        when(followUpService.assignTask(eq(TASK_ID), eq(ASSIGNEE_ID)))
                .thenReturn(assignedTask(ASSIGNEE_ID, "Test Sales Agent"));

        mockMvc.perform(
                        put("/api/follow-up-tasks/{id}/assign", TASK_ID)
                                .header("Authorization", "Bearer admin-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(assignPayload(ASSIGNEE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.campaignId").value(CAMPAIGN_ID.toString()));

        verify(followUpService).assignTask(TASK_ID, ASSIGNEE_ID);
    }

    @Test
    void followUpTaskCanBeReassignedToAnotherUser() throws Exception {
        UUID otherAssigneeId = UUID.fromString("10000000-0000-0000-0000-000000000006");

        when(jwtService.validateToken("sales-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.SALES_AGENT));
        when(followUpService.assignTask(eq(TASK_ID), eq(otherAssigneeId)))
                .thenReturn(assignedTask(otherAssigneeId, "Test Customer Service Agent"));

        mockMvc.perform(
                        put("/api/follow-up-tasks/{id}/assign", TASK_ID)
                                .header("Authorization", "Bearer sales-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(assignPayload(otherAssigneeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Follow-up task assigned"))
                .andExpect(jsonPath("$.data.assignedToUserId").value(otherAssigneeId.toString()))
                .andExpect(
                        jsonPath("$.data.assignedToFullName")
                                .value("Test Customer Service Agent"));

        verify(followUpService).assignTask(TASK_ID, otherAssigneeId);
    }

    @ParameterizedTest
    @MethodSource("unauthorizedAssignRoles")
    void unauthorizedRolesCannotAssignFollowUpTask(SystemRoleName role) throws Exception {
        when(jwtService.validateToken("denied-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(role));

        mockMvc.perform(
                        put("/api/follow-up-tasks/{id}/assign", TASK_ID)
                                .header("Authorization", "Bearer denied-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(assignPayload(ASSIGNEE_ID)))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("Follow-up task assigned"))));

        verify(followUpService, never()).assignTask(eq(TASK_ID), eq(ASSIGNEE_ID));
    }

    @Test
    void unauthenticatedRequestCannotAssignFollowUpTask() throws Exception {
        mockMvc.perform(
                        put("/api/follow-up-tasks/{id}/assign", TASK_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(assignPayload(ASSIGNEE_ID)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(not(containsString("Follow-up task assigned"))));

        verify(followUpService, never()).assignTask(eq(TASK_ID), eq(ASSIGNEE_ID));
    }

    @Test
    void assignRequestRequiresAssignedToBodyField() throws Exception {
        when(jwtService.validateToken("sales-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.SALES_AGENT));

        mockMvc.perform(
                        put("/api/follow-up-tasks/{id}/assign", TASK_ID)
                                .header("Authorization", "Bearer sales-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest());

        verify(followUpService, never()).assignTask(eq(TASK_ID), eq(ASSIGNEE_ID));
    }

    @Test
    void assignPropagatesServiceNotFoundAndValidationErrors() throws Exception {
        when(jwtService.validateToken("sales-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.SALES_AGENT));
        when(followUpService.assignTask(eq(TASK_ID), eq(ASSIGNEE_ID)))
                .thenThrow(new ResourceNotFoundException("FollowUpTask", TASK_ID));

        mockMvc.perform(
                        put("/api/follow-up-tasks/{id}/assign", TASK_ID)
                                .header("Authorization", "Bearer sales-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(assignPayload(ASSIGNEE_ID)))
                .andExpect(status().isNotFound());

        when(followUpService.assignTask(eq(TASK_ID), eq(ASSIGNEE_ID)))
                .thenThrow(new ValidationException("assignedTo is required", List.of()));

        mockMvc.perform(
                        put("/api/follow-up-tasks/{id}/assign", TASK_ID)
                                .header("Authorization", "Bearer sales-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(assignPayload(ASSIGNEE_ID)))
                .andExpect(status().isBadRequest());
    }

    private static Stream<SystemRoleName> unauthorizedAssignRoles() {
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
                UUID.fromString("10000000-0000-0000-0000-000000000391"),
                "test.user@bayer-westphalian.test",
                List.of(role));
    }

    private static String assignPayload(UUID assignedTo) {
        return """
                {
                  "assignedTo": "%s"
                }
                """
                .formatted(assignedTo);
    }

    private static FollowUpTask assignedTask(UUID assigneeId, String assigneeName) {
        Customer customer = Customer.create(CustomerType.PROSPECT, "Ada", "Prospect");
        ReflectionTestUtils.setField(customer, "id", CUSTOMER_ID);

        User assignee = User.create("assignee@bayer-westphalian.test", "{noop}x", assigneeName);
        ReflectionTestUtils.setField(assignee, "id", assigneeId);

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
        return task;
    }
}
