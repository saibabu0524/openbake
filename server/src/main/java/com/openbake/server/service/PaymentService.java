package com.openbake.server.service;

import com.openbake.server.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Mirrors backend/app/services/payment_service.py — PayU hosted-checkout signature/verification. */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final Set<String> BLANK_VALUES = Set.of("no value", "none", "null", "nil");
    private static final List<String> PAYMENT_OPTIONS = List.of("upi_intent", "upi_collect", "cards", "netbanking", "wallets");

    private final AppProperties appProperties;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(12)).build();

    public PaymentService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    private static String normalizedAmount(double amountRupees) {
        return String.format("%.2f", amountRupees);
    }

    private static String normalizePayuField(Object value) {
        String normalized = value == null ? "" : value.toString().trim();
        return BLANK_VALUES.contains(normalized.toLowerCase()) ? "" : normalized;
    }

    private static String sha512(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public Map<String, Object> createPayuPaymentPayload(String orderId, double amountRupees, String customerName,
                                                          String customerEmail, String customerPhone, String platform,
                                                          String callbackBaseUrl) {
        String key = appProperties.getPayu().getMerchantKey();
        String salt = appProperties.getPayu().getMerchantSalt();

        String txnid = "txn_" + orderId.replace("-", "").substring(0, Math.min(20, orderId.replace("-", "").length()))
                + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String amount = normalizedAmount(amountRupees);
        String productinfo = "OpenBake Order " + orderId.substring(Math.max(0, orderId.length() - 8));
        String firstname = (customerName == null || customerName.isBlank() ? "Customer" : customerName).trim();
        firstname = firstname.substring(0, Math.min(60, firstname.length()));
        String email = (customerEmail == null || customerEmail.isBlank() ? "customer@openbake.in" : customerEmail).trim();
        String phone = (customerPhone == null || customerPhone.isBlank() ? "9999999999" : customerPhone).trim();

        String udf1 = orderId;
        String udf2 = platform;
        String udf3 = "";
        String udf4 = "";
        String udf5 = "";

        Map<String, Object> result = new LinkedHashMap<>();

        if (key == null || key.isBlank() || salt == null || salt.isBlank()) {
            log.warn("payu_credentials_missing_dev_mock order_id={}", orderId);
            result.put("provider", "payu");
            result.put("mode", "mock");
            result.put("txnid", "txn_dev_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            result.put("amount", amount);
            result.put("currency", "INR");
            result.put("payment_url", "");
            result.put("method", "POST");
            result.put("fields", Map.of());
            result.put("payment_options", PAYMENT_OPTIONS);
            result.put("upi_intent_supported", true);
            return result;
        }

        String raw = String.join("|", key, txnid, amount, productinfo, firstname, email,
                udf1, udf2, udf3, udf4, udf5) + "||||||" + salt;
        String hash = sha512(raw);

        String callbackBase = callbackBaseUrl.replaceAll("/+$", "");
        String surl = callbackBase + "/api/payments/payu/callback/success";
        String furl = callbackBase + "/api/payments/payu/callback/failure";

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("key", key);
        fields.put("txnid", txnid);
        fields.put("amount", amount);
        fields.put("productinfo", productinfo);
        fields.put("firstname", firstname);
        fields.put("email", email);
        fields.put("phone", phone);
        fields.put("surl", surl);
        fields.put("furl", furl);
        fields.put("hash", hash);
        fields.put("service_provider", "payu_paisa");
        fields.put("udf1", udf1);
        fields.put("udf2", udf2);
        fields.put("udf3", udf3);
        fields.put("udf4", udf4);
        fields.put("udf5", udf5);

        result.put("provider", "payu");
        result.put("mode", "live");
        result.put("txnid", txnid);
        result.put("amount", amount);
        result.put("currency", "INR");
        result.put("payment_url", appProperties.getPayu().getPaymentUrl());
        result.put("method", "POST");
        result.put("fields", fields);
        result.put("payment_options", PAYMENT_OPTIONS);
        result.put("upi_intent_supported", true);
        return result;
    }

    public boolean verifyPayuResponseHash(Map<String, Object> payload) {
        String salt = appProperties.getPayu().getMerchantSalt();
        String key = appProperties.getPayu().getMerchantKey();

        if (salt == null || salt.isBlank() || key == null || key.isBlank()) {
            return true; // Dev-mode compatibility
        }

        String status = normalizePayuField(payload.get("status"));
        String txnid = normalizePayuField(payload.get("txnid"));
        String amount = normalizePayuField(payload.get("amount"));
        String productinfo = normalizePayuField(payload.get("productinfo"));
        String firstname = normalizePayuField(payload.get("firstname"));
        String email = normalizePayuField(payload.get("email"));
        String udf1 = normalizePayuField(payload.get("udf1"));
        String udf2 = normalizePayuField(payload.get("udf2"));
        String udf3 = normalizePayuField(payload.get("udf3"));
        String udf4 = normalizePayuField(payload.get("udf4"));
        String udf5 = normalizePayuField(payload.get("udf5"));
        String receivedHash = normalizePayuField(payload.get("hash")).toLowerCase();
        String additionalCharges = normalizePayuField(
                payload.getOrDefault("additionalCharges", payload.get("additionalcharges")));

        String reverseBase = salt + "|" + status + "||||||" + udf5 + "|" + udf4 + "|" + udf3 + "|" + udf2 + "|" + udf1
                + "|" + email + "|" + firstname + "|" + productinfo + "|" + amount + "|" + txnid + "|" + key;
        if (!additionalCharges.isEmpty()) {
            reverseBase = additionalCharges + "|" + reverseBase;
        }

        String expectedHash = sha512(reverseBase).toLowerCase();
        return expectedHash.equals(receivedHash);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> fetchPayuTransactionStatus(String txnid) {
        String key = appProperties.getPayu().getMerchantKey();
        String salt = appProperties.getPayu().getMerchantSalt();

        Map<String, Object> empty = Map.of("status", "", "mihpayid", "", "raw", Map.of());
        if (key == null || key.isBlank() || salt == null || salt.isBlank() || txnid == null || txnid.isBlank()) {
            return empty;
        }

        String command = "verify_payment";
        String requestHash = sha512(key + "|" + command + "|" + txnid + "|" + salt);

        try {
            Map<String, String> form = new LinkedHashMap<>();
            form.put("key", key);
            form.put("command", command);
            form.put("var1", txnid);
            form.put("hash", requestHash);

            StringBuilder body = new StringBuilder();
            for (var entry : form.entrySet()) {
                if (body.length() > 0) body.append('&');
                body.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                        .append('=')
                        .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(appProperties.getPayu().getStatusApiUrl()))
                    .timeout(Duration.ofSeconds(12))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> responseBody = mapper.readValue(response.body(), Map.class);

            Object detailsObj = responseBody.get("transaction_details");
            Map<String, Object> details = detailsObj instanceof Map ? (Map<String, Object>) detailsObj : Map.of();
            Object txnDetailsObj = details.get(txnid);
            Map<String, Object> txnDetails = txnDetailsObj instanceof Map ? (Map<String, Object>) txnDetailsObj : new HashMap<>();

            String statusValue = normalizePayuField(txnDetails.get("status")).toLowerCase();
            String mihpayid = normalizePayuField(txnDetails.get("mihpayid"));

            return Map.of("status", statusValue, "mihpayid", mihpayid, "raw", txnDetails);
        } catch (Exception e) {
            log.warn("payu_status_fetch_failed txnid={} error={}", txnid, e.getMessage());
            return empty;
        }
    }
}
