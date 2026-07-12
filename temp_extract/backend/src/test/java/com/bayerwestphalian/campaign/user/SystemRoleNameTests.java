package com.bayerwestphalian.campaign.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SystemRoleNameTests {

    @Test
    void matchesKbSystemRoleNameValues() {
        assertThat(SystemRoleName.values())
                .containsExactly(
                        SystemRoleName.ADMIN,
                        SystemRoleName.CAMPAIGN_MANAGER,
                        SystemRoleName.BI_ANALYST,
                        SystemRoleName.PRODUCT_MANAGER,
                        SystemRoleName.COMPLIANCE_OFFICER,
                        SystemRoleName.CUSTOMER_SERVICE_AGENT,
                        SystemRoleName.SALES_AGENT,
                        SystemRoleName.MARKETING_ANALYST,
                        SystemRoleName.EXECUTIVE_VIEWER,
                        SystemRoleName.SYSTEM_AUDITOR);
    }
}
