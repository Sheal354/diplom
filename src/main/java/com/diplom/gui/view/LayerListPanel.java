package com.diplom.gui.view;

import com.diplom.gerber.model.BoardLayer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Панель со списком слоёв платы в виде таблицы с колонками:
 * Цвет | Слой | Вид
 */
public class LayerListPanel extends VBox {

    private final GridPane grid = new GridPane();
    private final List<BoardLayer> currentLayers = new ArrayList<>();

    // Колбэки
    @Setter
    private BiConsumer<BoardLayer, Boolean> onToggle;
    @Setter
    private BiConsumer<BoardLayer, Color> onColorChange;

    public LayerListPanel() {
        // Заголовок
        Label title = new Label("Слои платы:");
        title.setStyle("-fx-font-weight: bold;");

        // Кнопки массового управления
        Button showAllBtn = new Button("Показать все");
        Button hideAllBtn = new Button("Скрыть все");
        HBox controlButtons = new HBox(10, showAllBtn, hideAllBtn);
        controlButtons.setPadding(new Insets(0, 0, 5, 0));

        showAllBtn.setOnAction(e -> toggleAll(true));
        hideAllBtn.setOnAction(e -> toggleAll(false));

        // Настройка GridPane
        grid.setHgap(10);
        grid.setVgap(5);
        grid.setAlignment(Pos.TOP_LEFT);

        // Скроллируемая область
        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(200);

        getChildren().addAll(title, controlButtons, scroll);
        setSpacing(5);
    }

    /**
     * Заполняет таблицу слоями.
     */
    public void setLayers(java.util.List<BoardLayer> layers) {
        grid.getChildren().clear();
        currentLayers.clear();
        currentLayers.addAll(layers);

        // Заголовки столбцов (без «Прозрачность»)
        Label colorHeader = new Label("Цвет");
        Label nameHeader = new Label("Слой");
        Label viewHeader = new Label("Вид");
        grid.addRow(0, colorHeader, nameHeader, viewHeader);
        colorHeader.setStyle("-fx-font-weight: bold;");
        nameHeader.setStyle("-fx-font-weight: bold;");
        viewHeader.setStyle("-fx-font-weight: bold;");

        int row = 1;
        for (BoardLayer layer : layers) {
            // ColorPicker
            ColorPicker colorPicker = new ColorPicker();
            colorPicker.setPrefWidth(35);
            colorPicker.setValue(Color.BLACK); // временно, позже обновится через updateLayerColor
            colorPicker.setOnAction(e -> {
                if (onColorChange != null) {
                    onColorChange.accept(layer, colorPicker.getValue());
                }
            });

            // Название слоя
            String displayName = String.format("%s  [%s]", layer.getName(), layer.getType().toString());
            Label label = new Label(displayName);
            label.setMinWidth(200);

            // Кнопка с глазом
            Button eyeBtn = new Button("\uD83D\uDC41"); // 👁 открытый глаз
            eyeBtn.setUserData(layer);
            eyeBtn.setOnAction(e -> {
                Button btn = (Button) e.getSource();
                boolean currentlyVisible = "\uD83D\uDC41".equals(btn.getText());
                if (currentlyVisible) {
                    btn.setText("\uD83D\uDEAB"); // 🚫
                    if (onToggle != null) onToggle.accept(layer, false);
                } else {
                    btn.setText("\uD83D\uDC41"); // 👁
                    if (onToggle != null) onToggle.accept(layer, true);
                }
            });

            grid.addRow(row++, colorPicker, label, eyeBtn);
        }
    }

    /**
     * Обновляет цвет в ColorPicker'е для указанного слоя.
     */
    public void updateLayerColor(BoardLayer layer, Color color) {
        int row = findRow(layer);
        if (row >= 0) {
            ColorPicker picker = (ColorPicker) getNodeByRowCol(row, 0);
            if (picker != null) picker.setValue(color);
        }
    }

    /**
     * Включает/выключает видимость всех слоёв.
     */
    private void toggleAll(boolean show) {
        for (int row = 1; row < grid.getRowCount(); row++) {
            Button btn = (Button) getNodeByRowCol(row, 2);
            if (btn != null && btn.getUserData() instanceof BoardLayer) {
                if (show && "\uD83D\uDEAB".equals(btn.getText())) {
                    btn.fire();
                } else if (!show && "\uD83D\uDC41".equals(btn.getText())) {
                    btn.fire();
                }
            }
        }
    }

    private int findRow(BoardLayer layer) {
        for (int row = 1; row < grid.getRowCount(); row++) {
            Button btn = (Button) getNodeByRowCol(row, 2);
            if (btn != null && btn.getUserData() == layer) {
                return row;
            }
        }
        return -1;
    }

    private javafx.scene.Node getNodeByRowCol(int row, int col) {
        for (javafx.scene.Node node : grid.getChildren()) {
            Integer r = GridPane.getRowIndex(node);
            Integer c = GridPane.getColumnIndex(node);
            if (r == null) r = 0;
            if (c == null) c = 0;
            if (r == row && c == col) return node;
        }
        return null;
    }
}