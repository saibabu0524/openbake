package com.openbake.server.dto.waitlist;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openbake.server.entity.Product;
import com.openbake.server.entity.WaitlistItem;

import java.time.LocalDateTime;

public class UserWaitlistEntry {

    private final String id;

    @JsonProperty("product_id")
    private final String productId;

    @JsonProperty("product_name")
    private final String productName;

    @JsonProperty("product_image")
    private final String productImage;

    @JsonProperty("product_price")
    private final Double productPrice;

    @JsonProperty("variant_id")
    private final String variantId;

    private final String status;

    @JsonProperty("notified_at")
    private final LocalDateTime notifiedAt;

    @JsonProperty("created_at")
    private final LocalDateTime createdAt;

    public UserWaitlistEntry(WaitlistItem item, Product product) {
        this.id = item.getId();
        this.productId = item.getProduct().getId();
        this.productName = product != null ? product.getName() : null;
        this.productImage = (product != null && !product.getImages().isEmpty()) ? product.getImages().get(0) : null;
        this.productPrice = product != null ? product.getPrice() : null;
        this.variantId = item.getVariant() != null ? item.getVariant().getId() : null;
        this.status = item.getStatus();
        this.notifiedAt = item.getNotifiedAt();
        this.createdAt = item.getCreatedAt();
    }

    public String getId() { return id; }
    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getProductImage() { return productImage; }
    public Double getProductPrice() { return productPrice; }
    public String getVariantId() { return variantId; }
    public String getStatus() { return status; }
    public LocalDateTime getNotifiedAt() { return notifiedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
