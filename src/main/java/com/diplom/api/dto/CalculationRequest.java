package com.diplom.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CalculationRequest {
    private double width;          // мм
    private double height;         // мм
    private int copperLayers;
    private int totalHoles;
    private double minTrackWidth;  // мм
    private double minClearance;   // мм
    private double minHoleDiameter;// мм
    private Integer tariffId;
    private int quantity = 1;

    private String boardMaterial = "FR-4";
    private String surfaceFinish = "HASL";
    private boolean hasBlindVia = false;
    private boolean hasBuriedVia = false;
    private boolean expedited = false;
}