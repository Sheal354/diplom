package com.diplom.gui.controller;

import com.deltaproto.deltagerber.model.drill.DrillDocument;
import com.deltaproto.deltagerber.model.gerber.BoundingBox;
import com.deltaproto.deltagerber.model.gerber.GerberDocument;
import com.deltaproto.deltagerber.parser.GerberParser;
import com.deltaproto.deltagerber.renderer.svg.MultiLayerSVGRenderer;
import com.diplom.gerber.model.BoardLayer;
import com.diplom.gerber.model.LayerType;
import javafx.scene.paint.Color;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


@Service
public class SvgRenderService {

    private GerberDocument outlineFillDoc;

    public String buildAnalyticSvg(Map<BoardLayer, Object> layerDocuments,
                                   Set<BoardLayer> visibleLayers,
                                   Map<BoardLayer, Color> layerColors) {
        List<BoardLayer> sorted = sortLayers(layerDocuments.keySet());
        MultiLayerSVGRenderer renderer = new MultiLayerSVGRenderer();
        List<MultiLayerSVGRenderer.Layer> layerList = new ArrayList<>();

        for (BoardLayer layer : sorted) {
            if (!visibleLayers.contains(layer)) continue;
            Object docObj = layerDocuments.get(layer);
            if (docObj == null) continue;

            if (docObj instanceof DrillDocument) {
                DrillDocument drillDoc = (DrillDocument) docObj;
                Color color = layerColors.getOrDefault(layer, Color.web("#d3d3d3"));
                layerList.add(new MultiLayerSVGRenderer.Layer(layer.getName(), drillDoc)
                        .setLayerType(com.deltaproto.deltagerber.renderer.svg.LayerType.DRILL)
                        .setColor(toHex(color))
                        .setOpacity(1.0));
            } else if (docObj instanceof GerberDocument) {
                GerberDocument gerberDoc = (GerberDocument) docObj;
                Color color = layerColors.getOrDefault(layer, getDefaultColor(layer.getType()));
                layerList.add(new MultiLayerSVGRenderer.Layer(layer.getName() + "_" + layer.getType(), gerberDoc)
                        .setColor(toHex(color))
                        .setOpacity(1.0));
            }
        }

        return renderer.render(layerList);
    }

    public String buildBoardSideSvg(Map<BoardLayer, Object> layerDocuments,
                                    Set<BoardLayer> sideLayers,
                                    Set<BoardLayer> visibleLayers,
                                    Map<BoardLayer, Color> layerColors,
                                    BoundingBox totalBounds) {
        List<BoardLayer> sorted = sortLayers(sideLayers);
        MultiLayerSVGRenderer renderer = new MultiLayerSVGRenderer();
        List<MultiLayerSVGRenderer.Layer> layerList = new ArrayList<>();

        for (BoardLayer layer : sorted) {
            if (!visibleLayers.contains(layer)) continue;
            Object docObj = layerDocuments.get(layer);
            if (docObj == null) continue;

            if (docObj instanceof DrillDocument) {
                DrillDocument drillDoc = (DrillDocument) docObj;
                Color color = layerColors.getOrDefault(layer, Color.web("#d3d3d3"));
                layerList.add(new MultiLayerSVGRenderer.Layer(layer.getName(), drillDoc)
                        .setLayerType(com.deltaproto.deltagerber.renderer.svg.LayerType.DRILL)
                        .setColor(toHex(color))
                        .setOpacity(1.0));
            } else if (docObj instanceof GerberDocument) {
                GerberDocument gerberDoc = (GerberDocument) docObj;
                Color color = layerColors.getOrDefault(layer, getDefaultColor(layer.getType()));
                layerList.add(new MultiLayerSVGRenderer.Layer(layer.getName() + "_" + layer.getType(), gerberDoc)
                        .setColor(toHex(color))
                        .setOpacity(1.0));
            }
        }

        String svg = renderer.render(layerList);

        if (outlineFillDoc == null) {
            outlineFillDoc = createOutlineFillDocument(totalBounds);
        }
        if (outlineFillDoc != null && totalBounds != null) {
            double minX = totalBounds.getMinX();
            double minY = totalBounds.getMinY();
            double width = totalBounds.getMaxX() - minX;
            double height = totalBounds.getMaxY() - minY;
            String rect = String.format(Locale.US,
                    "<rect x=\"%.6f\" y=\"%.6f\" width=\"%.6f\" height=\"%.6f\" fill=\"#1a4c1a\"/>",
                    minX, minY, width, height);
            svg = svg.replaceFirst("(<svg[^>]*>)", "$1\n" + rect);
        }

        return svg;
    }

    private List<BoardLayer> sortLayers(Set<BoardLayer> layers) {
        List<BoardLayer> sorted = new ArrayList<>(layers);
        sorted.sort(Comparator
                .comparingInt((BoardLayer l) -> l.getType() == LayerType.OUTLINE ? -1 : 0)
                .thenComparingInt(l -> {
                    switch (l.getType()) {
                        case OUTLINE:      return 0;
                        case COPPER:       return 1;
                        case SOLDER_MASK:
                        case PASTE:        return 2;
                        case SILKSCREEN:   return 3;
                        case DRILL:        return 4;
                        default:           return 99;
                    }
                })
                .thenComparingInt(l -> {
                    if (l.getSide() == null) return 0;
                    return l.getSide().equalsIgnoreCase("TOP") ? 1 : 0;
                })
        );
        return sorted;
    }

    private GerberDocument createOutlineFillDocument(BoundingBox bounds) {
        if (bounds == null) return null;
        double minX = bounds.getMinX();
        double maxX = bounds.getMaxX();
        double minY = bounds.getMinY();
        double maxY = bounds.getMaxY();

        String gerber = String.format(Locale.US,
                "%%FSLAX34Y34*%%\n" +
                        "%%MOMM*%%\n" +
                        "%%ADD10C,0.01*%%\n" +
                        "G36*\n" +
                        "X%.4fY%.4fD02*\n" +
                        "X%.4fY%.4fD01*\n" +
                        "X%.4fY%.4fD01*\n" +
                        "X%.4fY%.4fD01*\n" +
                        "X%.4fY%.4fD01*\n" +
                        "G37*\n" +
                        "M02*\n",
                minX, minY, maxX, minY, maxX, maxY, minX, maxY, minX, minY);

        try {
            GerberParser parser = new GerberParser();
            return parser.parse(gerber);
        } catch (Exception e) {
            return null;
        }
    }

    public String wrapSvgInHtml(String svg) {
        return "<html><body style='margin:0;overflow:hidden;background:#d3d3d3;'>" + svg + "</body></html>";
    }

    private String toHex(Color color) {
        return String.format("#%02x%02x%02x",
                (int)(color.getRed() * 255),
                (int)(color.getGreen() * 255),
                (int)(color.getBlue() * 255));
    }

    private Color getDefaultColor(LayerType type) {
        switch (type) {
            case OUTLINE:       return Color.rgb(26, 76, 26);
            case COPPER:        return Color.rgb(51, 101, 51);
            case SOLDER_MASK:
            case PASTE:         return Color.rgb(204, 153, 51);
            case SILKSCREEN:    return Color.WHITE;
            case DRILL:         return Color.web("#d3d3d3");
            default:            return Color.rgb(128, 128, 128);
        }
    }
}