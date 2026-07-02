package com.openbake.server.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProductCreateRequest {

    @NotBlank
    @JsonProperty("category_id")
    private String categoryId;

    @NotBlank
    private String name;

    private String description;
    private double price;
    private List<String> images = List.of();

    @JsonProperty("is_available")
    private boolean available = true;

    @JsonProperty("is_eggless_available")
    private boolean egglessAvailable = false;

    private boolean customizable = false;

    @JsonProperty("stock_count")
    private int stockCount = 0;

    @JsonProperty("unlimited_stock")
    private boolean unlimitedStock = false;

    @Valid
    private List<ProductVariantCreateRequest> variants = List.of();
}
