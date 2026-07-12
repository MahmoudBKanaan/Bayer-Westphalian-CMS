package com.bayerwestphalian.campaign.product;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findByActiveTrueAndDeletedAtIsNullOrderByNameAsc();

    List<Product> findByProductTypeAndDeletedAtIsNullOrderByNameAsc(ProductType productType);

    @Query(
            """
            select product
            from Product product
            where product.deletedAt is null
              and (
                lower(product.name) like lower(concat('%', :term, '%'))
                or lower(product.description) like lower(concat('%', :term, '%'))
              )
            order by product.name asc
            """)
    List<Product> searchByName(@Param("term") String term);

    default List<Product> findActive() {
        return findByActiveTrueAndDeletedAtIsNullOrderByNameAsc();
    }

    default List<Product> findByType(ProductType productType) {
        return findByProductTypeAndDeletedAtIsNullOrderByNameAsc(productType);
    }

    default List<Product> searchByNameOrType(String term) {
        List<Product> matches = new ArrayList<>(searchByName(term));
        parseProductType(term).ifPresent(productType -> matches.addAll(findByType(productType)));
        return matches.stream().distinct().toList();
    }

    private static java.util.Optional<ProductType> parseProductType(String term) {
        if (term == null || term.isBlank()) {
            return java.util.Optional.empty();
        }

        String normalized =
                term.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
        try {
            return java.util.Optional.of(ProductType.valueOf(normalized));
        } catch (IllegalArgumentException ignored) {
            return java.util.Optional.empty();
        }
    }
}
