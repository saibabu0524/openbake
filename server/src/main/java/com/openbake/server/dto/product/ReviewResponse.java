package com.openbake.server.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openbake.server.entity.Review;
import com.openbake.server.util.IstTime;

public class ReviewResponse {

    private final String id;

    @JsonProperty("user_id")
    private final String userId;

    @JsonProperty("product_id")
    private final String productId;

    @JsonProperty("order_id")
    private final String orderId;

    private final int rating;
    private final String comment;

    @JsonProperty("created_at")
    private final String createdAt;

    public ReviewResponse(Review review) {
        this.id = review.getId();
        this.userId = review.getUser().getId();
        this.productId = review.getProduct().getId();
        this.orderId = review.getOrderId();
        this.rating = review.getRating();
        this.comment = review.getComment();
        this.createdAt = IstTime.toIso(review.getCreatedAt());
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getProductId() { return productId; }
    public String getOrderId() { return orderId; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public String getCreatedAt() { return createdAt; }
}
