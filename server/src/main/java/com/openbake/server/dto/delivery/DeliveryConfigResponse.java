package com.openbake.server.dto.delivery;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openbake.server.service.DeliveryService;

public class DeliveryConfigResponse {

    @JsonProperty("bakery_lat")
    private final double bakeryLat;

    @JsonProperty("bakery_lng")
    private final double bakeryLng;

    @JsonProperty("free_delivery_radius_km")
    private final double freeDeliveryRadiusKm;

    @JsonProperty("delivery_fee_default")
    private final double deliveryFeeDefault;

    @JsonProperty("speed_min_per_km")
    private final double speedMinPerKm;

    @JsonProperty("cod_enabled")
    private final boolean codEnabled;

    public DeliveryConfigResponse(DeliveryService.Config config) {
        this.bakeryLat = config.bakeryLat();
        this.bakeryLng = config.bakeryLng();
        this.freeDeliveryRadiusKm = config.freeDeliveryRadiusKm();
        this.deliveryFeeDefault = config.deliveryFeeDefault();
        this.speedMinPerKm = config.speedMinPerKm();
        this.codEnabled = config.codEnabled();
    }

    public double getBakeryLat() { return bakeryLat; }
    public double getBakeryLng() { return bakeryLng; }
    public double getFreeDeliveryRadiusKm() { return freeDeliveryRadiusKm; }
    public double getDeliveryFeeDefault() { return deliveryFeeDefault; }
    public double getSpeedMinPerKm() { return speedMinPerKm; }
    public boolean isCodEnabled() { return codEnabled; }
}
