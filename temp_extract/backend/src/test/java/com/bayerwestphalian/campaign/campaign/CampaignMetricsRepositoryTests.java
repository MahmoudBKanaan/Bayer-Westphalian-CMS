package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

/** KB item 282: CampaignMetricsRepository supports one metrics row per campaign. */
class CampaignMetricsRepositoryTests {

    @Test
    void extendsJpaRepositoryForCampaignMetricsAggregate() {
        assertThat(JpaRepository.class).isAssignableFrom(CampaignMetricsRepository.class);

        ParameterizedType repositoryType =
                (ParameterizedType)
                        List.of(CampaignMetricsRepository.class.getGenericInterfaces()).stream()
                                .filter(ParameterizedType.class::isInstance)
                                .findFirst()
                                .orElseThrow();

        assertThat(repositoryType.getRawType()).isEqualTo(JpaRepository.class);
        assertThat(repositoryType.getActualTypeArguments())
                .containsExactly(CampaignMetrics.class, UUID.class);
    }

    @Test
    void declaresCampaignLookupMethod() throws Exception {
        Method findByCampaign =
                CampaignMetricsRepository.class.getMethod("findByCampaign_Id", UUID.class);

        assertThat(findByCampaign.getGenericReturnType()).isEqualTo(optionalMetrics());
    }

    private static java.lang.reflect.Type optionalMetrics() throws NoSuchMethodException {
        return ReturnTypes.class.getDeclaredMethod("optionalMetrics").getGenericReturnType();
    }

    private interface ReturnTypes {
        Optional<CampaignMetrics> optionalMetrics();
    }
}
