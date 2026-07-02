package com.openbake.server.controller;

import com.openbake.server.dto.auth.MessageResponse;
import com.openbake.server.dto.profile.*;
import com.openbake.server.entity.*;
import com.openbake.server.exception.ApiException;
import com.openbake.server.repository.*;
import com.openbake.server.service.AvatarStorageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** 1:1 port of backend/app/routers/profile.py. */
@RestController
@RequestMapping("/api")
public class ProfileController {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final WishlistRepository wishlistRepository;
    private final AvatarStorageService avatarStorageService;

    public ProfileController(UserRepository userRepository, AddressRepository addressRepository,
                              ReviewRepository reviewRepository, ProductRepository productRepository,
                              OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                              WishlistRepository wishlistRepository, AvatarStorageService avatarStorageService) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.wishlistRepository = wishlistRepository;
        this.avatarStorageService = avatarStorageService;
    }

    // --- Profile ---

    @GetMapping("/profile")
    public UserResponse getProfile(@AuthenticationPrincipal User currentUser) {
        return new UserResponse(currentUser);
    }

    @PatchMapping("/profile")
    @Transactional
    public UserResponse updateProfile(@RequestBody ProfileUpdateRequest data, @AuthenticationPrincipal User currentUser) {
        if (data.getName() != null) currentUser.setName(data.getName());
        if (data.getPhone() != null) currentUser.setPhone(data.getPhone());
        if (data.getProfileImageUrl() != null) currentUser.setProfileImageUrl(data.getProfileImageUrl());
        return new UserResponse(userRepository.save(currentUser));
    }

    @PostMapping("/profile/avatar")
    @Transactional
    public UserResponse uploadAvatar(@RequestParam("file") MultipartFile file, @AuthenticationPrincipal User currentUser) {
        String url = avatarStorageService.store(currentUser.getId(), file, currentUser.getProfileImageUrl());
        currentUser.setProfileImageUrl(url);
        return new UserResponse(userRepository.save(currentUser));
    }

    // --- Addresses ---

    @GetMapping("/addresses")
    public List<AddressResponse> listAddresses(@AuthenticationPrincipal User currentUser) {
        return addressRepository.findByUserId(currentUser.getId()).stream().map(AddressResponse::new).toList();
    }

    @PostMapping("/addresses")
    @Transactional
    public AddressResponse addAddress(@Valid @RequestBody AddressCreateRequest data, @AuthenticationPrincipal User currentUser) {
        Address address = new Address();
        address.setUser(currentUser);
        applyAddressFields(address, data);
        return new AddressResponse(addressRepository.save(address));
    }

    @PatchMapping("/addresses/{addressId}")
    @Transactional
    public AddressResponse updateAddress(@PathVariable String addressId, @Valid @RequestBody AddressCreateRequest data,
                                          @AuthenticationPrincipal User currentUser) {
        Address address = addressRepository.findById(addressId)
                .filter(a -> a.getUser().getId().equals(currentUser.getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Address not found"));
        applyAddressFields(address, data);
        return new AddressResponse(addressRepository.save(address));
    }

    @DeleteMapping("/addresses/{addressId}")
    @Transactional
    public MessageResponse deleteAddress(@PathVariable String addressId, @AuthenticationPrincipal User currentUser) {
        Address address = addressRepository.findById(addressId)
                .filter(a -> a.getUser().getId().equals(currentUser.getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Address not found"));
        addressRepository.delete(address);
        return new MessageResponse("Address deleted");
    }

    private void applyAddressFields(Address address, AddressCreateRequest data) {
        address.setLabel(data.getLabel());
        address.setRecipientName(data.getRecipientName());
        address.setRecipientPhone(data.getRecipientPhone());
        address.setHouseNumber(data.getHouseNumber());
        address.setStreet(data.getStreet());
        address.setLandmark(data.getLandmark());
        address.setFullAddress(data.getFullAddress());
        address.setCity(data.getCity());
        address.setState(data.getState());
        address.setPincode(data.getPincode());
        address.setLat(data.getLat());
        address.setLng(data.getLng());
        address.setDefault(data.isDefault());
    }

    // --- Reviews ---

    @PostMapping("/reviews")
    @Transactional
    public com.openbake.server.dto.product.ReviewResponse submitReview(@Valid @RequestBody ReviewCreateRequest data,
                                                                        @AuthenticationPrincipal User currentUser) {
        Product product = productRepository.findById(data.getProductId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));

        Order order = orderRepository.findById(data.getOrderId())
                .filter(o -> o.getUser().getId().equals(currentUser.getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Order not found or does not belong to you"));

        if (!"delivered".equals(order.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Reviews can only be submitted for delivered orders");
        }

        boolean productInOrder = order.getItems().stream().anyMatch(i -> i.getProduct().getId().equals(data.getProductId()));
        if (!productInOrder) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This product was not part of the specified order");
        }

        if (reviewRepository.existsByUserIdAndProductIdAndOrderId(currentUser.getId(), data.getProductId(), data.getOrderId())) {
            throw new ApiException(HttpStatus.CONFLICT, "You have already reviewed this product for this order");
        }

        Review review = new Review();
        review.setUser(currentUser);
        review.setProduct(product);
        review.setOrderId(data.getOrderId());
        review.setRating(data.getRating());
        review.setComment(data.getComment());
        Review saved = reviewRepository.save(review);

        List<Review> allReviews = reviewRepository.findByProductId(data.getProductId());
        double avgRating = allReviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        product.setRating(Math.round(avgRating * 10.0) / 10.0);
        productRepository.save(product);

        return new com.openbake.server.dto.product.ReviewResponse(saved);
    }

    // --- Wishlist ---

    @GetMapping("/wishlist")
    @Transactional(readOnly = true)
    public List<WishlistItemResponse> getWishlist(@AuthenticationPrincipal User currentUser) {
        return wishlistRepository.findByUserId(currentUser.getId()).stream().map(WishlistItemResponse::new).toList();
    }

    @PostMapping("/wishlist/{productId}")
    @Transactional
    public MessageResponse addToWishlist(@PathVariable String productId, @AuthenticationPrincipal User currentUser) {
        if (wishlistRepository.findByUserIdAndProductId(currentUser.getId(), productId).isPresent()) {
            return new MessageResponse("Already in wishlist");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));
        WishlistItem item = new WishlistItem();
        item.setUser(currentUser);
        item.setProduct(product);
        wishlistRepository.save(item);
        return new MessageResponse("Added to wishlist");
    }

    @DeleteMapping("/wishlist/{productId}")
    @Transactional
    public MessageResponse removeFromWishlist(@PathVariable String productId, @AuthenticationPrincipal User currentUser) {
        WishlistItem item = wishlistRepository.findByUserIdAndProductId(currentUser.getId(), productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Item not in wishlist"));
        wishlistRepository.delete(item);
        return new MessageResponse("Removed from wishlist");
    }
}
