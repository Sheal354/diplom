package com.diplom.gui.view;

import com.diplom.api.dto.AuthResponse;
import com.diplom.gui.controller.SvgRenderService;
import com.diplom.gui.service.*;
import com.diplom.gerber.model.BoardLayer;
import com.diplom.gerber.model.BoardParameters;
import com.diplom.gerber.parser.GerberFileParser;
import com.diplom.gerber.parser.ParseResult;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

public class MainView extends BorderPane {

    private final GerberFileParser parser;
    private final SvgRenderService svgService;
    private final ProducerApiService producerApiService;
    private final AuthApiService authApiService;
    private final TariffApiService tariffApiService;
    private final CalculationApiService calculationApiService;

    private TextArea parametersArea;
    private LayerListPanel layerListPanel;
    private VisualizationPane visualizationPane;

    private ProducerPanel producerPanel;
    private Button togglePanelButton;
    private boolean panelVisible = false;

    private BoardParameters currentBoardParameters;

    public MainView(GerberFileParser parser, SvgRenderService svgService,
                    ProducerApiService producerApiService, AuthApiService authApiService,
                    TariffApiService tariffApiService, CalculationApiService calculationApiService) {
        this.parser = parser;
        this.svgService = svgService;
        this.producerApiService = producerApiService;
        this.authApiService = authApiService;
        this.tariffApiService = tariffApiService;
        this.calculationApiService = calculationApiService;
        buildUI();
    }

    private void buildUI() {
        // Верхняя панель
        Button chooseFileBtn = new Button("Выбрать ZIP-архив");
        Label fileLabel = new Label("Файл не выбран");

        ToggleButton boardViewToggle = new ToggleButton("Вид платы");
        boardViewToggle.setOnAction(e -> visualizationPane.setBoardViewMode(boardViewToggle.isSelected()));

        togglePanelButton = new Button("Смотреть производителей");
        togglePanelButton.setVisible(false);

        Button loginBtn = new Button("Войти как производитель");
        loginBtn.setOnAction(e -> {
            LoginStage loginStage = new LoginStage(authApiService);
            loginStage.showAndWait();
            AuthResponse result = loginStage.getAuthResult();
            if (result != null) {
                ProducerCabinetStage cabinet = new ProducerCabinetStage(
                        result, producerApiService, tariffApiService);
                cabinet.show();
            }
        });

        HBox topPanel = new HBox(10, chooseFileBtn, fileLabel, boardViewToggle,
                togglePanelButton, new Region(), loginBtn);
        topPanel.setPadding(new Insets(10));
        HBox.setHgrow(topPanel.getChildren().get(4), Priority.ALWAYS);
        setTop(topPanel);

        // Левая часть
        parametersArea = new TextArea();
        parametersArea.setEditable(false);
        parametersArea.setPrefRowCount(20);
        parametersArea.setStyle("-fx-font-family: 'monospaced';");

        layerListPanel = new LayerListPanel();
        layerListPanel.setOnToggle(this::onLayerToggle);
        layerListPanel.setOnColorChange(this::onLayerColorChange);

        VBox leftPanel = new VBox(10, parametersArea, layerListPanel);
        leftPanel.setPadding(new Insets(5));
        VBox.setVgrow(parametersArea, Priority.ALWAYS);

        // Правая часть – визуализация
        visualizationPane = new VisualizationPane(svgService);

        SplitPane centerSplit = new SplitPane(leftPanel, visualizationPane);
        centerSplit.setDividerPositions(0.35f);

        // Панель производителей
        producerPanel = new ProducerPanel(
                producerApiService,
                () -> currentBoardParameters,
                calculationApiService
        );
        VBox slidingPanel = producerPanel.getPanel();
        slidingPanel.setVisible(false);

        togglePanelButton.setOnAction(e -> {
            panelVisible = !panelVisible;
            slidingPanel.setVisible(panelVisible);
            if (panelVisible) {
                producerPanel.loadProducersIfNeeded();
            }
        });

        // Наложение панели справа
        StackPane centerWrapper = new StackPane(centerSplit, slidingPanel);
        StackPane.setAlignment(slidingPanel, Pos.CENTER_RIGHT);
        setCenter(centerWrapper);

        leftPanel.setMinWidth(250);
        visualizationPane.setMinWidth(400);
        visualizationPane.setMinHeight(300);
        SplitPane.setResizableWithParent(leftPanel, false);

        chooseFileBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("ZIP архивы", "*.zip"));
            File file = chooser.showOpenDialog(getScene().getWindow());
            if (file != null) {
                fileLabel.setText(file.getName());
                parseAndDisplay(file);
            }
        });
    }

    private void parseAndDisplay(File zipFile) {
        new Thread(() -> {
            try (FileInputStream fis = new FileInputStream(zipFile)) {
                ParseResult parseResult = parser.parseFromZip(fis);
                BoardParameters board = parseResult.getBoardParameters();
                Map<BoardLayer, ?> docs = parseResult.getLayerDocuments();

                Platform.runLater(() -> {
                    currentBoardParameters = board;
                    parametersArea.setText(formatParameters(board));
                    layerListPanel.setLayers(board.getLayers());
                    visualizationPane.setLayers(docs);
                    syncColorsFromVisualization(docs);
                    togglePanelButton.setVisible(true);
                });
            } catch (IOException ex) {
                Platform.runLater(() ->
                        parametersArea.setText("Ошибка чтения архива: " + ex.getMessage()));
            } catch (Exception ex) {
                Platform.runLater(() ->
                        parametersArea.setText("Ошибка: " + ex.getMessage()));
            }
        }).start();
    }

    private String formatParameters(BoardParameters board) {
        StringBuilder sb = new StringBuilder();
        sb.append("===== Параметры платы =====\n");
        sb.append(String.format("Габариты: %.2f x %.2f мм (площадь %.2f мм²)%n",
                board.getWidth(), board.getHeight(), board.getArea()));
        sb.append("Количество медных слоёв: ").append(board.getCopperLayersCount()).append("\n");
        sb.append("Отверстия: всего ").append(board.getTotalHoles())
                .append(", металлизированных ").append(board.getPlatedHoles())
                .append(", неметаллизированных ").append(board.getNonPlatedHoles()).append("\n");

        if (board.getMinTrackWidth() <= 0) {
            sb.append("Мин. ширина проводника: не удалось определить\n");
        } else {
            sb.append(String.format("Мин. ширина проводника: %.3f мм%n", board.getMinTrackWidth()));
        }

        double clearance = board.getMinClearance();
        if (clearance < 0) {
            sb.append("Мин. зазор: не удалось определить\n");
        } else if (clearance == 0.0) {
            sb.append("Мин. зазор: 0.000 мм (объекты могут быть соединены)\n");
        } else {
            sb.append(String.format("Мин. зазор: %.3f мм%n", clearance));
        }

        sb.append("Слоёв маски: ").append(board.getSolderMaskLayersCount()).append("\n");
        sb.append("Слоёв шелкографии: ").append(board.getSilkscreenLayersCount()).append("\n");

        if (board.getMinHoleDiameter() <= 0) {
            sb.append("Мин. диаметр отверстия: не удалось определить\n");
        } else {
            sb.append(String.format("Мин. диаметр отверстия: %.3f мм%n", board.getMinHoleDiameter()));
        }

        sb.append("Глухие отверстия: ").append(board.isHasBlindVia() ? "да" : "нет").append("\n");
        sb.append("Скрытые отверстия: ").append(board.isHasBuriedVia() ? "да" : "нет").append("\n");

        return sb.toString();
    }

    private void onLayerToggle(BoardLayer layer, boolean show) {
        visualizationPane.setLayerVisible(layer, show);
    }

    private void onLayerColorChange(BoardLayer layer, Color color) {
        visualizationPane.setLayerColor(layer, color);
    }

    private void syncColorsFromVisualization(Map<BoardLayer, ?> docs) {
        for (BoardLayer layer : docs.keySet()) {
            Color color = visualizationPane.getLayerColor(layer);
            if (color != null) {
                layerListPanel.updateLayerColor(layer, color);
            }
        }
    }
}