package com.poker.view;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.image.Image;
import javafx.geometry.Insets;

public class HistoryPanelNode extends VBox {
    private final VBox logContent = new VBox(4);
    private final ScrollPane scrollPane = new ScrollPane();

    public HistoryPanelNode() {
        this.setPrefWidth(265);
        this.setMinWidth(235);
        this.setPadding(new Insets(14, 12, 14, 12));
        this.setStyle("-fx-background-color: linear-gradient(to bottom, rgba(8,16,24,0.97), rgba(5,11,18,0.98)); " +
                "-fx-border-color: rgba(229,185,68,0.30); -fx-border-width: 0 0 0 1; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.55), 12, 0, -3, 0);");

        Label title = new Label("◉  LIVE GAME LOG");
        title.setMaxWidth(Double.MAX_VALUE);
        title.setStyle("-fx-text-fill: #f0cb68; -fx-font-weight: 900; -fx-font-size: 13px; -fx-padding: 2 0 12 0; " +
                "-fx-border-color: rgba(240,203,104,0.22); -fx-border-width: 0 0 1 0;");
        title.setAlignment(Pos.CENTER);

        logContent.setStyle("-fx-background-color: transparent;");

        scrollPane.setContent(logContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        Label chipsTitle = new Label("POKER CHIPS");
        chipsTitle.setMaxWidth(Double.MAX_VALUE);
        chipsTitle.setAlignment(Pos.CENTER);
        chipsTitle.setStyle("-fx-text-fill: #f0cb68; -fx-font-weight: 900; -fx-font-size: 12px;");

        GridPane chipTray = new GridPane();
        chipTray.setAlignment(Pos.CENTER);
        chipTray.setHgap(13);
        chipTray.setVgap(5);
        chipTray.setStyle("-fx-background-color: rgba(34, 20, 13, 0.45); -fx-padding: 7;");
        int[] denominations = {1, 5, 25, 100, 500, 1000};
        for (int i = 0; i < denominations.length; i++) {
            int value = denominations[i];
            ImageView chip = new ImageView(AssetLoader.getChipImageForValue(value));
            chip.setFitWidth(43);
            chip.setFitHeight(43);
            chip.setPreserveRatio(true);
            Label amount = new Label(value == 1000 ? "1K" : Integer.toString(value));
            amount.setStyle("-fx-text-fill: #fff0ba; -fx-font-size: 10px; -fx-font-weight: bold;");
            VBox cell = new VBox(1, chip, amount);
            cell.setAlignment(Pos.CENTER);
            chipTray.add(cell, i % 3, i / 3);
        }

        this.getChildren().addAll(title, chipsTitle, chipTray, scrollPane);
        VBox.setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);
    }

    public void addLog(String text, String category) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);

        String style = "-fx-font-size: 11px; -fx-padding: 7 9; -fx-background-color: rgba(255,255,255,0.035); " +
                "-fx-background-radius: 7; -fx-border-color: transparent transparent transparent rgba(255,255,255,0.12); " +
                "-fx-border-width: 0 0 0 2;";
        switch (category.toLowerCase()) {
            case "system":
            case "phase":
                style += " -fx-text-fill: #f39c12; -fx-font-weight: bold;";
                break;
            case "win":
                style += " -fx-text-fill: #2ecc71; -fx-font-weight: bold; -fx-background-color: rgba(46, 204, 113, 0.15); -fx-background-radius: 4;";
                break;
            case "raise":
                style += " -fx-text-fill: #e74c3c; -fx-font-weight: bold;";
                break;
            case "call":
                style += " -fx-text-fill: #3498db;";
                break;
            case "fold":
                style += " -fx-text-fill: #7f8c8d;";
                break;
            case "blind":
                style += " -fx-text-fill: #9b59b6;";
                break;
            default:
                style += " -fx-text-fill: #ecf0f1;";
                break;
        }

        label.setStyle(style);
        logContent.getChildren().add(label);

        // Auto scroll to bottom
        scrollPane.setVvalue(1.0);
    }

    public void clearLog() {
        logContent.getChildren().clear();
    }
}
