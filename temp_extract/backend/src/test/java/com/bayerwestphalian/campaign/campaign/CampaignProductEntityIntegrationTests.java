package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;

import com.bayerwestphalian.campaign.product.Product;
import com.bayerwestphalian.campaign.product.ProductType;
import com.bayerwestphalian.campaign.user.User;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * KB item 212: CampaignProduct persists composite key rows on {@code campaign_products}.
 */
@DataJpaTest(
        properties = {
            "spring.flyway.enabled=true",
            "spring.flyway.locations=classpath:db/migration",
            "spring.jpa.hibernate.ddl-auto=validate"
        })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class CampaignProductEntityIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("bwc_campaign_product_entity_tests")
                    .withUsername("bwc_app")
                    .withPassword("bwc_app");

    @Autowired private EntityManager entityManager;

    @DynamicPropertySource
    static void registerPostgreSqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @Test
    void persistsCampaignProductLinkWithCompositePrimaryKey() {
        User owner = persistUser("campaign-product-owner");
        Campaign campaign =
                Campaign.create(
                        "Product link campaign",
                        "Promote selected products",
                        owner,
                        null,
                        CampaignChannel.EMAIL);
        persistAndFlush(campaign);

        Product product =
                Product.create(
                        "Homeowner Secure",
                        ProductType.HOMEOWNER_INSURANCE,
                        new BigDecimal("29.99"),
                        12);
        persistAndFlush(product);

        CampaignProduct link = CampaignProduct.link(campaign, product);
        persistAndFlush(link);
        entityManager.clear();

        CampaignProductId key = new CampaignProductId(campaign.getId(), product.getId());
        CampaignProduct reloaded = entityManager.find(CampaignProduct.class, key);

        assertThat(reloaded).isNotNull();
        assertThat(reloaded.getId().getCampaignId()).isEqualTo(campaign.getId());
        assertThat(reloaded.getId().getProductId()).isEqualTo(product.getId());
        assertThat(reloaded.getCampaign().getId()).isEqualTo(campaign.getId());
        assertThat(reloaded.getProduct().getId()).isEqualTo(product.getId());
        assertThat(reloaded.getProduct().getProductType())
                .isEqualTo(ProductType.HOMEOWNER_INSURANCE);
        assertThat(reloaded.links(campaign.getId(), product.getId())).isTrue();
    }

    @Test
    void allowsMultipleProductsOnSameCampaign() {
        User owner = persistUser("multi-product-owner");
        Campaign campaign =
                Campaign.create(
                        "Multi product campaign",
                        "Promote two products",
                        owner,
                        null,
                        CampaignChannel.MIXED);
        persistAndFlush(campaign);

        Product life =
                Product.create(
                        "Life Plan", ProductType.LIFE_INSURANCE, new BigDecimal("40.00"), 12);
        Product fund =
                Product.create(
                        "Growth Fund", ProductType.INVESTMENT_FUND, new BigDecimal("100.00"), 24);
        persistAndFlush(life);
        persistAndFlush(fund);

        persistAndFlush(CampaignProduct.link(campaign, life));
        persistAndFlush(CampaignProduct.link(campaign, fund));
        entityManager.clear();

        CampaignProduct lifeLink =
                entityManager.find(
                        CampaignProduct.class,
                        new CampaignProductId(campaign.getId(), life.getId()));
        CampaignProduct fundLink =
                entityManager.find(
                        CampaignProduct.class,
                        new CampaignProductId(campaign.getId(), fund.getId()));

        assertThat(lifeLink).isNotNull();
        assertThat(fundLink).isNotNull();
        assertThat(lifeLink.getProduct().getName()).isEqualTo("Life Plan");
        assertThat(fundLink.getProduct().getName()).isEqualTo("Growth Fund");
    }

    private User persistUser(String emailPrefix) {
        User user =
                User.create(
                        emailPrefix + "@campaign-product-entity-integration.test",
                        "{noop}password",
                        "Campaign Product Entity Integration User");
        persistAndFlush(user);
        return user;
    }

    private <T> T persistAndFlush(T entity) {
        entityManager.persist(entity);
        entityManager.flush();
        return entity;
    }
}
