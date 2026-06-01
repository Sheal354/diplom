package com.diplom.gerber.parser;

import com.deltaproto.deltagerber.model.gerber.BoundingBox;
import com.deltaproto.deltagerber.model.gerber.GerberDocument;
import com.deltaproto.deltagerber.model.gerber.aperture.Aperture;
import com.deltaproto.deltagerber.model.gerber.aperture.CircleAperture;
import com.deltaproto.deltagerber.model.gerber.aperture.MacroAperture;
import com.deltaproto.deltagerber.model.gerber.aperture.ObroundAperture;
import com.deltaproto.deltagerber.model.gerber.aperture.PolygonAperture;
import com.deltaproto.deltagerber.model.gerber.aperture.RectangleAperture;
import com.deltaproto.deltagerber.model.gerber.operation.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.locationtech.jts.util.GeometricShapeFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Анализатор геометрии Gerber-слоя.
 * <p>
 * Предоставляет методы для вычисления минимальной ширины проводника и минимального зазора
 * на основе графических объектов Gerber-документа. Вся работа с точной геометрией ведётся
 * с использованием библиотеки JTS (Java Topology Suite).
 */
@Component
public class GeometryAnalyzer {

    /** Фабрика геометрических объектов JTS. */
    private static final GeometryFactory GEOM_FACTORY = new GeometryFactory();

    /**
     * Вычисляет минимальную ширину проводника (дорожки) на слое.
     * <p>
     * Анализирует все апертуры, зарегистрированные в документе.
     *
     * @param doc Gerber-документ слоя
     * @return минимальная ширина дорожки в миллиметрах, или -1, если определить не удалось
     */
    public double computeMinTrackWidth(GerberDocument doc) {
        Collection<Aperture> apertures = doc.getApertures().values();
        double min = Double.MAX_VALUE;
        for (Aperture ap : apertures) {
            double size = 0;
            if (ap instanceof CircleAperture) {
                size = ((CircleAperture) ap).getDiameter();
            } else if (ap instanceof RectangleAperture rect) {
                size = Math.min(rect.getWidth(), rect.getHeight());
            } else if (ap instanceof ObroundAperture ob) {
                // Минимальный размер овала — его ширина (диаметр полукруга)
                size = ob.getWidth();
            } else if (ap instanceof PolygonAperture poly) {
                size = Math.min(poly.getBoundingBox().getWidth(),
                        poly.getBoundingBox().getHeight());
            } else if (ap instanceof MacroAperture macro) {
                size = Math.min(macro.getBoundingBox().getWidth(),
                        macro.getBoundingBox().getHeight());
            }
            if (size > 0 && size < min) {
                min = size;
            }
        }
        return min == Double.MAX_VALUE ? -1 : min;
    }

    /**
     * Вычисляет минимальный зазор между разными электрическими цепями (кластерами) на слое.
     *
     * @param doc Gerber-документ слоя
     * @return минимальный зазор в миллиметрах, или -1, если кластеров меньше двух
     */
    public double computeMinClearance(GerberDocument doc) {
        List<GraphicsObject> objects = doc.getObjects();
        if (objects.size() < 2) return -1;

        // 1. Строим JTS-геометрии для всех объектов
        int n = objects.size();
        Geometry[] geoms = new Geometry[n];
        for (int i = 0; i < n; i++) {
            geoms[i] = toJtsGeometry(objects.get(i));
        }

        // 2. Кластеризация по точному пересечению (union-find)
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;
        for (int i = 0; i < n; i++) {
            if (geoms[i].isEmpty()) continue;
            for (int j = i + 1; j < n; j++) {
                if (geoms[j].isEmpty()) continue;
                if (geoms[i].intersects(geoms[j])) {
                    union(parent, i, j);
                }
            }
        }

        // 3. Группируем объекты по кластерам
        Map<Integer, List<Integer>> clusterMap = new HashMap<>();
        for (int i = 0; i < n; i++) {
            int root = find(parent, i);
            clusterMap.computeIfAbsent(root, k -> new ArrayList<>()).add(i);
        }

        // 4. Для каждого кластера создаём GeometryCollection (без объединения)
        List<Geometry> clusterGeoms = new ArrayList<>();
        for (List<Integer> indices : clusterMap.values()) {
            List<Geometry> parts = new ArrayList<>();
            for (int idx : indices) {
                if (!geoms[idx].isEmpty()) {
                    parts.add(geoms[idx]);
                }
            }
            if (parts.isEmpty()) continue;
            clusterGeoms.add(GEOM_FACTORY.buildGeometry(parts));
        }

        if (clusterGeoms.size() < 2) return -1;

        // 5. Минимальное расстояние между разными кластерами
        double minClear = Double.MAX_VALUE;
        for (int i = 0; i < clusterGeoms.size(); i++) {
            for (int j = i + 1; j < clusterGeoms.size(); j++) {
                double d = clusterGeoms.get(i).distance(clusterGeoms.get(j));
                if (d < minClear) minClear = d;
            }
        }
        return minClear == Double.MAX_VALUE ? -1 : minClear;
    }

    /**
     * Преобразует графический объект Gerber в JTS-геометрию.
     *
     * @param obj графический объект Gerber
     * @return эквивалентная JTS-геометрия (или пустая геометрия, если тип не поддерживается)
     */
    private Geometry toJtsGeometry(GraphicsObject obj) {
        if (obj instanceof Flash f) {
            return apertureToJts(f.getAperture(), f.getX(), f.getY());
        } else if (obj instanceof Draw d) {
            double radius = getApertureRadius(d.getAperture());
            LineString line = GEOM_FACTORY.createLineString(new Coordinate[]{
                    new Coordinate(d.getStartX(), d.getStartY()),
                    new Coordinate(d.getEndX(), d.getEndY())
            });
            return buffer(line, radius);
        } else if (obj instanceof Arc a) {
            double radius = getApertureRadius(a.getAperture());
            LineString arcLine = approximateArc(a);
            return buffer(arcLine, radius);
        } else if (obj instanceof Region) {
            return regionToJts((Region) obj);
        }
        return GEOM_FACTORY.createEmpty(2);
    }

    /**
     * Возвращает радиус апертуры (половину диаметра для круглых,
     * половину максимальной стороны для прямоугольных).
     *
     * @param ap апертура Gerber
     * @return радиус в миллиметрах, для неизвестных типов
     * возвращает 0.1 мм (защитное значение).
     */
    private double getApertureRadius(Aperture ap) {
        if (ap instanceof CircleAperture) {
            return ((CircleAperture) ap).getDiameter() / 2.0;
        } else if (ap instanceof RectangleAperture rect) {
            return Math.max(rect.getWidth(), rect.getHeight()) / 2.0;
        }
        return 0.1;
    }

    /**
     * Создаёт буферизованную геометрию (отступ) с заданным радиусом.
     *
     * @param geom   исходная геометрия
     * @param radius радиус буфера (мм)
     * @return расширенная геометрия, если радиус <= 0, возвращает исходную геометрию без изменений.
     */
    private Geometry buffer(Geometry geom, double radius) {
        if (radius <= 0) return geom;
        BufferOp bufOp = new BufferOp(geom, new BufferParameters());
        return bufOp.getResultGeometry(radius);
    }

    /**
     * Создаёт JTS-геометрию для апертуры, размещённой в заданной точке.
     *
     * @param ap апертура
     * @param x  координата X центра (мм)
     * @param y  координата Y центра (мм)
     * @return JTS-геометрия апертуры
     */
    private Geometry apertureToJts(Aperture ap, double x, double y) {
        if (ap instanceof CircleAperture) {
            double r = ((CircleAperture) ap).getDiameter() / 2.0;
            return buffer(GEOM_FACTORY.createPoint(new Coordinate(x, y)), r);
        } else if (ap instanceof RectangleAperture rect) {
            double w = rect.getWidth();
            double h = rect.getHeight();
            GeometricShapeFactory factory = new GeometricShapeFactory(GEOM_FACTORY);
            factory.setNumPoints(4);
            factory.setWidth(w);
            factory.setHeight(h);
            factory.setCentre(new Coordinate(x, y));
            return factory.createRectangle();
        } else if (ap instanceof ObroundAperture ob) {
            // Строим как прямоугольник со скруглёнными углами
            double w = ob.getWidth();
            double h = ob.getHeight();
            double r = w / 2.0; // радиус скругления равен половине ширины
            GeometricShapeFactory rectFactory = new GeometricShapeFactory(GEOM_FACTORY);
            rectFactory.setWidth(w);
            rectFactory.setHeight(h - w); // высота прямого участка
            rectFactory.setCentre(new Coordinate(x, y));
            Geometry centralRect = rectFactory.createRectangle();
            // добавляем два полукруга
            Coordinate c1 = new Coordinate(x, y - (h - w)/2);
            Coordinate c2 = new Coordinate(x, y + (h - w)/2);
            Geometry circle1 = buffer(GEOM_FACTORY.createPoint(c1), r);
            Geometry circle2 = buffer(GEOM_FACTORY.createPoint(c2), r);
            return centralRect.union(circle1).union(circle2);
        } else if (ap instanceof PolygonAperture poly) {
            BoundingBox bb = poly.getBoundingBox();
            GeometricShapeFactory factory = new GeometricShapeFactory(GEOM_FACTORY);
            factory.setWidth(bb.getWidth());
            factory.setHeight(bb.getHeight());
            factory.setCentre(new Coordinate(x, y));
            return factory.createRectangle(); // приближение прямоугольником
        } else if (ap instanceof MacroAperture macro) {
            BoundingBox bb = macro.getBoundingBox();
            GeometricShapeFactory factory = new GeometricShapeFactory(GEOM_FACTORY);
            factory.setWidth(bb.getWidth());
            factory.setHeight(bb.getHeight());
            factory.setCentre(new Coordinate(x, y));
            return factory.createRectangle(); // приближение прямоугольником
        }
        return GEOM_FACTORY.createEmpty(2);
    }

    /**
     * Аппроксимирует дугу последовательностью отрезков и возвращает ломаную линию (LineString).
     * Количество отрезков зависит от угла развёртки и радиуса.
     *
     * @param a дуга Gerber
     * @return аппроксимирующая линия
     */
    private LineString approximateArc(Arc a) {
        double cx = a.getCenterX(), cy = a.getCenterY();
        double r = a.getRadius();
        double startAngle = Math.atan2(a.getStartY() - cy, a.getStartX() - cx);
        double endAngle = Math.atan2(a.getEndY() - cy, a.getEndX() - cx);
        double sweep;
        if (a.isClockwise()) {
            sweep = startAngle - endAngle;
            if (sweep <= 0) sweep += 2 * Math.PI;
        } else {
            sweep = endAngle - startAngle;
            if (sweep <= 0) sweep += 2 * Math.PI;
        }
        int steps = Math.max(8, (int)(sweep * r * 10));
        Coordinate[] coords = new Coordinate[steps + 1];
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double angle = a.isClockwise() ? startAngle - sweep * t : startAngle + sweep * t;
            coords[i] = new Coordinate(cx + r * Math.cos(angle), cy + r * Math.sin(angle));
        }
        return GEOM_FACTORY.createLineString(coords);
    }

    /**
     * Преобразует Gerber-регион в JTS-полигон.
     * Первый контур считается внешней границей, остальные — дырками.
     *
     * @param region регион Gerber
     * @return JTS-полигон (или пустая геометрия, если контуры отсутствуют)
     */
    private Geometry regionToJts(Region region) {
        List<Contour> contours = region.getContours();
        if (contours.isEmpty()) return GEOM_FACTORY.createEmpty(2);
        List<Coordinate> shellCoords = contourToCoordinates(contours.get(0));
        if (shellCoords.isEmpty()) return GEOM_FACTORY.createEmpty(2);
        LinearRing shell = GEOM_FACTORY.createLinearRing(shellCoords.toArray(new Coordinate[0]));
        LinearRing[] holes = null;
        if (contours.size() > 1) {
            holes = new LinearRing[contours.size() - 1];
            for (int i = 1; i < contours.size(); i++) {
                List<Coordinate> holeCoords = contourToCoordinates(contours.get(i));
                if (!holeCoords.isEmpty()) {
                    holes[i - 1] = GEOM_FACTORY.createLinearRing(holeCoords.toArray(new Coordinate[0]));
                }
            }
        }
        return GEOM_FACTORY.createPolygon(shell, holes);
    }

    /**
     * Преобразует контур Gerber в список координат JTS.
     * Дуговые сегменты аппроксимируются отрезками.
     *
     * @param contour контур Gerber
     * @return список координат, пригодный для построения кольца
     */
    private List<Coordinate> contourToCoordinates(Contour contour) {
        List<Coordinate> coords = new ArrayList<>();
        coords.add(new Coordinate(contour.getStartX(), contour.getStartY()));
        double curX = contour.getStartX(), curY = contour.getStartY();
        for (Contour.ContourSegment seg : contour.getSegments()) {
            if (seg.isArc()) {
                double cx = seg.getCenterX(), cy = seg.getCenterY();
                double dx = curX - cx, dy = curY - cy;
                double r = Math.sqrt(dx * dx + dy * dy);
                double startAngle = Math.atan2(dy, dx);
                double endAngle = Math.atan2(seg.getY() - cy, seg.getX() - cx);
                double sweep;
                if (seg.isClockwise()) {
                    sweep = startAngle - endAngle;
                    if (sweep <= 0) sweep += 2 * Math.PI;
                } else {
                    sweep = endAngle - startAngle;
                    if (sweep <= 0) sweep += 2 * Math.PI;
                }
                int steps = Math.max(8, (int)(sweep * r * 10));
                for (int i = 1; i <= steps; i++) {
                    double t = (double) i / steps;
                    double angle = seg.isClockwise() ? startAngle - sweep * t : startAngle + sweep * t;
                    coords.add(new Coordinate(cx + r * Math.cos(angle), cy + r * Math.sin(angle)));
                }
            } else {
                coords.add(new Coordinate(seg.getX(), seg.getY()));
            }
            curX = seg.getX();
            curY = seg.getY();
        }
        return coords;
    }

    /**
     * Находит корень дерева в структуре Union-Find (со сжатием пути).
     *
     * @param parent массив родителей
     * @param x      индекс элемента
     * @return корневой индекс
     */
    private int find(int[] parent, int x) {
        if (parent[x] != x) parent[x] = find(parent, parent[x]);
        return parent[x];
    }

    /**
     * Объединяет два множества в Union-Find.
     *
     * @param parent массив родителей
     * @param a      индекс первого элемента
     * @param b      индекс второго элемента
     */
    private void union(int[] parent, int a, int b) {
        int ra = find(parent, a), rb = find(parent, b);
        if (ra != rb) parent[ra] = rb;
    }
}