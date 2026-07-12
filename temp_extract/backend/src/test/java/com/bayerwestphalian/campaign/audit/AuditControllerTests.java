package com.bayerwestphalian.campaign.audit;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuditControllerTests {

    private static final UUID AUDIT_ID =
            UUID.fromString("53000000-0000-0000-0000-000000000001");
    private static final UUID PRODUCT_ID =
            UUID.fromString("41000000-0000-0000-0000-000000000201");
    private static final UUID CONSENT_ID =
            UUID.fromString("53000000-0000-0000-0000-000000000101");
    private static final UUID ACTOR_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000001");
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
}
