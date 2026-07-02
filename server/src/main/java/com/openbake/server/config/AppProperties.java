package com.openbake.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String env = "development";
    private String secretKey;
    private String algorithm = "HS256";
    private int accessTokenExpireMinutes = 15;
    private int refreshTokenExpireDays = 30;
    private String allowedOrigins = "*";
    private String firebaseCredentialsPath;
    private String webBaseUrl;
    private String androidDeepLinkBase;

    private final Cloudinary cloudinary = new Cloudinary();
    private final PayU payu = new PayU();
    private final Delivery delivery = new Delivery();
    private final Admin admin = new Admin();

    public boolean isProduction() {
        return "production".equalsIgnoreCase(env);
    }

    public String getEnv() { return env; }
    public void setEnv(String env) { this.env = env; }

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }

    public int getAccessTokenExpireMinutes() { return accessTokenExpireMinutes; }
    public void setAccessTokenExpireMinutes(int accessTokenExpireMinutes) { this.accessTokenExpireMinutes = accessTokenExpireMinutes; }

    public int getRefreshTokenExpireDays() { return refreshTokenExpireDays; }
    public void setRefreshTokenExpireDays(int refreshTokenExpireDays) { this.refreshTokenExpireDays = refreshTokenExpireDays; }

    public String getAllowedOrigins() { return allowedOrigins; }
    public void setAllowedOrigins(String allowedOrigins) { this.allowedOrigins = allowedOrigins; }

    public String getFirebaseCredentialsPath() { return firebaseCredentialsPath; }
    public void setFirebaseCredentialsPath(String firebaseCredentialsPath) { this.firebaseCredentialsPath = firebaseCredentialsPath; }

    public String getWebBaseUrl() { return webBaseUrl; }
    public void setWebBaseUrl(String webBaseUrl) { this.webBaseUrl = webBaseUrl; }

    public String getAndroidDeepLinkBase() { return androidDeepLinkBase; }
    public void setAndroidDeepLinkBase(String androidDeepLinkBase) { this.androidDeepLinkBase = androidDeepLinkBase; }

    public Cloudinary getCloudinary() { return cloudinary; }
    public PayU getPayu() { return payu; }
    public Delivery getDelivery() { return delivery; }
    public Admin getAdmin() { return admin; }

    public static class Cloudinary {
        private String cloudName;
        private String apiKey;
        private String apiSecret;

        public String getCloudName() { return cloudName; }
        public void setCloudName(String cloudName) { this.cloudName = cloudName; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getApiSecret() { return apiSecret; }
        public void setApiSecret(String apiSecret) { this.apiSecret = apiSecret; }
    }

    public static class PayU {
        private String merchantKey;
        private String merchantSalt;
        private String clientId;
        private String clientSecret;
        private String paymentUrl;
        private String statusApiUrl;
        private String callbackBaseUrl;

        public String getMerchantKey() { return merchantKey; }
        public void setMerchantKey(String merchantKey) { this.merchantKey = merchantKey; }
        public String getMerchantSalt() { return merchantSalt; }
        public void setMerchantSalt(String merchantSalt) { this.merchantSalt = merchantSalt; }
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
        public String getPaymentUrl() { return paymentUrl; }
        public void setPaymentUrl(String paymentUrl) { this.paymentUrl = paymentUrl; }
        public String getStatusApiUrl() { return statusApiUrl; }
        public void setStatusApiUrl(String statusApiUrl) { this.statusApiUrl = statusApiUrl; }
        public String getCallbackBaseUrl() { return callbackBaseUrl; }
        public void setCallbackBaseUrl(String callbackBaseUrl) { this.callbackBaseUrl = callbackBaseUrl; }
    }

    public static class Delivery {
        private double bakeryLat = 12.9716;
        private double bakeryLng = 77.5946;
        private double freeDeliveryRadiusKm = 5.0;
        private double deliveryFeeDefault = 40.0;
        private double freeDeliveryOrderValue = 500.0;

        public double getBakeryLat() { return bakeryLat; }
        public void setBakeryLat(double bakeryLat) { this.bakeryLat = bakeryLat; }
        public double getBakeryLng() { return bakeryLng; }
        public void setBakeryLng(double bakeryLng) { this.bakeryLng = bakeryLng; }
        public double getFreeDeliveryRadiusKm() { return freeDeliveryRadiusKm; }
        public void setFreeDeliveryRadiusKm(double freeDeliveryRadiusKm) { this.freeDeliveryRadiusKm = freeDeliveryRadiusKm; }
        public double getDeliveryFeeDefault() { return deliveryFeeDefault; }
        public void setDeliveryFeeDefault(double deliveryFeeDefault) { this.deliveryFeeDefault = deliveryFeeDefault; }
        public double getFreeDeliveryOrderValue() { return freeDeliveryOrderValue; }
        public void setFreeDeliveryOrderValue(double freeDeliveryOrderValue) { this.freeDeliveryOrderValue = freeDeliveryOrderValue; }
    }

    public static class Admin {
        private String email = "admin@srivinayakabakery.in";
        private String password = "Admin@1234";

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
