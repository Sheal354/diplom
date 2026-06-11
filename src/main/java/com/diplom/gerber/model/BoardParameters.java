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

    /**
     * Пересчитывает габариты платы, количество слоёв и минимальные допуски
     * на основе текущего списка слоёв (после удаления).
     */
    public void recalculateFromLayers() {
        // Габариты по контуру или объединению границ
        RectBounds outline = null;
        for (BoardLayer l : layers) {
            if (l.getType() == LayerType.OUTLINE && l.getBounds() != null) {
                outline = l.getBounds();
                break;
            }
        }
        if (outline != null) {
            this.width = outline.getWidth();
            this.height = outline.getHeight();
            this.area = outline.getArea();
        } else {
            double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
            double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
            for (BoardLayer l : layers) {
                RectBounds b = l.getBounds();
                if (b != null) {
                    minX = Math.min(minX, b.getMinX());
                    maxX = Math.max(maxX, b.getMaxX());
                    minY = Math.min(minY, b.getMinY());
                    maxY = Math.max(maxY, b.getMaxY());
                }
            }
            if (minX == Double.MAX_VALUE) {
                this.width = 0;
                this.height = 0;
                this.area = 0;
            } else {
                this.width = maxX - minX;
                this.height = maxY - minY;
                this.area = this.width * this.height;
            }
        }

        // Пересчёт количества слоёв разных типов
        this.copperLayersCount = (int) layers.stream()
                .filter(l -> l.getType() == LayerType.COPPER).count();
        this.solderMaskLayersCount = (int) layers.stream()
                .filter(l -> l.getType() == LayerType.SOLDER_MASK).count();
        this.silkscreenLayersCount = (int) layers.stream()
                .filter(l -> l.getType() == LayerType.SILKSCREEN).count();

        // Минимальная ширина проводника
        double minTrack = Double.MAX_VALUE;
        for (BoardLayer l : layers) {
            if (l.getType() == LayerType.COPPER && l.getMinTrackWidth() > 0) {
                minTrack = Math.min(minTrack, l.getMinTrackWidth());
            }
        }
        this.minTrackWidth = (minTrack == Double.MAX_VALUE) ? -1 : minTrack;

        // Минимальный зазор
        double minClear = Double.MAX_VALUE;
        for (BoardLayer l : layers) {
            if (l.getType() == LayerType.COPPER && l.getMinClearance() > 0) {
                minClear = Math.min(minClear, l.getMinClearance());
            }
        }
        this.minClearance = (minClear == Double.MAX_VALUE) ? -1 : minClear;

        // Параметры отверстий остаются без изменений, так как сверловка не удаляется
    }
}