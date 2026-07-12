package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.audit.AuditLog;
import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.common.exception.BusinessRuleException;
import com.bayerwestphalian.campaign.common.exception.ForbiddenException;
import com.bayerwestphalian.campaign.product.Product;
import com.bayerwestphalian.campaign.product.ProductType;
import com.bayerwestphalian.campaign.segment.Segment;
import com.bayerwestphalian.campaign.segment.SegmentVisibility;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import com.bayerwestphalian.campaign.user.User;
import java.math.BigDecimal;
import java.time.LocalDate;
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
 * KB item 218: update draft campaign endpoint path through {@link CampaignService#updateCampaign}
 * (FR-057). Persists field changes for DRAFT/REJECTED only; blocks non-owners and non-editable
 * statuses.
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
class CampaignUpdateDraftEndpointIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("bwc_campaign_update_draft_endpoint_tests")
                    .withUsername("bwc_app")
                    .withPassword("bwc_app");

    @Autowired private TestEntityManager entityManager;
    @Autowired private CampaignService campaignService;
    @Autowired private CampaignRepository campaignRepository;
    @Autowired private CampaignProductRepository campaignProductRepository;

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
        owner = persistUser("update-draft-owner");
        other = persistUser("update-draft-other");
        when(authorizationExpressions.currentUserId()).thenReturn(owner.getId());
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(authorizationExpressions.isAuthenticated()).thenReturn(true);
    }

    @Test
    void updateDraftEndpointPersistsFieldChangesAndProductReplacement() {
        Segment originalSegment =
                entityManager.persistAndFlush(
                        Segment.create("Original segment", null, owner, SegmentVisibility.PRIVATE));
        Segment newSegment =
                entityManager.persistAndFlush(
                        Segment.create("Updated segment", null, owner, SegmentVisibility.TEAM));
        Product originalProduct =
                entityManager.persistAndFlush(
                        Product.create(
                                "Original product",
                                ProductType.LIFE_INSURANCE,
                                new BigDecimal("10.00"),
                                12));
        Product newProduct =
                entityManager.persistAndFlush(
                        Product.create(
                                "Replacement product",
                                ProductType.HOMEOWNER_INSURANCE,
                                new BigDecimal("20.00"),
                                12));

        CampaignView created =
                campaignService.createCampaign(
                        new CreateCampaignCommand(
                                "Original name",
                                "Original objective",
                                originalSegment.getId(),
                                CampaignChannel.EMAIL,
                                "Original subject",
                                "Original body",
                                LocalDate.of(2026, 8, 1),
                                LocalDate.of(2026, 8, 31),
                                List.of(originalProduct.getId())));

        UpdateCampaignCommand updateCommand =
                new UpdateCampaignRequest(
                                "Updated life renewal",
                                "Refined renewal objective",
                                newSegment.getId(),
                                CampaignChannel.SMS,
                                "Updated subject",
                                "Updated body",
                                LocalDate.of(2026, 10, 1),
                                LocalDate.of(2026, 10, 31),
                                List.of(newProduct.getId()))
                        .toCommand();

        CampaignView updated = campaignService.updateCampaign(created.id(), updateCommand);

        entityManager.flush();
        entityManager.clear();

        assertThat(updated.name()).isEqualTo("Updated life renewal");
        assertThat(updated.objective()).isEqualTo("Refined renewal objective");
        assertThat(updated.status()).isEqualTo(CampaignStatus.DRAFT);
        assertThat(updated.channel()).isEqualTo(CampaignChannel.SMS);
        assertThat(updated.segmentId()).isEqualTo(newSegment.getId());
        assertThat(updated.messageSubject()).isEqualTo("Updated subject");
        assertThat(updated.productIds()).containsExactly(newProduct.getId());

        Campaign reloaded = campaignRepository.findById(created.id()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Updated life renewal");
        assertThat(reloaded.getChannel()).isEqualTo(CampaignChannel.SMS);
        assertThat(reloaded.getSegment().getId()).isEqualTo(newSegment.getId());
        assertThat(reloaded.getStartDate()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(campaignProductRepository.findByCampaignId(created.id()))
                .extracting(link -> link.getProduct().getId())
                .containsExactly(newProduct.getId());

        List<AuditLog> updates =
                entityManager
                        .getEntityManager()
                        .createQuery(
                                "select a from AuditLog a where a.entityType = :entityType "
                                        + "and a.entityId = :entityId and a.action = :action "
                                        + "order by a.createdAt desc",
                                AuditLog.class)
                        .setParameter("entityType", CampaignService.AUDIT_ENTITY_TYPE)
                        .setParameter("entityId", created.id())
                        .setParameter("action", "UPDATE")
                        .getResultList();
        assertThat(updates).isNotEmpty();
    }

    @Test
    void updateDraftEndpointAllowsRejectedCampaignEditAndResubmitPath() {
        CampaignView created =
                campaignService.createCampaign(
                        new CreateCampaignCommand(
                                "Rejected path",
                                "Will be rejected",
                                null,
                                CampaignChannel.EMAIL,
                                null,
                                null,
                                null,
                                null,
                                List.of()));
        campaignService.submitCampaign(created.id());

        User compliance = persistUser("update-draft-compliance");
        when(authorizationExpressions.currentUserId()).thenReturn(compliance.getId());
        campaignService.rejectCampaign(
                created.id(), new RejectCampaignCommand("Needs clearer objective"));

        when(authorizationExpressions.currentUserId()).thenReturn(owner.getId());
        CampaignView updated =
                campaignService.updateCampaign(
                        created.id(),
                        new UpdateCampaignCommand(
                                "Rejected path fixed",
                                "Clearer objective after rejection",
                                null,
                                CampaignChannel.EMAIL,
                                "Fixed subject",
                                "Fixed body",
                                null,
                                null,
                                null));

        assertThat(updated.status()).isEqualTo(CampaignStatus.REJECTED);
        assertThat(updated.name()).isEqualTo("Rejected path fixed");
        assertThat(updated.messageSubject()).isEqualTo("Fixed subject");
    }

    @Test
    void updateDraftEndpointRejectsNonOwner() {
        CampaignView created =
                campaignService.createCampaign(
                        new CreateCampaignCommand(
                                "Owned draft",
                                "Objective",
                                null,
                                CampaignChannel.PHONE,
                                null,
                                null,
                                null,
                                null,
                                List.of()));

        when(authorizationExpressions.currentUserId()).thenReturn(other.getId());

        assertThatThrownBy(
                        () ->
                                campaignService.updateCampaign(
                                        created.id(),
                                        new UpdateCampaignCommand(
                                                "Hijacked",
                                                "Not allowed",
                                                null,
                                                CampaignChannel.PHONE,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("not owned");
    }

    @Test
    void updateDraftEndpointRejectsSubmittedCampaignWithoutWorkflow() {
        CampaignView created =
                campaignService.createCampaign(
                        new CreateCampaignCommand(
                                "Submitted draft",
                                "Objective",
                                null,
                                CampaignChannel.EMAIL,
                                null,
                                null,
                                null,
                                null,
                                List.of()));
        campaignService.submitCampaign(created.id());

        assertThatThrownBy(
                        () ->
                                campaignService.updateCampaign(
                                        created.id(),
                                        new UpdateCampaignCommand(
                                                "Illegal edit",
                                                "Still submitted",
                                                null,
                                                CampaignChannel.EMAIL,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cannot be edited");
    }

    @Test
    void updateDraftEndpointLeavesProductsUnchangedWhenProductIdsNull() {
        Product product =
                entityManager.persistAndFlush(
                        Product.create(
                                "Keep product",
                                ProductType.AUTO_INSURANCE,
                                new BigDecimal("15.00"),
                                6));
        CampaignView created =
                campaignService.createCampaign(
                        new CreateCampaignCommand(
                                "Products stay",
                                "Objective",
                                null,
                                CampaignChannel.MIXED,
                                null,
                                null,
                                null,
                                null,
                                List.of(product.getId())));

        CampaignView updated =
                campaignService.updateCampaign(
                        created.id(),
                        new UpdateCampaignCommand(
                                "Products stay renamed",
                                "Objective updated",
                                null,
                                CampaignChannel.MIXED,
                                null,
                                null,
                                null,
                                null,
                                null));

        assertThat(updated.name()).isEqualTo("Products stay renamed");
        assertThat(updated.productIds()).containsExactly(product.getId());
        assertThat(campaignProductRepository.findByCampaignId(created.id())).hasSize(1);
    }

    private User persistUser(String emailPrefix) {
        User user =
                User.create(
                        emailPrefix + "@campaign-update-draft-endpoint-integration.test",
                        "{noop}password",
                        "Campaign Update Draft Endpoint User");
        return entityManager.persistAndFlush(user);
    }
}
