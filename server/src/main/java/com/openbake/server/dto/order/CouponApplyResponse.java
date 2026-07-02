package com.openbake.server.dto.order;

public class CouponApplyResponse {

    private final boolean valid;
    private final double discount;
    private final String message;

    public CouponApplyResponse(boolean valid, double discount, String message) {
        this.valid = valid;
        this.discount = discount;
        this.message = message;
    }

    public boolean isValid() { return valid; }
    public double getDiscount() { return discount; }
    public String getMessage() { return message; }
}
