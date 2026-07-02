package com.openbake.server.dto.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openbake.server.entity.WishlistItem;

public class WishlistItemResponse {

    private final String id;

    @JsonProperty("product_id")
    private final String productId;

    private final WishlistProductInfo product;

    public WishlistItemResponse(WishlistItem item) {
        this.id = item.getId();
        this.productId = item.getProduct().getId();
        this.product = new WishlistProductInfo(item.getProduct());
    }

    public String getId() { return id; }
    public String getProductId() { return productId; }
    public WishlistProductInfo getProduct() { return product; }
}
