package com.openbake.server.dto.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openbake.server.entity.Product;

import java.util.List;

public class WishlistProductInfo {

    private final String id;
    private final String name;
    private final String description;
    private final double price;
    private final List<String> images;
    private final double rating;

    @JsonProperty("is_available")
    private final boolean available;

    @JsonProperty("is_eggless_available")
    private final boolean egglessAvailable;

    @JsonProperty("category_id")
    private final String categoryId;

    private final boolean customizable;

    @JsonProperty("stock_count")
    private final int stockCount;

    private final List<Object> variants = List.of();

    public WishlistProductInfo(Product product) {
        this.id = product.getId();
        this.name = product.getName();
        this.description = product.getDescription();
        this.price = product.getPrice();
        this.images = product.getImages();
        this.rating = product.getRating();
        this.available = product.isAvailable();
        this.egglessAvailable = product.isEgglessAvailable();
        this.categoryId = product.getCategory().getId();
        this.customizable = product.isCustomizable();
        this.stockCount = product.getStockCount();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public List<String> getImages() { return images; }
    public double getRating() { return rating; }
    public boolean isAvailable() { return available; }
    public boolean isEgglessAvailable() { return egglessAvailable; }
    public String getCategoryId() { return categoryId; }
    public boolean isCustomizable() { return customizable; }
    public int getStockCount() { return stockCount; }
    public List<Object> getVariants() { return variants; }
}
