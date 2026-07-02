package com.openbake.server.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePaymentOrderRequest {

    @NotBlank
    @JsonProperty("order_id")
    private String orderId;

    private String platform = "web";
}
