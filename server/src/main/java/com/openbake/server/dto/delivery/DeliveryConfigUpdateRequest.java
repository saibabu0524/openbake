package com.openbake.server.dto.delivery;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class DeliveryConfigUpdateRequest {

    @JsonProperty("bakery_lat")
    private Double bakeryLat;

    @JsonProperty("bakery_lng")
    private Double bakeryLng;

    @JsonProperty("free_delivery_radius_km")
    private Double freeDeliveryRadiusKm;

    @JsonProperty("delivery_fee_default")
    private Double deliveryFeeDefault;

    @JsonProperty("speed_min_per_km")
    private Double speedMinPerKm;

    @JsonProperty("cod_enabled")
    private Boolean codEnabled;

    public Map<String, Object> toUpdatesMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        if (bakeryLat != null) map.put("bakery_lat", bakeryLat);
        if (bakeryLng != null) map.put("bakery_lng", bakeryLng);
        if (freeDeliveryRadiusKm != null) map.put("free_delivery_radius_km", freeDeliveryRadiusKm);
        if (deliveryFeeDefault != null) map.put("delivery_fee_default", deliveryFeeDefault);
        if (speedMinPerKm != null) map.put("speed_min_per_km", speedMinPerKm);
        if (codEnabled != null) map.put("cod_enabled", codEnabled);
        return map;
    }
}
