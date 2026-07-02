package com.openbake.server.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class VerifyPaymentRequest {

    @NotBlank
    @JsonProperty("order_id")
    private String orderId;

    private String txnid;
    private String status;
    private String mihpayid;
    private String hash;

    @JsonProperty("raw_payload")
    private Map<String, String> rawPayload;
}
