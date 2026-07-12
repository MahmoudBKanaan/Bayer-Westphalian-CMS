package com.bayerwestphalian.campaign.product;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductChangeRequestRepository
        extends JpaRepository<ProductChangeRequest, UUID> {

    List<ProductChangeRequest> findByStatusOrderByCreatedAtDesc(ProductChangeStatus status);

    List<ProductChangeRequest> findByProductIdOrderByCreatedAtDesc(UUID productId);

    default List<ProductChangeRequest> findByStatus(ProductChangeStatus status) {
        return findByStatusOrderByCreatedAtDesc(status);
    }

    default List<ProductChangeRequest> findByProductId(UUID productId) {
        return findByProductIdOrderByCreatedAtDesc(productId);
    }
}