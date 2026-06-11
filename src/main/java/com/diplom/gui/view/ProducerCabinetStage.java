package com.diplom.gui.view;

import com.diplom.api.dto.*;
import com.diplom.gui.service.ProducerApiService;
import com.diplom.gui.service.TariffApiService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProducerCabinetStage extends Stage {

    private final AuthResponse authData;
    private final ProducerApiService producerApiService;
    private final TariffApiService tariffApiService;

    // Профиль
    private TextField nameField, contactField, emailField, phoneField;
    private PasswordField passwordField;
    private TextField passwordTextField;
    private CheckBox activeCheckBox;
    private Label profileStatusLabel;

    // Тарифы
    private VBox tariffsList;
    private List<TariffResponseDto> currentTariffs = new ArrayList<>();

    public ProducerCabinetStage(AuthResponse authData, ProducerApiService producerApiService,
                                TariffApiService tariffApiService) {
        this.authData = authData;
        this.producerApiService = producerApiService;
        this.tariffApiService = tariffApiService;
        setTitle("Личный кабинет");
        buildUI();
        loadProfile();
        loadTariffs();
    }

    private void buildUI() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        Label title = new Label("Личный кабинет");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // --- Профиль ---
        VBox profileBox = new VBox(5);
        profileBox.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 10; -fx-border-color: #ccc; -fx-border-radius: 4;");

        Label profileTitle = new Label("Личная информация");
        profileTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        nameField = new TextField(); nameField.setPromptText("Название организации");
        contactField = new TextField(); contactField.setPromptText("Контактное лицо");
        emailField = new TextField(); emailField.setPromptText("Email");
        phoneField = new TextField(); phoneField.setPromptText("Телефон");

        passwordField = new PasswordField();
        passwordField.setPromptText("Пароль (оставьте пустым, чтобы не менять)");
        passwordTextField = new TextField();
        passwordTextField.setPromptText("Пароль (оставьте пустым, чтобы не менять)");
        passwordTextField.setVisible(false);
        Button togglePasswordBtn = new Button("👁");
        togglePasswordBtn.setOnAction(e -> {
            boolean show = passwordTextField.isVisible();
            passwordField.setVisible(show);
            passwordTextField.setVisible(!show);
            if (!show) passwordTextField.setText(passwordField.getText());
            else passwordField.setText(passwordTextField.getText());
        });
        StackPane passwordStack = new StackPane(passwordField, passwordTextField);
        HBox passwordBox = new HBox(5, passwordStack, togglePasswordBtn);
        HBox.setHgrow(passwordStack, Priority.ALWAYS);

        activeCheckBox = new CheckBox("Активно");

        Button applyProfileBtn = new Button("Применить изменения");
        applyProfileBtn.setOnAction(e -> applyProfileChanges());
        profileStatusLabel = new Label();

        profileBox.getChildren().addAll(
                profileTitle,
                new Label("Название организации:"), nameField,
                new Label("Контактное лицо:"), contactField,
                new Label("Email:"), emailField,
                new Label("Телефон:"), phoneField,
                new Label("Пароль:"), passwordBox,
                activeCheckBox,
                applyProfileBtn,
                profileStatusLabel
        );

        // --- Тарифы ---
        VBox tariffBox = new VBox(8);
        tariffBox.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 10; -fx-border-color: #ccc; -fx-border-radius: 4;");

        Label tariffsTitle = new Label("Тарифы");
        tariffsTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Button createTariffBtn = new Button("Создать тариф");
        createTariffBtn.setOnAction(e -> showCreateTariffForm());

        tariffsList = new VBox(5);
        ScrollPane tariffScroll = new ScrollPane(tariffsList);
        tariffScroll.setFitToWidth(true);
        tariffScroll.setPrefViewportHeight(300);

        tariffBox.getChildren().addAll(tariffsTitle, createTariffBtn, tariffScroll);

        root.getChildren().addAll(title, profileBox, tariffBox);
        setScene(new Scene(root, 500, 800));
    }

    // ---------------------- Профиль ----------------------
    private void loadProfile() {
        new Thread(() -> {
            try {
                ProducerProfileDto profile = producerApiService.getMyProfile(authData.getToken());
                Platform.runLater(() -> {
                    nameField.setText(profile.getName());
                    contactField.setText(profile.getContactPerson());
                    emailField.setText(profile.getEmail());
                    phoneField.setText(profile.getPhone());
                    activeCheckBox.setSelected(profile.getIsActive() != null && profile.getIsActive());
                });
            } catch (Exception ex) {
                Platform.runLater(() -> showProfileError("Ошибка загрузки профиля: " + ex.getMessage()));
            }
        }).start();
    }

    private void applyProfileChanges() {
        String name = nameField.getText().trim();
        String contact = contactField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        Boolean isActive = activeCheckBox.isSelected();
        String newPassword = passwordTextField.isVisible() ?
                passwordTextField.getText().trim() : passwordField.getText().trim();

        if (name.isEmpty() || email.isEmpty()) {
            showProfileError("Название и Email обязательны");
            return;
        }

        UpdateProducerRequest request = new UpdateProducerRequest();
        request.setName(name);
        request.setContactPerson(contact);
        request.setEmail(email);
        request.setPhone(phone);
        if (!newPassword.isEmpty()) request.setPassword(newPassword);
        request.setIsActive(isActive);

        new Thread(() -> {
            try {
                ProducerProfileDto updated = producerApiService.updateMyProfile(authData.getToken(), request);
                Platform.runLater(() -> {
                    nameField.setText(updated.getName());
                    contactField.setText(updated.getContactPerson());
                    emailField.setText(updated.getEmail());
                    phoneField.setText(updated.getPhone());
                    activeCheckBox.setSelected(updated.getIsActive() != null && updated.getIsActive());
                    showProfileSuccess("Профиль обновлён");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> showProfileError("Ошибка обновления профиля: " + ex.getMessage()));
            }
        }).start();
    }

    private void showProfileError(String msg) {
        profileStatusLabel.setText(msg);
        profileStatusLabel.setStyle("-fx-text-fill: red;");
    }

    private void showProfileSuccess(String msg) {
        profileStatusLabel.setText(msg);
        profileStatusLabel.setStyle("-fx-text-fill: green;");
    }

    // ---------------------- Тарифы ----------------------
    private void loadTariffs() {
        new Thread(() -> {
            try {
                List<TariffResponseDto> tariffs = tariffApiService.getMyTariffs(authData.getToken());
                Platform.runLater(() -> displayTariffs(tariffs));
            } catch (Exception ex) {
                Platform.runLater(() -> showProfileError("Ошибка загрузки тарифов: " + ex.getMessage()));
            }
        }).start();
    }

    private void displayTariffs(List<TariffResponseDto> tariffs) {
        currentTariffs = tariffs;
        tariffsList.getChildren().clear();
        for (TariffResponseDto tariff : tariffs) {
            tariffsList.getChildren().add(createTariffPane(tariff));
        }
    }

    private TitledPane createTariffPane(TariffResponseDto tariff) {
        VBox content = buildTariffForm(tariff);
        TitledPane pane = new TitledPane(tariff.getName() + " (" + tariff.getBasePricePerCm2() + " руб/см²)", content);
        pane.setAnimated(true);
        pane.setExpanded(false);
        return pane;
    }

    private VBox buildTariffForm(TariffResponseDto tariff) {
        VBox form = new VBox(5);
        form.setPadding(new Insets(5));

        TextField nameField = new TextField(tariff.getName());
        nameField.setPromptText("Название тарифа");

        TextField basePriceField = new TextField(tariff.getBasePricePerCm2().toString());
        basePriceField.setPromptText("Цена за см²");

        TextField layerPriceField = new TextField(tariff.getExtraLayerPrice().toString());
        layerPriceField.setPromptText("Цена за слой");

        TextField expeditedField = new TextField(tariff.getExpeditedFee().toString());
        expeditedField.setPromptText("Срочность");

        TextField setupField = new TextField(tariff.getSetupCost().toString());
        setupField.setPromptText("Подготовка");

        TextField minQtyField = new TextField(String.valueOf(tariff.getMinQuantity()));
        minQtyField.setPromptText("Мин. тираж");

        CheckBox activeCheckBox = new CheckBox("Активен");
        activeCheckBox.setSelected(tariff.getIsActive() != null && tariff.getIsActive());

        // Материалы и покрытия
        TextField materialsField = new TextField(
                tariff.getSupportedMaterials() != null ? String.join(", ", tariff.getSupportedMaterials()) : ""
        );
        materialsField.setPromptText("Материалы с толщиной (через запятую, например: FR-4 1.6mm, Алюминий 2.0mm)");

        TextField finishesField = new TextField(
                tariff.getSupportedFinishes() != null ? String.join(", ", tariff.getSupportedFinishes()) : ""
        );
        finishesField.setPromptText("Покрытия (через запятую, например: HASL, ENIG)");

        // Ограничения
        Label limitsHeader = new Label("Ограничения:");
        limitsHeader.setStyle("-fx-font-weight: bold;");

        TextField minTrackField = new TextField(tariff.getMinTrackWidthMm().toString());
        minTrackField.setPromptText("Мин. дорожка (мм)");

        TextField minClearField = new TextField(tariff.getMinClearanceMm().toString());
        minClearField.setPromptText("Мин. зазор (мм)");

        TextField minHoleField = new TextField(tariff.getMinHoleDiameterMm().toString());
        minHoleField.setPromptText("Мин. отверстие (мм)");

        TextField maxLayersField = new TextField(String.valueOf(tariff.getMaxLayers()));
        maxLayersField.setPromptText("Макс. слоёв");

        CheckBox blindViaCheck = new CheckBox("Глухие отверстия");
        blindViaCheck.setSelected(tariff.getSupportsBlindVia());

        CheckBox buriedViaCheck = new CheckBox("Скрытые отверстия");
        buriedViaCheck.setSelected(tariff.getSupportsBuriedVia());

        Label localStatus = new Label();

        Button saveBtn = new Button("Сохранить изменения");
        saveBtn.setOnAction(e -> saveTariff(tariff.getId(), nameField, basePriceField, layerPriceField,
                expeditedField, setupField, minQtyField, activeCheckBox,
                materialsField, finishesField,
                minTrackField, minClearField, minHoleField, maxLayersField,
                blindViaCheck, buriedViaCheck, localStatus));

        Button deleteBtn = new Button("Удалить");
        deleteBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2 5;");
        deleteBtn.setOnAction(e -> deleteTariff(tariff.getId()));

        HBox buttons = new HBox(10, saveBtn, deleteBtn);

        form.getChildren().addAll(
                new Label("Название:"), nameField,
                new Label("Цена за см²:"), basePriceField,
                new Label("Цена за слой:"), layerPriceField,
                new Label("Срочность:"), expeditedField,
                new Label("Подготовка:"), setupField,
                new Label("Мин. тираж:"), minQtyField,
                activeCheckBox,
                new Label("Материалы:"), materialsField,
                new Label("Покрытия:"), finishesField,
                limitsHeader,
                new Label("Мин. ширина дорожки:"), minTrackField,
                new Label("Мин. зазор:"), minClearField,
                new Label("Мин. диаметр отверстия:"), minHoleField,
                new Label("Макс. слоёв:"), maxLayersField,
                blindViaCheck, buriedViaCheck,
                localStatus,
                buttons
        );
        return form;
    }

    private void showCreateTariffForm() {
        VBox form = new VBox(5);
        form.setPadding(new Insets(5));

        TextField nameField = new TextField(); nameField.setPromptText("Название тарифа");
        TextField basePriceField = new TextField(); basePriceField.setPromptText("Цена за см²");
        TextField layerPriceField = new TextField("0.0"); layerPriceField.setPromptText("Цена за слой");
        TextField expeditedField = new TextField("0.0"); expeditedField.setPromptText("Срочность");
        TextField setupField = new TextField("0.0"); setupField.setPromptText("Подготовка");
        TextField minQtyField = new TextField("1"); minQtyField.setPromptText("Мин. тираж");
        CheckBox activeCheckBox = new CheckBox("Активен");
        activeCheckBox.setSelected(true);

        // Материалы и покрытия
        TextField materialsField = new TextField();
        materialsField.setPromptText("Материалы (через запятую, например: FR-4, Алюминий)");
        TextField finishesField = new TextField();
        finishesField.setPromptText("Покрытия (через запятую, например: HASL, ENIG)");

        // Ограничения
        TextField minTrackField = new TextField(); minTrackField.setPromptText("Мин. дорожка (мм)");
        TextField minClearField = new TextField(); minClearField.setPromptText("Мин. зазор (мм)");
        TextField minHoleField = new TextField(); minHoleField.setPromptText("Мин. отверстие (мм)");
        TextField maxLayersField = new TextField(); maxLayersField.setPromptText("Макс. слоёв");
        CheckBox blindViaCheck = new CheckBox("Глухие отверстия");
        CheckBox buriedViaCheck = new CheckBox("Скрытые отверстия");

        Label localStatus = new Label();

        Button createBtn = new Button("Создать");
        createBtn.setOnAction(e -> createTariff(nameField, basePriceField, layerPriceField,
                expeditedField, setupField, minQtyField, activeCheckBox,
                materialsField, finishesField,
                minTrackField, minClearField, minHoleField, maxLayersField,
                blindViaCheck, buriedViaCheck, localStatus));

        form.getChildren().addAll(
                new Label("Название:"), nameField,
                new Label("Цена за см²:"), basePriceField,
                new Label("Цена за слой:"), layerPriceField,
                new Label("Срочность:"), expeditedField,
                new Label("Подготовка:"), setupField,
                new Label("Мин. тираж:"), minQtyField,
                activeCheckBox,
                new Label("Материалы:"), materialsField,
                new Label("Покрытия:"), finishesField,
                new Label("Ограничения:"),
                new Label("Мин. ширина дорожки:"), minTrackField,
                new Label("Мин. зазор:"), minClearField,
                new Label("Мин. диаметр отверстия:"), minHoleField,
                new Label("Макс. слоёв:"), maxLayersField,
                blindViaCheck, buriedViaCheck,
                localStatus,
                createBtn
        );

        TitledPane newPane = new TitledPane("Новый тариф", form);
        newPane.setExpanded(true);
        tariffsList.getChildren().add(0, newPane);
    }

    private void createTariff(TextField nameField, TextField basePriceField, TextField layerPriceField,
                              TextField expeditedField, TextField setupField, TextField minQtyField,
                              CheckBox activeCheckBox, TextField materialsField, TextField finishesField,
                              TextField minTrackField, TextField minClearField,
                              TextField minHoleField, TextField maxLayersField, CheckBox blindCheck,
                              CheckBox buriedCheck, Label statusLabel) {
        if (!validateTariffFields(nameField, basePriceField, minTrackField, minClearField, minHoleField, maxLayersField, statusLabel)) return;

        try {
            TariffCreateRequest request = new TariffCreateRequest();
            request.setName(nameField.getText().trim());
            request.setBasePricePerCm2(new BigDecimal(basePriceField.getText().trim()));
            request.setExtraLayerPrice(new BigDecimal(layerPriceField.getText().trim()));
            request.setExpeditedFee(new BigDecimal(expeditedField.getText().trim()));
            request.setSetupCost(new BigDecimal(setupField.getText().trim()));
            request.setMinQuantity(Integer.parseInt(minQtyField.getText().trim()));

            // Материалы
            String materials = materialsField.getText().trim();
            request.setSupportedMaterials(materials.isEmpty() ? new String[0] : materials.split("\\s*,\\s*"));

            // Покрытия
            String finishes = finishesField.getText().trim();
            request.setSupportedFinishes(finishes.isEmpty() ? new String[0] : finishes.split("\\s*,\\s*"));

            request.setMinTrackWidthMm(new BigDecimal(minTrackField.getText().trim()));
            request.setMinClearanceMm(new BigDecimal(minClearField.getText().trim()));
            request.setMinHoleDiameterMm(new BigDecimal(minHoleField.getText().trim()));
            request.setMaxLayers(Integer.parseInt(maxLayersField.getText().trim()));
            request.setSupportsBlindVia(blindCheck.isSelected());
            request.setSupportsBuriedVia(buriedCheck.isSelected());
            request.setIsActive(activeCheckBox.isSelected());

            new Thread(() -> {
                try {
                    TariffResponseDto created = tariffApiService.createTariff(authData.getToken(), request);
                    Platform.runLater(() -> loadTariffs());
                } catch (Exception ex) {
                    Platform.runLater(() -> statusLabel.setText("Ошибка: " + ex.getMessage()));
                }
            }).start();
        } catch (NumberFormatException e) {
            statusLabel.setText("Неверный формат чисел");
        }
    }

    private void saveTariff(Integer tariffId, TextField nameField, TextField basePriceField,
                            TextField layerPriceField, TextField expeditedField, TextField setupField,
                            TextField minQtyField, CheckBox activeCheckBox,
                            TextField materialsField, TextField finishesField,
                            TextField minTrackField, TextField minClearField, TextField minHoleField,
                            TextField maxLayersField, CheckBox blindCheck, CheckBox buriedCheck,
                            Label statusLabel) {
        if (!validateTariffFields(nameField, basePriceField, minTrackField, minClearField, minHoleField, maxLayersField, statusLabel)) return;

        try {
            TariffUpdateRequest request = new TariffUpdateRequest();
            request.setName(nameField.getText().trim());
            request.setBasePricePerCm2(new BigDecimal(basePriceField.getText().trim()));
            request.setExtraLayerPrice(new BigDecimal(layerPriceField.getText().trim()));
            request.setExpeditedFee(new BigDecimal(expeditedField.getText().trim()));
            request.setSetupCost(new BigDecimal(setupField.getText().trim()));
            request.setMinQuantity(Integer.parseInt(minQtyField.getText().trim()));
            request.setIsActive(activeCheckBox.isSelected());

            // Материалы
            String materials = materialsField.getText().trim();
            request.setSupportedMaterials(materials.isEmpty() ? new String[0] : materials.split("\\s*,\\s*"));

            // Покрытия
            String finishes = finishesField.getText().trim();
            request.setSupportedFinishes(finishes.isEmpty() ? new String[0] : finishes.split("\\s*,\\s*"));

            request.setMinTrackWidthMm(new BigDecimal(minTrackField.getText().trim()));
            request.setMinClearanceMm(new BigDecimal(minClearField.getText().trim()));
            request.setMinHoleDiameterMm(new BigDecimal(minHoleField.getText().trim()));
            request.setMaxLayers(Integer.parseInt(maxLayersField.getText().trim()));
            request.setSupportsBlindVia(blindCheck.isSelected());
            request.setSupportsBuriedVia(buriedCheck.isSelected());

            new Thread(() -> {
                try {
                    TariffResponseDto updated = tariffApiService.updateTariff(authData.getToken(), tariffId, request);
                    Platform.runLater(() -> loadTariffs());
                } catch (Exception ex) {
                    Platform.runLater(() -> statusLabel.setText("Ошибка: " + ex.getMessage()));
                }
            }).start();
        } catch (NumberFormatException e) {
            statusLabel.setText("Неверный формат чисел");
        }
    }

    private boolean validateTariffFields(TextField nameField, TextField basePriceField,
                                         TextField minTrackField, TextField minClearField,
                                         TextField minHoleField, TextField maxLayersField,
                                         Label statusLabel) {
        if (nameField.getText().trim().isEmpty() ||
                basePriceField.getText().trim().isEmpty() ||
                minTrackField.getText().trim().isEmpty() ||
                minClearField.getText().trim().isEmpty() ||
                minHoleField.getText().trim().isEmpty() ||
                maxLayersField.getText().trim().isEmpty()) {
            statusLabel.setText("Заполните все обязательные поля");
            statusLabel.setStyle("-fx-text-fill: red;");
            return false;
        }
        return true;
    }

    private void deleteTariff(Integer tariffId) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Удалить тариф?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(type -> {
            if (type == ButtonType.YES) {
                new Thread(() -> {
                    try {
                        tariffApiService.deleteTariff(authData.getToken(), tariffId);
                        Platform.runLater(() -> loadTariffs());
                    } catch (Exception ex) {
                        Platform.runLater(() -> showProfileError("Ошибка удаления тарифа: " + ex.getMessage()));
                    }
                }).start();
            }
        });
    }
}