package com.diplom.gui.view;

import com.diplom.api.dto.AuthResponse;
import com.diplom.gui.service.AuthApiService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class LoginStage extends Stage {

    private final AuthApiService authApiService;
    private AuthResponse authResult;
    private boolean registerMode = false;

    private TextField emailField;
    private PasswordField passwordField;
    private TextField passwordTextField;
    private Button togglePasswordBtn;

    private TextField nameField, contactField, phoneField;
    private Label nameLabel, contactLabel, phoneLabel;
    private Button submitBtn;
    private Hyperlink toggleLink;

    public LoginStage(AuthApiService authApiService) {
        this.authApiService = authApiService;
        initModality(Modality.APPLICATION_MODAL);
        buildUI();
    }

    public AuthResponse getAuthResult() { return authResult; }

    private void buildUI() {
        VBox root = new VBox(8);
        root.setPadding(new Insets(15));
        root.setAlignment(Pos.TOP_LEFT);

        // Email
        Label emailLabel = new Label("Email");
        emailField = new TextField();
        emailField.setPromptText("Введите email");

        // Пароль с переключателем
        Label passLabel = new Label("Пароль");
        passwordField = new PasswordField();
        passwordField.setPromptText("Введите пароль");
        passwordTextField = new TextField();
        passwordTextField.setPromptText("Введите пароль");
        passwordTextField.setVisible(false);

        togglePasswordBtn = new Button("👁");
        togglePasswordBtn.setOnAction(e -> togglePasswordVisibility());

        StackPane passwordStack = new StackPane(passwordField, passwordTextField);
        HBox passwordBox = new HBox(5, passwordStack, togglePasswordBtn);
        HBox.setHgrow(passwordStack, Priority.ALWAYS);

        // Регистрационные поля
        nameLabel = new Label("Название организации");
        nameField = new TextField();
        nameField.setPromptText("Введите название организации");
        contactLabel = new Label("Контактное лицо");
        contactField = new TextField();
        contactField.setPromptText("Введите контактное лицо");
        phoneLabel = new Label("Телефон");
        phoneField = new TextField();
        phoneField.setPromptText("Введите телефон");

        // Кнопка отправки
        submitBtn = new Button("Войти");
        submitBtn.setOnAction(e -> handleSubmit());

        // Ссылка переключения
        toggleLink = new Hyperlink("Нет учетной записи? Зарегистрируйтесь");
        toggleLink.setFont(Font.font(11));
        toggleLink.setOnAction(e -> {
            registerMode = !registerMode;
            updateModeUI();
        });

        root.getChildren().addAll(
                emailLabel, emailField,
                passLabel, passwordBox,
                nameLabel, nameField,
                contactLabel, contactField,
                phoneLabel, phoneField,
                submitBtn, toggleLink
        );

        updateModeUI();
        setScene(new Scene(root, 320, 420));
    }

    private void togglePasswordVisibility() {
        boolean show = passwordTextField.isVisible();
        passwordField.setVisible(show);
        passwordTextField.setVisible(!show);
        if (!show) {
            passwordTextField.setText(passwordField.getText());
        } else {
            passwordField.setText(passwordTextField.getText());
        }
        togglePasswordBtn.setText(show ? "👁" : "🚫");
    }

    private void updateModeUI() {
        nameLabel.setVisible(registerMode);
        nameField.setVisible(registerMode);
        contactLabel.setVisible(registerMode);
        contactField.setVisible(registerMode);
        phoneLabel.setVisible(registerMode);
        phoneField.setVisible(registerMode);

        if (registerMode) {
            setTitle("Регистрация");
            submitBtn.setText("Зарегистрироваться");
            toggleLink.setText("Уже зарегистрированы? Авторизуйтесь");
        } else {
            setTitle("Авторизация");
            submitBtn.setText("Войти");
            toggleLink.setText("Нет учетной записи? Зарегистрируйтесь");
        }
    }

    private String getPassword() {
        return passwordTextField.isVisible() ? passwordTextField.getText() : passwordField.getText();
    }

    private void handleSubmit() {
        String email = emailField.getText().trim();
        String password = getPassword().trim();
        if (email.isEmpty() || password.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Заполните Email и Пароль").show();
            return;
        }
        try {
            if (registerMode) {
                String name = nameField.getText().trim();
                String contact = contactField.getText().trim();
                String phone = phoneField.getText().trim();
                if (name.isEmpty() || contact.isEmpty() || phone.isEmpty()) {
                    new Alert(Alert.AlertType.WARNING, "Заполните все поля").show();
                    return;
                }
                authResult = authApiService.register(name, contact, email, phone, password);
            } else {
                authResult = authApiService.login(email, password);
            }
            close();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, ex.getMessage()).show();
        }
    }
}