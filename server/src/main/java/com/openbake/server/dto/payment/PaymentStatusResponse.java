package com.openbake.server.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PaymentStatusResponse {

    @JsonProperty("order_id")
    private final String orderId;

    @JsonProperty("payment_status")
    private final String paymentStatus;

    private final String message;

    public PaymentStatusResponse(String orderId, String paymentStatus, String message) {
        this.orderId = orderId;
        this.paymentStatus = paymentStatus;
        this.message = message;
    }

    public String getOrderId() { return orderId; }
    public String getPaymentStatus() { return paymentStatus; }
    public String getMessage() { return message; }
}
