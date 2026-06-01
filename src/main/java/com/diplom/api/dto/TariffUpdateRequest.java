package com.diplom.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class TariffUpdateRequest {
    private String name;
    private BigDecimal basePricePerCm2;
    private BigDecimal extraLayerPrice;
    private BigDecimal expeditedFee;
    private BigDecimal setupCost;
    private Integer minQuantity;
    private Boolean isActive;

    // Поля ограничений
    private BigDecimal minTrackWidthMm;
    private BigDecimal minClearanceMm;
    private BigDecimal minHoleDiameterMm;
    private Integer maxLayers;
    private String[] supportedMaterials;
    private String[] supportedFinishes;
    private Boolean supportsBlindVia;
    private Boolean supportsBuriedVia;
}