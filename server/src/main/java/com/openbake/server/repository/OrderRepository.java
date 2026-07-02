package com.openbake.server.repository;

import com.openbake.server.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String> {
    Page<Order> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<Order> findByIdempotencyKey(String idempotencyKey);
    boolean existsByUserIdAndCouponCodeAndStatusNot(String userId, String couponCode, String status);
    List<Order> findByStatusOrderByCreatedAtDesc(String status);
    List<Order> findAllByOrderByCreatedAtDesc();

    long countByCreatedAtGreaterThanEqual(LocalDateTime since);
    long countByStatus(String status);

    @Query("select coalesce(sum(o.total), 0) from Order o where o.createdAt >= :since and o.paymentStatus = 'paid'")
    double sumRevenueSince(@Param("since") LocalDateTime since);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("select coalesce(sum(o.total), 0) from Order o where o.createdAt >= :start and o.createdAt < :end and o.paymentStatus = 'paid'")
    double sumRevenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("select o.status as k, count(o) as c from Order o group by o.status")
    List<Object[]> countGroupByStatus();

    @Query("select o.orderType as k, count(o) as c from Order o group by o.orderType")
    List<Object[]> countGroupByOrderType();

    @Query("select o.paymentMethod as k, count(o) as c from Order o where o.paymentMethod is not null group by o.paymentMethod")
    List<Object[]> countGroupByPaymentMethod();
}
