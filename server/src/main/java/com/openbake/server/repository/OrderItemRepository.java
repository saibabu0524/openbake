package com.openbake.server.repository;

import com.openbake.server.entity.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, String> {

    @Query("select oi.product.name as name, sum(oi.quantity) as units, sum(oi.unitPrice * oi.quantity) as revenue "
            + "from OrderItem oi group by oi.product.id, oi.product.name order by sum(oi.quantity) desc")
    List<Object[]> topProductsByUnitsSold(Pageable pageable);
}
