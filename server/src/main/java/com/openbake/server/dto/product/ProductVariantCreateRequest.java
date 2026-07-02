package com.openbake.server.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductVariantCreateRequest {

    /** size | flavor */
    @NotBlank
    @JsonProperty("variant_type")
    private String variantType;

    @NotBlank
    private String value;

    @JsonProperty("extra_price")
    private double extraPrice = 0.0;
}
