package com.openbake.server.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openbake.server.entity.Product;

import java.util.List;

public class ProductResponse {

    private final String id;

    @JsonProperty("category_id")
    private final String categoryId;

    private final String name;
    private final String description;
    private final double price;
    private final List<String> images;

    @JsonProperty("is_available")
    private final boolean available;

    @JsonProperty("is_eggless_available")
    private final boolean egglessAvailable;

    private final boolean customizable;

    @JsonProperty("stock_count")
    private final int stockCount;

    @JsonProperty("unlimited_stock")
    private final boolean unlimitedStock;

    private final double rating;
    private final List<ProductVariantResponse> variants;

    public ProductResponse(Product product) {
        this.id = product.getId();
        this.categoryId = product.getCategory().getId();
        this.name = product.getName();
        this.description = product.getDescription();
        this.price = product.getPrice();
        this.images = product.getImages();
        this.available = product.isAvailable();
        this.egglessAvailable = product.isEgglessAvailable();
        this.customizable = product.isCustomizable();
        this.stockCount = product.getStockCount();
        this.unlimitedStock = product.isUnlimitedStock();
        this.rating = product.getRating();
        this.variants = product.getVariants().stream().map(ProductVariantResponse::new).toList();
    }

    public String getId() { return id; }
    public String getCategoryId() { return categoryId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public List<String> getImages() { return images; }
    public boolean isAvailable() { return available; }
    public boolean isEgglessAvailable() { return egglessAvailable; }
    public boolean isCustomizable() { return customizable; }
    public int getStockCount() { return stockCount; }
    public boolean isUnlimitedStock() { return unlimitedStock; }
    public double getRating() { return rating; }
    public List<ProductVariantResponse> getVariants() { return variants; }
}
