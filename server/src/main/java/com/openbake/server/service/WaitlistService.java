package com.openbake.server.service;

import com.openbake.server.entity.Product;
import com.openbake.server.entity.ProductVariant;
import com.openbake.server.entity.User;
import com.openbake.server.entity.WaitlistItem;
import com.openbake.server.exception.ApiException;
import com.openbake.server.repository.ProductRepository;
import com.openbake.server.repository.ProductVariantRepository;
import com.openbake.server.repository.UserRepository;
import com.openbake.server.repository.WaitlistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/** Mirrors backend/app/services/waitlist_service.py. */
@Service
public class WaitlistService {

    private static final Logger log = LoggerFactory.getLogger(WaitlistService.class);

    private final WaitlistRepository waitlistRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;

    public WaitlistService(WaitlistRepository waitlistRepository, ProductRepository productRepository,
                            ProductVariantRepository productVariantRepository, UserRepository userRepository) {
        this.waitlistRepository = waitlistRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public WaitlistItem joinWaitlist(String userId, String productId, String variantId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Product not found"));

        if (product.getStockCount() > 0 && product.isAvailable()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Product is currently in stock — add it to cart instead");
        }

        boolean alreadyWaiting = waitlistRepository.findByUserId(userId).stream()
                .anyMatch(w -> w.getProduct().getId().equals(productId) && "waiting".equals(w.getStatus()));
        if (alreadyWaiting) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Already on the waitlist for this product");
        }

        WaitlistItem item = new WaitlistItem();
        item.setUser(userRepository.getReferenceById(userId));
        item.setProduct(product);
        if (variantId != null) {
            ProductVariant variant = productVariantRepository.findById(variantId).orElse(null);
            item.setVariant(variant);
        }
        item.setStatus("waiting");

        WaitlistItem saved = waitlistRepository.save(item);
        log.info("waitlist_joined user_id={} product_id={}", userId, productId);
        return saved;
    }

    @Transactional
    public void leaveWaitlist(String userId, String productId) {
        WaitlistItem item = waitlistRepository.findByUserId(userId).stream()
                .filter(w -> w.getProduct().getId().equals(productId) && "waiting".equals(w.getStatus()))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Not on the waitlist for this product"));
        waitlistRepository.delete(item);
        log.info("waitlist_left user_id={} product_id={}", userId, productId);
    }

    public List<WaitlistItem> getUserWaitlist(String userId) {
        return waitlistRepository.findByUserId(userId).stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }

    public List<WaitlistItem> getAdminWaitlist(String productId) {
        List<WaitlistItem> items = productId != null
                ? waitlistRepository.findByProductId(productId)
                : waitlistRepository.findAll();
        return items.stream().sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())).toList();
    }

    @Transactional
    public int notifyWaitlistUsers(String productId) {
        List<WaitlistItem> waiting = waitlistRepository.findByProductIdAndStatus(productId, "waiting").stream()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .toList();

        int notified = 0;
        for (WaitlistItem item : waiting) {
            item.setStatus("notified");
            item.setNotifiedAt(LocalDateTime.now(ZoneOffset.UTC));
            waitlistRepository.save(item);
            notified++;
            // TODO: Send push notification via FCM when device tokens are available.
            log.info("waitlist_user_notified user_id={} product_id={}", item.getUser().getId(), productId);
        }
        if (notified > 0) {
            log.info("waitlist_notifications_sent product_id={} count={}", productId, notified);
        }
        return notified;
    }
}
