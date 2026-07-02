package com.openbake.server.repository;

import com.openbake.server.entity.WaitlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WaitlistRepository extends JpaRepository<WaitlistItem, String> {
    List<WaitlistItem> findByUserId(String userId);
    List<WaitlistItem> findByProductIdAndStatus(String productId, String status);
    List<WaitlistItem> findByProductId(String productId);
}
