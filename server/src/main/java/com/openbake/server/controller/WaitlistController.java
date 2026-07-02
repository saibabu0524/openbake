package com.openbake.server.controller;

import com.openbake.server.dto.auth.MessageResponse;
import com.openbake.server.dto.waitlist.UserWaitlistEntry;
import com.openbake.server.dto.waitlist.WaitlistItemResponse;
import com.openbake.server.entity.Product;
import com.openbake.server.entity.User;
import com.openbake.server.entity.WaitlistItem;
import com.openbake.server.repository.ProductRepository;
import com.openbake.server.service.WaitlistService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 1:1 port of backend/app/routers/waitlist.py. */
@RestController
@RequestMapping("/api")
public class WaitlistController {

    private final WaitlistService waitlistService;
    private final ProductRepository productRepository;

    public WaitlistController(WaitlistService waitlistService, ProductRepository productRepository) {
        this.waitlistService = waitlistService;
        this.productRepository = productRepository;
    }

    public record JoinWaitlistRequest(String variantId) {}

    @PostMapping("/waitlist/{productId}")
    @Transactional
    public WaitlistItemResponse joinWaitlist(@PathVariable String productId,
                                              @RequestBody(required = false) JoinWaitlistRequest data,
                                              @AuthenticationPrincipal User currentUser) {
        String variantId = data != null ? data.variantId() : null;
        WaitlistItem item = waitlistService.joinWaitlist(currentUser.getId(), productId, variantId);
        Product product = productRepository.findById(productId).orElse(null);
        return new WaitlistItemResponse(item, product != null ? product.getName() : null);
    }

    @DeleteMapping("/waitlist/{productId}")
    @Transactional
    public MessageResponse leaveWaitlist(@PathVariable String productId, @AuthenticationPrincipal User currentUser) {
        waitlistService.leaveWaitlist(currentUser.getId(), productId);
        return new MessageResponse("Removed from waitlist");
    }

    @GetMapping("/waitlist")
    @Transactional(readOnly = true)
    public List<UserWaitlistEntry> getMyWaitlist(@AuthenticationPrincipal User currentUser) {
        return waitlistService.getUserWaitlist(currentUser.getId()).stream()
                .map(item -> new UserWaitlistEntry(item, productRepository.findById(item.getProduct().getId()).orElse(null)))
                .toList();
    }

    @GetMapping("/admin/waitlist")
    @Transactional(readOnly = true)
    public List<WaitlistItemResponse> adminGetWaitlist(@RequestParam(name = "product_id", required = false) String productId) {
        return waitlistService.getAdminWaitlist(productId).stream()
                .map(item -> new WaitlistItemResponse(item, productRepository.findById(item.getProduct().getId()).map(Product::getName).orElse(null)))
                .toList();
    }
}
