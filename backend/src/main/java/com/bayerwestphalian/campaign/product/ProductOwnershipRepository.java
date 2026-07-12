package com.bayerwestphalian.campaign.product;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductOwnershipRepository extends JpaRepository<ProductOwnership, UUID> {

    List<ProductOwnership> findByCustomerIdOrderByStartDateDesc(UUID customerId);

    List<ProductOwnership> findByProductIdAndStatusOrderByStartDateDesc(
            UUID productId, OwnershipStatus status);

    @Query(
            """
            select ownership
            from ProductOwnership ownership
            where ownership.status = 'ACTIVE'
              and ownership.expirationDate is not null
              and ownership.expirationDate between :startDate and :endDate
            order by ownership.expirationDate asc, ownership.createdAt asc
            """)
    List<ProductOwnership> findExpiringBetween(
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    default List<ProductOwnership> findByCustomerId(UUID customerId) {
        return findByCustomerIdOrderByStartDateDesc(customerId);
    }

    default List<ProductOwnership> findActiveByProduct(UUID productId) {
        return findByProductIdAndStatusOrderByStartDateDesc(productId, OwnershipStatus.ACTIVE);
    }
}
