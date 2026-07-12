package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.common.exception.BusinessRuleException;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.product.Product;
import com.bayerwestphalian.campaign.product.ProductType;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import com.bayerwestphalian.campaign.user.User;
import java.math.BigDecimal;
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
 * KB item 221 / FR-052: campaign product selection persists {@code campaign_products} links and
 * enforces active-product + draft-only rules.
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
class CampaignProductSelectionIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("bwc_campaign_product_selection_tests")
                    .withUsername("bwc_app")
                    .withPassword("bwc_app");

    @Autowired private TestEntityManager entityManager;
    @Autowired private CampaignService campaignService;
    @Autowired private CampaignProductRepository campaignProductRepository;

    @MockBean private AuthorizationExpressions authorizationExpressions;

    private User owner;

    @DynamicPropertySource
    static void registerPostgreSqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @BeforeEach
    void setUp() {
        owner = persistUser("product-selection-owner");
        when(authorizationExpressions.currentUserId()).thenReturn(owner.getId());
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(authorizationExpressions.isAuthenticated()).thenReturn(true);
    }

    @Test
    void selectsMultipleActiveProductsAndListsThem() {
        Product life = persistProduct("Life Plan", ProductType.LIFE_INSURANCE);
        Product home = persistProduct("Home Secure", ProductType.HOMEOWNER_INSURANCE);
        CampaignView campaign = createDraft("Product selection campaign");

        CampaignView updated =
                campaignService.selectProducts(
                        campaign.id(),
                        new SelectCampaignProductsRequest(List.of(life.getId(), home.getId()))
                                .toCommand());

        assertThat(updated.productIds()).containsExactlyInAnyOrder(life.getId(), home.getId());
        assertThat(campaignService.listSelectedProductIds(campaign.id()))
                .containsExactlyInAnyOrder(life.getId(), home.getId());
        assertThat(campaignProductRepository.findByCampaignId(campaign.id())).hasSize(2);
        assertThat(
                        campaignProductRepository.existsByCampaign_IdAndProduct_Id(
                                campaign.id(), life.getId()))
                .isTrue();
    }

    @Test
    void createCampaignAcceptsInitialProductSelection() {
        Product life = persistProduct("Life on create", ProductType.LIFE_INSURANCE);

        CampaignView created =
                campaignService.createCampaign(
                        new CreateCampaignCommand(
                                "Created with products",
                                "Objective",
                                null,
                                CampaignChannel.EMAIL,
                                null,
                                null,
                                null,
                                null,
                                List.of(life.getId())));

        assertThat(created.productIds()).containsExactly(life.getId());
        assertThat(campaignProductRepository.findByCampaignId(created.id())).hasSize(1);
    }

    @Test
    void updateCampaignReplacesProductSelectionWhenProductIdsProvided() {
        Product original = persistProduct("Original", ProductType.AUTO_INSURANCE);
        Product replacement = persistProduct("Replacement", ProductType.HEALTH_INSURANCE);
        CampaignView created =
                campaignService.createCampaign(
                        new CreateCampaignCommand(
                                "Update products",
                                "Objective",
                                null,
                                CampaignChannel.EMAIL,
                                null,
                                null,
                                null,
                                null,
                                List.of(original.getId())));

        CampaignView updated =
                campaignService.updateCampaign(
                        created.id(),
                        new UpdateCampaignCommand(
                                "Update products",
                                "Objective",
                                null,
                                CampaignChannel.EMAIL,
                                null,
                                null,
                                null,
                                null,
                                List.of(replacement.getId())));

        assertThat(updated.productIds()).containsExactly(replacement.getId());
        assertThat(campaignProductRepository.findByCampaignId(created.id()))
                .extracting(link -> link.getProduct().getId())
                .containsExactly(replacement.getId());
    }

    @Test
    void selectProductsReplacesPreviousSelectionAndDeduplicates() {
        Product a = persistProduct("Product A", ProductType.LIFE_INSURANCE);
        Product b = persistProduct("Product B", ProductType.INVESTMENT_FUND);
        CampaignView campaign = createDraft("Replace selection");

        campaignService.selectProducts(
                campaign.id(),
                new SelectCampaignProductsCommand(List.of(a.getId(), a.getId(), b.getId())));

        assertThat(campaignService.listSelectedProductIds(campaign.id()))
                .containsExactlyInAnyOrder(a.getId(), b.getId());
        assertThat(campaignProductRepository.findByCampaignId(campaign.id())).hasSize(2);
    }

    @Test
    void selectProductsClearsAllWhenEmptyList() {
        Product a = persistProduct("Clear me", ProductType.OTHER);
        CampaignView campaign = createDraft("Clear products");
        campaignService.selectProducts(
                campaign.id(), new SelectCampaignProductsCommand(List.of(a.getId())));

        CampaignView cleared =
                campaignService.selectProducts(
                        campaign.id(), new SelectCampaignProductsCommand(List.of()));

        assertThat(cleared.productIds()).isEmpty();
        assertThat(campaignProductRepository.findByCampaignId(campaign.id())).isEmpty();
    }

    @Test
    void rejectsUnknownProductId() {
        CampaignView campaign = createDraft("Unknown product");
        java.util.UUID missing = java.util.UUID.fromString("40000000-0000-0000-0000-00000000dead");

        assertThatThrownBy(
                        () ->
                                campaignService.selectProducts(
                                        campaign.id(),
                                        new SelectCampaignProductsCommand(List.of(missing))))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product");
    }

    @Test
    void rejectsInactiveProduct() {
        Product inactive = persistProduct("Inactive", ProductType.LIFE_INSURANCE);
        inactive.deactivate();
        entityManager.persistAndFlush(inactive);
        CampaignView campaign = createDraft("Inactive product");

        assertThatThrownBy(
                        () ->
                                campaignService.selectProducts(
                                        campaign.id(),
                                        new SelectCampaignProductsCommand(
                                                List.of(inactive.getId()))))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Campaign product validation failed");
    }

    @Test
    void rejectsProductSelectionOnSubmittedCampaign() {
        Product a = persistProduct("Too late", ProductType.LIFE_INSURANCE);
        CampaignView campaign = createDraft("Submitted products blocked");
        campaignService.submitCampaign(campaign.id());

        assertThatThrownBy(
                        () ->
                                campaignService.selectProducts(
                                        campaign.id(),
                                        new SelectCampaignProductsCommand(List.of(a.getId()))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("targeting");
    }

    @Test
    void allowsProductSelectionOnRejectedCampaign() {
        Product a = persistProduct("After reject", ProductType.LIFE_INSURANCE);
        CampaignView campaign = createDraft("Rejected products allowed");
        campaignService.submitCampaign(campaign.id());

        User compliance = persistUser("product-selection-compliance");
        when(authorizationExpressions.currentUserId()).thenReturn(compliance.getId());
        campaignService.rejectCampaign(campaign.id(), new RejectCampaignCommand("Fix products"));

        when(authorizationExpressions.currentUserId()).thenReturn(owner.getId());
        CampaignView updated =
                campaignService.selectProducts(
                        campaign.id(), new SelectCampaignProductsCommand(List.of(a.getId())));

        assertThat(updated.status()).isEqualTo(CampaignStatus.REJECTED);
        assertThat(updated.productIds()).containsExactly(a.getId());
    }

    private CampaignView createDraft(String name) {
        return campaignService.createCampaign(
                new CreateCampaignCommand(
                        name,
                        "Objective for " + name,
                        null,
                        CampaignChannel.EMAIL,
                        null,
                        null,
                        null,
                        null,
                        List.of()));
    }

    private Product persistProduct(String name, ProductType type) {
        return entityManager.persistAndFlush(
                Product.create(name, type, new BigDecimal("25.00"), 12));
    }

    private User persistUser(String emailPrefix) {
        User user =
                User.create(
                        emailPrefix + "@campaign-product-selection-integration.test",
                        "{noop}password",
                        "Campaign Product Selection User");
        return entityManager.persistAndFlush(user);
    }
}
