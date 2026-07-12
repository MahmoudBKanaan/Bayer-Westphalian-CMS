package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.bayerwestphalian.campaign.product.Product;
import com.bayerwestphalian.campaign.product.ProductType;
import com.bayerwestphalian.campaign.user.User;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * KB item 212: CampaignProduct entity maps {@code campaign_products} (FR-052 promoted products).
 */
class CampaignProductTests {

    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID PRODUCT_ID =
            UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID OWNER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000101");

    @Test
    void mapsKbCampaignProductsTableAsJpaEntity() throws Exception {
        assertThat(CampaignProduct.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(CampaignProduct.class.getAnnotation(Table.class).name())
                .isEqualTo("campaign_products");
        assertThat(field("id").isAnnotationPresent(EmbeddedId.class)).isTrue();
    }

    @Test
    void providesProtectedNoArgsConstructorForJpa() throws Exception {
        Constructor<CampaignProduct> constructor = CampaignProduct.class.getDeclaredConstructor();

        assertThat(Modifier.isProtected(constructor.getModifiers())).isTrue();
    }

    @Test
    void mapsCampaignAndProductWithCompositeKeyParts() throws Exception {
        assertManyToOne("campaign", "campaignId", "campaign_id");
        assertManyToOne("product", "productId", "product_id");
        assertThat(field("campaign").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(field("product").isAnnotationPresent(NotNull.class)).isTrue();
    }

    @Test
    void linksCampaignToPromotedProduct() {
        Campaign campaign = campaign();
        Product product = product();

        CampaignProduct link = CampaignProduct.link(campaign, product);

        assertThat(link.getCampaign()).isSameAs(campaign);
        assertThat(link.getProduct()).isSameAs(product);
        assertThat(link.getCampaignId()).isEqualTo(CAMPAIGN_ID);
        assertThat(link.getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(link.linksCampaign(CAMPAIGN_ID)).isTrue();
        assertThat(link.linksProduct(PRODUCT_ID)).isTrue();
        assertThat(link.links(CAMPAIGN_ID, PRODUCT_ID)).isTrue();
        assertThat(link.links(CAMPAIGN_ID, UUID.randomUUID())).isFalse();
    }

    @Test
    void rejectsNullCampaignOrProduct() {
        Campaign campaign = campaign();
        Product product = product();

        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> CampaignProduct.link(null, product))
                .withMessageContaining("Campaign is required");

        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> CampaignProduct.link(campaign, null))
                .withMessageContaining("Product is required");
    }

    @Test
    void factoryMethodIsNamedLinkForKbCampaignProductConstructorConcept() throws Exception {
        assertThat(CampaignProduct.class.getMethod("link", Campaign.class, Product.class))
                .isNotNull();
        assertThat(Modifier.isStatic(
                        CampaignProduct.class
                                .getMethod("link", Campaign.class, Product.class)
                                .getModifiers()))
                .isTrue();
    }

    private static void assertManyToOne(
            String fieldName, String mapsIdValue, String joinColumnName) throws Exception {
        Field relationship = field(fieldName);
        ManyToOne manyToOne = relationship.getAnnotation(ManyToOne.class);
        JoinColumn joinColumn = relationship.getAnnotation(JoinColumn.class);
        MapsId mapsId = relationship.getAnnotation(MapsId.class);

        assertThat(manyToOne.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(manyToOne.optional()).isFalse();
        assertThat(joinColumn.name()).isEqualTo(joinColumnName);
        assertThat(joinColumn.nullable()).isFalse();
        assertThat(mapsId.value()).isEqualTo(mapsIdValue);
    }

    private static Field field(String name) throws Exception {
        Field field = CampaignProduct.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static Campaign campaign() {
        User owner =
                User.create(
                        "campaign.manager@bayer-westphalian.test",
                        "$2a$10$hashed-password-placeholder",
                        "Campaign Manager");
        ReflectionTestUtils.setField(owner, "id", OWNER_ID);
        Campaign campaign =
                Campaign.create(
                        "Life renewal outreach",
                        "Promote life insurance renewals",
                        owner,
                        null,
                        CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);
        return campaign;
    }

    private static Product product() {
        Product product =
                Product.create(
                        "Life Protection Plan",
                        ProductType.LIFE_INSURANCE,
                        new BigDecimal("49.99"),
                        12);
        ReflectionTestUtils.setField(product, "id", PRODUCT_ID);
        return product;
    }
}
