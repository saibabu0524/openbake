package com.openbake.server.dto.order;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class CartValidatedItem {

    @JsonProperty("product_id")
    private final String productId;

    @JsonProperty("product_name")
    private final String productName;

    private final int quantity;

    @JsonProperty("unit_price")
    private final double unitPrice;

    @JsonProperty("stock_count")
    private final int stockCount;

    @JsonProperty("is_available")
    private final boolean available;

    private final Map<String, Object> customization;

    public CartValidatedItem(String productId, String productName, int quantity, double unitPrice,
                              int stockCount, boolean available, Map<String, Object> customization) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.stockCount = stockCount;
        this.available = available;
        this.customization = customization;
    }

    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public double getUnitPrice() { return unitPrice; }
    public int getStockCount() { return stockCount; }
    public boolean isAvailable() { return available; }
    public Map<String, Object> getCustomization() { return customization; }
}
