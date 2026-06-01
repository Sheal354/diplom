package com.diplom.gui.view;

import com.diplom.api.dto.CalculationRequest;
import com.diplom.api.dto.ProducerDto;
import com.diplom.api.dto.TariffDto;
import com.diplom.gerber.model.BoardParameters;
import com.diplom.gui.service.CalculationApiService;
import com.diplom.gui.service.ProducerApiService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Supplier;

public class ProducerPanel {

    private final ProducerApiService producerApiService;
    private VBox panel;
    private VBox producersList;
    private boolean producersLoaded = false;

    private final Supplier<BoardParameters> boardParametersSupplier;
    private final CalculationApiService calculationApiService;

    public ProducerPanel(ProducerApiService producerApiService,
                         Supplier<BoardParameters> boardSupplier,
                         CalculationApiService calcService) {
        this.producerApiService = producerApiService;
        this.boardParametersSupplier = boardSupplier;
        this.calculationApiService = calcService;
        buildPanel();
    }

    public VBox getPanel() {
        return panel;
    }

    public void loadProducersIfNeeded() {
        if (!producersLoaded) {
            loadProducers();
        }
    }

    private void buildPanel() {
        panel = new VBox(10);
        panel.setPadding(new Insets(10));
        panel.setStyle("-fx-background-color: #ffffff; -fx-border-color: #d0d0d0; -fx-border-width: 0 0 0 1;");
        panel.setMinWidth(250);
        panel.setPrefWidth(300);
        panel.setMaxWidth(400);

        // Заголовок с кнопкой обновления
        Label panelTitle = new Label("Производители");
        panelTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Button refreshBtn = new Button("\uD83D\uDD04");  // 🔄
        refreshBtn.setTooltip(new Tooltip("Обновить список"));
        refreshBtn.setOnAction(e -> {
            producersLoaded = false;
            loadProducers();
        });
        Region spacer = new Region();
        HBox headerRow = new HBox(panelTitle, spacer, refreshBtn);
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Список производителей
        producersList = new VBox(10);
        ScrollPane scrollPane = new ScrollPane(producersList);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(400);

        panel.getChildren().addAll(headerRow, scrollPane);
    }

    private void loadProducers() {
        new Thread(() -> {
            List<ProducerDto> producers = producerApiService.fetchProducers();
            Platform.runLater(() -> displayProducers(producers));
        }).start();
    }

    private void displayProducers(List<ProducerDto> producers) {
        producersList.getChildren().clear();
        if (producers.isEmpty()) {
            producersList.getChildren().add(new Label("Нет активных производителей"));
            return;
        }
        for (ProducerDto producer : producers) {
            VBox card = new VBox(5);
            card.setStyle("-fx-background-color: #f8f8f8; -fx-padding: 8; -fx-border-color: #cccccc; -fx-border-radius: 4;");
            Label nameLabel = new Label(producer.getName());
            nameLabel.setStyle("-fx-font-weight: bold;");
            Label contactLabel = new Label("Контакт: " + producer.getContactPerson() + ", " + producer.getEmail() + ", " + producer.getPhone());
            contactLabel.setWrapText(true);
            card.getChildren().addAll(nameLabel, contactLabel);

            if (producer.getTariffs() != null && !producer.getTariffs().isEmpty()) {
                Label tariffsHeader = new Label("Тарифы:");
                tariffsHeader.setStyle("-fx-font-weight: bold;");
                card.getChildren().add(tariffsHeader);
                for (TariffDto tariff : producer.getTariffs()) {
                    VBox details = new VBox(5);
                    details.setPadding(new Insets(5));
                    details.getChildren().add(new Label("Цена за слой: " + tariff.getExtraLayerPrice() + " руб/см²"));
                    details.getChildren().add(new Label("Надбавка за срочность: " + tariff.getExpeditedFee() + " руб"));
                    details.getChildren().add(new Label("Подготовка производства: " + tariff.getSetupCost() + " руб"));
                    details.getChildren().add(new Label("Мин. тираж: " + tariff.getMinQuantity() + " шт."));
                    details.getChildren().add(new Label("Ограничения:"));
                    details.getChildren().add(new Label("  Мин. ширина дорожки: " + tariff.getMinTrackWidthMm() + " мм"));
                    details.getChildren().add(new Label("  Мин. зазор: " + tariff.getMinClearanceMm() + " мм"));
                    details.getChildren().add(new Label("  Мин. диаметр отверстия: " + tariff.getMinHoleDiameterMm() + " мм"));
                    details.getChildren().add(new Label("  Макс. слоёв: " + tariff.getMaxLayers()));
                    details.getChildren().add(new Label("  Материалы: " + (tariff.getSupportedMaterials() != null ? String.join(", ", tariff.getSupportedMaterials()) : "любые")));
                    details.getChildren().add(new Label("  Покрытия: " + (tariff.getSupportedFinishes() != null ? String.join(", ", tariff.getSupportedFinishes()) : "любые")));
                    details.getChildren().add(new Label("  Глухие отверстия: " + (tariff.getSupportsBlindVia() ? "да" : "нет")));
                    details.getChildren().add(new Label("  Скрытые отверстия: " + (tariff.getSupportsBuriedVia() ? "да" : "нет")));

                    // --- Выбор материала и покрытия ---
                    ComboBox<String> materialBox = new ComboBox<>();
                    if (tariff.getSupportedMaterials() != null && tariff.getSupportedMaterials().length > 0) {
                        materialBox.getItems().addAll(tariff.getSupportedMaterials());
                        materialBox.setValue(tariff.getSupportedMaterials()[0]);
                    } else {
                        materialBox.getItems().add("FR-4");
                        materialBox.setValue("FR-4");
                    }

                    ComboBox<String> finishBox = new ComboBox<>();
                    if (tariff.getSupportedFinishes() != null && tariff.getSupportedFinishes().length > 0) {
                        finishBox.getItems().addAll(tariff.getSupportedFinishes());
                        finishBox.setValue(tariff.getSupportedFinishes()[0]);
                    } else {
                        finishBox.getItems().add("HASL");
                        finishBox.setValue("HASL");
                    }

                    // --- Срочность ---
                    CheckBox expeditedCheck = new CheckBox("Срочное изготовление");

                    // --- Количество плат ---
                    TextField quantityField = new TextField("1");
                    quantityField.setMaxWidth(60);

                    Button calculateBtn = new Button("Рассчитать смету");
                    calculateBtn.setOnAction(ev -> {
                        BoardParameters board = boardParametersSupplier.get();
                        if (board == null) {
                            new Alert(Alert.AlertType.WARNING, "Сначала загрузите Gerber-файл").show();
                            return;
                        }
                        CalculationRequest request = new CalculationRequest();
                        request.setWidth(board.getWidth());
                        request.setHeight(board.getHeight());
                        request.setCopperLayers(board.getCopperLayersCount());
                        request.setTotalHoles(board.getTotalHoles());
                        request.setMinTrackWidth(board.getMinTrackWidth());
                        request.setMinClearance(board.getMinClearance());
                        request.setMinHoleDiameter(board.getMinHoleDiameter());
                        request.setTariffId(tariff.getId());
                        request.setQuantity(Integer.parseInt(quantityField.getText().trim()));
                        request.setBoardMaterial(materialBox.getValue());
                        request.setSurfaceFinish(finishBox.getValue());
                        request.setExpedited(expeditedCheck.isSelected());

                        new Thread(() -> {
                            try {
                                byte[] pdf = calculationApiService.getReport(request);
                                Platform.runLater(() -> {
                                    FileChooser fileChooser = new FileChooser();
                                    fileChooser.setTitle("Сохранить смету");
                                    fileChooser.getExtensionFilters().add(
                                            new FileChooser.ExtensionFilter("PDF", "*.pdf"));
                                    File file = fileChooser.showSaveDialog(panel.getScene().getWindow());
                                    if (file != null) {
                                        try {
                                            Files.write(file.toPath(), pdf);
                                            new Alert(Alert.AlertType.INFORMATION, "Смета сохранена").show();
                                        } catch (IOException e) {
                                            new Alert(Alert.AlertType.ERROR, "Ошибка сохранения: " + e.getMessage()).show();
                                        }
                                    }
                                });
                            } catch (Exception ex) {
                                Platform.runLater(() ->
                                        new Alert(Alert.AlertType.ERROR, "Ошибка расчёта: " + ex.getMessage()).show());
                            }
                        }).start();
                    });

                    details.getChildren().addAll(
                            new Label("Материал:"), materialBox,
                            new Label("Покрытие:"), finishBox,
                            expeditedCheck,
                            new Label("Количество:"), quantityField,
                            calculateBtn
                    );

                    TitledPane titledPane = new TitledPane(
                            tariff.getName() + " (от " + tariff.getBasePricePerCm2() + " руб/см²)",
                            details
                    );
                    titledPane.setAnimated(true);
                    titledPane.setExpanded(false);
                    card.getChildren().add(titledPane);
                }
            } else {
                card.getChildren().add(new Label("Нет активных тарифов"));
            }

            producersList.getChildren().add(card);
        }
        producersLoaded = true;
    }
}