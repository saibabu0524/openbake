package com.openbake.server.dto.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openbake.server.entity.Coupon;

import java.time.LocalDate;

public class CouponResponse {

    private final String id;
    private final String code;

    @JsonProperty("discount_type")
    private final String discountType;

    @JsonProperty("discount_value")
    private final double discountValue;

    @JsonProperty("min_order_value")
    private final double minOrderValue;

    @JsonProperty("max_uses")
    private final int maxUses;

    @JsonProperty("used_count")
    private final int usedCount;

    @JsonProperty("valid_from")
    private final LocalDate validFrom;

    @JsonProperty("valid_until")
    private final LocalDate validUntil;

    @JsonProperty("is_active")
    private final boolean active;

    public CouponResponse(Coupon coupon) {
        this.id = coupon.getId();
        this.code = coupon.getCode();
        this.discountType = coupon.getDiscountType();
        this.discountValue = coupon.getDiscountValue();
        this.minOrderValue = coupon.getMinOrderValue();
        this.maxUses = coupon.getMaxUses();
        this.usedCount = coupon.getUsedCount();
        this.validFrom = coupon.getValidFrom();
        this.validUntil = coupon.getValidUntil();
        this.active = coupon.isActive();
    }

    public String getId() { return id; }
    public String getCode() { return code; }
    public String getDiscountType() { return discountType; }
    public double getDiscountValue() { return discountValue; }
    public double getMinOrderValue() { return minOrderValue; }
    public int getMaxUses() { return maxUses; }
    public int getUsedCount() { return usedCount; }
    public LocalDate getValidFrom() { return validFrom; }
    public LocalDate getValidUntil() { return validUntil; }
    public boolean isActive() { return active; }
}
