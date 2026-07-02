package com.openbake.server.dto.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openbake.server.dto.profile.AddressResponse;
import com.openbake.server.entity.Order;
import com.openbake.server.util.IstTime;

import java.util.List;
import java.util.Map;

/** Mirrors backend/app/schemas/order.py's OrderResponse (IST-formatted timestamps). */
public class OrderResponse {

    private final String id;

    @JsonProperty("user_id")
    private final String userId;

    @JsonProperty("address_id")
    private final String addressId;

    private final AddressResponse address;

    @JsonProperty("order_type")
    private final String orderType;

    private final String status;
    private final double subtotal;
    private final double discount;

    @JsonProperty("delivery_fee")
    private final double deliveryFee;

    private final double total;

    @JsonProperty("coupon_code")
    private final String couponCode;

    @JsonProperty("payment_method")
    private final String paymentMethod;

    @JsonProperty("payment_status")
    private final String paymentStatus;

    @JsonProperty("razorpay_order_id")
    private final String razorpayOrderId;

    @JsonProperty("razorpay_payment_id")
    private final String razorpayPaymentId;

    @JsonProperty("idempotency_key")
    private final String idempotencyKey;

    @JsonProperty("estimated_delivery_minutes")
    private final Integer estimatedDeliveryMinutes;

    @JsonProperty("scheduled_date")
    private final java.time.LocalDate scheduledDate;

    @JsonProperty("time_slot")
    private final String timeSlot;

    @JsonProperty("special_note")
    private final String specialNote;

    @JsonProperty("created_at")
    private final String createdAt;

    @JsonProperty("updated_at")
    private final String updatedAt;

    @JsonProperty("status_timestamps")
    private final Map<String, Object> statusTimestamps;

    private final List<OrderItemResponse> items;

    public OrderResponse(Order order) {
        this.id = order.getId();
        this.userId = order.getUser().getId();
        this.addressId = order.getAddress() != null ? order.getAddress().getId() : null;
        this.address = order.getAddress() != null ? new AddressResponse(order.getAddress()) : null;
        this.orderType = order.getOrderType();
        this.status = order.getStatus();
        this.subtotal = order.getSubtotal();
        this.discount = order.getDiscount();
        this.deliveryFee = order.getDeliveryFee();
        this.total = order.getTotal();
        this.couponCode = order.getCouponCode();
        this.paymentMethod = order.getPaymentMethod();
        this.paymentStatus = order.getPaymentStatus();
        this.razorpayOrderId = order.getRazorpayOrderId();
        this.razorpayPaymentId = order.getRazorpayPaymentId();
        this.idempotencyKey = order.getIdempotencyKey();
        this.estimatedDeliveryMinutes = order.getEstimatedDeliveryMinutes();
        this.scheduledDate = order.getScheduledDate();
        this.timeSlot = order.getTimeSlot();
        this.specialNote = order.getSpecialNote();
        this.createdAt = IstTime.toIso(order.getCreatedAt());
        this.updatedAt = IstTime.toIso(order.getUpdatedAt());
        this.statusTimestamps = order.getStatusTimestamps();
        this.items = order.getItems().stream().map(OrderItemResponse::new).toList();
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getAddressId() { return addressId; }
    public AddressResponse getAddress() { return address; }
    public String getOrderType() { return orderType; }
    public String getStatus() { return status; }
    public double getSubtotal() { return subtotal; }
    public double getDiscount() { return discount; }
    public double getDeliveryFee() { return deliveryFee; }
    public double getTotal() { return total; }
    public String getCouponCode() { return couponCode; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getPaymentStatus() { return paymentStatus; }
    public String getRazorpayOrderId() { return razorpayOrderId; }
    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Integer getEstimatedDeliveryMinutes() { return estimatedDeliveryMinutes; }
    public java.time.LocalDate getScheduledDate() { return scheduledDate; }
    public String getTimeSlot() { return timeSlot; }
    public String getSpecialNote() { return specialNote; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public Map<String, Object> getStatusTimestamps() { return statusTimestamps; }
    public List<OrderItemResponse> getItems() { return items; }
}
