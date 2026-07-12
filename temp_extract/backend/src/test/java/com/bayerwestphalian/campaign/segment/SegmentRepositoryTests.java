package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

class SegmentRepositoryTests {

    @Test
    void extendsJpaRepositoryForSegmentAggregate() {
        assertThat(JpaRepository.class).isAssignableFrom(SegmentRepository.class);

        ParameterizedType repositoryType =
                (ParameterizedType)
                        List.of(SegmentRepository.class.getGenericInterfaces()).stream()
                                .filter(ParameterizedType.class::isInstance)
                                .findFirst()
                                .orElseThrow();

        assertThat(repositoryType.getRawType()).isEqualTo(JpaRepository.class);
        assertThat(repositoryType.getActualTypeArguments())
                .containsExactly(Segment.class, UUID.class);
    }

    @Test
    void declaresKbOwnerLookupMethod() throws Exception {
        Method findByOwner = SegmentRepository.class.getMethod("findByOwner", UUID.class);
        Method findByOwnerId =
                SegmentRepository.class.getMethod("findByOwner_IdOrderByNameAsc", UUID.class);

        assertThat(findByOwner.getGenericReturnType()).isEqualTo(segmentList());
        assertThat(findByOwnerId.getGenericReturnType()).isEqualTo(segmentList());
    }

    @Test
    void declaresKbVisibilityAndGlobalLookupMethods() throws Exception {
        Method findByVisibility =
                SegmentRepository.class.getMethod("findByVisibility", SegmentVisibility.class);
        Method findByVisibilityOrdered =
                SegmentRepository.class.getMethod(
                        "findByVisibilityOrderByNameAsc", SegmentVisibility.class);
        Method findGlobal = SegmentRepository.class.getMethod("findGlobal");

        assertThat(findByVisibility.getGenericReturnType()).isEqualTo(segmentList());
        assertThat(findByVisibilityOrdered.getGenericReturnType()).isEqualTo(segmentList());
        assertThat(findGlobal.getGenericReturnType()).isEqualTo(segmentList());
    }

    private static Type segmentList() throws NoSuchMethodException {
        return ReturnTypes.class.getDeclaredMethod("segmentList").getGenericReturnType();
    }

    private interface ReturnTypes {
        List<Segment> segmentList();
    }
}