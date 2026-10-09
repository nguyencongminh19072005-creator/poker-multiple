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
import java.util.function.BiFunction;

public class LoginView extends BorderPane {

    public interface LoginActions {
        void onLoginSuccess(String username);
        void onSwitchToRegister();
    }

    public LoginView(LoginActions actionHandler,
                     BiFunction<String, String, CompletableFuture<String>> onlineLogin) {
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

        VBox formBox = new VBox(16);
        formBox.setAlignment(Pos.CENTER);
        formBox.setPrefSize(440, 520);
        formBox.setMaxSize(440, 520);
        formBox.setPadding(new Insets(42, 52, 42, 52));
        formBox.setStyle(OrnateUi.framedStyle() +
                " -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.70), 22, 0, 0, 8);");

        Label titleLabel = new Label("WELCOME BACK");
        titleLabel.setTextFill(Color.web("#f5d77c"));
        titleLabel.setFont(Font.font("Georgia", FontWeight.BOLD, 21));
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setAlignment(Pos.CENTER);
        Label brandLabel = new Label("♠  POKER  ♠");
        brandLabel.setStyle("-fx-text-fill: #ffdf8d; -fx-font-family: Georgia; -fx-font-size: 42px; " +
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
        usernameField.setPrefHeight(48);
        usernameField.setMaxWidth(Double.MAX_VALUE);
        usernameField.setStyle(authFieldStyle());

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setPrefHeight(48);
        passwordField.setMaxWidth(Double.MAX_VALUE);
        passwordField.setStyle(authFieldStyle());

        Button loginButton = new Button("LOGIN");
        loginButton.setPrefHeight(48);
        loginButton.setMaxWidth(Double.MAX_VALUE);
        OrnateUi.button(loginButton, "button-red.png", 15);

        loginButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();

            if (username.isEmpty() || password.isEmpty()) {
                errorLabel.setText("Please enter username & password");
                errorLabel.setTextFill(Color.RED);
                errorLabel.setVisible(true);
                return;
            }

            loginButton.setDisable(true);
            errorLabel.setText("Authenticating with Server...");
            errorLabel.setTextFill(Color.YELLOW);
            errorLabel.setVisible(true);

            onlineLogin.apply(username, password).whenComplete((loggedInUser, error) -> Platform.runLater(() -> {
                loginButton.setDisable(false);
                if (error == null) actionHandler.onLoginSuccess(loggedInUser);
                else {
                    errorLabel.setTextFill(Color.RED);
                    errorLabel.setText("Login error: " + (error.getCause() == null ? error.getMessage() : error.getCause().getMessage()));
                    errorLabel.setVisible(true);
                }
            }));
        });

        Button registerButton = new Button("Don't have an account? Register");
        registerButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #b8c8ce; -fx-underline: true; -fx-cursor: hand;");
        registerButton.setOnAction(e -> actionHandler.onSwitchToRegister());

        formBox.getChildren().addAll(brandLabel, brandSubtitle, titleLabel, errorLabel, usernameField, passwordField,
                loginButton, registerButton);

        BorderPane.setAlignment(formBox, Pos.CENTER);
        this.setCenter(formBox);
    }

    private String authFieldStyle() {
        return "-fx-font-size: 14px; -fx-background-color: #2e211a; -fx-text-fill: #f8e8c8; " +
                "-fx-prompt-text-fill: #b8a58a; -fx-padding: 0 14; -fx-background-radius: 7; " +
                "-fx-border-color: #987549; -fx-border-radius: 7;";
    }
}
