package com.diplom.gui.view;

import com.deltaproto.deltagerber.model.drill.DrillDocument;
import com.deltaproto.deltagerber.model.gerber.BoundingBox;
import com.deltaproto.deltagerber.model.gerber.GerberDocument;
import com.diplom.gerber.model.BoardLayer;
import com.diplom.gerber.model.LayerType;
import com.diplom.gui.controller.SvgRenderService;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import lombok.Getter;

import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class VisualizationPane extends VBox {

    private WebView analyticWebView;
    private WebView topWebView;
    private WebView bottomWebView;
    private StackPane topContainer;
    private StackPane bottomContainer;
    private SplitPane boardSplit;
    private StackPane mainContainer;

    /**
     * -- GETTER --
     * Возвращает текущую карту документов для синхронизации цветов.
     */
    @Getter
    private Map<BoardLayer, Object> layerDocuments = new HashMap<>();
    private Map<BoardLayer, Color> layerColors = new HashMap<>();
    private Set<BoardLayer> visibleLayers = new HashSet<>();
    private BoundingBox totalBounds;
    private boolean boardViewMode = false;

    private final SvgRenderService svgService;

    public VisualizationPane(SvgRenderService svgService) {
        this.svgService = svgService;

        analyticWebView = new WebView();
        analyticWebView.setMouseTransparent(true);

        topWebView = new WebView();
        bottomWebView = new WebView();
        topContainer = new StackPane(topWebView);
        bottomContainer = new StackPane(bottomWebView);

        boardSplit = new SplitPane(topContainer, bottomContainer);
        boardSplit.setDividerPositions(0.5f);
        boardSplit.setVisible(false);

        mainContainer = new StackPane(analyticWebView, boardSplit);
        getChildren().add(mainContainer);
        VBox.setVgrow(mainContainer, javafx.scene.layout.Priority.ALWAYS);
        setStyle("-fx-background-color: #d3d3d3;");

        analyticWebView.prefWidthProperty().bind(mainContainer.widthProperty());
        analyticWebView.prefHeightProperty().bind(mainContainer.heightProperty());
        topWebView.prefWidthProperty().bind(topContainer.widthProperty());
        topWebView.prefHeightProperty().bind(topContainer.heightProperty());
        bottomWebView.prefWidthProperty().bind(bottomContainer.widthProperty());
        bottomWebView.prefHeightProperty().bind(bottomContainer.heightProperty());
    }

    public void setLayers(Map<BoardLayer, ?> documents) {
        this.layerDocuments = new HashMap<>();
        for (Map.Entry<BoardLayer, ?> entry : documents.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof GerberDocument || value instanceof DrillDocument) {
                layerDocuments.put(entry.getKey(), value);
            }
        }
        visibleLayers.clear();
        layerColors.clear();
        for (BoardLayer layer : layerDocuments.keySet()) {
            layerColors.put(layer, getDefaultColor(layer.getType()));
            visibleLayers.add(layer);
        }
        computeTotalBounds();
        updateAllViews();
    }

    public void setLayerVisible(BoardLayer layer, boolean visible) {
        if (visible) visibleLayers.add(layer); else visibleLayers.remove(layer);
        updateAllViews();
    }

    public void setLayerColor(BoardLayer layer, Color color) {
        layerColors.put(layer, color);
        updateAllViews();
    }

    public Color getLayerColor(BoardLayer layer) {
        return layerColors.get(layer);
    }

    public void setBoardViewMode(boolean enabled) {
        this.boardViewMode = enabled;
        boardSplit.setVisible(enabled);
        analyticWebView.setVisible(!enabled);
        updateAllViews();
    }

    private void computeTotalBounds() {
        totalBounds = new BoundingBox();
        for (Object doc : layerDocuments.values()) {
            BoundingBox bbox = null;
            if (doc instanceof GerberDocument) bbox = ((GerberDocument) doc).getBoundingBox();
            else if (doc instanceof DrillDocument) bbox = ((DrillDocument) doc).getBoundingBox();
            if (bbox != null && bbox.isValid()) totalBounds.include(bbox);
        }
    }

    private void updateAllViews() {
        if (layerDocuments == null || totalBounds == null || !totalBounds.isValid()) return;
        if (boardViewMode) {
            updateBoardViews();
        } else {
            updateAnalyticView();
        }
    }

    private void updateAnalyticView() {
        String svg = svgService.buildAnalyticSvg(layerDocuments, visibleLayers, layerColors);
        analyticWebView.getEngine().loadContent(svgService.wrapSvgInHtml(svg));
    }

    private void updateBoardViews() {
        Set<BoardLayer> topSet = filterBySide("TOP");
        Set<BoardLayer> bottomSet = filterBySide("BOTTOM");
        for (BoardLayer layer : layerDocuments.keySet()) {
            if (layer.getType() == LayerType.OUTLINE || layer.getType() == LayerType.DRILL) {
                topSet.add(layer);
                bottomSet.add(layer);
            }
        }

        String topSvg = svgService.buildBoardSideSvg(layerDocuments, topSet, visibleLayers, layerColors, totalBounds);
        String bottomSvg = svgService.buildBoardSideSvg(layerDocuments, bottomSet, visibleLayers, layerColors, totalBounds);

        topWebView.getEngine().loadContent(svgService.wrapSvgInHtml(topSvg));
        bottomWebView.getEngine().loadContent(svgService.wrapSvgInHtml(bottomSvg));

        bottomWebView.setScaleX(-1);
    }

    private Set<BoardLayer> filterBySide(String side) {
        Set<BoardLayer> set = new LinkedHashSet<>();
        for (BoardLayer layer : layerDocuments.keySet()) {
            if (layer.getSide() != null && layer.getSide().equalsIgnoreCase(side)) {
                set.add(layer);
            }
        }
        return set;
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

    /**
     * Полностью удаляет слой из визуализации.
     */
    public void removeLayer(BoardLayer layer) {
        layerDocuments.remove(layer);
        layerColors.remove(layer);
        visibleLayers.remove(layer);
        computeTotalBounds();
        updateAllViews();
    }
}