package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.user.User;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * KB item 413: CampaignMetricRepository ({@link CampaignMetricsRepository}) declares JPA access and
 * {@code findByCampaignId()} for the campaign metrics aggregate.
 */
class CampaignMetricsRepositoryTests {

    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000413");

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
    void declaresKbFindByCampaignIdMethod() throws Exception {
        Method findByCampaignId =
                CampaignMetricsRepository.class.getMethod("findByCampaignId", UUID.class);
        Method findByCampaignPropertyPath =
                CampaignMetricsRepository.class.getMethod("findByCampaign_Id", UUID.class);

        assertThat(findByCampaignId.getGenericReturnType()).isEqualTo(optionalMetrics());
        assertThat(findByCampaignPropertyPath.getGenericReturnType()).isEqualTo(optionalMetrics());
        assertThat(findByCampaignId.isDefault()).isTrue();
    }

    @Test
    void declaresExistsByCampaignIdHelpers() throws Exception {
        Method existsByCampaignId =
                CampaignMetricsRepository.class.getMethod("existsByCampaignId", UUID.class);
        Method existsByCampaignPropertyPath =
                CampaignMetricsRepository.class.getMethod("existsByCampaign_Id", UUID.class);

        assertThat(existsByCampaignId.getReturnType()).isEqualTo(boolean.class);
        assertThat(existsByCampaignPropertyPath.getReturnType()).isEqualTo(boolean.class);
        assertThat(existsByCampaignId.isDefault()).isTrue();
    }

    @Test
    void findByCampaignIdDelegatesToPropertyPathLookup() {
        CampaignMetrics metrics = CampaignMetrics.forCampaign(sampleCampaign());
        CampaignMetricsRepository repository =
                mock(CampaignMetricsRepository.class, Mockito.CALLS_REAL_METHODS);
        when(repository.findByCampaign_Id(CAMPAIGN_ID)).thenReturn(Optional.of(metrics));
        when(repository.findByCampaign_Id(UUID.fromString("50000000-0000-0000-0000-000000000999")))
                .thenReturn(Optional.empty());

        assertThat(repository.findByCampaignId(CAMPAIGN_ID)).contains(metrics);
        assertThat(
                        repository.findByCampaignId(
                                UUID.fromString("50000000-0000-0000-0000-000000000999")))
                .isEmpty();
    }

    @Test
    void existsByCampaignIdDelegatesToPropertyPathLookup() {
        CampaignMetricsRepository repository =
                mock(CampaignMetricsRepository.class, Mockito.CALLS_REAL_METHODS);
        when(repository.existsByCampaign_Id(CAMPAIGN_ID)).thenReturn(true);
        when(repository.existsByCampaign_Id(UUID.fromString("50000000-0000-0000-0000-000000000999")))
                .thenReturn(false);

        assertThat(repository.existsByCampaignId(CAMPAIGN_ID)).isTrue();
        assertThat(
                        repository.existsByCampaignId(
                                UUID.fromString("50000000-0000-0000-0000-000000000999")))
                .isFalse();
    }

    private static java.lang.reflect.Type optionalMetrics() throws NoSuchMethodException {
        return ReturnTypes.class.getDeclaredMethod("optionalMetrics").getGenericReturnType();
    }

    private interface ReturnTypes {
        Optional<CampaignMetrics> optionalMetrics();
    }

    private static Campaign sampleCampaign() {
        User owner = User.create("metrics-repo@test.example", "{noop}x", "Metrics Repo");
        Campaign campaign =
                Campaign.create(
                        "Metrics repo campaign",
                        "Repository lookup",
                        owner,
                        null,
                        CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);
        return campaign;
    }
}
