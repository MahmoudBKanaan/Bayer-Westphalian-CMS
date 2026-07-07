package com.bayerwestphalian.campaign.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AuthenticationDesignDocumentationTests {

    private static final Path AUTH_DESIGN =
            Path.of("../docs/architecture/authentication-design.md");
    private static final Path ROLE_ACCESS_DESIGN =
            Path.of("../docs/architecture/role-based-access.md");
    private static final Path USER_MANAGEMENT_GUIDE =
            Path.of("../docs/admin/user-management-guide.md");

    @Test
    void documentsKbAuthenticationDesignDecisions() throws Exception {
        String design = Files.readString(AUTH_DESIGN, StandardCharsets.UTF_8);

        assertThat(design)
                .contains("POST /api/auth/login")
                .contains("AuthService")
                .contains("PasswordHashingService")
                .contains("BCrypt")
                .contains("Disabled or locked users are rejected")
                .contains("JWT access token and refresh token")
                .contains("sessionStorage")
                .contains("Authorization: Bearer <token>")
                .contains("Protected frontend routes")
                .contains("Spring Security is the authoritative access-control boundary")
                .contains("Protected endpoints fail without authentication")
                .contains("403 Forbidden")
                .contains("ADMIN")
                .contains("user_roles")
                .contains("Password hashes are never returned");
    }

    @Test
    void documentationIndexLinksAuthenticationDesign() throws Exception {
        String index = Files.readString(Path.of("../docs/README.md"), StandardCharsets.UTF_8);

        assertThat(index).contains("architecture/authentication-design.md");
    }

    @Test
    void documentsKbRoleBasedAccessDecisions() throws Exception {
        String design = Files.readString(ROLE_ACCESS_DESIGN, StandardCharsets.UTF_8);

        assertThat(design)
                .contains("Backend authorization is authoritative")
                .contains("Frontend checks only control navigation and user experience")
                .contains("Pages and APIs are restricted by role")
                .contains("403 Forbidden")
                .contains("denied by default")
                .contains("ADMIN")
                .contains("CAMPAIGN_MANAGER")
                .contains("BI_ANALYST")
                .contains("PRODUCT_MANAGER")
                .contains("COMPLIANCE_OFFICER")
                .contains("CUSTOMER_SERVICE_AGENT")
                .contains("SALES_AGENT")
                .contains("MARKETING_ANALYST")
                .contains("EXECUTIVE_VIEWER")
                .contains("SYSTEM_AUDITOR")
                .contains("/api/users/**")
                .contains("/api/roles/**")
                .contains("Product read workflows")
                .contains("Campaign read workflows")
                .contains("Reminder read workflows")
                .contains("AI recommendation reads")
                .contains("Campaign Manager cannot manage users")
                .contains("role-based menus show only allowed menus");
    }

    @Test
    void documentationIndexLinksRoleBasedAccessDesign() throws Exception {
        String index = Files.readString(Path.of("../docs/README.md"), StandardCharsets.UTF_8);

        assertThat(index).contains("architecture/role-based-access.md");
    }

    @Test
    void documentsKbAdminUserManagementGuide() throws Exception {
        String guide = Files.readString(USER_MANAGEMENT_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("Admin User-Management Guide")
                .contains("There is no public signup")
                .contains("Only users with the `ADMIN` role can manage users and roles")
                .contains("Create employee users")
                .contains("Edit employee names and account status")
                .contains("Disable employee users")
                .contains("Assign roles")
                .contains("Reset passwords")
                .contains("BCrypt hash")
                .contains("Password hashes are never returned")
                .contains("Disabled or locked users cannot log in")
                .contains("duplicate role choices")
                .contains("Non-Admin users receive a forbidden response")
                .contains("User creation")
                .contains("User disable")
                .contains("Role assignment");
    }

    @Test
    void documentationIndexLinksAdminUserManagementGuide() throws Exception {
        String index = Files.readString(Path.of("../docs/README.md"), StandardCharsets.UTF_8);

        assertThat(index).contains("admin/user-management-guide.md");
    }
}
