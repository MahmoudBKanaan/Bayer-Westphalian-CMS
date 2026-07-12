package com.bayerwestphalian.campaign.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * KB item 469: {@link AiRecommendationType} maps PostgreSQL {@code ai_recommendation_type}.
 */
@DisplayName("469 AiRecommendationType enum")
class AiRecommendationTypeTests {

    @Test
    void declaresKbAiRecommendationTypeValues() {
        assertThat(AiRecommendationType.values())
                .containsExactly(
                        AiRecommendationType.PRODUCT,
                        AiRecommendationType.SEGMENT,
                        AiRecommendationType.COPY,
                        AiRecommendationType.RISK,
                        AiRecommendationType.DUPLICATE_WARNING);
    }
}
