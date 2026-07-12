package com.bayerwestphalian.campaign.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

/**
 * KB item 519: {@link AuditController} unit coverage for {@code GET /api/audit-logs} and entity
 * history endpoints (E22 / Admin, Compliance, System Auditor).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("519 Implement AuditController")
class AuditControllerTests {

    private static final UUID AUDIT_ID = UUID.fromString("53000000-0000-0000-0000-000000000001");
    private static final UUID PRODUCT_ID = UUID.fromString("41000000-0000-0000-0000-000000000201");
    private static final UUID CONSENT_ID = UUID.fromString("53000000-0000-0000-0000-000000000101");
    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000519");
    private static final UUID ACTOR_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final Instant CREATED_AT = Instant.parse("2026-07-07T15:02:46Z");

    @Mock private AuditService auditService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(new AuditController(auditService))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    @Test
    @DisplayName("552 Audit log cannot be edited by normal users")
    void auditControllerExposesReadOnlyEndpointsOnly() {
        List<Method> controllerMethods =
                Arrays.stream(AuditController.class.getDeclaredMethods())
                        .filter(method -> !method.isSynthetic())
                        .toList();

        assertThat(controllerMethods)
                .allMatch(method -> method.isAnnotationPresent(GetMapping.class));
        assertThat(controllerMethods)
                .noneMatch(method -> method.isAnnotationPresent(PostMapping.class))
                .noneMatch(method -> method.isAnnotationPresent(PutMapping.class))
                .noneMatch(method -> method.isAnnotationPresent(PatchMapping.class))
                .noneMatch(method -> method.isAnnotationPresent(DeleteMapping.class));
    }

    @Test
    @DisplayName("553 Unauthorized user cannot view audit logs")
    void auditReadEndpointsRequireAuditRoles() {
        List<Method> controllerMethods =
                Arrays.stream(AuditController.class.getDeclaredMethods())
                        .filter(method -> !method.isSynthetic())
                        .toList();

        assertThat(controllerMethods).isNotEmpty();
        assertThat(controllerMethods)
                .allSatisfy(
                        method -> {
                            PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
                            assertThat(preAuthorize).as(method.getName()).isNotNull();
                            assertThat(preAuthorize.value())
                                    .as(method.getName())
                                    .isEqualTo(
                                            "@authz.hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER',"
                                                    + " 'SYSTEM_AUDITOR')");
                        });
    }

    @Test
    void listsConsentAndProductAuditLogsForAuditPageEvidence() throws Exception {
        when(auditService.listAuditLogs())
                .thenReturn(
                        List.of(
                                new AuditLogView(
                                        AUDIT_ID,
                                        ACTOR_ID,
                                        "CREATE",
                                        "products",
                                        PRODUCT_ID,
                                        null,
                                        Map.of("name", "Life Protection"),
                                        null,
                                        CREATED_AT),
                                new AuditLogView(
                                        UUID.fromString("53000000-0000-0000-0000-000000000002"),
                                        ACTOR_ID,
                                        "WITHDRAW_CONSENT",
                                        "consent_records",
                                        CONSENT_ID,
                                        Map.of("status", "GIVEN"),
                                        Map.of("status", "WITHDRAWN"),
                                        null,
                                        CREATED_AT.minusSeconds(60))));

        mockMvc.perform(get("/api/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Audit logs loaded"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].entityType").value("products"))
                .andExpect(jsonPath("$.data[0].newValue.name").value("Life Protection"))
                .andExpect(jsonPath("$.data[1].action").value("WITHDRAW_CONSENT"))
                .andExpect(jsonPath("$.data[1].entityType").value("consent_records"));

        verify(auditService).listAuditLogs();
        verify(auditService, never()).listAuditLogs(any(AuditLogSearchCriteria.class));
    }

    @Test
    void listsProductChangeRequestAuditLogsForTrackingEvidence() throws Exception {
        UUID requestId = UUID.fromString("42000000-0000-0000-0000-000000000001");

        when(auditService.listAuditLogs())
                .thenReturn(
                        List.of(
                                new AuditLogView(
                                        UUID.fromString("53000000-0000-0000-0000-000000000003"),
                                        ACTOR_ID,
                                        "CREATE",
                                        "product_change_requests",
                                        requestId,
                                        null,
                                        Map.of(
                                                "productId",
                                                PRODUCT_ID,
                                                "requestType",
                                                "PRICE_CHANGE",
                                                "status",
                                                "OPEN"),
                                        null,
                                        CREATED_AT),
                                new AuditLogView(
                                        UUID.fromString("53000000-0000-0000-0000-000000000004"),
                                        ACTOR_ID,
                                        "APPROVE",
                                        "product_change_requests",
                                        requestId,
                                        Map.of("status", "OPEN"),
                                        Map.of("status", "APPROVED"),
                                        null,
                                        CREATED_AT.minusSeconds(30))));

        mockMvc.perform(get("/api/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].entityType").value("product_change_requests"))
                .andExpect(jsonPath("$.data[0].newValue.status").value("OPEN"))
                .andExpect(jsonPath("$.data[1].action").value("APPROVE"))
                .andExpect(jsonPath("$.data[1].oldValue.status").value("OPEN"))
                .andExpect(jsonPath("$.data[1].newValue.status").value("APPROVED"));
    }

    @Test
    void listsAuditLogsWithActorActionEntityAndDateFilters() throws Exception {
        Instant from = Instant.parse("2026-07-01T00:00:00Z");
        Instant to = Instant.parse("2026-07-11T23:59:59Z");
        when(auditService.listAuditLogs(any(AuditLogSearchCriteria.class)))
                .thenReturn(
                        List.of(
                                new AuditLogView(
                                        AUDIT_ID,
                                        ACTOR_ID,
                                        "APPROVE",
                                        "campaigns",
                                        CAMPAIGN_ID,
                                        Map.of("status", "SUBMITTED"),
                                        Map.of("status", "APPROVED"),
                                        "203.0.113.10",
                                        CREATED_AT)));

        mockMvc.perform(
                        get("/api/audit-logs")
                                .param("actorUserId", ACTOR_ID.toString())
                                .param("action", "APPROVE")
                                .param("entityType", "campaigns")
                                .param("entityId", CAMPAIGN_ID.toString())
                                .param("createdFrom", from.toString())
                                .param("createdTo", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Audit logs loaded"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].action").value("APPROVE"))
                .andExpect(jsonPath("$.data[0].entityType").value("campaigns"))
                .andExpect(jsonPath("$.data[0].entityId").value(CAMPAIGN_ID.toString()))
                .andExpect(jsonPath("$.data[0].ipAddress").value("203.0.113.10"));

        ArgumentCaptor<AuditLogSearchCriteria> captor =
                ArgumentCaptor.forClass(AuditLogSearchCriteria.class);
        verify(auditService).listAuditLogs(captor.capture());
        verify(auditService, never()).listAuditLogs();
        AuditLogSearchCriteria criteria = captor.getValue();
        assertThat(criteria.actorUserId()).isEqualTo(ACTOR_ID);
        assertThat(criteria.action()).isEqualTo("APPROVE");
        assertThat(criteria.entityType()).isEqualTo("campaigns");
        assertThat(criteria.entityId()).isEqualTo(CAMPAIGN_ID);
        assertThat(criteria.createdFrom()).isEqualTo(from);
        assertThat(criteria.createdTo()).isEqualTo(to);
    }

    @Test
    void getEntityHistoryViaQueryParams() throws Exception {
        when(auditService.getEntityHistory("campaigns", CAMPAIGN_ID))
                .thenReturn(
                        List.of(
                                new AuditLogView(
                                        AUDIT_ID,
                                        ACTOR_ID,
                                        "LAUNCH",
                                        "campaigns",
                                        CAMPAIGN_ID,
                                        Map.of("status", "APPROVED"),
                                        Map.of("status", "ACTIVE"),
                                        null,
                                        CREATED_AT),
                                new AuditLogView(
                                        UUID.fromString("53000000-0000-0000-0000-000000000519"),
                                        ACTOR_ID,
                                        "APPROVE",
                                        "campaigns",
                                        CAMPAIGN_ID,
                                        Map.of("status", "SUBMITTED"),
                                        Map.of("status", "APPROVED"),
                                        null,
                                        CREATED_AT.minusSeconds(120))));

        mockMvc.perform(
                        get("/api/audit-logs/entity-history")
                                .param("entityType", "campaigns")
                                .param("entityId", CAMPAIGN_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Entity audit history loaded"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].action").value("LAUNCH"))
                .andExpect(jsonPath("$.data[1].action").value("APPROVE"));

        verify(auditService).getEntityHistory("campaigns", CAMPAIGN_ID);
    }

    @Test
    void getEntityHistoryViaPathVariables() throws Exception {
        when(auditService.getEntityHistory("consent_records", CONSENT_ID))
                .thenReturn(
                        List.of(
                                new AuditLogView(
                                        AUDIT_ID,
                                        ACTOR_ID,
                                        "WITHDRAW_CONSENT",
                                        "consent_records",
                                        CONSENT_ID,
                                        Map.of("status", "GIVEN"),
                                        Map.of("status", "WITHDRAWN"),
                                        null,
                                        CREATED_AT)));

        mockMvc.perform(get("/api/audit-logs/entities/consent_records/" + CONSENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Entity audit history loaded"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].entityType").value("consent_records"))
                .andExpect(jsonPath("$.data[0].action").value("WITHDRAW_CONSENT"));

        verify(auditService).getEntityHistory(eq("consent_records"), eq(CONSENT_ID));
    }

    @Test
    void getEntityHistoryReturnsValidationErrorWhenServiceRejectsBlankEntityType()
            throws Exception {
        when(auditService.getEntityHistory(eq(" "), any(UUID.class)))
                .thenThrow(
                        new ValidationException(
                                "Audit validation failed",
                                List.of("entityType: must not be blank")));

        mockMvc.perform(
                        get("/api/audit-logs/entity-history")
                                .param("entityType", " ")
                                .param("entityId", CAMPAIGN_ID.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Audit validation failed"));
    }

    @Test
    void getEntityHistoryRequiresEntityIdQueryParam() throws Exception {
        mockMvc.perform(get("/api/audit-logs/entity-history").param("entityType", "campaigns"))
                .andExpect(status().isBadRequest());

        verify(auditService, never()).getEntityHistory(any(), any());
    }

    @Test
    void getEntityHistoryRequiresEntityTypeQueryParam() throws Exception {
        mockMvc.perform(
                        get("/api/audit-logs/entity-history")
                                .param("entityId", CAMPAIGN_ID.toString()))
                .andExpect(status().isBadRequest());

        verify(auditService, never()).getEntityHistory(any(), any());
    }
}
