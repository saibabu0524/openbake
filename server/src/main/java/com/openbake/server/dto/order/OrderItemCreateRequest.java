package com.openbake.server.dto.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class OrderItemCreateRequest {

    @NotBlank
    @JsonProperty("product_id")
    private String productId;

    @Min(1)
    @Max(50)
    private int quantity = 1;

    /** {eggless, size, flavor, cake_message} */
    private Map<String, Object> customization;
}
