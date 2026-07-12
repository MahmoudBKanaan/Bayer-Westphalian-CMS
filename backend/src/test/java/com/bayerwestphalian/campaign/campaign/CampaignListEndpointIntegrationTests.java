package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.segment.Segment;
import com.bayerwestphalian.campaign.segment.SegmentVisibility;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import com.bayerwestphalian.campaign.user.User;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * KB item 219: campaign list endpoint path through {@link CampaignService#searchCampaigns} with
 * term/status/owner/segment filters.
 */
@DataJpaTest(
        properties = {
            "spring.flyway.enabled=true",
            "spring.flyway.locations=classpath:db/migration",
            "spring.jpa.hibernate.ddl-auto=validate"
        })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@Import({CampaignService.class, AuditService.class})
class CampaignListEndpointIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("bwc_campaign_list_endpoint_tests")
                    .withUsername("bwc_app")
                    .withPassword("bwc_app");

    @Autowired private TestEntityManager entityManager;
    @Autowired private CampaignService campaignService;

    @MockBean private AuthorizationExpressions authorizationExpressions;

    private User owner;
    private User other;

    @DynamicPropertySource
    static void registerPostgreSqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @BeforeEach
    void setUp() {
        owner = persistUser("list-endpoint-owner");
        other = persistUser("list-endpoint-other");
        when(authorizationExpressions.currentUserId()).thenReturn(owner.getId());
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(authorizationExpressions.isAuthenticated()).thenReturn(true);
    }

    @Test
    void listEndpointReturnsAllCampaignsWhenNoFilters() {
        campaignService.createCampaign(minimalCreate("Alpha campaign", CampaignChannel.EMAIL));
        campaignService.createCampaign(minimalCreate("Beta campaign", CampaignChannel.SMS));

        // Mirrors GET /api/campaigns with empty query params.
        List<CampaignView> listed =
                campaignService.searchCampaigns(
                        new CampaignSearchRequest(null, null, null, null).toCriteria());

        assertThat(listed)
                .extracting(CampaignView::name)
                .contains("Alpha campaign", "Beta campaign");
        assertThat(listed).allMatch(view -> view.status() == CampaignStatus.DRAFT);
    }

    @Test
    void listEndpointFiltersByTermOnNameAndObjective() {
        campaignService.createCampaign(
                new CreateCampaignCommand(
                        "Life renewal outreach",
                        "Promote renewals",
                        null,
                        CampaignChannel.EMAIL,
                        null,
                        null,
                        null,
                        null,
                        List.of()));
        campaignService.createCampaign(
                new CreateCampaignCommand(
                        "Auto cross-sell",
                        "Other product push",
                        null,
                        CampaignChannel.PHONE,
                        null,
                        null,
                        null,
                        null,
                        List.of()));

        List<CampaignView> byName =
                campaignService.searchCampaigns(
                        new CampaignSearchRequest("life", null, null, null).toCriteria());
        List<CampaignView> byObjective =
                campaignService.searchCampaigns(
                        new CampaignSearchRequest("  renewals  ", null, null, null).toCriteria());

        assertThat(byName).extracting(CampaignView::name).containsExactly("Life renewal outreach");
        assertThat(byObjective)
                .extracting(CampaignView::name)
                .containsExactly("Life renewal outreach");
    }

    @Test
    void listEndpointFiltersByStatus() {
        CampaignView draft =
                campaignService.createCampaign(minimalCreate("Draft only", CampaignChannel.EMAIL));
        CampaignView submitted =
                campaignService.createCampaign(minimalCreate("Will submit", CampaignChannel.EMAIL));
        campaignService.submitCampaign(submitted.id());

        List<CampaignView> drafts =
                campaignService.searchCampaigns(
                        new CampaignSearchRequest(null, null, CampaignStatus.DRAFT, null)
                                .toCriteria());
        List<CampaignView> submittedList =
                campaignService.searchCampaigns(
                        new CampaignSearchRequest(null, null, CampaignStatus.SUBMITTED, null)
                                .toCriteria());

        assertThat(drafts).extracting(CampaignView::id).contains(draft.id());
        assertThat(drafts).noneMatch(view -> view.id().equals(submitted.id()));
        assertThat(submittedList).extracting(CampaignView::id).containsExactly(submitted.id());
        assertThat(submittedList.getFirst().status()).isEqualTo(CampaignStatus.SUBMITTED);
    }

    @Test
    void listEndpointFiltersByOwnerUserId() {
        campaignService.createCampaign(minimalCreate("Owner campaign", CampaignChannel.EMAIL));

        when(authorizationExpressions.currentUserId()).thenReturn(other.getId());
        campaignService.createCampaign(minimalCreate("Other owner campaign", CampaignChannel.SMS));

        when(authorizationExpressions.currentUserId()).thenReturn(owner.getId());
        List<CampaignView> owned =
                campaignService.searchCampaigns(
                        new CampaignSearchRequest(null, owner.getId(), null, null).toCriteria());

        assertThat(owned).isNotEmpty();
        assertThat(owned).allMatch(view -> owner.getId().equals(view.ownerUserId()));
        assertThat(owned).extracting(CampaignView::name).contains("Owner campaign");
        assertThat(owned).extracting(CampaignView::name).doesNotContain("Other owner campaign");
    }

    @Test
    void listEndpointFiltersBySegmentId() {
        Segment segment =
                entityManager.persistAndFlush(
                        Segment.create("List segment", null, owner, SegmentVisibility.TEAM));
        campaignService.createCampaign(
                new CreateCampaignCommand(
                        "Linked to segment",
                        "Uses segment",
                        segment.getId(),
                        CampaignChannel.EMAIL,
                        null,
                        null,
                        null,
                        null,
                        List.of()));
        campaignService.createCampaign(minimalCreate("No segment", CampaignChannel.EMAIL));

        List<CampaignView> linked =
                campaignService.searchCampaigns(
                        new CampaignSearchRequest(null, null, null, segment.getId()).toCriteria());

        assertThat(linked).extracting(CampaignView::name).containsExactly("Linked to segment");
        assertThat(linked.getFirst().segmentId()).isEqualTo(segment.getId());
    }

    @Test
    void listEndpointCombinesStatusAndTermFilters() {
        campaignService.createCampaign(
                new CreateCampaignCommand(
                        "Life draft",
                        "Draft life offer",
                        null,
                        CampaignChannel.EMAIL,
                        null,
                        null,
                        null,
                        null,
                        List.of()));
        CampaignView lifeSubmitted =
                campaignService.createCampaign(
                        new CreateCampaignCommand(
                                "Life submitted",
                                "Submitted life offer",
                                null,
                                CampaignChannel.EMAIL,
                                null,
                                null,
                                null,
                                null,
                                List.of()));
        campaignService.submitCampaign(lifeSubmitted.id());
        campaignService.createCampaign(
                new CreateCampaignCommand(
                        "Auto submitted",
                        "Submitted auto offer",
                        null,
                        CampaignChannel.SMS,
                        null,
                        null,
                        null,
                        null,
                        List.of()));
        // leave auto as draft for contrast — only life submitted should match status+term
        CampaignView autoSubmitted =
                campaignService.createCampaign(
                        new CreateCampaignCommand(
                                "Auto submitted only",
                                "Not life",
                                null,
                                CampaignChannel.SMS,
                                null,
                                null,
                                null,
                                null,
                                List.of()));
        campaignService.submitCampaign(autoSubmitted.id());

        List<CampaignView> results =
                campaignService.searchCampaigns(
                        new CampaignSearchRequest("life", null, CampaignStatus.SUBMITTED, null)
                                .toCriteria());

        assertThat(results).extracting(CampaignView::name).containsExactly("Life submitted");
        assertThat(results.getFirst().status()).isEqualTo(CampaignStatus.SUBMITTED);
    }

    private CreateCampaignCommand minimalCreate(String name, CampaignChannel channel) {
        return new CreateCampaignCommand(
                name, "Objective for " + name, null, channel, null, null, null, null, List.of());
    }

    private User persistUser(String emailPrefix) {
        User user =
                User.create(
                        emailPrefix + "@campaign-list-endpoint-integration.test",
                        "{noop}password",
                        "Campaign List Endpoint User");
        return entityManager.persistAndFlush(user);
    }
}
