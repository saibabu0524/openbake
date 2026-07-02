package com.openbake.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * DB-backed configurable settings for the admin dashboard.
 * Default keys: bakery_lat, bakery_lng, free_delivery_radius_km, delivery_fee_default
 */
@Entity
@Table(name = "app_settings")
@Getter
@Setter
public class AppSettings extends BaseEntity {

    @Column(name = "setting_key", nullable = false, unique = true, length = 100)
    private String key;

    @Column(name = "setting_value", nullable = false, length = 500)
    private String value;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
