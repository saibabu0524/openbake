package com.openbake.server.dto.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Mirrors backend/app/schemas/auth.py's AddressCreate, including its field validators/normalizers. */
public class AddressCreateRequest {

    @NotBlank
    private String label = "Home";

    @NotBlank
    @JsonProperty("recipient_name")
    private String recipientName;

    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Recipient phone must be a valid 10-digit Indian mobile number")
    @JsonProperty("recipient_phone")
    private String recipientPhone;

    @NotBlank
    @JsonProperty("house_number")
    private String houseNumber;

    @NotBlank
    private String street;

    private String landmark;

    @NotBlank
    @Size(min = 5, message = "Full address must be at least 5 characters")
    @JsonProperty("full_address")
    private String fullAddress;

    @NotBlank
    @Size(min = 2, message = "City name must be at least 2 characters")
    private String city;

    @NotBlank
    @Size(min = 2, message = "State name must be at least 2 characters")
    private String state;

    @Pattern(regexp = "^\\d{6}$", message = "Pincode must be exactly 6 digits")
    private String pincode;

    private Double lat;
    private Double lng;

    private boolean isDefault = false;

    public String getLabel() { return label == null ? null : label.trim(); }
    public void setLabel(String label) { this.label = label; }

    public String getRecipientName() { return recipientName == null ? null : recipientName.trim(); }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

    public String getRecipientPhone() { return recipientPhone; }
    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone == null ? null : recipientPhone.replaceAll("\\D", "");
    }

    public String getHouseNumber() { return houseNumber == null ? null : houseNumber.trim(); }
    public void setHouseNumber(String houseNumber) { this.houseNumber = houseNumber; }

    public String getStreet() { return street == null ? null : street.trim(); }
    public void setStreet(String street) { this.street = street; }

    public String getLandmark() { return landmark; }
    public void setLandmark(String landmark) { this.landmark = landmark; }

    public String getFullAddress() { return fullAddress == null ? null : fullAddress.trim(); }
    public void setFullAddress(String fullAddress) { this.fullAddress = fullAddress; }

    public String getCity() { return city == null ? null : city.trim(); }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state == null ? null : state.trim(); }
    public void setState(String state) { this.state = state; }

    public String getPincode() { return pincode; }
    public void setPincode(String pincode) {
        this.pincode = pincode == null ? null : pincode.replaceAll("\\D", "");
    }

    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }

    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }

    @JsonProperty("is_default")
    public boolean isDefault() { return isDefault; }

    @JsonProperty("is_default")
    public void setDefault(boolean aDefault) { isDefault = aDefault; }
}
