package com.diplom.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class TariffCreateRequest {
    private String name;
    private BigDecimal basePricePerCm2;
    private BigDecimal extraLayerPrice = BigDecimal.ZERO;
    private BigDecimal expeditedFee = BigDecimal.ZERO;
    private BigDecimal setupCost = BigDecimal.ZERO;
    private Integer minQuantity = 1;
    private Boolean isActive;

    // Поля ограничений
    private BigDecimal minTrackWidthMm;
    private BigDecimal minClearanceMm;
    private BigDecimal minHoleDiameterMm;
    private Integer maxLayers;
    private String[] supportedMaterials;
    private String[] supportedFinishes;
    private Boolean supportsBlindVia = false;
    private Boolean supportsBuriedVia = false;
}