package com.poker.view;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.concurrent.CompletableFuture;

public class RegisterView extends BorderPane {

    public interface RegisterActions {
        void onRegisterSuccess(String username);
        void onSwitchToLogin();
    }

    @FunctionalInterface
    public interface OnlineRegister {
        CompletableFuture<Void> register(String username, String email, String password);
    }

    public RegisterView(RegisterActions actionHandler, OnlineRegister onlineRegister) {
        // Set Background
        Image bgImg = AssetLoader.getAuthBackgroundImage();
        if (bgImg != null) {
            BackgroundImage myBI = new BackgroundImage(bgImg,
                    BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER,
                    new BackgroundSize(100, 100, true, true, true, true));
            this.setBackground(new Background(myBI));
        } else {
            this.setStyle("-fx-background-color: #07090e;");
        }

        VBox formBox = new VBox(15);
        formBox.setAlignment(Pos.CENTER);
        formBox.setPrefSize(440, 580);
        formBox.setMaxSize(440, 580);
        formBox.setPadding(new Insets(42, 52, 42, 52));
        formBox.setStyle(OrnateUi.framedStyle() +
                " -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.70), 22, 0, 0, 8);");

        Label titleLabel = new Label("CREATE ACCOUNT");
        titleLabel.setTextFill(Color.web("#f5d77c"));
        titleLabel.setFont(Font.font("Georgia", FontWeight.BOLD, 21));
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setAlignment(Pos.CENTER);
        Label brandLabel = new Label("♠  POKER  ♠");
        brandLabel.setStyle("-fx-text-fill: #ffdf8d; -fx-font-family: Georgia; -fx-font-size: 39px; " +
                "-fx-font-weight: 900; -fx-effect: dropshadow(three-pass-box, #4d0b0f, 6, 0, 0, 3);");
        brandLabel.setMaxWidth(Double.MAX_VALUE);
        brandLabel.setAlignment(Pos.CENTER);
        Label brandSubtitle = new Label("WORLD CHAMPIONS");
        brandSubtitle.setStyle("-fx-text-fill: #dfb874; -fx-font-size: 11px; -fx-font-weight: bold;");
        brandSubtitle.setMaxWidth(Double.MAX_VALUE);
        brandSubtitle.setAlignment(Pos.CENTER);

        Label errorLabel = new Label();
        errorLabel.setTextFill(Color.RED);
        errorLabel.setVisible(false);
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(Double.MAX_VALUE);
        errorLabel.setAlignment(Pos.CENTER);

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setPrefHeight(46);
        usernameField.setMaxWidth(Double.MAX_VALUE);
        usernameField.setStyle(authFieldStyle());

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setPrefHeight(46);
        passwordField.setMaxWidth(Double.MAX_VALUE);
        passwordField.setStyle(authFieldStyle());

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm Password");
        confirmPasswordField.setPrefHeight(46);
        confirmPasswordField.setMaxWidth(Double.MAX_VALUE);
        confirmPasswordField.setStyle(authFieldStyle());

        Button registerButton = new Button("REGISTER");
        registerButton.setPrefHeight(48);
        registerButton.setMaxWidth(Double.MAX_VALUE);
        OrnateUi.button(registerButton, "button-red.png", 15);

        registerButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();
            String confirmPassword = confirmPasswordField.getText();

            if (username.isEmpty() || password.isEmpty()) {
                errorLabel.setText("Please fill all fields!");
                errorLabel.setTextFill(Color.RED);
                errorLabel.setVisible(true);
                return;
            } else if (!password.equals(confirmPassword)) {
                errorLabel.setText("Passwords do not match!");
                errorLabel.setTextFill(Color.RED);
                errorLabel.setVisible(true);
                return;
            } else if (!username.matches("[A-Za-z0-9_]{3,50}")) {
                errorLabel.setText("Username must be 3-50 characters: letters, numbers, or _.");
                errorLabel.setTextFill(Color.RED);
                errorLabel.setVisible(true);
                return;
            } else if (password.length() < 8 || password.length() > 72) {
                errorLabel.setText("Password must be 8-72 characters.");
                errorLabel.setTextFill(Color.RED);
                errorLabel.setVisible(true);
                return;
            }

            registerButton.setDisable(true);
            errorLabel.setText("Registering with Server...");
            errorLabel.setTextFill(Color.YELLOW);
            errorLabel.setVisible(true);

            String email = username.toLowerCase() + "@poker.com";
            onlineRegister.register(username, email, password).whenComplete((ignored, error) -> Platform.runLater(() -> {
                registerButton.setDisable(false);
                if (error == null) actionHandler.onRegisterSuccess(username);
                else {
                    errorLabel.setTextFill(Color.RED);
                    errorLabel.setText("Register error: " + (error.getCause() == null ? error.getMessage() : error.getCause().getMessage()));
                    errorLabel.setVisible(true);
                }
            }));
        });

        Button loginButton = new Button("Already have an account? Login");
        loginButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #b8c8ce; -fx-underline: true; -fx-cursor: hand;");
        loginButton.setOnAction(e -> actionHandler.onSwitchToLogin());

        formBox.getChildren().addAll(brandLabel, brandSubtitle, titleLabel, errorLabel, usernameField, passwordField,
                confirmPasswordField, registerButton, loginButton);

        BorderPane.setAlignment(formBox, Pos.CENTER);
        this.setCenter(formBox);
    }

    private String authFieldStyle() {
        return "-fx-font-size: 14px; -fx-background-color: #2e211a; -fx-text-fill: #f8e8c8; " +
                "-fx-prompt-text-fill: #b8a58a; -fx-padding: 0 14; -fx-background-radius: 7; " +
                "-fx-border-color: #987549; -fx-border-radius: 7;";
    }
}
