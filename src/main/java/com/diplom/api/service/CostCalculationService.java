package com.diplom.api.service;

import com.diplom.api.dto.CalculationRequest;
import com.diplom.api.dto.CalculationResponse;
import com.diplom.persistence.entity.*;
import com.diplom.persistence.repository.TariffRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Сервис расчёта стоимости изготовления печатной платы.
 * <p>
 * Выполняет расчёт на основе переданных параметров платы и выбранного тарифа.
 * Также проводит базовый DFM-анализ.
 */
@Service
public class CostCalculationService {

    private final TariffRepository tariffRepository;

    /**
     * @param tariffRepository репозиторий для доступа к тарифам и ограничениям
     */
    public CostCalculationService(TariffRepository tariffRepository) {
        this.tariffRepository = tariffRepository;
    }

    /**
     * Рассчитывает стоимость изготовления платы по заданным параметрам.
     *
     * @param request параметры платы и идентификатор тарифа
     * @return ответ с ценой, разбивкой по статьям и предупреждениями DFM
     * @throws RuntimeException если тариф с указанным идентификатором не найден
     */
    public CalculationResponse calculate(CalculationRequest request) {
        TariffEntity tariff = tariffRepository.findById(request.getTariffId())
                .orElseThrow(() -> new RuntimeException("Tariff not found"));
        LimitationEntity limits = tariff.getLimitation();

        double areaCm2 = (request.getWidth() * request.getHeight()) / 100.0; // мм² → см²
        BigDecimal baseCost = BigDecimal.valueOf(areaCm2).multiply(tariff.getBasePricePerCm2());
        int extraLayers = Math.max(0, request.getCopperLayers() - 2);
        BigDecimal layerCost = tariff.getExtraLayerPrice().multiply(BigDecimal.valueOf(extraLayers));
        BigDecimal setupCost = tariff.getSetupCost();
        BigDecimal expeditedFee = tariff.getExpeditedFee();

        // Итого: (base + layers + setup + expedited) * quantity
        BigDecimal total = baseCost.add(layerCost).add(setupCost).add(expeditedFee)
                .multiply(BigDecimal.valueOf(request.getQuantity()));

        // DFM-проверки
        List<String> warnings = new ArrayList<>();
        if (limits != null) {
            if (request.getMinTrackWidth() < limits.getMinTrackWidthMm().doubleValue()) {
                warnings.add("Мин. ширина проводника меньше допустимой (" + limits.getMinTrackWidthMm() + " мм)");
            }
            if (request.getMinClearance() < limits.getMinClearanceMm().doubleValue()) {
                warnings.add("Мин. зазор меньше допустимого (" + limits.getMinClearanceMm() + " мм)");
            }
            if (request.getMinHoleDiameter() < limits.getMinHoleDiameterMm().doubleValue()) {
                warnings.add("Мин. диаметр отверстия меньше допустимого (" + limits.getMinHoleDiameterMm() + " мм)");
            }
            if (request.getCopperLayers() > limits.getMaxLayers()) {
                warnings.add("Количество слоёв превышает максимум (" + limits.getMaxLayers() + ")");
            }

            // Глухие отверстия
            if (request.isHasBlindVia() && !limits.getSupportsBlindVia()) {
                warnings.add("Глухие отверстия не поддерживаются тарифом");
            }

            // Скрытые отверстия
            if (request.isHasBuriedVia() && !limits.getSupportsBuriedVia()) {
                warnings.add("Скрытые отверстия не поддерживаются тарифом");
            }

            // Срочность
            if (request.isExpedited()) { total = total.add(expeditedFee); }
        }

        Map<String, Double> breakdown = new LinkedHashMap<>();
        breakdown.put("Base", baseCost.setScale(2, RoundingMode.HALF_UP).doubleValue());
        breakdown.put("Layers", layerCost.setScale(2, RoundingMode.HALF_UP).doubleValue());
        breakdown.put("Setup", setupCost.setScale(2, RoundingMode.HALF_UP).doubleValue());
        breakdown.put("Expedited", expeditedFee.setScale(2, RoundingMode.HALF_UP).doubleValue());
        breakdown.put("Total", total.setScale(2, RoundingMode.HALF_UP).doubleValue());

        return CalculationResponse.builder()
                .totalPrice(total.doubleValue())
                .breakdown(breakdown)
                .warnings(warnings)
                .build();
    }
}