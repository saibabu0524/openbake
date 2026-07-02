package com.openbake.server.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class CreatePaymentOrderResponse {

    private final String provider;
    private final String mode;
    private final String txnid;
    private final String amount;
    private final String currency;

    @JsonProperty("order_id")
    private final String orderId;

    @JsonProperty("payment_url")
    private final String paymentUrl;

    @JsonProperty("checkout_url")
    private final String checkoutUrl;

    private final String method;
    private final Map<String, String> fields;

    @JsonProperty("payment_options")
    private final List<String> paymentOptions;

    @JsonProperty("upi_intent_supported")
    private final boolean upiIntentSupported;

    @SuppressWarnings("unchecked")
    public CreatePaymentOrderResponse(Map<String, Object> payload, String orderId, String checkoutUrl) {
        this.provider = (String) payload.get("provider");
        this.mode = (String) payload.get("mode");
        this.txnid = (String) payload.get("txnid");
        this.amount = (String) payload.get("amount");
        this.currency = (String) payload.getOrDefault("currency", "INR");
        this.orderId = orderId;
        this.paymentUrl = (String) payload.get("payment_url");
        this.checkoutUrl = checkoutUrl;
        this.method = (String) payload.get("method");
        this.fields = (Map<String, String>) payload.get("fields");
        this.paymentOptions = (List<String>) payload.get("payment_options");
        this.upiIntentSupported = (boolean) payload.get("upi_intent_supported");
    }

    public String getProvider() { return provider; }
    public String getMode() { return mode; }
    public String getTxnid() { return txnid; }
    public String getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getOrderId() { return orderId; }
    public String getPaymentUrl() { return paymentUrl; }
    public String getCheckoutUrl() { return checkoutUrl; }
    public String getMethod() { return method; }
    public Map<String, String> getFields() { return fields; }
    public List<String> getPaymentOptions() { return paymentOptions; }
    public boolean isUpiIntentSupported() { return upiIntentSupported; }
}
