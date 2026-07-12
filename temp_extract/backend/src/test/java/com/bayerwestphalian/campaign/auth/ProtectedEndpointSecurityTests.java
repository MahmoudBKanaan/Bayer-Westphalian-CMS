package com.bayerwestphalian.campaign.auth;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.user.SystemRoleName;
import java.time.Instant;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = ProtectedEndpointSecurityTests.ProtectedEndpointController.class)
@Import({
    SecurityConfiguration.class,
    JwtAuthenticationFilter.class,
    ProtectedEndpointSecurityTests.ProtectedEndpointController.class
})
class ProtectedEndpointSecurityTests {

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private final MockMvc mockMvc;

    @Autowired
    ProtectedEndpointSecurityTests(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void protectedEndpointFailsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/protected-check"))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(not(containsString("protected user data"))));
    }

    @Test
    void protectedEndpointFailsWithInsufficientRole() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());

        mockMvc.perform(
                        get("/api/users/protected-check")
                                .header("Authorization", "Bearer campaign-manager-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("protected user data"))));
    }

    @Test
    void campaignManagerCannotManageUsers() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(jwtService.validateToken("campaign-manager-disable-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(jwtService.validateToken("campaign-manager-role-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());

        mockMvc.perform(
                        post("/api/users")
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "email": "advisor@bayer-westphalian.test",
                                          "password": "StrongPassword!2026",
                                          "fullName": "Advisor User"
                                        }
                                        """))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("user created"))));

        mockMvc.perform(
                        patch(
                                        "/api/users/{id}/disable",
                                        UUID.fromString("10000000-0000-0000-0000-000000009901"))
                                .header("Authorization", "Bearer campaign-manager-disable-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("user disabled"))));

        mockMvc.perform(
                        post(
                                        "/api/users/{id}/roles",
                                        UUID.fromString("10000000-0000-0000-0000-000000009901"))
                                .header("Authorization", "Bearer campaign-manager-role-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "roleName": "ADMIN",
                                          "assignedByUserId": "10000000-0000-0000-0000-000000009902"
                                        }
                                        """))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("role assigned"))));
    }

    @Test
    void adminCanAccessCreateUserEndpoint() throws Exception {
        when(jwtService.validateToken("admin-token", JwtTokenType.ACCESS))
                .thenReturn(adminClaims());

        mockMvc.perform(
                        post("/api/users")
                                .header("Authorization", "Bearer admin-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "email": "advisor@bayer-westphalian.test",
                                          "password": "StrongPassword!2026",
                                          "fullName": "Advisor User"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(content().string("user created"));
    }

    @Test
    void adminCanAccessDisableUserEndpoint() throws Exception {
        when(jwtService.validateToken("admin-token", JwtTokenType.ACCESS))
                .thenReturn(adminClaims());

        mockMvc.perform(
                        patch(
                                        "/api/users/{id}/disable",
                                        UUID.fromString("10000000-0000-0000-0000-000000009902"))
                                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("user disabled"));
    }

    @Test
    void adminCanAccessAssignRoleEndpoint() throws Exception {
        when(jwtService.validateToken("admin-token", JwtTokenType.ACCESS))
                .thenReturn(adminClaims());

        mockMvc.perform(
                        post(
                                        "/api/users/{id}/roles",
                                        UUID.fromString("10000000-0000-0000-0000-000000009902"))
                                .header("Authorization", "Bearer admin-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "roleName": "CAMPAIGN_MANAGER",
                                          "assignedByUserId": "10000000-0000-0000-0000-000000009901"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(content().string("role assigned"));
    }

    @Test
    void unauthorizedRoleCannotEditCustomers() throws Exception {
        when(jwtService.validateToken("bi-analyst-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.BI_ANALYST));
        when(jwtService.validateToken("customer-service-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CUSTOMER_SERVICE_AGENT));

        mockMvc.perform(
                        put(
                                        "/api/customers/{id}",
                                        UUID.fromString("20000000-0000-0000-0000-000000000001"))
                                .header("Authorization", "Bearer bi-analyst-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(customerUpdatePayload()))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("customer updated"))));

        mockMvc.perform(
                        put(
                                        "/api/customers/{id}",
                                        UUID.fromString("20000000-0000-0000-0000-000000000001"))
                                .header("Authorization", "Bearer customer-service-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(customerUpdatePayload()))
                .andExpect(status().isOk())
                .andExpect(content().string("customer updated"));
    }

    @Test
    void kbProtectedResourcesFailWithoutAuthentication() throws Exception {
        List<String> protectedResources =
                List.of(
                        "/api/products/protected-check",
                        "/api/segments/protected-check",
                        "/api/beneficiaries/protected-check",
                        "/api/consents/protected-check",
                        "/api/campaigns/protected-check",
                        "/api/reminders/protected-check",
                        "/api/analytics/protected-check",
                        "/api/reports/protected-check",
                        "/api/ai/protected-check",
                        "/api/audit-logs/protected-check");

        for (String protectedResource : protectedResources) {
            mockMvc.perform(get(protectedResource))
                    .andExpect(status().is4xxClientError())
                    .andExpect(content().string(not(containsString("protected"))));
        }
    }

    @Test
    void protectedProductReadRequiresKbProductReadRole() throws Exception {
        when(jwtService.validateToken("marketing-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.MARKETING_ANALYST));
        when(jwtService.validateToken("sales-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.SALES_AGENT));

        mockMvc.perform(
                        get("/api/products/protected-check")
                                .header("Authorization", "Bearer marketing-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("protected product data"))));

        mockMvc.perform(
                        get("/api/products/protected-check")
                                .header("Authorization", "Bearer sales-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("protected product data"));
    }

    @Test
    void protectedBeneficiaryReadRequiresKbBeneficiaryReadRole() throws Exception {
        when(jwtService.validateToken("sales-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.SALES_AGENT));
        when(jwtService.validateToken("auditor-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.SYSTEM_AUDITOR));

        mockMvc.perform(
                        get("/api/beneficiaries/protected-check")
                                .header("Authorization", "Bearer sales-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("protected beneficiary data"))));

        mockMvc.perform(
                        get("/api/beneficiaries/protected-check")
                                .header("Authorization", "Bearer auditor-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("protected beneficiary data"));
    }

    @Test
    void protectedBeneficiaryWriteRequiresKbBeneficiaryWriteRole() throws Exception {
        when(jwtService.validateToken("bi-analyst-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.BI_ANALYST));
        when(jwtService.validateToken("compliance-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.COMPLIANCE_OFFICER));
        when(jwtService.validateToken("customer-service-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CUSTOMER_SERVICE_AGENT));

        mockMvc.perform(
                        post("/api/beneficiaries")
                                .header("Authorization", "Bearer bi-analyst-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("beneficiary created"))));

        mockMvc.perform(
                        put("/api/beneficiaries/{id}", UUID.randomUUID())
                                .header("Authorization", "Bearer compliance-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().string("beneficiary updated"));

        mockMvc.perform(
                        post("/api/beneficiaries")
                                .header("Authorization", "Bearer customer-service-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().string("beneficiary created"));
    }

    @Test
    void protectedConsentReadRequiresKbConsentReadRole() throws Exception {
        when(jwtService.validateToken("sales-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.SALES_AGENT));
        when(jwtService.validateToken("auditor-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.SYSTEM_AUDITOR));

        mockMvc.perform(
                        get("/api/consents/protected-check")
                                .header("Authorization", "Bearer sales-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("protected consent data"))));

        mockMvc.perform(
                        get("/api/consents/protected-check")
                                .header("Authorization", "Bearer auditor-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("protected consent data"));
    }

    @Test
    void protectedConsentWriteRequiresKbConsentWriteRole() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(jwtService.validateToken("customer-service-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CUSTOMER_SERVICE_AGENT));

        mockMvc.perform(
                        post("/api/consents")
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("consent recorded"))));

        mockMvc.perform(
                        post("/api/consents")
                                .header("Authorization", "Bearer customer-service-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().string("consent recorded"));
    }

    @Test
    void protectedCampaignReadRequiresKbCampaignReadRole() throws Exception {
        when(jwtService.validateToken("marketing-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.MARKETING_ANALYST));
        when(jwtService.validateToken("auditor-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.SYSTEM_AUDITOR));

        mockMvc.perform(
                        get("/api/campaigns/protected-check")
                                .header("Authorization", "Bearer marketing-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("protected campaign data"))));

        mockMvc.perform(
                        get("/api/campaigns/protected-check")
                                .header("Authorization", "Bearer auditor-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("protected campaign data"));
    }

    @Test
    void unauthorizedRoleCannotApproveComplianceCampaign() throws Exception {
        UUID campaignId = UUID.fromString("50000000-0000-0000-0000-000000000101");
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(jwtService.validateToken("compliance-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.COMPLIANCE_OFFICER));

        mockMvc.perform(
                        post("/api/campaigns/{id}/approve", campaignId)
                                .header("Authorization", "Bearer campaign-manager-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("campaign approved"))));

        mockMvc.perform(
                        post("/api/campaigns/{id}/approve", campaignId)
                                .header("Authorization", "Bearer compliance-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("campaign approved"));
    }

    @Test
    void unauthorizedRoleCannotRecordComplianceReviewNotes() throws Exception {
        UUID campaignId = UUID.fromString("50000000-0000-0000-0000-000000000101");
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(jwtService.validateToken("compliance-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.COMPLIANCE_OFFICER));
        when(jwtService.validateToken("admin-token", JwtTokenType.ACCESS))
                .thenReturn(adminClaims());

        mockMvc.perform(
                        put("/api/campaigns/{id}/compliance-review-notes", campaignId)
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"complianceReviewNotes\":\"Draft notes\"}"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("compliance notes recorded"))));

        mockMvc.perform(
                        put("/api/campaigns/{id}/compliance-review-notes", campaignId)
                                .header("Authorization", "Bearer compliance-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"complianceReviewNotes\":\"Pending legal review\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("compliance notes recorded"));

        mockMvc.perform(
                        put("/api/campaigns/{id}/compliance-review-notes", campaignId)
                                .header("Authorization", "Bearer admin-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"complianceReviewNotes\":\"Admin notes\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("compliance notes recorded"));
    }

    @Test
    void segmentEndpointsFollowKbRoleRules() throws Exception {
        UUID segmentId = UUID.fromString("42000000-0000-0000-0000-000000000001");
        when(jwtService.validateToken("bi-analyst-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.BI_ANALYST));
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(jwtService.validateToken("compliance-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.COMPLIANCE_OFFICER));
        when(jwtService.validateToken("product-manager-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.PRODUCT_MANAGER));
        when(jwtService.validateToken("admin-token", JwtTokenType.ACCESS))
                .thenReturn(adminClaims());

        mockMvc.perform(
                        get("/api/segments/protected-check")
                                .header("Authorization", "Bearer bi-analyst-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("protected segment data"));

        mockMvc.perform(
                        get("/api/segments/protected-check")
                                .header("Authorization", "Bearer compliance-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("protected segment data"));

        mockMvc.perform(
                        get("/api/segments/protected-check")
                                .header("Authorization", "Bearer product-manager-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("protected segment data"))));

        mockMvc.perform(
                        get("/api/segments/{id}", segmentId)
                                .header("Authorization", "Bearer bi-analyst-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("segment details loaded"));

        mockMvc.perform(
                        get("/api/segments/{id}", segmentId)
                                .header("Authorization", "Bearer compliance-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("segment details loaded"));

        mockMvc.perform(
                        get("/api/segments/{id}", segmentId)
                                .header("Authorization", "Bearer product-manager-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("segment details loaded"))));

        mockMvc.perform(
                        post("/api/segments/preview")
                                .header("Authorization", "Bearer bi-analyst-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().string("segment preview loaded"));

        mockMvc.perform(
                        post("/api/segments/preview")
                                .header("Authorization", "Bearer compliance-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("segment preview loaded"))));

        mockMvc.perform(
                        post("/api/segments")
                                .header("Authorization", "Bearer bi-analyst-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("segment created"))));

        mockMvc.perform(
                        post("/api/segments")
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().string("segment created"));

        mockMvc.perform(
                        post("/api/segments")
                                .header("Authorization", "Bearer admin-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().string("segment created"));

        mockMvc.perform(
                        put("/api/segments/{id}", segmentId)
                                .header("Authorization", "Bearer bi-analyst-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("segment updated"))));

        mockMvc.perform(
                        delete("/api/segments/{id}", segmentId)
                                .header("Authorization", "Bearer bi-analyst-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("segment deleted"))));

        mockMvc.perform(
                        delete("/api/segments/{id}", segmentId)
                                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("segment deleted"));
    }

    @Test
    void productManagerPermissionsFollowKbRules() throws Exception {
        when(jwtService.validateToken("product-manager-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.PRODUCT_MANAGER));

        mockMvc.perform(
                        get("/api/customers/protected-check")
                                .header("Authorization", "Bearer product-manager-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("protected customer data"));

        mockMvc.perform(
                        post("/api/customers")
                                .header("Authorization", "Bearer product-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("customer created"))));

        mockMvc.perform(
                        put(
                                        "/api/customers/{id}",
                                        UUID.fromString("20000000-0000-0000-0000-000000000001"))
                                .header("Authorization", "Bearer product-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(customerUpdatePayload()))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("customer updated"))));

        mockMvc.perform(
                        get("/api/products/protected-check")
                                .header("Authorization", "Bearer product-manager-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("protected product data"));

        mockMvc.perform(
                        post("/api/products")
                                .header("Authorization", "Bearer product-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().string("product created"));

        mockMvc.perform(
                        post("/api/product-ownerships")
                                .header("Authorization", "Bearer product-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().string("product ownership assigned"));

        mockMvc.perform(
                        get("/api/payment-records/protected-check")
                                .header("Authorization", "Bearer product-manager-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("protected payment data"))));

        mockMvc.perform(
                        get("/api/campaigns/protected-check")
                                .header("Authorization", "Bearer product-manager-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("protected campaign data"));

        mockMvc.perform(
                        post("/api/campaigns/{id}/approve", UUID.randomUUID())
                                .header("Authorization", "Bearer product-manager-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("campaign approved"))));

        mockMvc.perform(
                        post("/api/campaigns/{id}/launch", UUID.randomUUID())
                                .header("Authorization", "Bearer product-manager-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("campaign launched"))));

        mockMvc.perform(
                        get("/api/users/protected-check")
                                .header("Authorization", "Bearer product-manager-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("protected user data"))));

        mockMvc.perform(
                        get("/api/audit-logs/protected-check")
                                .header("Authorization", "Bearer product-manager-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("protected audit data"))));
    }

    @Test
    void productManagerCannotLaunchCampaign() throws Exception {
        UUID campaignId = UUID.fromString("50000000-0000-0000-0000-000000000305");
        when(jwtService.validateToken("product-manager-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.PRODUCT_MANAGER));

        mockMvc.perform(
                        post("/api/campaigns/{id}/launch", campaignId)
                                .header("Authorization", "Bearer product-manager-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("campaign launched"))));
    }

    @Test
    void campaignManagerCanLaunchCampaignEndpoint() throws Exception {
        UUID campaignId = UUID.fromString("50000000-0000-0000-0000-000000000306");
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());

        mockMvc.perform(
                        post("/api/campaigns/{id}/launch", campaignId)
                                .header("Authorization", "Bearer campaign-manager-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("campaign launched"));
    }

    @Test
    void protectedReminderReadRequiresKbReminderReadRole() throws Exception {
        when(jwtService.validateToken("product-manager-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.PRODUCT_MANAGER));
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());

        mockMvc.perform(
                        get("/api/reminders/protected-check")
                                .header("Authorization", "Bearer product-manager-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("protected reminder data"))));

        mockMvc.perform(
                        get("/api/reminders/protected-check")
                                .header("Authorization", "Bearer campaign-manager-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("protected reminder data"));
    }

    @Test
    void paymentRecordUpdateRequiresKbPaymentMutationRole() throws Exception {
        when(jwtService.validateToken("product-manager-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.PRODUCT_MANAGER));
        when(jwtService.validateToken("customer-service-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CUSTOMER_SERVICE_AGENT));

        UUID paymentId = UUID.fromString("43000000-0000-0000-0000-000000000001");

        mockMvc.perform(
                        put("/api/payment-records/{id}", paymentId)
                                .header("Authorization", "Bearer product-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("payment record updated"))));

        mockMvc.perform(
                        put("/api/payment-records/{id}", paymentId)
                                .header("Authorization", "Bearer customer-service-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().string("payment record updated"));
    }

    @Test
    void protectedAnalyticsReportsAndAiRequireKbRoles() throws Exception {
        when(jwtService.validateToken("customer-service-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CUSTOMER_SERVICE_AGENT));
        when(jwtService.validateToken("admin-token", JwtTokenType.ACCESS))
                .thenReturn(adminClaims());
        when(jwtService.validateToken("marketing-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.MARKETING_ANALYST));
        when(jwtService.validateToken("compliance-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.COMPLIANCE_OFFICER));

        mockMvc.perform(
                        get("/api/analytics/protected-check")
                                .header("Authorization", "Bearer customer-service-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("protected analytics data"))));

        mockMvc.perform(
                        get("/api/analytics/protected-check")
                                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("protected analytics data"));

        mockMvc.perform(
                        get("/api/reports/protected-check")
                                .header("Authorization", "Bearer customer-service-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("protected report data"))));

        mockMvc.perform(
                        get("/api/reports/protected-check")
                                .header("Authorization", "Bearer marketing-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("protected report data"));

        mockMvc.perform(
                        get("/api/ai/protected-check")
                                .header("Authorization", "Bearer customer-service-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("protected ai data"))));

        mockMvc.perform(
                        get("/api/ai/protected-check")
                                .header("Authorization", "Bearer compliance-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("protected ai data"));
    }

    @Test
    void unconfiguredProtectedResourceIsDeniedByDefault() throws Exception {
        when(jwtService.validateToken("admin-token", JwtTokenType.ACCESS))
                .thenReturn(adminClaims());

        mockMvc.perform(
                        get("/api/unconfigured/protected-check")
                                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("unconfigured protected data"))));
    }

    private static JwtTokenClaims adminClaims() {
        return new JwtTokenClaims(
                UUID.fromString("10000000-0000-0000-0000-000000009901"),
                "admin@bayer-westphalian.test",
                List.of(SystemRoleName.ADMIN),
                JwtTokenType.ACCESS,
                "bayer-westphalian-campaign-platform-test",
                Instant.parse("2026-07-04T12:00:00Z"),
                Instant.parse("2026-07-04T12:15:00Z"),
                "admin-access-token-id");
    }

    private static JwtTokenClaims campaignManagerClaims() {
        return new JwtTokenClaims(
                UUID.fromString("10000000-0000-0000-0000-000000009902"),
                "campaign.manager@bayer-westphalian.test",
                List.of(SystemRoleName.CAMPAIGN_MANAGER),
                JwtTokenType.ACCESS,
                "bayer-westphalian-campaign-platform-test",
                Instant.parse("2026-07-04T12:00:00Z"),
                Instant.parse("2026-07-04T12:15:00Z"),
                "access-token-id");
    }

    private static JwtTokenClaims roleClaims(SystemRoleName roleName) {
        return new JwtTokenClaims(
                UUID.fromString("10000000-0000-0000-0000-000000009903"),
                roleName.name().toLowerCase().replace('_', '.') + "@bayer-westphalian.test",
                List.of(roleName),
                JwtTokenType.ACCESS,
                "bayer-westphalian-campaign-platform-test",
                Instant.parse("2026-07-04T12:00:00Z"),
                Instant.parse("2026-07-04T12:15:00Z"),
                roleName.name().toLowerCase() + "-access-token-id");
    }

    @RestController
    public static class ProtectedEndpointController {

        @GetMapping("/api/users/protected-check")
        String protectedEndpoint() {
            return "protected user data";
        }

        @PostMapping("/api/users")
        String createUser() {
            return "user created";
        }

        @PatchMapping("/api/users/{id}/disable")
        String disableUser() {
            return "user disabled";
        }

        @PostMapping("/api/users/{id}/roles")
        String assignRole() {
            return "role assigned";
        }

        @GetMapping("/api/customers/protected-check")
        String protectedCustomers() {
            return "protected customer data";
        }

        @PostMapping("/api/customers")
        String createCustomer() {
            return "customer created";
        }

        @PutMapping("/api/customers/{id}")
        String updateCustomer() {
            return "customer updated";
        }

        @GetMapping("/api/products/protected-check")
        String protectedProducts() {
            return "protected product data";
        }

        @GetMapping("/api/segments/protected-check")
        String protectedSegments() {
            return "protected segment data";
        }

        @GetMapping("/api/segments/{id}")
        String loadSegmentDetails() {
            return "segment details loaded";
        }

        @PostMapping("/api/segments/preview")
        String previewSegment() {
            return "segment preview loaded";
        }

        @PostMapping("/api/segments")
        String createSegment() {
            return "segment created";
        }

        @PutMapping("/api/segments/{id}")
        String updateSegment() {
            return "segment updated";
        }

        @DeleteMapping("/api/segments/{id}")
        String deleteSegment() {
            return "segment deleted";
        }

        @PostMapping("/api/products")
        String createProduct() {
            return "product created";
        }

        @PostMapping("/api/product-ownerships")
        String assignProductOwnership() {
            return "product ownership assigned";
        }

        @GetMapping("/api/payment-records/protected-check")
        String protectedPaymentRecords() {
            return "protected payment data";
        }

        @PutMapping("/api/payment-records/{id}")
        String updatePaymentRecord() {
            return "payment record updated";
        }

        @GetMapping("/api/beneficiaries/protected-check")
        String protectedBeneficiaries() {
            return "protected beneficiary data";
        }

        @PostMapping("/api/beneficiaries")
        String createBeneficiary() {
            return "beneficiary created";
        }

        @PutMapping("/api/beneficiaries/{id}")
        String updateBeneficiary() {
            return "beneficiary updated";
        }

        @GetMapping("/api/consents/protected-check")
        String protectedConsents() {
            return "protected consent data";
        }

        @PostMapping("/api/consents")
        String recordConsent() {
            return "consent recorded";
        }

        @GetMapping("/api/campaigns/protected-check")
        String protectedCampaigns() {
            return "protected campaign data";
        }

        @PostMapping("/api/campaigns/{id}/approve")
        String approveCampaign() {
            return "campaign approved";
        }

        @PutMapping("/api/campaigns/{id}/compliance-review-notes")
        String recordComplianceReviewNotes() {
            return "compliance notes recorded";
        }

        @PostMapping("/api/campaigns/{id}/launch")
        String launchCampaign() {
            return "campaign launched";
        }

        @GetMapping("/api/reminders/protected-check")
        String protectedReminders() {
            return "protected reminder data";
        }

        @GetMapping("/api/analytics/protected-check")
        String protectedAnalytics() {
            return "protected analytics data";
        }

        @GetMapping("/api/reports/protected-check")
        String protectedReports() {
            return "protected report data";
        }

        @GetMapping("/api/ai/protected-check")
        String protectedAi() {
            return "protected ai data";
        }

        @GetMapping("/api/audit-logs/protected-check")
        String protectedAuditLogs() {
            return "protected audit data";
        }

        @GetMapping("/api/unconfigured/protected-check")
        String unconfiguredProtectedResource() {
            return "unconfigured protected data";
        }
    }

    private static String customerUpdatePayload() {
        return """
                {
                  "firstName": "Ada",
                  "lastName": "Policyholder",
                  "email": "ada@bayer-westphalian.test",
                  "phone": "+49-555-0100",
                  "status": "ACTIVE",
                  "doNotContact": false
                }
                """;
    }
}
