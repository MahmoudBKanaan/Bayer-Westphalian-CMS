package com.bayerwestphalian.campaign.product;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

class ProductRepositoryTests {

    @Test
    void extendsJpaRepositoryForProductAggregate() {
        assertThat(JpaRepository.class).isAssignableFrom(ProductRepository.class);

        ParameterizedType repositoryType =
                (ParameterizedType)
                        List.of(ProductRepository.class.getGenericInterfaces()).stream()
                                .filter(ParameterizedType.class::isInstance)
                                .findFirst()
                                .orElseThrow();

        assertThat(repositoryType.getRawType()).isEqualTo(JpaRepository.class);
        assertThat(repositoryType.getActualTypeArguments())
                .containsExactly(Product.class, UUID.class);
    }

    @Test
    void declaresKbActiveProductFinderThatExcludesSoftDeletedProducts() throws Exception {
        assertThat(
                        ProductRepository.class
                                .getMethod("findActive")
                                .getGenericReturnType())
                .isEqualTo(productList());
        assertThat(
                        ProductRepository.class
                                .getMethod("findByActiveTrueAndDeletedAtIsNullOrderByNameAsc")
                                .getGenericReturnType())
                .isEqualTo(productList());
    }

    @Test
    void declaresKbProductTypeFinderThatExcludesSoftDeletedProducts() throws Exception {
        assertThat(
                        ProductRepository.class
                                .getMethod("findByType", ProductType.class)
                                .getGenericReturnType())
                .isEqualTo(productList());
        assertThat(
                        ProductRepository.class
                                .getMethod(
                                        "findByProductTypeAndDeletedAtIsNullOrderByNameAsc",
                                        ProductType.class)
                                .getGenericReturnType())
                .isEqualTo(productList());
    }

    @Test
    void declaresKbProductSearchByNameOrTypeContract() throws Exception {
        Method searchByNameOrType =
                ProductRepository.class.getMethod("searchByNameOrType", String.class);
        Method searchByName = ProductRepository.class.getMethod("searchByName", String.class);
        Query query = searchByName.getAnnotation(Query.class);

        assertThat(searchByNameOrType.getGenericReturnType()).isEqualTo(productList());
        assertThat(searchByName.getGenericReturnType()).isEqualTo(productList());
        assertThat(searchByName.getParameters()[0].getAnnotation(Param.class).value())
                .isEqualTo("term");
        assertThat(query.value())
                .contains("product.deletedAt is null")
                .contains("product.name")
                .contains("product.description")
                .contains("order by product.name asc");
    }

    private static Type productList() throws NoSuchMethodException {
        return ReturnTypes.class.getDeclaredMethod("productList").getGenericReturnType();
    }

    private interface ReturnTypes {
        List<Product> productList();
    }
}
