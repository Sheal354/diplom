package com.diplom.gui.view;

import com.diplom.gerber.model.BoardLayer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class LayerListPanel extends VBox {

    private final GridPane grid = new GridPane();
    private final List<BoardLayer> currentLayers = new ArrayList<>();

    @Setter
    private BiConsumer<BoardLayer, Boolean> onToggle;
    @Setter
    private BiConsumer<BoardLayer, Color> onColorChange;
    @Setter
    private Consumer<BoardLayer> onDelete;

    public LayerListPanel() {
        Label title = new Label("Слои платы:");
        title.setStyle("-fx-font-weight: bold;");

        Button showAllBtn = new Button("Показать все");
        Button hideAllBtn = new Button("Скрыть все");
        HBox controlButtons = new HBox(10, showAllBtn, hideAllBtn);
        controlButtons.setPadding(new Insets(0, 0, 5, 0));

        showAllBtn.setOnAction(e -> toggleAll(true));
        hideAllBtn.setOnAction(e -> toggleAll(false));

        grid.setHgap(10);
        grid.setVgap(5);
        grid.setAlignment(Pos.TOP_LEFT);

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(200);

        getChildren().addAll(title, controlButtons, scroll);
        setSpacing(5);
    }

    public void setLayers(List<BoardLayer> layers) {
        grid.getChildren().clear();
        currentLayers.clear();
        currentLayers.addAll(layers);

        // Заголовки
        Label colorHeader = new Label("Цвет");
        Label nameHeader = new Label("Слой");
        Label viewHeader = new Label("Вид");
        Label delHeader = new Label("");  // пустой заголовок для кнопки удаления
        grid.addRow(0, colorHeader, nameHeader, viewHeader, delHeader);
        colorHeader.setStyle("-fx-font-weight: bold;");
        nameHeader.setStyle("-fx-font-weight: bold;");
        viewHeader.setStyle("-fx-font-weight: bold;");

        Image openEye = new Image(getClass().getResourceAsStream("/icons/open-eye-icon.png"));
        Image closedEye = new Image(getClass().getResourceAsStream("/icons/eye-closed-icon.png"));

        int row = 1;
        for (BoardLayer layer : layers) {
            // ColorPicker (как раньше)
            ColorPicker colorPicker = new ColorPicker();
            colorPicker.setPrefWidth(35);
            colorPicker.setValue(Color.BLACK);
            colorPicker.setOnAction(e -> {
                if (onColorChange != null) onColorChange.accept(layer, colorPicker.getValue());
            });

            String displayName = String.format("%s  [%s]", layer.getName(), layer.getType());
            Label label = new Label(displayName);
            label.setMinWidth(150);

            // Кнопка глаза
            ToggleButton eyeBtn = new ToggleButton();
            eyeBtn.setUserData(layer);
            eyeBtn.setSelected(true);
            eyeBtn.setGraphic(new ImageView(openEye));
            eyeBtn.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
                if (isNowSelected) {
                    eyeBtn.setGraphic(new ImageView(openEye));
                    if (onToggle != null) onToggle.accept(layer, true);
                } else {
                    eyeBtn.setGraphic(new ImageView(closedEye));
                    if (onToggle != null) onToggle.accept(layer, false);
                }
            });

            // Кнопка удаления
            Button deleteBtn = new Button("✕");
            deleteBtn.setStyle("-fx-font-size: 10px; -fx-padding: 1 4;");
            deleteBtn.setTooltip(new Tooltip("Удалить слой"));
            deleteBtn.setOnAction(e -> {
                if (onDelete != null) onDelete.accept(layer);
            });

            grid.addRow(row++, colorPicker, label, eyeBtn, deleteBtn);
        }
    }

    public void updateLayerColor(BoardLayer layer, Color color) {
        int row = findRow(layer);
        if (row >= 0) {
            ColorPicker picker = (ColorPicker) getNodeByRowCol(row, 0);
            if (picker != null) picker.setValue(color);
        }
    }

    private void toggleAll(boolean show) {
        for (BoardLayer layer : currentLayers) {
            int row = findRow(layer);
            if (row >= 0) {
                ToggleButton btn = (ToggleButton) getNodeByRowCol(row, 2);
                if (btn != null && btn.isSelected() != show) {
                    btn.setSelected(show);   // программное переключение, вызовет слушатель
                }
            }
        }
    }

    private int findRow(BoardLayer layer) {
        for (int row = 1; row < grid.getRowCount(); row++) {
            ToggleButton btn = (ToggleButton) getNodeByRowCol(row, 2);
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