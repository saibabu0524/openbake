package com.openbake.server.dto.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openbake.server.entity.User;

public class UserResponse {

    private final String id;
    private final String name;
    private final String email;
    private final String phone;

    @JsonProperty("auth_provider")
    private final String authProvider;

    private final String role;

    @JsonProperty("profile_image_url")
    private final String profileImageUrl;

    public UserResponse(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.authProvider = user.getAuthProvider();
        this.role = user.getRole();
        this.profileImageUrl = user.getProfileImageUrl();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAuthProvider() { return authProvider; }
    public String getRole() { return role; }
    public String getProfileImageUrl() { return profileImageUrl; }
}
