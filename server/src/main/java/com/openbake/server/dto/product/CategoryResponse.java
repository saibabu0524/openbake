package com.openbake.server.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openbake.server.entity.Category;

public class CategoryResponse {

    private final String id;
    private final String name;

    @JsonProperty("image_url")
    private final String imageUrl;

    @JsonProperty("is_active")
    private final boolean active;

    public CategoryResponse(Category category) {
        this.id = category.getId();
        this.name = category.getName();
        this.imageUrl = category.getImageUrl();
        this.active = category.isActive();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getImageUrl() { return imageUrl; }
    public boolean isActive() { return active; }
}
