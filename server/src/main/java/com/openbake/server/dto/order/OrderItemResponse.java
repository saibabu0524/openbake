package com.openbake.server.dto.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openbake.server.entity.OrderItem;

import java.util.Map;

public class OrderItemResponse {

    private final String id;

    @JsonProperty("product_id")
    private final String productId;

    @JsonProperty("product_name")
    private final String productName;

    private final int quantity;

    @JsonProperty("unit_price")
    private final double unitPrice;

    private final Map<String, Object> customization;

    public OrderItemResponse(OrderItem item) {
        this.id = item.getId();
        this.productId = item.getProduct().getId();
        this.productName = item.getProductName();
        this.quantity = item.getQuantity();
        this.unitPrice = item.getUnitPrice();
        this.customization = item.getCustomization();
    }

    public String getId() { return id; }
    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public double getUnitPrice() { return unitPrice; }
    public Map<String, Object> getCustomization() { return customization; }
}
