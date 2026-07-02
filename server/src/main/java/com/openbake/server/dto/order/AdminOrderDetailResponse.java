package com.openbake.server.dto.order;

import com.openbake.server.entity.Order;

/** Enriched order response that includes customer details for admin views. */
public class AdminOrderDetailResponse extends OrderResponse {

    private final OrderCustomerInfo customer;

    public AdminOrderDetailResponse(Order order) {
        super(order);
        this.customer = order.getUser() != null ? new OrderCustomerInfo(order.getUser()) : null;
    }

    public OrderCustomerInfo getCustomer() {
        return customer;
    }
}
