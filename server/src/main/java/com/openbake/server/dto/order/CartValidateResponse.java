package com.openbake.server.dto.order;

import java.util.List;

public class CartValidateResponse {

    private final boolean valid;
    private final double subtotal;
    private final List<CartValidatedItem> items;
    private final List<String> errors;

    public CartValidateResponse(boolean valid, double subtotal, List<CartValidatedItem> items, List<String> errors) {
        this.valid = valid;
        this.subtotal = subtotal;
        this.items = items;
        this.errors = errors;
    }

    public boolean isValid() { return valid; }
    public double getSubtotal() { return subtotal; }
    public List<CartValidatedItem> getItems() { return items; }
    public List<String> getErrors() { return errors; }
}
