package com.openbake.server.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order extends BaseEntity {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    private Address address;

    /** delivery | pickup */
    @Column(name = "order_type", nullable = false, length = 20)
    private String orderType = "delivery";

    /** placed | accepted | preparing | dispatched | delivered | cancelled */
    @Column(nullable = false, length = 20)
    private String status = "placed";

    @Column(nullable = false)
    private double subtotal;

    private double discount = 0.0;

    @Column(name = "delivery_fee")
    private double deliveryFee = 0.0;

    @Column(nullable = false)
    private double total;

    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    /** pending | paid | failed */
    @Column(name = "payment_status", nullable = false, length = 20)
    private String paymentStatus = "pending";

    @Column(name = "razorpay_order_id", length = 100)
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id", length = 100)
    private String razorpayPaymentId;

    @Column(name = "idempotency_key", length = 64, unique = true)
    private String idempotencyKey;

    @Column(name = "estimated_delivery_minutes")
    private Integer estimatedDeliveryMinutes;

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @Column(name = "time_slot", length = 50)
    private String timeSlot;

    @Column(name = "special_note", columnDefinition = "TEXT")
    private String specialNote;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "status_timestamps", columnDefinition = "LONGTEXT")
    private String statusTimestampsJson;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Transient
    @SuppressWarnings("unchecked")
    public Map<String, Object> getStatusTimestamps() {
        try {
            if (statusTimestampsJson == null || statusTimestampsJson.isBlank()) {
                return null;
            }
            return MAPPER.readValue(statusTimestampsJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    public void setStatusTimestamps(Map<String, Object> value) {
        try {
            this.statusTimestampsJson = (value == null || value.isEmpty()) ? null : MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            this.statusTimestampsJson = null;
        }
    }
}
