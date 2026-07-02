package com.openbake.server.dto.waitlist;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openbake.server.entity.WaitlistItem;

import java.time.LocalDateTime;

/** Mirrors backend/app/routers/waitlist.py's WaitlistItemResponse — naive UTC datetimes, no IST conversion. */
public class WaitlistItemResponse {

    private final String id;

    @JsonProperty("user_id")
    private final String userId;

    @JsonProperty("product_id")
    private final String productId;

    @JsonProperty("product_name")
    private final String productName;

    @JsonProperty("variant_id")
    private final String variantId;

    private final String status;

    @JsonProperty("notified_at")
    private final LocalDateTime notifiedAt;

    @JsonProperty("created_at")
    private final LocalDateTime createdAt;

    public WaitlistItemResponse(WaitlistItem item, String productName) {
        this.id = item.getId();
        this.userId = item.getUser().getId();
        this.productId = item.getProduct().getId();
        this.productName = productName;
        this.variantId = item.getVariant() != null ? item.getVariant().getId() : null;
        this.status = item.getStatus();
        this.notifiedAt = item.getNotifiedAt();
        this.createdAt = item.getCreatedAt();
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getVariantId() { return variantId; }
    public String getStatus() { return status; }
    public LocalDateTime getNotifiedAt() { return notifiedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
