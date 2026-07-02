package com.openbake.server.dto.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileUpdateRequest {
    private String name;
    private String phone;

    @JsonProperty("profile_image_url")
    private String profileImageUrl;
}
