package com.diplom.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "tariffs")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TariffEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producer_id", nullable = false)
    private ProducerEntity producer;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "base_price_per_cm2", nullable = false, precision = 10, scale = 4)
    private BigDecimal basePricePerCm2;

    @Column(name = "extra_layer_price", precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal extraLayerPrice = BigDecimal.ZERO;

    @Column(name = "expedited_fee", precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal expeditedFee = BigDecimal.ZERO;

    @Column(name = "setup_cost", precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal setupCost = BigDecimal.ZERO;

    @Column(name = "min_quantity")
    @Builder.Default
    private Integer minQuantity = 1;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @OneToOne(mappedBy = "tariff", cascade = CascadeType.ALL, orphanRemoval = true)
    private LimitationEntity limitation;
}