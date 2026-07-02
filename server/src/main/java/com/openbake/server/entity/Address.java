package com.openbake.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "addresses")
@Getter
@Setter
public class Address extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String label = "Home";

    @Column(name = "recipient_name")
    private String recipientName;

    @Column(name = "recipient_phone", length = 20)
    private String recipientPhone;

    @Column(name = "house_number", length = 100)
    private String houseNumber;

    private String street;

    private String landmark;

    @Column(name = "full_address", nullable = false, length = 500)
    private String fullAddress;

    @Column(nullable = false)
    private String city;

    private String state;

    @Column(nullable = false, length = 10)
    private String pincode;

    private Double lat;

    private Double lng;

    @Column(name = "is_default")
    private boolean isDefault = false;
}
