package com.openbake.server.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "products")
@Getter
@Setter
public class Product extends BaseEntity {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private double price;

    /** Stored as a JSON array string, exposed as a List via images()/setImages(). */
    @Column(name = "images", columnDefinition = "LONGTEXT")
    private String imagesJson = "[]";

    @Column(name = "is_available")
    private boolean isAvailable = true;

    @Column(name = "is_eggless_available")
    private boolean isEgglessAvailable = false;

    private boolean customizable = false;

    @Column(name = "stock_count")
    private int stockCount = 0;

    @Column(name = "unlimited_stock")
    private boolean unlimitedStock = false;

    private double rating = 0.0;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WishlistItem> wishlistItems = new ArrayList<>();

    @Transient
    public List<String> getImages() {
        try {
            if (imagesJson == null || imagesJson.isBlank()) {
                return Collections.emptyList();
            }
            return MAPPER.readValue(imagesJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public void setImages(List<String> images) {
        try {
            this.imagesJson = (images == null || images.isEmpty()) ? "[]" : MAPPER.writeValueAsString(images);
        } catch (Exception e) {
            this.imagesJson = "[]";
        }
    }
}
