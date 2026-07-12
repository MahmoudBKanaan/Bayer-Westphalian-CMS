package com.bayerwestphalian.campaign.followup;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * KB item 403: Follow-up task documentation exists and covers package boundary, API, domain rules,
 * authorization, frontend boundary, and acceptance criteria for epic E17.
 */
class FollowUpTaskDocumentationTests {

    private static final Path FOLLOW_UP_TASK_DOC = Path.of("../docs/modules/follow-up-tasks.md");
    private static final Path DOCS_INDEX = Path.of("../docs/README.md");
    private static final Path PACKAGE_INFO =
            Path.of("src/main/java/com/bayerwestphalian/campaign/followup/package-info.java");

    @Test
    void documentsFollowUpTaskModuleBoundaryAndApiSurface() throws Exception {
        String documentation = Files.readString(FOLLOW_UP_TASK_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("# Follow-Up Task Documentation")
                .contains("## Package Boundary")
                .contains("com.bayerwestphalian.campaign.followup")
                .contains("FollowUpTask")
                .contains("FollowUpRepository")
                .contains("FollowUpService")
                .contains("FollowUpController")
                .contains("follow_up_tasks")
                .contains("/api/follow-up-tasks")
                .contains("## REST API")
                .contains("POST")
                .contains("PUT")
                .contains("GET")
                .contains("/assign")
                .contains("/complete")
                .contains("/status")
                .contains("customerId")
                .contains("assignedTo")
                .contains("priority")
                .contains("status")
                .contains("dueDateFrom")
                .contains("dueDateTo");
    }

    @Test
    void documentsFollowUpTaskDomainRulesAndLifecycle() throws Exception {
        String documentation = Files.readString(FOLLOW_UP_TASK_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## Data Model")
                .contains("## Domain Rules")
                .contains("OPEN")
                .contains("IN_PROGRESS")
                .contains("COMPLETED")
                .contains("CANCELLED")
                .contains("LOW")
                .contains("MEDIUM")
                .contains("HIGH")
                .contains("completed_at")
                .contains("complete()")
                .contains("customerId` and `title` are required")
                .contains("requires the user to be **active**")
                .contains("E17")
                .contains("FR-093")
                .contains("FR-088");
    }

    @Test
    void documentsFollowUpTaskAuthorizationAndRoles() throws Exception {
        String documentation = Files.readString(FOLLOW_UP_TASK_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## Authorization")
                .contains("Spring Security")
                .contains("method-level authorization")
                .contains("ADMIN")
                .contains("CUSTOMER_SERVICE_AGENT")
                .contains("SALES_AGENT")
                .contains("CAMPAIGN_MANAGER")
                .contains("Create task")
                .contains("Assign task")
                .contains("Complete task")
                .contains("backend role authorization");
    }

    @Test
    void documentsFollowUpTaskFrontendBoundaryAndAcceptanceCriteria() throws Exception {
        String documentation = Files.readString(FOLLOW_UP_TASK_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## Frontend Boundary")
                .contains("FollowUpTasksPage.tsx")
                .contains("CustomerDetailsPage.tsx")
                .contains("frontend/src/api/followUpTasks.ts")
                .contains("## Downstream Use")
                .contains("## Acceptance Criteria")
                .contains("A follow-up task can be created")
                .contains("A follow-up task can be assigned")
                .contains("A follow-up task can be completed")
                .contains("filtered by assignee")
                .contains("Unauthorized roles cannot")
                .contains("communication-tracking.md")
                .contains("campaign-lifecycle.md");
    }

    @Test
    void followUpPackageInfoReferencesModuleDocumentation() throws Exception {
        String packageInfo = Files.readString(PACKAGE_INFO, StandardCharsets.UTF_8);

        assertThat(packageInfo)
                .contains("E17")
                .contains("FR-093")
                .contains("docs/modules/follow-up-tasks.md");
    }

    @Test
    void documentationIndexLinksFollowUpTaskDocumentation() throws Exception {
        String index = Files.readString(DOCS_INDEX, StandardCharsets.UTF_8);

        assertThat(index)
                .contains("modules/follow-up-tasks.md")
                .contains("Follow-Up Task");
    }
}
