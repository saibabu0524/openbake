package com.openbake.server.dto.order;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CouponApplyRequest {
    @NotBlank
    private String code;
    private double subtotal;
}
