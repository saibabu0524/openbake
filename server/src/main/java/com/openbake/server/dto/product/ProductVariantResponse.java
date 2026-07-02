package com.openbake.server.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openbake.server.entity.ProductVariant;

public class ProductVariantResponse {

    private final String id;

    @JsonProperty("variant_type")
    private final String variantType;

    private final String value;

    @JsonProperty("extra_price")
    private final double extraPrice;

    public ProductVariantResponse(ProductVariant variant) {
        this.id = variant.getId();
        this.variantType = variant.getVariantType();
        this.value = variant.getValue();
        this.extraPrice = variant.getExtraPrice();
    }

    public String getId() { return id; }
    public String getVariantType() { return variantType; }
    public String getValue() { return value; }
    public double getExtraPrice() { return extraPrice; }
}
