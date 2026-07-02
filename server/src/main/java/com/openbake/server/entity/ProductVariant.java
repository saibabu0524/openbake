package com.openbake.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "product_variants")
@Getter
@Setter
public class ProductVariant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** size | flavor */
    @Column(name = "variant_type", nullable = false, length = 50)
    private String variantType;

    /** e.g. 0.5kg, 1kg | chocolate, vanilla */
    @Column(nullable = false, length = 100)
    private String value;

    @Column(name = "extra_price")
    private double extraPrice = 0.0;
}
