package com.openbake.server.dto.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class CartItemValidateRequest {
    @NotBlank
    @JsonProperty("product_id")
    private String productId;

    private int quantity;
    private Map<String, Object> customization;
}
