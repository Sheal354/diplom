package com.diplom.gerber.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Модель одного Gerber-слоя платы.
 * Содержит классификационные признаки и ключевые геометрические параметры,
 * необходимые для оценки стоимости и DFM-анализа.
 */
@Data
@NoArgsConstructor
public class BoardLayer {
    private LayerType type;          // тип слоя (медь, маска и т.д.)
    private String name;             // имя слоя (напр. "Top Copper")
    private String side;             // "TOP", "BOTTOM" или null
    private Integer layerNumber;     // порядковый номер внутреннего слоя (null для внешних)
    private RectBounds bounds;       // габаритный прямоугольник слоя в мм
    private double minTrackWidth;    // минимальная ширина проводника (мм), для COPPER
    private double minClearance;     // минимальный зазор между элементами (мм), для COPPER

    @Override
    public String toString() {
        return String.format("BoardLayer{type=%s, name='%s', side=%s, layer=%s, bounds=%s, minTrack=%.3f, minClear=%.3f}",
                type, name, side, layerNumber, bounds, minTrackWidth, minClearance);
    }
}