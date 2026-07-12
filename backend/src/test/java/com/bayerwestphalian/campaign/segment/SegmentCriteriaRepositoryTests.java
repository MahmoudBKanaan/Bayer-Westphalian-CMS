package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

class SegmentCriteriaRepositoryTests {

    @Test
    void extendsJpaRepositoryForSegmentCriteriaAggregate() {
        assertThat(JpaRepository.class).isAssignableFrom(SegmentCriteriaRepository.class);

        ParameterizedType repositoryType =
                (ParameterizedType)
                        List.of(SegmentCriteriaRepository.class.getGenericInterfaces()).stream()
                                .filter(ParameterizedType.class::isInstance)
                                .findFirst()
                                .orElseThrow();

        assertThat(repositoryType.getRawType()).isEqualTo(JpaRepository.class);
        assertThat(repositoryType.getActualTypeArguments())
                .containsExactly(SegmentCriteria.class, UUID.class);
    }

    @Test
    void declaresKbSegmentCriteriaLookupMethod() throws Exception {
        Method findBySegmentId =
                SegmentCriteriaRepository.class.getMethod("findBySegmentId", UUID.class);
        Method findBySegmentIdOrdered =
                SegmentCriteriaRepository.class.getMethod(
                        "findBySegment_IdOrderByFieldNameAsc", UUID.class);

        assertThat(findBySegmentId.getGenericReturnType()).isEqualTo(segmentCriteriaList());
        assertThat(findBySegmentIdOrdered.getGenericReturnType()).isEqualTo(segmentCriteriaList());
    }

    private static Type segmentCriteriaList() throws NoSuchMethodException {
        return ReturnTypes.class.getDeclaredMethod("segmentCriteriaList").getGenericReturnType();
    }

    private interface ReturnTypes {
        List<SegmentCriteria> segmentCriteriaList();
    }
}
