package com.diplom.gerber.model;

import lombok.Getter;

/**
 * Прямоугольные границы слоя (или всей платы) в миллиметрах.
 */
@Getter
public class RectBounds {
    private double minX, maxX, minY, maxY;

    public RectBounds(double minX, double maxX, double minY, double maxY) {
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
    }

    public double getWidth()  { return maxX - minX; }
    public double getHeight() { return maxY - minY; }
    public double getArea()   { return getWidth() * getHeight(); }

    @Override
    public String toString() {
        return String.format("[%.3f..%.3f] x [%.3f..%.3f] mm", minX, maxX, minY, maxY);
    }
}