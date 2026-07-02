package com.openbake.server.controller;

import com.openbake.server.config.AppProperties;
import com.openbake.server.dto.payment.CreatePaymentOrderRequest;
import com.openbake.server.dto.payment.CreatePaymentOrderResponse;
import com.openbake.server.dto.payment.PaymentStatusResponse;
import com.openbake.server.dto.payment.VerifyPaymentRequest;
import com.openbake.server.entity.Order;
import com.openbake.server.entity.User;
import com.openbake.server.exception.ApiException;
import com.openbake.server.repository.OrderRepository;
import com.openbake.server.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** 1:1 port of backend/app/routers/payments.py. */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    private static final Set<String> HOSTED_ALLOWED_KEYS = Set.of(
            "key", "txnid", "amount", "productinfo", "firstname", "email", "phone",
            "surl", "furl", "hash", "service_provider", "udf1", "udf2", "udf3", "udf4", "udf5");

    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final AppProperties appProperties;

    public PaymentController(OrderRepository orderRepository, PaymentService paymentService, AppProperties appProperties) {
        this.orderRepository = orderRepository;
        this.paymentService = paymentService;
        this.appProperties = appProperties;
    }

    @PostMapping("/create-order")
    @Transactional
    public CreatePaymentOrderResponse createPaymentOrder(@Valid @RequestBody CreatePaymentOrderRequest data,
                                                           HttpServletRequest request,
                                                           @AuthenticationPrincipal User currentUser) {
        Order order = orderRepository.findById(data.getOrderId())
                .filter(o -> o.getUser().getId().equals(currentUser.getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));

        if ("paid".equals(order.getPaymentStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Order is already paid");
        }
        if ("cod".equals(order.getPaymentMethod())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "COD orders do not require online payment");
        }

        String callbackBaseUrl = appProperties.getPayu().getCallbackBaseUrl();
        String backendBaseUrl = (callbackBaseUrl != null && !callbackBaseUrl.isBlank())
                ? callbackBaseUrl.trim()
                : baseUrlFromRequest(request);

        String platform = (data.getPlatform() == null || data.getPlatform().isBlank() ? "web" : data.getPlatform()).toLowerCase();

        Map<String, Object> payload = paymentService.createPayuPaymentPayload(
                order.getId(), order.getTotal(), currentUser.getName(), currentUser.getEmail(),
                currentUser.getPhone(), platform, backendBaseUrl);

        order.setRazorpayOrderId((String) payload.get("txnid"));
        orderRepository.save(order);

        @SuppressWarnings("unchecked")
        Map<String, String> fields = (Map<String, String>) payload.get("fields");
        String checkoutUrl = backendBaseUrl.replaceAll("/+$", "") + "/api/payments/payu/hosted?" + urlEncodeForm(fields);

        return new CreatePaymentOrderResponse(payload, order.getId(), checkoutUrl);
    }

    @GetMapping(value = "/payu/hosted", produces = MediaType.TEXT_HTML_VALUE)
    public String payuHostedPage(@RequestParam Map<String, String> queryParams) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (var entry : queryParams.entrySet()) {
            if (HOSTED_ALLOWED_KEYS.contains(entry.getKey())) {
                fields.put(entry.getKey(), entry.getValue());
            }
        }
        StringBuilder inputs = new StringBuilder();
        for (var entry : fields.entrySet()) {
            inputs.append("<input type=\"hidden\" name=\"").append(escapeHtml(entry.getKey()))
                    .append("\" value=\"").append(escapeHtml(entry.getValue())).append("\" />\n");
        }
        return "<!doctype html><html><head><meta charset='utf-8'/>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1'/>"
                + "<title>Redirecting to PayU...</title></head><body>"
                + "<p style='font-family:sans-serif;padding:16px'>Redirecting to secure payment...</p>"
                + "<form id='payuForm' method='post' action='" + escapeHtml(appProperties.getPayu().getPaymentUrl()) + "'>" + inputs + "</form>"
                + "<script>document.getElementById('payuForm').submit();</script>"
                + "</body></html>";
    }

    @PostMapping("/verify")
    @Transactional
    public PaymentStatusResponse verifyPayment(@Valid @RequestBody VerifyPaymentRequest data, @AuthenticationPrincipal User currentUser) {
        Order order = orderRepository.findById(data.getOrderId())
                .filter(o -> o.getUser().getId().equals(currentUser.getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));

        if ("paid".equals(order.getPaymentStatus())) {
            return new PaymentStatusResponse(order.getId(), "paid", "Payment already verified");
        }

        Map<String, Object> payload = new HashMap<>(data.getRawPayload() != null ? data.getRawPayload() : Map.of());
        payload.put("txnid", firstNonBlank(data.getTxnid(), (String) payload.get("txnid"), order.getRazorpayOrderId()));
        payload.put("status", firstNonBlank(data.getStatus(), (String) payload.get("status"), order.getPaymentStatus()));
        payload.put("mihpayid", firstNonBlank(data.getMihpayid(), (String) payload.get("mihpayid"), ""));
        payload.put("hash", firstNonBlank(data.getHash(), (String) payload.get("hash"), ""));
        payload.put("udf1", firstNonBlank(null, (String) payload.get("udf1"), order.getId()));

        boolean isValid = paymentService.verifyPayuResponseHash(payload);
        boolean isSuccess = "success".equalsIgnoreCase(String.valueOf(payload.getOrDefault("status", "")));

        if (isValid && isSuccess) {
            order.setPaymentStatus("paid");
            Object mihpayid = payload.get("mihpayid");
            if (mihpayid != null && !mihpayid.toString().isBlank()) {
                order.setRazorpayPaymentId(mihpayid.toString());
            }
            orderRepository.save(order);
            log.info("payment_verified order_id={} txnid={}", order.getId(), payload.get("txnid"));
            return new PaymentStatusResponse(order.getId(), "paid", "Payment verified successfully");
        }

        order.setPaymentStatus("failed");
        orderRepository.save(order);
        log.warn("payment_verification_failed order_id={}", order.getId());
        throw new ApiException(HttpStatus.BAD_REQUEST, "Payment verification failed");
    }

    @RequestMapping(value = "/payu/callback/success", method = {RequestMethod.GET, RequestMethod.POST})
    @Transactional
    public ResponseEntity<Void> payuSuccessCallback(@RequestParam Map<String, String> params) {
        String orderId = params.getOrDefault("udf1", "");
        String platform = params.getOrDefault("udf2", "web").toLowerCase();
        String txnid = params.getOrDefault("txnid", "");
        String statusValue = params.getOrDefault("status", "").toLowerCase();

        if (orderId.isBlank()) {
            return redirectTo(buildResultRedirect("unknown", "failed", platform));
        }

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null) {
            Map<String, Object> payload = new HashMap<>(params);
            boolean hashValid = paymentService.verifyPayuResponseHash(payload);
            if (hashValid && "success".equals(statusValue)) {
                order.setPaymentStatus("paid");
                order.setRazorpayOrderId(!txnid.isBlank() ? txnid : order.getRazorpayOrderId());
                order.setRazorpayPaymentId(params.getOrDefault("mihpayid", order.getRazorpayPaymentId()));
                orderRepository.save(order);
            } else if ("success".equals(statusValue)) {
                log.warn("payu_success_hash_mismatch order_id={} txnid={}", orderId, txnid);
                order.setPaymentStatus("pending");
                order.setRazorpayOrderId(!txnid.isBlank() ? txnid : order.getRazorpayOrderId());
                orderRepository.save(order);
            } else {
                order.setPaymentStatus("failed");
                orderRepository.save(order);
            }
        }

        return redirectTo(buildResultRedirect(orderId, statusValue.isBlank() ? "failed" : statusValue, platform));
    }

    @RequestMapping(value = "/payu/callback/failure", method = {RequestMethod.GET, RequestMethod.POST})
    @Transactional
    public ResponseEntity<Void> payuFailureCallback(@RequestParam Map<String, String> params) {
        String orderId = params.getOrDefault("udf1", "");
        String platform = params.getOrDefault("udf2", "web").toLowerCase();
        String txnid = params.getOrDefault("txnid", "");

        if (!orderId.isBlank()) {
            orderRepository.findById(orderId).ifPresent(order -> {
                order.setPaymentStatus("failed");
                order.setRazorpayOrderId(!txnid.isBlank() ? txnid : order.getRazorpayOrderId());
                orderRepository.save(order);
            });
        }

        return redirectTo(buildResultRedirect(orderId.isBlank() ? "unknown" : orderId, "failed", platform));
    }

    @GetMapping("/status/{orderId}")
    @Transactional
    public PaymentStatusResponse paymentStatus(@PathVariable String orderId, @AuthenticationPrincipal User currentUser) {
        Order order = orderRepository.findById(orderId)
                .filter(o -> o.getUser().getId().equals(currentUser.getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));

        if ("pending".equals(order.getPaymentStatus())
                && !"cod".equalsIgnoreCase(String.valueOf(order.getPaymentMethod()))
                && order.getRazorpayOrderId() != null) {
            Map<String, Object> remote = paymentService.fetchPayuTransactionStatus(order.getRazorpayOrderId());
            String remoteStatus = String.valueOf(remote.getOrDefault("status", "")).toLowerCase();

            if ("success".equals(remoteStatus)) {
                order.setPaymentStatus("paid");
                Object mihpayid = remote.get("mihpayid");
                if (mihpayid != null && !mihpayid.toString().isBlank()) {
                    order.setRazorpayPaymentId(mihpayid.toString());
                }
                orderRepository.save(order);
                log.info("payment_status_reconciled_paid order_id={} txnid={}", order.getId(), order.getRazorpayOrderId());
            } else if (Set.of("failure", "failed", "cancelled", "cancel", "dropped", "bounced").contains(remoteStatus)) {
                order.setPaymentStatus("failed");
                orderRepository.save(order);
                log.info("payment_status_reconciled_failed order_id={} txnid={}", order.getId(), order.getRazorpayOrderId());
            }
        }

        return new PaymentStatusResponse(order.getId(), order.getPaymentStatus(), "Payment status fetched");
    }

    private ResponseEntity<Void> redirectTo(String url) {
        return ResponseEntity.status(302).location(URI.create(url)).build();
    }

    private String buildResultRedirect(String orderId, String statusValue, String platform) {
        boolean isSuccess = "success".equalsIgnoreCase(statusValue);
        if ("android".equals(platform)) {
            String base = appProperties.getAndroidDeepLinkBase().replaceAll("/+$", "");
            return base + "?order_id=" + urlEncode(orderId) + "&payment_status=" + (isSuccess ? "paid" : "failed");
        }
        String base = appProperties.getWebBaseUrl().replaceAll("/+$", "");
        return base + "/orders?order_id=" + urlEncode(orderId) + "&payment_status=" + (isSuccess ? "paid" : "failed") + "&source=payu";
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return "";
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String urlEncodeForm(Map<String, String> fields) {
        if (fields == null) return "";
        StringBuilder sb = new StringBuilder();
        for (var entry : fields.entrySet()) {
            if (sb.length() > 0) sb.append('&');
            sb.append(urlEncode(entry.getKey())).append('=').append(urlEncode(entry.getValue()));
        }
        return sb.toString();
    }

    private static String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String baseUrlFromRequest(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append(request.getScheme()).append("://").append(request.getServerName());
        int port = request.getServerPort();
        boolean isDefaultPort = ("http".equals(request.getScheme()) && port == 80)
                || ("https".equals(request.getScheme()) && port == 443);
        if (!isDefaultPort) {
            sb.append(':').append(port);
        }
        return sb.toString();
    }
}
