package com.diplom.gerber.model;

import lombok.Data;
import lombok.AllArgsConstructor;

/**
 * Модель отверстия
 */
@Data
@AllArgsConstructor
public class DrillHole {
    private double x;          // координата X центра отверстия, мм
    private double y;          // координата Y центра отверстия, мм
    private double diameter;   // диаметр отверстия, мм
    private boolean plated;    // металлизированное (true) или неметаллизированное (false)
}