package com.diplom.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "limitations")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class LimitationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tariff_id", unique = true, nullable = false)
    private TariffEntity tariff;

    @Column(name = "min_track_width_mm", nullable = false, precision = 10, scale = 4)
    private BigDecimal minTrackWidthMm;

    @Column(name = "min_clearance_mm", nullable = false, precision = 10, scale = 4)
    private BigDecimal minClearanceMm;

    @Column(name = "min_hole_diameter_mm", nullable = false, precision = 10, scale = 4)
    private BigDecimal minHoleDiameterMm;

    @Column(name = "max_layers", nullable = false)
    private Integer maxLayers;

    // Для PostgreSQL массива TEXT[] потребуется зависимость hibernate-types
    @Column(name = "supported_materials", columnDefinition = "TEXT[]")
    private String[] supportedMaterials;

    @Column(name = "supported_finishes", columnDefinition = "TEXT[]")
    private String[] supportedFinishes;

    @Column(name = "supports_blind_via")
    @Builder.Default
    private Boolean supportsBlindVia = false;

    @Column(name = "supports_buried_via")
    @Builder.Default
    private Boolean supportsBuriedVia = false;
}