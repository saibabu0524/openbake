package com.openbake.server.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Entity
@Table(name = "order_items")
@Getter
@Setter
public class OrderItem extends BaseEntity {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity = 1;

    @Column(name = "unit_price", nullable = false)
    private double unitPrice;

    @Column(name = "customization", columnDefinition = "LONGTEXT")
    private String customizationJson;

    @Transient
    @SuppressWarnings("unchecked")
    public Map<String, Object> getCustomization() {
        try {
            if (customizationJson == null || customizationJson.isBlank()) {
                return null;
            }
            return MAPPER.readValue(customizationJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    public void setCustomization(Map<String, Object> value) {
        try {
            this.customizationJson = (value == null || value.isEmpty()) ? null : MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            this.customizationJson = null;
        }
    }

    @Transient
    public String getProductName() {
        return product != null ? product.getName() : null;
    }
}
