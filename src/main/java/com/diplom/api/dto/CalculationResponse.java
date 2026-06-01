package com.diplom.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;
import java.util.Map;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CalculationResponse {
    private double totalPrice;
    private Map<String, Double> breakdown;
    private List<String> warnings;
}