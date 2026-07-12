package com.bayerwestphalian.campaign.followup;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import com.bayerwestphalian.campaign.user.User;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = FollowUpController.class)
@Import({
    SecurityConfiguration.class,
    JwtAuthenticationFilter.class,
    AuthorizationExpressions.class,
    GlobalExceptionHandler.class
})
class FollowUpControllerTests {

    private static final UUID CUSTOMER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID AGENT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID CAMPAIGN_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID TASK_ID = UUID.fromString("70000000-0000-0000-0000-000000000001");

    @Autowired private MockMvc mockMvc;

    @MockBean private FollowUpService followUpService;
    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private Customer customer;
    private User agent;
    private FollowUpTask task;

    @BeforeEach
    void setUp() {
        customer = Customer.create(CustomerType.PROSPECT, "Ada", "Lovelace");
        ReflectionTestUtils.setField(customer, "id", CUSTOMER_ID);

        agent = User.create("sales.agent@test.example", "{noop}password", "Sales Agent");
        ReflectionTestUtils.setField(agent, "id", AGENT_ID);

        task = new FollowUpTask(customer, agent, "Call Ada back", LocalDate.of(2026, 9, 15));
        ReflectionTestUtils.setField(task, "id", TASK_ID);
        ReflectionTestUtils.setField(task, "createdAt", Instant.parse("2026-07-10T12:00:00Z"));
    }

    @Test
    void customerServiceAgentCanCreateTask() throws Exception {
        when(jwtService.validateToken("csa-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CUSTOMER_SERVICE_AGENT));
        when(followUpService.createTask(any(CreateFollowUpTaskCommand.class))).thenReturn(task);

        String payload =
                """
                {
                  "customerId": "%s",
                  "campaignId": "%s",
                  "assignedTo": "%s",
                  "title": "Call Ada back",
                  "description": "Discuss premium options",
                  "dueDate": "2026-09-15",
                  "priority": "HIGH"
                }
                """
                        .formatted(CUSTOMER_ID, CAMPAIGN_ID, AGENT_ID);

        mockMvc.perform(
                        post("/api/follow-up-tasks")
                                .header("Authorization", "Bearer csa-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Follow-up task created"))
                .andExpect(jsonPath("$.data.id").value(TASK_ID.toString()))
                .andExpect(jsonPath("$.data.title").value("Call Ada back"))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.priority").value("MEDIUM"));

        verify(followUpService)
                .createTask(
                        argThat(
                                command ->
                                        command.customerId().equals(CUSTOMER_ID)
                                                && command.campaignId().equals(CAMPAIGN_ID)
                                                && command.assignedTo().equals(AGENT_ID)
                                                && command.title().equals("Call Ada back")
                                                && command.description()
                                                        .equals("Discuss premium options")
                                                && command.dueDate()
                                                        .equals(LocalDate.of(2026, 9, 15))
                                                && command.priority() == FollowUpTaskPriority.HIGH));
    }

    @Test
    void campaignManagerCanCreateTask() throws Exception {
        when(jwtService.validateToken("campaign-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CAMPAIGN_MANAGER));
        when(followUpService.createTask(any(CreateFollowUpTaskCommand.class))).thenReturn(task);

        String payload =
                """
                {
                  "customerId": "%s",
                  "title": "Call Ada back"
                }
                """
                        .formatted(CUSTOMER_ID);

        mockMvc.perform(
                        post("/api/follow-up-tasks")
                                .header("Authorization", "Bearer campaign-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Follow-up task created"))
                .andExpect(jsonPath("$.data.id").value(TASK_ID.toString()));

        verify(followUpService)
                .createTask(
                        argThat(
                                command ->
                                        command.customerId().equals(CUSTOMER_ID)
                                                && command.campaignId() == null
                                                && command.assignedTo() == null
                                                && command.title().equals("Call Ada back")
                                                && command.description() == null
                                                && command.dueDate() == null
                                                && command.priority()
                                                        == FollowUpTaskPriority.MEDIUM));
    }

    @Test
    void salesAgentCanAssignTask() throws Exception {
        when(jwtService.validateToken("agent-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.SALES_AGENT));

        User otherAgent = User.create("other@test.example", "{noop}password", "Other Agent");
        UUID otherAgentId = UUID.fromString("10000000-0000-0000-0000-000000000009");
        ReflectionTestUtils.setField(otherAgent, "id", otherAgentId);

        task.assignTo(otherAgent);
        when(followUpService.assignTask(eq(TASK_ID), eq(otherAgentId))).thenReturn(task);

        String payload =
                """
                {
                  "assignedTo": "%s"
                }
                """
                        .formatted(otherAgentId);

        mockMvc.perform(
                        put("/api/follow-up-tasks/{id}/assign", TASK_ID)
                                .header("Authorization", "Bearer agent-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Follow-up task assigned"))
                .andExpect(jsonPath("$.data.assignedToUserId").value(otherAgentId.toString()))
                .andExpect(jsonPath("$.data.assignedToFullName").value("Other Agent"));

        verify(followUpService).assignTask(TASK_ID, otherAgentId);
    }

    @Test
    void campaignManagerCanAssignTask() throws Exception {
        when(jwtService.validateToken("campaign-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CAMPAIGN_MANAGER));

        User otherAgent =
                User.create(
                        "campaign-assignee@test.example", "{noop}password", "Other Agent");
        UUID otherAgentId = UUID.fromString("10000000-0000-0000-0000-000000000011");
        ReflectionTestUtils.setField(otherAgent, "id", otherAgentId);

        task.assignTo(otherAgent);
        when(followUpService.assignTask(eq(TASK_ID), eq(otherAgentId))).thenReturn(task);

        String payload =
                """
                {
                  "assignedTo": "%s"
                }
                """
                        .formatted(otherAgentId);

        mockMvc.perform(
                        put("/api/follow-up-tasks/{id}/assign", TASK_ID)
                                .header("Authorization", "Bearer campaign-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Follow-up task assigned"))
                .andExpect(jsonPath("$.data.assignedToUserId").value(otherAgentId.toString()));

        verify(followUpService).assignTask(TASK_ID, otherAgentId);
    }

    @Test
    void salesAgentCanCompleteTask() throws Exception {
        when(jwtService.validateToken("agent-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.SALES_AGENT));

        task.complete();
        when(followUpService.completeTask(TASK_ID)).thenReturn(task);

        mockMvc.perform(
                        put("/api/follow-up-tasks/{id}/complete", TASK_ID)
                                .header("Authorization", "Bearer agent-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Follow-up task completed"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.completedAt").exists());

        verify(followUpService).completeTask(TASK_ID);
    }

    @Test
    void salesAgentCanUpdateTaskStatus() throws Exception {
        when(jwtService.validateToken("agent-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.SALES_AGENT));

        task.updateStatus(FollowUpTaskStatus.IN_PROGRESS);
        when(followUpService.updateTaskStatus(eq(TASK_ID), eq(FollowUpTaskStatus.IN_PROGRESS)))
                .thenReturn(task);

        mockMvc.perform(
                        put("/api/follow-up-tasks/{id}/status", TASK_ID)
                                .header("Authorization", "Bearer agent-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Follow-up task status updated"))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        verify(followUpService).updateTaskStatus(TASK_ID, FollowUpTaskStatus.IN_PROGRESS);
    }

    @Test
    void salesAgentCanUpdateTaskDetails() throws Exception {
        when(jwtService.validateToken("agent-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.SALES_AGENT));

        task.setDescription("New detailed notes");
        task.updatePriority(FollowUpTaskPriority.HIGH);
        when(followUpService.updateTask(
                        eq(TASK_ID), eq("New detailed notes"), eq(FollowUpTaskPriority.HIGH)))
                .thenReturn(task);

        String payload =
                """
                {
                  "description": "New detailed notes",
                  "priority": "HIGH"
                }
                """;

        mockMvc.perform(
                        put("/api/follow-up-tasks/{id}", TASK_ID)
                                .header("Authorization", "Bearer agent-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Follow-up task updated"))
                .andExpect(jsonPath("$.data.description").value("New detailed notes"))
                .andExpect(jsonPath("$.data.priority").value("HIGH"));

        verify(followUpService)
                .updateTask(TASK_ID, "New detailed notes", FollowUpTaskPriority.HIGH);
    }

    @Test
    void salesAgentCanListAssignedTasks() throws Exception {
        when(jwtService.validateToken("agent-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.SALES_AGENT));
        when(followUpService.searchTasks(any(FollowUpTaskSearchCriteria.class)))
                .thenReturn(List.of(task));

        mockMvc.perform(
                        get("/api/follow-up-tasks")
                                .header("Authorization", "Bearer agent-token")
                                .param("customerId", CUSTOMER_ID.toString())
                                .param("assignedTo", AGENT_ID.toString())
                                .param("priority", "MEDIUM")
                                .param("status", "OPEN")
                                .param("dueDateFrom", "2026-09-01")
                                .param("dueDateTo", "2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Follow-up tasks loaded"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(TASK_ID.toString()));

        verify(followUpService)
                .searchTasks(
                        argThat(
                                criteria ->
                                        criteria.customerId().equals(CUSTOMER_ID)
                                                && criteria.assignedTo().equals(AGENT_ID)
                                                && criteria.priority()
                                                        == FollowUpTaskPriority.MEDIUM
                                                && criteria.status() == FollowUpTaskStatus.OPEN
                                                && criteria.dueDateFrom()
                                                        .equals(LocalDate.of(2026, 9, 1))
                                                && criteria.dueDateTo()
                                                        .equals(LocalDate.of(2026, 9, 30))));
    }

    private static JwtTokenClaims roleClaims(SystemRoleName role) {
        return new JwtTokenClaims(
                UUID.fromString("10000000-0000-0000-0000-000000000002"),
                "test.user@bayer-westphalian.test",
                List.of(role));
    }
}
