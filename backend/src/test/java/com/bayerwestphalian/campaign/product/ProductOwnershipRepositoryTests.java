package com.bayerwestphalian.campaign.product;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

class ProductOwnershipRepositoryTests {

    @Test
    void extendsJpaRepositoryForProductOwnershipAggregate() {
        assertThat(JpaRepository.class).isAssignableFrom(ProductOwnershipRepository.class);

        ParameterizedType repositoryType =
                (ParameterizedType)
                        List.of(ProductOwnershipRepository.class.getGenericInterfaces()).stream()
                                .filter(ParameterizedType.class::isInstance)
                                .findFirst()
                                .orElseThrow();

        assertThat(repositoryType.getRawType()).isEqualTo(JpaRepository.class);
        assertThat(repositoryType.getActualTypeArguments())
                .containsExactly(ProductOwnership.class, UUID.class);
    }

    @Test
    void declaresKbCustomerOwnershipLookupMethod() throws Exception {
        assertThat(
                        ProductOwnershipRepository.class
                                .getMethod("findByCustomerId", UUID.class)
                                .getGenericReturnType())
                .isEqualTo(productOwnershipList());
    }

    @Test
    void concreteFinderUsesStableStartDateOrderingForCustomerOwnershipLists() throws Exception {
        assertThat(
                        ProductOwnershipRepository.class
                                .getMethod("findByCustomerIdOrderByStartDateDesc", UUID.class)
                                .getGenericReturnType())
                .isEqualTo(productOwnershipList());
    }

    @Test
    void declaresKbExpiringBetweenQueryForActiveOwnershipsInDateRange() throws Exception {
        Method method =
                ProductOwnershipRepository.class.getMethod(
                        "findExpiringBetween", LocalDate.class, LocalDate.class);
        Query query = method.getAnnotation(Query.class);

        assertThat(method.getGenericReturnType()).isEqualTo(productOwnershipList());
        assertThat(method.getParameters()[0].getAnnotation(Param.class).value())
                .isEqualTo("startDate");
        assertThat(method.getParameters()[1].getAnnotation(Param.class).value())
                .isEqualTo("endDate");
        assertThat(query.value())
                .contains("ownership.status = 'ACTIVE'")
                .contains("ownership.expirationDate is not null")
                .contains("ownership.expirationDate between :startDate and :endDate")
                .contains("order by ownership.expirationDate asc, ownership.createdAt asc");
    }

    @Test
    void declaresKbActiveProductOwnershipFinder() throws Exception {
        assertThat(
                        ProductOwnershipRepository.class
                                .getMethod("findActiveByProduct", UUID.class)
                                .getGenericReturnType())
                .isEqualTo(productOwnershipList());
        assertThat(
                        ProductOwnershipRepository.class
                                .getMethod(
                                        "findByProductIdAndStatusOrderByStartDateDesc",
                                        UUID.class,
                                        OwnershipStatus.class)
                                .getGenericReturnType())
                .isEqualTo(productOwnershipList());
    }

    private static Type productOwnershipList() throws NoSuchMethodException {
        return ReturnTypes.class.getDeclaredMethod("productOwnershipList").getGenericReturnType();
    }

    private interface ReturnTypes {
        List<ProductOwnership> productOwnershipList();
    }
}
