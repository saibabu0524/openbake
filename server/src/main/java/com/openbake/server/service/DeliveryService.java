package com.openbake.server.service;

import com.openbake.server.entity.AppSettings;
import com.openbake.server.repository.AppSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Mirrors backend/app/services/delivery_service.py. */
@Service
public class DeliveryService {

    private static final double DEFAULT_BAKERY_LAT = 12.9716;
    private static final double DEFAULT_BAKERY_LNG = 77.5946;
    private static final double DEFAULT_FREE_DELIVERY_RADIUS_KM = 5.0;
    private static final double DEFAULT_DELIVERY_FEE = 40.0;
    private static final double DEFAULT_SPEED_MIN_PER_KM = 3.0;
    private static final Set<String> ALLOWED_KEYS = Set.of(
            "bakery_lat", "bakery_lng", "free_delivery_radius_km", "delivery_fee_default", "speed_min_per_km", "cod_enabled");

    private final AppSettingsRepository appSettingsRepository;

    public DeliveryService(AppSettingsRepository appSettingsRepository) {
        this.appSettingsRepository = appSettingsRepository;
    }

    public record DeliveryEstimate(double distanceKm, double deliveryFee, Integer estimatedTimeMinutes,
                                    boolean freeDelivery, boolean deliverable) {}

    public record Config(double bakeryLat, double bakeryLng, double freeDeliveryRadiusKm,
                          double deliveryFeeDefault, double speedMinPerKm, boolean codEnabled) {}

    private double getSetting(String key, double fallback) {
        return appSettingsRepository.findByKey(key)
                .map(AppSettings::getValue)
                .map(v -> {
                    try {
                        return Double.parseDouble(v);
                    } catch (NumberFormatException e) {
                        return fallback;
                    }
                })
                .orElse(fallback);
    }

    private boolean getBoolSetting(String key, boolean fallback) {
        return appSettingsRepository.findByKey(key)
                .map(AppSettings::getValue)
                .map(v -> v.equalsIgnoreCase("true") || v.equals("1") || v.equalsIgnoreCase("yes"))
                .orElse(fallback);
    }

    public Config getDeliveryConfig() {
        return new Config(
                getSetting("bakery_lat", DEFAULT_BAKERY_LAT),
                getSetting("bakery_lng", DEFAULT_BAKERY_LNG),
                getSetting("free_delivery_radius_km", DEFAULT_FREE_DELIVERY_RADIUS_KM),
                getSetting("delivery_fee_default", DEFAULT_DELIVERY_FEE),
                getSetting("speed_min_per_km", DEFAULT_SPEED_MIN_PER_KM),
                getBoolSetting("cod_enabled", true));
    }

    @Transactional
    public Config updateDeliveryConfig(Map<String, Object> updates) {
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            if (!ALLOWED_KEYS.contains(entry.getKey()) || entry.getValue() == null) {
                continue;
            }
            String strValue = entry.getValue() instanceof Boolean b ? String.valueOf(b) : String.valueOf(entry.getValue());
            AppSettings row = appSettingsRepository.findByKey(entry.getKey()).orElseGet(() -> {
                AppSettings s = new AppSettings();
                s.setKey(entry.getKey());
                return s;
            });
            row.setValue(strValue);
            appSettingsRepository.save(row);
        }
        return getDeliveryConfig();
    }

    private static double haversine(double lat1, double lng1, double lat2, double lng2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return r * c;
    }

    public DeliveryEstimate calculateDeliveryFee(double customerLat, double customerLng) {
        Config config = getDeliveryConfig();

        double distance = haversine(config.bakeryLat(), config.bakeryLng(), customerLat, customerLng);
        double distanceKm = Math.round(distance * 100.0) / 100.0;

        int prepTime = 15;
        Integer estimatedTime = null;
        if (config.speedMinPerKm() > 0) {
            long travelTime = (long) Math.ceil(distanceKm * config.speedMinPerKm());
            estimatedTime = prepTime + (int) travelTime;
        }

        boolean isFree = distanceKm <= config.freeDeliveryRadiusKm();
        double deliveryFee = isFree ? 0.0 : config.deliveryFeeDefault();
        boolean isDeliverable = distanceKm <= 25.0;

        return new DeliveryEstimate(distanceKm, deliveryFee, estimatedTime, isFree, isDeliverable);
    }

    public Map<String, Object> configAsMap(Config config) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("bakery_lat", config.bakeryLat());
        map.put("bakery_lng", config.bakeryLng());
        map.put("free_delivery_radius_km", config.freeDeliveryRadiusKm());
        map.put("delivery_fee_default", config.deliveryFeeDefault());
        map.put("speed_min_per_km", config.speedMinPerKm());
        map.put("cod_enabled", config.codEnabled());
        return map;
    }
}
