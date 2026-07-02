package com.openbake.server.dto.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CouponCreateRequest {

    @NotBlank
    private String code;

    /** flat | percent */
    @NotBlank
    @JsonProperty("discount_type")
    private String discountType;

    @JsonProperty("discount_value")
    private double discountValue;

    @JsonProperty("min_order_value")
    private double minOrderValue = 0.0;

    @JsonProperty("max_uses")
    private int maxUses = 100;

    @JsonProperty("valid_from")
    private LocalDate validFrom;

    @JsonProperty("valid_until")
    private LocalDate validUntil;

    @JsonProperty("is_active")
    private boolean active = true;
}
