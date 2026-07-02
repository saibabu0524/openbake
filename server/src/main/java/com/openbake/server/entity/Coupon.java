package com.openbake.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "coupons")
@Getter
@Setter
public class Coupon extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    /** flat | percent */
    @Column(name = "discount_type", nullable = false, length = 20)
    private String discountType;

    @Column(name = "discount_value", nullable = false)
    private double discountValue;

    @Column(name = "min_order_value")
    private double minOrderValue = 0.0;

    @Column(name = "max_uses")
    private int maxUses = 100;

    @Column(name = "used_count")
    private int usedCount = 0;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_until", nullable = false)
    private LocalDate validUntil;

    @Column(name = "is_active")
    private boolean isActive = true;
}
