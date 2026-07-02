package com.openbake.server.controller;

import com.openbake.server.dto.product.CategoryResponse;
import com.openbake.server.dto.product.PaginatedProductResponse;
import com.openbake.server.dto.product.ProductResponse;
import com.openbake.server.dto.product.ReviewResponse;
import com.openbake.server.entity.Product;
import com.openbake.server.exception.ApiException;
import com.openbake.server.repository.CategoryRepository;
import com.openbake.server.repository.ProductRepository;
import com.openbake.server.repository.ReviewRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/** 1:1 port of backend/app/routers/products.py. */
@RestController
@RequestMapping("/api")
public class ProductController {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;

    public ProductController(CategoryRepository categoryRepository, ProductRepository productRepository,
                              ReviewRepository reviewRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.reviewRepository = reviewRepository;
    }

    @GetMapping("/categories")
    public List<CategoryResponse> listCategories() {
        return categoryRepository.findByIsActiveTrue().stream().map(CategoryResponse::new).toList();
    }

    @GetMapping("/products")
    @Transactional(readOnly = true)
    public PaginatedProductResponse listProducts(
            @RequestParam(name = "category_id", required = false) String categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(name = "min_price", required = false) Double minPrice,
            @RequestParam(name = "max_price", required = false) Double maxPrice,
            @RequestParam(name = "min_rating", required = false) Double minRating,
            @RequestParam(name = "eggless_only", required = false, defaultValue = "false") boolean egglessOnly,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(name = "page_size", required = false, defaultValue = "20") int pageSize
    ) {
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("isAvailable")));
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (search != null && !search.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            if (minRating != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), minRating));
            }
            if (egglessOnly) {
                predicates.add(cb.isTrue(root.get("isEgglessAvailable")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(pageSize, 1), 100));
        var result = productRepository.findAll(spec, pageable);

        List<ProductResponse> items = result.getContent().stream().map(ProductResponse::new).toList();
        return new PaginatedProductResponse(items, result.getTotalElements(), page, pageSize);
    }

    @GetMapping("/products/{productId}")
    @Transactional(readOnly = true)
    public ProductResponse getProduct(@PathVariable String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));
        return new ProductResponse(product);
    }

    @GetMapping("/products/{productId}/reviews")
    @Transactional(readOnly = true)
    public List<ReviewResponse> getProductReviews(
            @PathVariable String productId,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(name = "page_size", required = false, defaultValue = "20") int pageSize
    ) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(pageSize, 1), 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable)
                .stream().map(ReviewResponse::new).toList();
    }
}
