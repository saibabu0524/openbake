package com.openbake.server.dto.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/** Mirrors backend/app/schemas/order.py's OrderCreate. */
@Getter
@Setter
public class OrderCreateRequest {

    @JsonProperty("address_id")
    private String addressId;

    /** delivery | pickup */
    @Pattern(regexp = "delivery|pickup", message = "order_type must be 'delivery' or 'pickup'")
    @JsonProperty("order_type")
    private String orderType = "delivery";

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItemCreateRequest> items;

    @JsonProperty("coupon_code")
    private String couponCode;

    /** upi | card | wallet | cod */
    @NotNull
    @Pattern(regexp = "upi|card|wallet|cod", message = "Invalid payment method")
    @JsonProperty("payment_method")
    private String paymentMethod;

    @JsonProperty("scheduled_date")
    private LocalDate scheduledDate;

    @JsonProperty("time_slot")
    private String timeSlot;

    @JsonProperty("special_note")
    private String specialNote;

    @JsonProperty("idempotency_key")
    private String idempotencyKey;

    public void setAddressId(String addressId) {
        this.addressId = (addressId == null || addressId.isEmpty()) ? null : addressId;
    }
}
