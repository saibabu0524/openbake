package com.openbake.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openbake.server.dto.order.*;
import com.openbake.server.entity.Address;
import com.openbake.server.entity.Coupon;
import com.openbake.server.entity.Order;
import com.openbake.server.entity.User;
import com.openbake.server.exception.ApiException;
import com.openbake.server.repository.AddressRepository;
import com.openbake.server.repository.CouponRepository;
import com.openbake.server.repository.OrderRepository;
import com.openbake.server.security.JwtService;
import com.openbake.server.service.OrderService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 1:1 port of backend/app/routers/orders.py. */
@RestController
@RequestMapping("/api")
public class OrderController {

    private static final Set<String> TERMINAL_STATES = Set.of("delivered", "cancelled");

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;
    private final CouponRepository couponRepository;
    private final JwtService jwtService;

    public OrderController(OrderService orderService, OrderRepository orderRepository,
                            AddressRepository addressRepository, CouponRepository couponRepository,
                            JwtService jwtService) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.addressRepository = addressRepository;
        this.couponRepository = couponRepository;
        this.jwtService = jwtService;
    }

    @PostMapping("/orders")
    @Transactional
    public OrderResponse createOrder(@Valid @RequestBody OrderCreateRequest data, @AuthenticationPrincipal User currentUser) {
        if (data.getIdempotencyKey() != null && !data.getIdempotencyKey().isBlank()) {
            Order existing = orderRepository.findByIdempotencyKey(data.getIdempotencyKey()).orElse(null);
            if (existing != null) {
                return new OrderResponse(existing);
            }
        }

        if ("delivery".equals(data.getOrderType())) {
            if (data.getAddressId() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Delivery address is required for delivery orders");
            }
            Address address = addressRepository.findById(data.getAddressId())
                    .filter(a -> a.getUser().getId().equals(currentUser.getId()))
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Address not found or does not belong to you"));
            if (address.getFullAddress() == null || address.getFullAddress().isBlank()
                    || address.getCity() == null || address.getCity().isBlank()
                    || address.getPincode() == null || address.getPincode().isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Address is incomplete — full_address, city, and pincode are required");
            }
        }

        List<OrderService.Item> items = data.getItems().stream()
                .map(i -> new OrderService.Item(i.getProductId(), i.getQuantity(), i.getCustomization()))
                .toList();

        try {
            Order order = orderService.placeOrder(currentUser.getId(), data.getAddressId(), data.getOrderType(), items,
                    data.getCouponCode(), data.getPaymentMethod(), data.getScheduledDate(), data.getTimeSlot(),
                    data.getSpecialNote(), data.getIdempotencyKey());
            return new OrderResponse(order);
        } catch (ApiException e) {
            throw e;
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            if (data.getIdempotencyKey() != null && !data.getIdempotencyKey().isBlank()) {
                Order existing = orderRepository.findByIdempotencyKey(data.getIdempotencyKey()).orElse(null);
                if (existing != null) {
                    return new OrderResponse(existing);
                }
            }
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid address or order data. Please check your delivery address.");
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong while placing your order. Please try again.");
        }
    }

    @GetMapping("/orders")
    @Transactional(readOnly = true)
    public List<OrderResponse> listOrders(@AuthenticationPrincipal User currentUser) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId())
                .stream().map(OrderResponse::new).toList();
    }

    @GetMapping("/orders/{orderId}")
    @Transactional(readOnly = true)
    public OrderResponse getOrder(@PathVariable String orderId, @AuthenticationPrincipal User currentUser) {
        Order order = orderRepository.findById(orderId)
                .filter(o -> o.getUser().getId().equals(currentUser.getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));
        return new OrderResponse(order);
    }

    @PatchMapping("/orders/{orderId}/cancel")
    @Transactional
    public OrderResponse cancelOrder(@PathVariable String orderId, @AuthenticationPrincipal User currentUser) {
        Order order = orderRepository.findById(orderId)
                .filter(o -> o.getUser().getId().equals(currentUser.getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!Set.of("placed", "accepted").contains(order.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Order can only be cancelled when status is 'placed' or 'accepted'");
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (order.getCreatedAt() != null && java.time.Duration.between(order.getCreatedAt(), now).toMinutes() > 10) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cancellation window has expired (10 minutes). Please contact support.");
        }

        order.setStatus("cancelled");
        orderService.restoreStockForOrder(order);
        Order saved = orderRepository.save(order);
        return new OrderResponse(saved);
    }

    @PostMapping("/cart/validate")
    public CartValidateResponse validateCart(@Valid @RequestBody CartValidateRequest data) {
        try {
            List<OrderService.Item> items = data.getItems().stream()
                    .map(i -> new OrderService.Item(i.getProductId(), i.getQuantity(), i.getCustomization()))
                    .toList();
            OrderService.Totals totals = orderService.calculateOrderTotals(items, null, "delivery", null, null);
            return new CartValidateResponse(true, totals.subtotal().doubleValue(), totals.validatedItems(), List.of());
        } catch (ApiException e) {
            return new CartValidateResponse(false, 0, List.of(), List.of(e.getMessage()));
        }
    }

    @PostMapping("/coupons/apply")
    @Transactional(readOnly = true)
    public CouponApplyResponse applyCoupon(@Valid @RequestBody CouponApplyRequest data, @AuthenticationPrincipal User currentUser) {
        String code = data.getCode().toUpperCase();
        Coupon coupon = couponRepository.findByCode(code).filter(Coupon::isActive).orElse(null);
        if (coupon == null) {
            return new CouponApplyResponse(false, 0, "Invalid coupon code");
        }

        LocalDate today = LocalDate.now();
        if (today.isBefore(coupon.getValidFrom()) || today.isAfter(coupon.getValidUntil())) {
            return new CouponApplyResponse(false, 0, "Coupon has expired");
        }
        if (coupon.getUsedCount() >= coupon.getMaxUses()) {
            return new CouponApplyResponse(false, 0, "Coupon usage limit reached");
        }
        if (orderRepository.existsByUserIdAndCouponCodeAndStatusNot(currentUser.getId(), code, "cancelled")) {
            return new CouponApplyResponse(false, 0, "You have already used this coupon");
        }
        if (data.getSubtotal() < coupon.getMinOrderValue()) {
            return new CouponApplyResponse(false, 0, "Minimum order value is ₹" + coupon.getMinOrderValue());
        }

        double discount = "flat".equals(coupon.getDiscountType())
                ? coupon.getDiscountValue()
                : Math.round(data.getSubtotal() * coupon.getDiscountValue() / 100.0 * 100.0) / 100.0;

        return new CouponApplyResponse(true, discount, "Coupon applied! You save ₹" + discount);
    }

    // --- SSE Order Tracking ---

    @GetMapping(value = "/orders/{orderId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamOrderStatus(@PathVariable String orderId,
                                         @RequestParam(required = false) String token,
                                         @RequestHeader(name = "Authorization", required = false) String authorization) {
        String accessToken = token;
        if ((accessToken == null || accessToken.isBlank()) && authorization != null) {
            accessToken = authorization.toLowerCase().startsWith("bearer ") ? authorization.substring(7).trim() : authorization.trim();
        }
        if (accessToken == null || accessToken.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Access token is required (query `token` or Authorization Bearer header)");
        }

        Claims claims;
        try {
            claims = jwtService.verify(accessToken, "access");
        } catch (JwtException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Could not validate credentials");
        }
        String userId = claims.getSubject();
        if (userId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid token payload");
        }

        Order order = orderRepository.findById(orderId)
                .filter(o -> o.getUser().getId().equals(userId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));

        SseEmitter emitter = new SseEmitter(0L);
        ObjectMapper mapper = new ObjectMapper();

        Thread worker = new Thread(() -> {
            try {
                String lastStatus = order.getStatus();
                emitter.send(SseEmitter.event().data(
                        mapper.writeValueAsString(Map.of("status", lastStatus, "order_id", orderId)), MediaType.APPLICATION_JSON));

                if (TERMINAL_STATES.contains(lastStatus)) {
                    emitter.complete();
                    return;
                }

                while (true) {
                    Thread.sleep(3000);
                    Order polled = orderRepository.findById(orderId).orElse(null);
                    if (polled == null) {
                        break;
                    }
                    String currentStatus = polled.getStatus();
                    if (!currentStatus.equals(lastStatus)) {
                        lastStatus = currentStatus;
                        emitter.send(SseEmitter.event().data(mapper.writeValueAsString(Map.of(
                                "status", currentStatus,
                                "order_id", orderId,
                                "estimated_delivery_minutes", polled.getEstimatedDeliveryMinutes(),
                                "payment_status", polled.getPaymentStatus()
                        )), MediaType.APPLICATION_JSON));
                    }
                    if (TERMINAL_STATES.contains(currentStatus)) {
                        break;
                    }
                }
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        worker.setDaemon(true);
        worker.start();

        return emitter;
    }
}
