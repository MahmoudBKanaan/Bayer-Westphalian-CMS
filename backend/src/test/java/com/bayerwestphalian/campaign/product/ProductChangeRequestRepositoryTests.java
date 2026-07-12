package com.bayerwestphalian.campaign.product;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

class ProductChangeRequestRepositoryTests {

    @Test
    void extendsJpaRepositoryForProductChangeRequestAggregate() {
        assertThat(JpaRepository.class).isAssignableFrom(ProductChangeRequestRepository.class);

        ParameterizedType repositoryType =
                (ParameterizedType)
                        List.of(ProductChangeRequestRepository.class.getGenericInterfaces())
                                .stream()
                                .filter(ParameterizedType.class::isInstance)
                                .findFirst()
                                .orElseThrow();

        assertThat(repositoryType.getRawType()).isEqualTo(JpaRepository.class);
        assertThat(repositoryType.getActualTypeArguments())
                .containsExactly(ProductChangeRequest.class, UUID.class);
    }

    @Test
    void declaresKbStatusAndProductLookupMethods() throws Exception {
        Method findByStatus =
                ProductChangeRequestRepository.class.getMethod(
                        "findByStatus", ProductChangeStatus.class);
        Method findByProductId =
                ProductChangeRequestRepository.class.getMethod("findByProductId", UUID.class);

        assertThat(findByStatus.getGenericReturnType()).isEqualTo(productChangeRequestList());
        assertThat(findByProductId.getGenericReturnType()).isEqualTo(productChangeRequestList());
    }

    @Test
    void concreteFindersUseStableCreatedAtOrderingForWorkflowQueues() throws Exception {
        assertThat(
                        ProductChangeRequestRepository.class
                                .getMethod(
                                        "findByStatusOrderByCreatedAtDesc",
                                        ProductChangeStatus.class)
                                .getGenericReturnType())
                .isEqualTo(productChangeRequestList());
        assertThat(
                        ProductChangeRequestRepository.class
                                .getMethod("findByProductIdOrderByCreatedAtDesc", UUID.class)
                                .getGenericReturnType())
                .isEqualTo(productChangeRequestList());
    }

    private static Type productChangeRequestList() throws NoSuchMethodException {
        return ReturnTypes.class
                .getDeclaredMethod("productChangeRequestList")
                .getGenericReturnType();
    }

    private interface ReturnTypes {
        List<ProductChangeRequest> productChangeRequestList();
    }
}
