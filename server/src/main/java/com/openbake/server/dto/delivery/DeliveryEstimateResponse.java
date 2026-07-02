package com.openbake.server.dto.delivery;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openbake.server.service.DeliveryService;

public class DeliveryEstimateResponse {

    @JsonProperty("distance_km")
    private final double distanceKm;

    @JsonProperty("delivery_fee")
    private final double deliveryFee;

    @JsonProperty("estimated_time_minutes")
    private final Integer estimatedTimeMinutes;

    @JsonProperty("is_free_delivery")
    private final boolean freeDelivery;

    @JsonProperty("is_deliverable")
    private final boolean deliverable;

    public DeliveryEstimateResponse(DeliveryService.DeliveryEstimate estimate) {
        this.distanceKm = estimate.distanceKm();
        this.deliveryFee = estimate.deliveryFee();
        this.estimatedTimeMinutes = estimate.estimatedTimeMinutes();
        this.freeDelivery = estimate.freeDelivery();
        this.deliverable = estimate.deliverable();
    }

    public double getDistanceKm() { return distanceKm; }
    public double getDeliveryFee() { return deliveryFee; }
    public Integer getEstimatedTimeMinutes() { return estimatedTimeMinutes; }
    public boolean isFreeDelivery() { return freeDelivery; }
    public boolean isDeliverable() { return deliverable; }
}
