package com.poker.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Nội dung hộp thoại lỗi; chữ luôn là JavaFX text để không bị lỗi font trong asset. */
public final class ErrorDialogView extends StackPane {
    private final Label messageLabel = new Label();

    public ErrorDialogView(String message, Runnable onClose) {
        setPrefSize(620, 320);
        setMinSize(620, 320);
        setMaxSize(620, 320);
        setStyle("-fx-background-color: transparent;");

        Image artwork = AssetLoader.getErrorFrameImage();
        if (artwork != null && !artwork.isError()) {
            ImageView frame = new ImageView(artwork);
            frame.setFitWidth(620);
            frame.setFitHeight(320);
            frame.setPreserveRatio(false);
            frame.setMouseTransparent(true);
            getChildren().add(frame);
        } else {
            setStyle(OrnateUi.framedStyle());
        }

        Label title = new Label("THÔNG BÁO LỖI");
        title.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 23px; -fx-font-weight: bold; "
                + "-fx-text-fill: #ffe3a0;");

        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(485);
        messageLabel.setAlignment(Pos.TOP_CENTER);
        messageLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 15px; -fx-text-fill: #f7ebe0;");
        setMessage(message);

        ScrollPane messageArea = new ScrollPane(messageLabel);
        messageArea.setFitToWidth(true);
        messageArea.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        messageArea.setPrefHeight(120);
        messageArea.setStyle("-fx-background: transparent; -fx-background-color: transparent; "
                + "-fx-border-color: transparent;");
        VBox.setVgrow(messageArea, Priority.ALWAYS);

        Button close = new Button("ĐÓNG");
        close.setPrefSize(145, 38);
        close.setDefaultButton(true);
        close.setCancelButton(true);
        OrnateUi.button(close, "button-red.png", 14);
        close.setOnAction(event -> onClose.run());

        VBox content = new VBox(16, title, messageArea, close);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(53, 65, 48, 65));
        getChildren().add(content);
    }

    public void setMessage(String message) {
        messageLabel.setText(message);
    }
}
