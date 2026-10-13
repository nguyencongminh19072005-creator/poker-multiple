package com.poker.view;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class HeaderNode extends HBox {
    public interface HeaderActions {
        void onResetGame();
        void onSwitchViewpoint(int seatIndex);
        void onLeaveRoom();
    }

    private final Label titleLabel = new Label("♠ OPENDECKS TEXAS HOLD'EM POKER ♠");
    private final Label infoLabel = new Label("Blinds: $10/$20 | Hand #0");
    private final Button resetBtn = new Button("RESET CHIPS");
    private final Button leaveBtn = new Button("RỜI PHÒNG");
    private final ComboBox<String> viewSelector = new ComboBox<>();

    public HeaderNode(HeaderActions actions) {
        this.setMinHeight(66);
        this.setSpacing(18);
        this.setAlignment(Pos.CENTER_LEFT);
        this.setStyle("-fx-background-color: linear-gradient(to right, rgba(7,13,22,0.98), rgba(18,31,42,0.96), rgba(7,13,22,0.98)); " +
                "-fx-padding: 10 24; -fx-border-color: rgba(238,190,74,0.65); -fx-border-width: 0 0 1.5 0; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.72), 14, 0.15, 0, 4);");

        titleLabel.setStyle("-fx-text-fill: linear-gradient(to bottom, #fff1a8, #e5b944); -fx-font-weight: 900; -fx-font-size: 17px; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(229,185,68,0.28), 8, 0.35, 0, 1);");

        infoLabel.setStyle("-fx-background-color: rgba(255,255,255,0.07); -fx-text-fill: #dce8ec; -fx-font-size: 12px; " +
                "-fx-font-weight: bold; -fx-padding: 7 12; -fx-background-radius: 14; " +
                "-fx-border-color: rgba(255,255,255,0.09); -fx-border-radius: 14;");

        // Viewpoint Selector Dropdown
        viewSelector.getItems().addAll(
                "🔍 View: Seat 0 (AHNKCO)",
                "🔍 View: Seat 1 (Boss lang)",
                "🔍 View: Seat 2 (Mark92)",
                "🔍 View: Seat 3 (Fishi shsi)",
                "🔍 View: Seat 4 (3060ti)",
                "🔍 View: Seat 5 (rappdo11)"
        );
        viewSelector.getSelectionModel().select(0);
        viewSelector.setStyle("-fx-background-color: rgba(27, 44, 57, 0.96); -fx-mark-color: #f1cc68; -fx-font-weight: bold; " +
                "-fx-font-size: 11px; -fx-background-radius: 9; -fx-border-color: rgba(241,204,104,0.3); " +
                "-fx-border-radius: 9; -fx-cursor: hand;");

        viewSelector.setOnAction(e -> {
            int selectedSeat = viewSelector.getSelectionModel().getSelectedIndex();
            if (actions != null && selectedSeat >= 0) {
                actions.onSwitchViewpoint(selectedSeat);
            }
        });

        styleHeaderButton(resetBtn, "#c0392b");
        styleHeaderButton(leaveBtn, "#e67e22");

        resetBtn.setOnAction(e -> {
            if (actions != null) actions.onResetGame();
        });

        leaveBtn.setOnAction(e -> {
            if (actions != null) actions.onLeaveRoom();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        this.getChildren().addAll(leaveBtn, titleLabel, infoLabel, spacer, viewSelector, resetBtn);
    }

    public void updateInfo(int handCount, int smallBlind, int bigBlind) {
        infoLabel.setText("Blinds: $" + smallBlind + "/$" + bigBlind + "  |  Hand #" + handCount);
    }

    /** Online play keeps the classic header; the server owns seats and chips. */
    public void onlineMode() {
        viewSelector.setVisible(false);
        viewSelector.setManaged(false);
        resetBtn.setVisible(false);
        resetBtn.setManaged(false);
    }

    public void updateOnlineInfo(long handCount, long smallBlind, long bigBlind) {
        infoLabel.setText("Blinds: $" + smallBlind + "/$" + bigBlind + "  |  Hand #" + handCount);
    }

    private void styleHeaderButton(Button btn, String bgHex) {
        btn.setStyle("-fx-background-color: " + bgHex + "; -fx-text-fill: white; " +
                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; " +
                "-fx-background-radius: 7; -fx-padding: 7 12;");
    }
}
