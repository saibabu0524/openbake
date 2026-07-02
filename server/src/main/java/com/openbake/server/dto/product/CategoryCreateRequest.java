package com.openbake.server.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryCreateRequest {

    @NotBlank
    private String name;

    @JsonProperty("image_url")
    private String imageUrl;

    @JsonProperty("is_active")
    private boolean active = true;
}
