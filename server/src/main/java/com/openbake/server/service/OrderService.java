package com.openbake.server.service;

import com.openbake.server.config.AppProperties;
import com.openbake.server.dto.order.CartValidatedItem;
import com.openbake.server.entity.Address;
import com.openbake.server.entity.Coupon;
import com.openbake.server.entity.Order;
import com.openbake.server.entity.OrderItem;
import com.openbake.server.entity.Product;
import com.openbake.server.exception.ApiException;
import com.openbake.server.repository.AddressRepository;
import com.openbake.server.repository.CouponRepository;
import com.openbake.server.repository.OrderRepository;
import com.openbake.server.repository.ProductRepository;
import com.openbake.server.util.IstTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/** Mirrors backend/app/services/order_service.py. */
@Service
public class OrderService {

    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final com.openbake.server.repository.UserRepository userRepository;
    private final DeliveryService deliveryService;
    private final AppProperties appProperties;

    public OrderService(ProductRepository productRepository, CouponRepository couponRepository,
                         AddressRepository addressRepository, OrderRepository orderRepository,
                         com.openbake.server.repository.UserRepository userRepository,
                         DeliveryService deliveryService, AppProperties appProperties) {
        this.productRepository = productRepository;
        this.couponRepository = couponRepository;
        this.addressRepository = addressRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.deliveryService = deliveryService;
        this.appProperties = appProperties;
    }

    public record Item(String productId, int quantity, Map<String, Object> customization) {}

    public record Totals(BigDecimal subtotal, BigDecimal discount, BigDecimal deliveryFee, BigDecimal total,
                          List<CartValidatedItem> validatedItems, Integer estimatedDeliveryMinutes) {}

    @Transactional
    public Totals calculateOrderTotals(List<Item> items, String couponCode, String orderType,
                                        String addressId, String userId) {
        BigDecimal subtotal = BigDecimal.ZERO;
        List<CartValidatedItem> validatedItems = new java.util.ArrayList<>();

        for (Item item : items) {
            Product product = productRepository.findByIdForUpdate(item.productId())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Product " + item.productId() + " not found"));
            if (!product.isAvailable()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Product '" + product.getName() + "' is currently unavailable");
            }
            if (!product.isUnlimitedStock() && product.getStockCount() < item.quantity()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Only " + product.getStockCount() + " units left for '" + product.getName() + "'");
            }

            BigDecimal unitPrice = BigDecimal.valueOf(product.getPrice());
            subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(item.quantity())));

            validatedItems.add(new CartValidatedItem(
                    product.getId(), product.getName(), item.quantity(), unitPrice.doubleValue(),
                    product.getStockCount(), product.isAvailable(), item.customization()));
        }

        BigDecimal discount = BigDecimal.ZERO;
        if (couponCode != null && !couponCode.isBlank()) {
            Coupon coupon = couponRepository.findActiveByCodeForUpdate(couponCode).orElse(null);
            if (coupon != null && subtotal.doubleValue() >= coupon.getMinOrderValue()) {
                if (userId != null && orderRepository.existsByUserIdAndCouponCodeAndStatusNot(userId, couponCode, "cancelled")) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "You have already used this coupon");
                }
                if ("flat".equals(coupon.getDiscountType())) {
                    discount = BigDecimal.valueOf(coupon.getDiscountValue());
                } else if ("percent".equals(coupon.getDiscountType())) {
                    discount = subtotal.multiply(BigDecimal.valueOf(coupon.getDiscountValue()))
                            .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
                }
                coupon.setUsedCount(coupon.getUsedCount() + 1);
            }
        }

        BigDecimal deliveryFee = BigDecimal.ZERO;
        Integer estimatedDeliveryMinutes = null;
        if ("delivery".equals(orderType)) {
            Address address = null;
            if (addressId != null) {
                address = addressRepository.findById(addressId)
                        .filter(a -> userId == null || userId.equals(a.getUser().getId()))
                        .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Address not found or does not belong to you."));
            }

            if (address != null && address.getLat() != null && address.getLng() != null) {
                DeliveryService.DeliveryEstimate estimate = deliveryService.calculateDeliveryFee(address.getLat(), address.getLng());
                deliveryFee = BigDecimal.valueOf(estimate.deliveryFee());
                estimatedDeliveryMinutes = estimate.estimatedTimeMinutes();
                if (!estimate.deliverable()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Delivery address is too far. Maximum delivery radius is 25 km.");
                }
            } else {
                deliveryFee = BigDecimal.valueOf(appProperties.getDelivery().getDeliveryFeeDefault());
            }

            if (subtotal.doubleValue() >= appProperties.getDelivery().getFreeDeliveryOrderValue()) {
                deliveryFee = BigDecimal.ZERO;
            }
        }

        BigDecimal total = subtotal.subtract(discount).add(deliveryFee);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }

        return new Totals(subtotal, discount, deliveryFee, total, validatedItems, estimatedDeliveryMinutes);
    }

    @Transactional
    public Order placeOrder(String userId, String addressId, String orderType, List<Item> items,
                             String couponCode, String paymentMethod, java.time.LocalDate scheduledDate,
                             String timeSlot, String specialNote, String idempotencyKey) {
        Totals totals = calculateOrderTotals(items, couponCode, orderType, addressId, userId);

        Order order = new Order();
        order.setUser(userRepository.getReferenceById(userId));
        if (addressId != null) {
            order.setAddress(addressRepository.getReferenceById(addressId));
        }
        order.setOrderType(orderType);
        order.setStatus("placed");
        order.setSubtotal(totals.subtotal().doubleValue());
        order.setDiscount(totals.discount().doubleValue());
        order.setDeliveryFee(totals.deliveryFee().doubleValue());
        order.setTotal(totals.total().doubleValue());
        order.setCouponCode(couponCode);
        order.setPaymentMethod(paymentMethod);
        order.setPaymentStatus("cod".equals(paymentMethod) ? "paid" : "pending");
        order.setEstimatedDeliveryMinutes(totals.estimatedDeliveryMinutes());
        order.setScheduledDate(scheduledDate);
        order.setTimeSlot(timeSlot);
        order.setSpecialNote(specialNote);
        order.setIdempotencyKey(idempotencyKey);
        order.setStatusTimestamps(Map.of("placed", IstTime.nowIso()));

        Order saved = orderRepository.save(order);

        for (CartValidatedItem item : totals.validatedItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(saved);
            orderItem.setProduct(productRepository.getReferenceById(item.getProductId()));
            orderItem.setQuantity(item.getQuantity());
            orderItem.setUnitPrice(item.getUnitPrice());
            orderItem.setCustomization(item.getCustomization());
            saved.getItems().add(orderItem);

            Product product = productRepository.findById(item.getProductId()).orElse(null);
            if (product != null && !product.isUnlimitedStock()) {
                product.setStockCount(Math.max(0, product.getStockCount() - item.getQuantity()));
            }
        }

        return orderRepository.save(saved);
    }

    @Transactional
    public void restoreStockForOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findByIdForUpdate(item.getProduct().getId()).orElse(null);
            if (product != null) {
                product.setStockCount(product.getStockCount() + item.getQuantity());
            }
        }
    }
}
