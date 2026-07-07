package com.bayerwestphalian.campaign.auth.method;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class MethodAuthorizationAnnotationTests {

    @Test
    void adminOnlyUsesKbAdminAuthorizationExpression() {
        assertMethodSecurityAnnotation(AdminOnly.class, "@authz.canManageUsers()");
    }

    @Test
    void campaignWriteAccessUsesKbCampaignManagerExpression() {
        assertMethodSecurityAnnotation(CampaignWriteAccess.class, "@authz.canManageCampaigns()");
    }

    @Test
    void campaignApprovalAccessUsesKbComplianceExpression() {
        assertMethodSecurityAnnotation(
                CampaignApprovalAccess.class, "@authz.canApproveCampaigns()");
    }

    @Test
    void productWriteAccessUsesKbProductManagerExpression() {
        assertMethodSecurityAnnotation(ProductWriteAccess.class, "@authz.canManageProducts()");
    }

    @Test
    void auditReadAccessUsesKbAuditExpression() {
        assertMethodSecurityAnnotation(AuditReadAccess.class, "@authz.canViewAuditLogs()");
    }

    @Test
    void reportReadAccessUsesKbReportExpression() {
        assertMethodSecurityAnnotation(ReportReadAccess.class, "@authz.canViewReports()");
    }

    @Test
    void authenticatedAccessUsesAuthenticatedExpression() {
        assertMethodSecurityAnnotation(AuthenticatedAccess.class, "@authz.isAuthenticated()");
    }

    private static void assertMethodSecurityAnnotation(
            Class<? extends Annotation> annotationType, String expression) {
        Target target = annotationType.getAnnotation(Target.class);
        Retention retention = annotationType.getAnnotation(Retention.class);
        PreAuthorize preAuthorize = annotationType.getAnnotation(PreAuthorize.class);

        assertThat(target.value()).containsExactlyInAnyOrder(ElementType.METHOD, ElementType.TYPE);
        assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
        assertThat(preAuthorize.value()).isEqualTo(expression);
    }
}
