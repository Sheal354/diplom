package com.diplom.gerber.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Модель параметров платы
 */
@Data
@NoArgsConstructor
public class BoardParameters {
    // Основные геометрические параметры
    private List<BoardLayer> layers;
    private double width;
    private double height;
    private double area;
    private int copperLayersCount;

    // Параметры точности
    private double minTrackWidth;
    private double minClearance;
    private double minHoleDiameter;

    // Статистика отверстий
    private int totalHoles;
    private int platedHoles;
    private int nonPlatedHoles;
    private boolean hasThroughVia = true;
    private boolean hasBlindVia;             // есть глухие отверстия
    private boolean hasBuriedVia;            // есть скрытые отверстия

    // Слои маски и шелкографии
    private int solderMaskLayersCount;       // количество слоёв паяльной маски
    private int silkscreenLayersCount;       // количество слоёв шелкографии

    // Опциональные параметры (заполняются пользователем)
    private String boardMaterial;            // материал основания
    private String surfaceFinish;            // финишное покрытие
}