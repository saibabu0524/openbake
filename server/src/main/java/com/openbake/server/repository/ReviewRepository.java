package com.openbake.server.repository;

import com.openbake.server.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, String> {
    Page<Review> findByProductIdOrderByCreatedAtDesc(String productId, Pageable pageable);
    List<Review> findByProductId(String productId);
    boolean existsByUserIdAndProductIdAndOrderId(String userId, String productId, String orderId);
}
