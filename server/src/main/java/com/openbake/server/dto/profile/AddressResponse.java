package com.openbake.server.dto.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openbake.server.entity.Address;

public class AddressResponse {

    private final String id;
    private final String label;

    @JsonProperty("recipient_name")
    private final String recipientName;

    @JsonProperty("recipient_phone")
    private final String recipientPhone;

    @JsonProperty("house_number")
    private final String houseNumber;

    private final String street;
    private final String landmark;

    @JsonProperty("full_address")
    private final String fullAddress;

    private final String city;
    private final String state;
    private final String pincode;
    private final Double lat;
    private final Double lng;

    private final boolean isDefault;

    public AddressResponse(Address address) {
        this.id = address.getId();
        this.label = address.getLabel();
        this.recipientName = address.getRecipientName();
        this.recipientPhone = address.getRecipientPhone();
        this.houseNumber = address.getHouseNumber();
        this.street = address.getStreet();
        this.landmark = address.getLandmark();
        this.fullAddress = address.getFullAddress();
        this.city = address.getCity();
        this.state = address.getState();
        this.pincode = address.getPincode();
        this.lat = address.getLat();
        this.lng = address.getLng();
        this.isDefault = address.isDefault();
    }

    public String getId() { return id; }
    public String getLabel() { return label; }
    public String getRecipientName() { return recipientName; }
    public String getRecipientPhone() { return recipientPhone; }
    public String getHouseNumber() { return houseNumber; }
    public String getStreet() { return street; }
    public String getLandmark() { return landmark; }
    public String getFullAddress() { return fullAddress; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getPincode() { return pincode; }
    public Double getLat() { return lat; }
    public Double getLng() { return lng; }
    @JsonProperty("is_default")
    public boolean isDefault() { return isDefault; }
}
