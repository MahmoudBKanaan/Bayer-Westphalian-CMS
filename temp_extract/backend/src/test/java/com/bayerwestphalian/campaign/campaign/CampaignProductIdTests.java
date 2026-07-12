package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** KB item 212: composite key for campaign_products. */
class CampaignProductIdTests {

    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID PRODUCT_ID =
            UUID.fromString("40000000-0000-0000-0000-000000000001");

    @Test
    void isEmbeddableSerializableCompositeKey() throws Exception {
        assertThat(CampaignProductId.class.isAnnotationPresent(Embeddable.class)).isTrue();
        assertThat(Serializable.class).isAssignableFrom(CampaignProductId.class);

        Constructor<CampaignProductId> noArgs = CampaignProductId.class.getDeclaredConstructor();
        assertThat(Modifier.isProtected(noArgs.getModifiers())).isTrue();
    }

    @Test
    void mapsKbCompositeKeyColumns() throws Exception {
        Field campaignId = CampaignProductId.class.getDeclaredField("campaignId");
        Field productId = CampaignProductId.class.getDeclaredField("productId");

        assertThat(campaignId.getAnnotation(Column.class).name()).isEqualTo("campaign_id");
        assertThat(campaignId.getAnnotation(Column.class).nullable()).isFalse();
        assertThat(productId.getAnnotation(Column.class).name()).isEqualTo("product_id");
        assertThat(productId.getAnnotation(Column.class).nullable()).isFalse();
    }

    @Test
    void equalsAndHashCodeUseBothKeyParts() {
        CampaignProductId left = new CampaignProductId(CAMPAIGN_ID, PRODUCT_ID);
        CampaignProductId same = new CampaignProductId(CAMPAIGN_ID, PRODUCT_ID);
        CampaignProductId otherCampaign =
                new CampaignProductId(UUID.fromString("50000000-0000-0000-0000-000000000002"), PRODUCT_ID);
        CampaignProductId otherProduct =
                new CampaignProductId(CAMPAIGN_ID, UUID.fromString("40000000-0000-0000-0000-000000000002"));

        assertThat(left.getCampaignId()).isEqualTo(CAMPAIGN_ID);
        assertThat(left.getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(left).isEqualTo(same).hasSameHashCodeAs(same);
        assertThat(left).isNotEqualTo(otherCampaign).isNotEqualTo(otherProduct);
    }
}
