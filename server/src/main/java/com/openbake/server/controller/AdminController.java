package com.openbake.server.controller;

import com.openbake.server.dto.auth.MessageResponse;
import com.openbake.server.dto.order.*;
import com.openbake.server.dto.product.*;
import com.openbake.server.entity.*;
import com.openbake.server.exception.ApiException;
import com.openbake.server.repository.*;
import com.openbake.server.service.WaitlistService;
import com.openbake.server.util.IstTime;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 1:1 port of backend/app/routers/admin.py. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Map<String, List<String>> VALID_TRANSITIONS = Map.of(
            "placed", List.of("accepted", "cancelled"),
            "accepted", List.of("preparing", "cancelled"),
            "preparing", List.of("dispatched"),
            "dispatched", List.of("delivered"));

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final WaitlistService waitlistService;

    public AdminController(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                            CategoryRepository categoryRepository, ProductRepository productRepository,
                            CouponRepository couponRepository, WaitlistService waitlistService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.couponRepository = couponRepository;
        this.waitlistService = waitlistService;
    }

    // --- Dashboard ---

    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime weekStart = todayStart.minusDays(todayStart.getDayOfWeek().getValue() - 1);
        LocalDateTime monthStart = todayStart.withDayOfMonth(1);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("today_orders", orderRepository.countByCreatedAtGreaterThanEqual(todayStart));
        result.put("today_revenue", orderRepository.sumRevenueSince(todayStart));
        result.put("week_orders", orderRepository.countByCreatedAtGreaterThanEqual(weekStart));
        result.put("week_revenue", orderRepository.sumRevenueSince(weekStart));
        result.put("month_orders", orderRepository.countByCreatedAtGreaterThanEqual(monthStart));
        result.put("month_revenue", orderRepository.sumRevenueSince(monthStart));
        result.put("pending_orders", orderRepository.countByStatus("placed"));
        return result;
    }

    // --- Analytics ---

    @GetMapping("/analytics")
    @Transactional(readOnly = true)
    public Map<String, Object> analytics() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        List<Map<String, Object>> daily = new java.util.ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDateTime dayStart = now.minusDays(i).toLocalDate().atStartOfDay();
            LocalDateTime dayEnd = dayStart.plusDays(1);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("date", dayStart.format(DateTimeFormatter.ofPattern("MMM dd")));
            entry.put("orders", orderRepository.countByCreatedAtBetween(dayStart, dayEnd));
            entry.put("revenue", orderRepository.sumRevenueBetween(dayStart, dayEnd));
            daily.add(entry);
        }

        List<Map<String, Object>> statusBreakdown = orderRepository.countGroupByStatus().stream()
                .map(row -> Map.of("status", (Object) row[0], "count", row[1])).toList();
        List<Map<String, Object>> orderTypeSplit = orderRepository.countGroupByOrderType().stream()
                .map(row -> Map.of("type", (Object) row[0], "count", row[1])).toList();
        List<Map<String, Object>> paymentSplit = orderRepository.countGroupByPaymentMethod().stream()
                .map(row -> Map.of("method", (Object) row[0], "count", row[1])).toList();

        List<Map<String, Object>> topProducts = orderItemRepository.topProductsByUnitsSold(PageRequest.of(0, 5)).stream()
                .map(row -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", row[0]);
                    m.put("units", ((Number) row[1]).intValue());
                    m.put("revenue", ((Number) row[2]).doubleValue());
                    return m;
                }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("daily_trend", daily);
        result.put("status_breakdown", statusBreakdown);
        result.put("order_type_split", orderTypeSplit);
        result.put("top_products", topProducts);
        result.put("payment_split", paymentSplit);
        return result;
    }

    // --- Orders Management ---

    @GetMapping("/orders")
    @Transactional(readOnly = true)
    public List<OrderResponse> listOrders(@RequestParam(name = "status_filter", required = false) String statusFilter) {
        List<Order> orders = statusFilter != null
                ? orderRepository.findByStatusOrderByCreatedAtDesc(statusFilter)
                : orderRepository.findAllByOrderByCreatedAtDesc();
        return orders.stream().map(OrderResponse::new).toList();
    }

    @PatchMapping("/orders/{orderId}")
    @Transactional
    public OrderResponse updateOrderStatus(@PathVariable String orderId, @Valid @RequestBody OrderStatusUpdateRequest data) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));

        List<String> allowed = VALID_TRANSITIONS.getOrDefault(order.getStatus(), List.of());
        if (!allowed.contains(data.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot transition from '" + order.getStatus() + "' to '" + data.getStatus() + "'");
        }

        order.setStatus(data.getStatus());
        Map<String, Object> timestamps = order.getStatusTimestamps() != null
                ? new LinkedHashMap<>(order.getStatusTimestamps()) : new LinkedHashMap<>();
        timestamps.put(data.getStatus(), IstTime.nowIso());
        order.setStatusTimestamps(timestamps);

        return new OrderResponse(orderRepository.save(order));
    }

    @GetMapping("/orders/{orderId}")
    @Transactional(readOnly = true)
    public AdminOrderDetailResponse getOrderDetail(@PathVariable String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));
        return new AdminOrderDetailResponse(order);
    }

    // --- Products Management ---

    @GetMapping("/products")
    @Transactional(readOnly = true)
    public List<ProductResponse> listProducts() {
        return productRepository.findAll().stream().map(ProductResponse::new).toList();
    }

    @PostMapping("/products")
    @Transactional
    public ProductResponse createProduct(@Valid @RequestBody ProductCreateRequest data) {
        Category category = categoryRepository.findById(data.getCategoryId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Category not found"));

        Product product = new Product();
        product.setCategory(category);
        product.setName(data.getName());
        product.setDescription(data.getDescription());
        product.setPrice(data.getPrice());
        product.setAvailable(data.isAvailable());
        product.setEgglessAvailable(data.isEgglessAvailable());
        product.setCustomizable(data.isCustomizable());
        product.setStockCount(data.getStockCount());
        product.setUnlimitedStock(data.isUnlimitedStock());
        product.setImages(data.getImages());
        Product saved = productRepository.save(product);

        for (ProductVariantCreateRequest v : data.getVariants()) {
            ProductVariant variant = new ProductVariant();
            variant.setProduct(saved);
            variant.setVariantType(v.getVariantType());
            variant.setValue(v.getValue());
            variant.setExtraPrice(v.getExtraPrice());
            saved.getVariants().add(variant);
        }

        return new ProductResponse(productRepository.save(saved));
    }

    @PatchMapping("/products/{productId}")
    @Transactional
    public ProductResponse updateProduct(@PathVariable String productId, @RequestBody Map<String, Object> updates) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));

        updates.forEach((field, value) -> {
            switch (field) {
                case "name" -> product.setName((String) value);
                case "description" -> product.setDescription((String) value);
                case "price" -> product.setPrice(((Number) value).doubleValue());
                case "images" -> product.setImages((List<String>) value);
                case "is_available" -> product.setAvailable((Boolean) value);
                case "is_eggless_available" -> product.setEgglessAvailable((Boolean) value);
                case "customizable" -> product.setCustomizable((Boolean) value);
                case "stock_count" -> product.setStockCount(((Number) value).intValue());
                case "unlimited_stock" -> product.setUnlimitedStock((Boolean) value);
                default -> { /* ignore unknown fields */ }
            }
        });

        return new ProductResponse(productRepository.save(product));
    }

    @DeleteMapping("/products/{productId}")
    @Transactional
    public MessageResponse deleteProduct(@PathVariable String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));
        productRepository.delete(product);
        return new MessageResponse("Product deleted");
    }

    // --- Inventory ---

    @GetMapping("/inventory")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> inventory(@RequestParam(required = false, defaultValue = "10") int threshold) {
        return productRepository.findAll().stream()
                .filter(p -> p.getStockCount() < threshold)
                .map(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", p.getId());
                    m.put("name", p.getName());
                    m.put("stock_count", p.getStockCount());
                    m.put("is_available", p.isAvailable());
                    return m;
                }).toList();
    }

    @PatchMapping("/inventory/{productId}")
    @Transactional
    public Map<String, Object> updateStock(@PathVariable String productId, @RequestParam("stock_count") int stockCount) {
        if (stockCount < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "stock_count must be >= 0");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));

        int oldStock = product.getStockCount();
        product.setStockCount(stockCount);
        if (stockCount > 0 && !product.isAvailable()) {
            product.setAvailable(true);
        }
        Product saved = productRepository.save(product);

        int notified = (oldStock == 0 && stockCount > 0) ? waitlistService.notifyWaitlistUsers(productId) : 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", saved.getId());
        result.put("name", saved.getName());
        result.put("stock_count", saved.getStockCount());
        result.put("is_available", saved.isAvailable());
        result.put("waitlist_notified", notified);
        return result;
    }

    // --- Categories ---

    @GetMapping("/categories")
    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories() {
        return categoryRepository.findAll().stream().map(CategoryResponse::new).toList();
    }

    @PostMapping("/categories")
    @Transactional
    public CategoryResponse createCategory(@Valid @RequestBody CategoryCreateRequest data) {
        Category category = new Category();
        category.setName(data.getName());
        category.setImageUrl(data.getImageUrl());
        category.setActive(data.isActive());
        return new CategoryResponse(categoryRepository.save(category));
    }

    @PatchMapping("/categories/{categoryId}")
    @Transactional
    public CategoryResponse updateCategory(@PathVariable String categoryId, @RequestBody Map<String, Object> updates) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found"));

        updates.forEach((field, value) -> {
            switch (field) {
                case "name" -> category.setName((String) value);
                case "image_url" -> category.setImageUrl((String) value);
                case "is_active" -> category.setActive((Boolean) value);
                default -> { }
            }
        });

        return new CategoryResponse(categoryRepository.save(category));
    }

    @DeleteMapping("/categories/{categoryId}")
    @Transactional
    public MessageResponse deleteCategory(@PathVariable String categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found"));
        categoryRepository.delete(category);
        return new MessageResponse("Category deleted");
    }

    // --- Coupons ---

    @GetMapping("/coupons")
    @Transactional(readOnly = true)
    public List<CouponResponse> listCoupons() {
        return couponRepository.findAll().stream().map(CouponResponse::new).toList();
    }

    @PostMapping("/coupons")
    @Transactional
    public CouponResponse createCoupon(@Valid @RequestBody CouponCreateRequest data) {
        Coupon coupon = new Coupon();
        coupon.setCode(data.getCode());
        coupon.setDiscountType(data.getDiscountType());
        coupon.setDiscountValue(data.getDiscountValue());
        coupon.setMinOrderValue(data.getMinOrderValue());
        coupon.setMaxUses(data.getMaxUses());
        coupon.setValidFrom(data.getValidFrom());
        coupon.setValidUntil(data.getValidUntil());
        coupon.setActive(data.isActive());
        return new CouponResponse(couponRepository.save(coupon));
    }

    @PatchMapping("/coupons/{couponId}")
    @Transactional
    public CouponResponse updateCoupon(@PathVariable String couponId, @RequestBody Map<String, Object> updates) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Coupon not found"));

        updates.forEach((field, value) -> {
            switch (field) {
                case "code" -> coupon.setCode((String) value);
                case "discount_type" -> coupon.setDiscountType((String) value);
                case "discount_value" -> coupon.setDiscountValue(((Number) value).doubleValue());
                case "min_order_value" -> coupon.setMinOrderValue(((Number) value).doubleValue());
                case "max_uses" -> coupon.setMaxUses(((Number) value).intValue());
                case "valid_from" -> coupon.setValidFrom(LocalDate.parse((String) value));
                case "valid_until" -> coupon.setValidUntil(LocalDate.parse((String) value));
                case "is_active" -> coupon.setActive((Boolean) value);
                default -> { }
            }
        });

        return new CouponResponse(couponRepository.save(coupon));
    }
}
