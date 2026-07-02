package com.openbake.server.dto.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewCreateRequest {

    @NotBlank
    @JsonProperty("product_id")
    private String productId;

    @NotBlank
    @JsonProperty("order_id")
    private String orderId;

    @Min(1)
    @Max(5)
    private int rating;

    private String comment;
}
