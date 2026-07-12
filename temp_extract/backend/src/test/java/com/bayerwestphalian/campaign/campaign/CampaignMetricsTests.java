package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.bayerwestphalian.campaign.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** KB item 282: campaign metrics are updated when a campaign launches. */
class CampaignMetricsTests {

    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000282");

    @Test
    void mapsKbCampaignMetricsTableAsJpaEntity() throws Exception {
        assertThat(CampaignMetrics.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(CampaignMetrics.class.getAnnotation(Table.class).name())
                .isEqualTo("campaign_metrics");
        assertThat(field("audienceSize").getAnnotation(Column.class).name())
                .isEqualTo("audience_size");
        assertThat(field("eligibleCount").getAnnotation(Column.class).name())
                .isEqualTo("eligible_count");
        assertThat(field("excludedCount").getAnnotation(Column.class).name())
                .isEqualTo("excluded_count");
        assertThat(field("sentCount").getAnnotation(Column.class).name()).isEqualTo("sent_count");
    }

    @Test
    void mapsCampaignAsUniqueOneToOneRelationship() throws Exception {
        Field campaign = field("campaign");
        OneToOne oneToOne = campaign.getAnnotation(OneToOne.class);
        JoinColumn joinColumn = campaign.getAnnotation(JoinColumn.class);

        assertThat(oneToOne.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(oneToOne.optional()).isFalse();
        assertThat(joinColumn.name()).isEqualTo("campaign_id");
        assertThat(joinColumn.nullable()).isFalse();
        assertThat(joinColumn.unique()).isTrue();
    }

    @Test
    void recordsLaunchCountsAndAudienceSize() {
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign());

        metrics.recordLaunchCounts(8, 2, 8);

        assertThat(metrics.getCampaignId()).isEqualTo(CAMPAIGN_ID);
        assertThat(metrics.getAudienceSize()).isEqualTo(10);
        assertThat(metrics.getEligibleCount()).isEqualTo(8);
        assertThat(metrics.getExcludedCount()).isEqualTo(2);
        assertThat(metrics.getSentCount()).isEqualTo(8);
    }

    @Test
    void rejectsNegativeLaunchCounts() {
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign());

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> metrics.recordLaunchCounts(-1, 0, 0))
                .withMessageContaining("Eligible count must not be negative");
    }

    private static Field field(String name) throws Exception {
        Field field = CampaignMetrics.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static Campaign campaign() {
        User owner =
                User.create(
                        "campaign-metrics-owner@test.example",
                        "{noop}password",
                        "Campaign Metrics Owner");
        Campaign campaign =
                Campaign.create(
                        "Metrics campaign",
                        "Update metrics on launch",
                        owner,
                        null,
                        CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);
        return campaign;
    }
}
