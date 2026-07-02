package com.openbake.server.controller;

import com.openbake.server.dto.delivery.DeliveryConfigResponse;
import com.openbake.server.dto.delivery.DeliveryConfigUpdateRequest;
import com.openbake.server.dto.delivery.DeliveryEstimateResponse;
import com.openbake.server.exception.ApiException;
import com.openbake.server.service.DeliveryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** 1:1 port of backend/app/routers/delivery.py. */
@RestController
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping("/api/delivery/estimate")
    public DeliveryEstimateResponse estimateDelivery(@RequestParam double lat, @RequestParam double lng) {
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid coordinates");
        }
        return new DeliveryEstimateResponse(deliveryService.calculateDeliveryFee(lat, lng));
    }

    @GetMapping("/api/admin/delivery-config")
    public DeliveryConfigResponse getDeliveryConfig() {
        return new DeliveryConfigResponse(deliveryService.getDeliveryConfig());
    }

    @PatchMapping("/api/admin/delivery-config")
    public DeliveryConfigResponse updateDeliveryConfig(@RequestBody DeliveryConfigUpdateRequest data) {
        var updates = data.toUpdatesMap();
        if (updates.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No fields to update");
        }
        return new DeliveryConfigResponse(deliveryService.updateDeliveryConfig(updates));
    }
}
