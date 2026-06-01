package com.diplom.gerber.model;

/**
 * Тип слоя печатной платы.
 */
public enum LayerType {
    COPPER,         // Медный слой (Top, Bottom, Inner)
    SOLDER_MASK,    // Паяльная маска (Top/Bottom)
    SILKSCREEN,     // Шелкография (Top/Bottom)
    PASTE,          // Паяльная паста (Top/Bottom)
    OUTLINE,        // Контур платы
    DRILL_DRAWING,  // Чертёж отверстий (вспомогательный Gerber)
    DRILL,          // файл сверловки
    UNKNOWN         // Неопознанный тип
}