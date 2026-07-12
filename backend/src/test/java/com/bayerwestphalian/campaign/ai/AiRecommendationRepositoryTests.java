package com.bayerwestphalian.campaign.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * KB item 470: AiRecommendationRepository declares JPA access and KB lookups {@code
 * findByTargetEntity()} / {@code findByRecommendationType()}.
 */
@DisplayName("470 Implement AiRecommendationRepository")
class AiRecommendationRepositoryTests {

    private static final UUID TARGET_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000470");
    private static final UUID APPROVER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000470");

    @Test
    void extendsJpaRepositoryForAiRecommendationAggregate() {
        assertThat(JpaRepository.class).isAssignableFrom(AiRecommendationRepository.class);

        ParameterizedType repositoryType =
                (ParameterizedType)
                        List.of(AiRecommendationRepository.class.getGenericInterfaces()).stream()
                                .filter(ParameterizedType.class::isInstance)
                                .findFirst()
                                .orElseThrow();

        assertThat(repositoryType.getRawType()).isEqualTo(JpaRepository.class);
        assertThat(repositoryType.getActualTypeArguments())
                .containsExactly(AiRecommendation.class, UUID.class);
    }

    @Test
    void declaresSpringDataPropertyPathQueryMethods() throws Exception {
        assertListOfRecommendations(
                AiRecommendationRepository.class.getMethod(
                        "findByTargetEntityTypeAndTargetEntityIdOrderByCreatedAtDesc",
                        String.class,
                        UUID.class));
        assertListOfRecommendations(
                AiRecommendationRepository.class.getMethod(
                        "findByRecommendationTypeOrderByCreatedAtDesc",
                        AiRecommendationType.class));
        assertListOfRecommendations(
                AiRecommendationRepository.class.getMethod("findAllByOrderByCreatedAtDesc"));
        assertListOfRecommendations(
                AiRecommendationRepository.class.getMethod(
                        "findByTargetEntityTypeOrderByCreatedAtDesc", String.class));
        assertListOfRecommendations(
                AiRecommendationRepository.class.getMethod(
                        "findByApprovedBy_IdOrderByCreatedAtDesc", UUID.class));
    }

    @Test
    void declaresKbFindByTargetEntityDefault() throws Exception {
        Method method =
                AiRecommendationRepository.class.getMethod(
                        "findByTargetEntity", String.class, UUID.class);

        assertThat(method.isDefault()).isTrue();
        assertListOfRecommendations(method);
    }

    @Test
    void declaresKbFindByRecommendationTypeDefault() throws Exception {
        Method method =
                AiRecommendationRepository.class.getMethod(
                        "findByRecommendationType", AiRecommendationType.class);

        assertThat(method.isDefault()).isTrue();
        assertListOfRecommendations(method);
    }

    @Test
    void findByTargetEntityDelegatesToPropertyPathLookup() {
        AiRecommendation product =
                AiRecommendation.create(
                        AiRecommendationType.PRODUCT,
                        "customer",
                        TARGET_ID,
                        "Owns life policy",
                        "Recommend homeowner product",
                        "Gap in homeowner coverage");
        AiRecommendationRepository repository =
                mock(AiRecommendationRepository.class, Mockito.CALLS_REAL_METHODS);
        when(repository.findByTargetEntityTypeAndTargetEntityIdOrderByCreatedAtDesc(
                        "customer", TARGET_ID))
                .thenReturn(List.of(product));
        when(repository.findByTargetEntityTypeAndTargetEntityIdOrderByCreatedAtDesc(
                        "customer", UUID.fromString("20000000-0000-0000-0000-00000000dead")))
                .thenReturn(List.of());

        assertThat(repository.findByTargetEntity("customer", TARGET_ID)).containsExactly(product);
        assertThat(
                        repository.findByTargetEntity(
                                "customer",
                                UUID.fromString("20000000-0000-0000-0000-00000000dead")))
                .isEmpty();
    }

    @Test
    void findByRecommendationTypeDelegatesToPropertyPathLookup() {
        AiRecommendation risk =
                AiRecommendation.create(
                        AiRecommendationType.RISK,
                        "customer",
                        TARGET_ID,
                        "Overdue payments",
                        "Elevated default risk",
                        "Payment history indicates risk");
        AiRecommendationRepository repository =
                mock(AiRecommendationRepository.class, Mockito.CALLS_REAL_METHODS);
        when(repository.findByRecommendationTypeOrderByCreatedAtDesc(AiRecommendationType.RISK))
                .thenReturn(List.of(risk));
        when(repository.findByRecommendationTypeOrderByCreatedAtDesc(AiRecommendationType.COPY))
                .thenReturn(List.of());

        assertThat(repository.findByRecommendationType(AiRecommendationType.RISK))
                .containsExactly(risk);
        assertThat(repository.findByRecommendationType(AiRecommendationType.COPY)).isEmpty();
    }

    @Test
    void approvedByLookupMethodUsesApproverUserId() throws Exception {
        Method method =
                AiRecommendationRepository.class.getMethod(
                        "findByApprovedBy_IdOrderByCreatedAtDesc", UUID.class);
        assertListOfRecommendations(method);
        assertThat(method.getParameterTypes()[0]).isEqualTo(UUID.class);
        assertThat(APPROVER_ID).isNotNull();
    }

    private static void assertListOfRecommendations(Method method) {
        assertThat(method.getReturnType()).isEqualTo(List.class);
        ParameterizedType generic = (ParameterizedType) method.getGenericReturnType();
        assertThat(generic.getActualTypeArguments()[0]).isEqualTo(AiRecommendation.class);
    }
}
