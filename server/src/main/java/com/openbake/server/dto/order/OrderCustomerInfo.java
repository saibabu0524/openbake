package com.openbake.server.dto.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openbake.server.entity.User;

/** Minimal user info exposed to admin for order context. */
public class OrderCustomerInfo {

    private final String id;
    private final String name;
    private final String email;
    private final String phone;

    @JsonProperty("profile_image_url")
    private final String profileImageUrl;

    public OrderCustomerInfo(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.profileImageUrl = user.getProfileImageUrl();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getProfileImageUrl() { return profileImageUrl; }
}
