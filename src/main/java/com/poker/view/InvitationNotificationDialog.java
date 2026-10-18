package com.poker.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/** Hộp thoại thông báo khi nhận được lời mời tham gia trận đấu. */
public final class InvitationNotificationDialog {

    private InvitationNotificationDialog() { }

    public static void show(Stage owner, String inviterName, String roomName, long roomId,
                            long smallBlind, long bigBlind, long buyIn,
                            Runnable onAccept, Runnable onDecline) {
        Stage modal = new Stage();
        modal.initStyle(StageStyle.UNDECORATED);
        modal.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) modal.initOwner(owner);

        VBox content = new VBox(16);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(24, 30, 24, 30));
        content.setStyle(OrnateUi.framedStyle() + "-fx-border-color: #facc15; -fx-border-width: 2; -fx-border-radius: 12;");
        content.setPrefWidth(420);
        content.setMaxWidth(420);

        Label title = new Label("🃏 LỜI MỜI THAM GIA BÀN CHƠI");
        title.setStyle("-fx-text-fill: #facc15; -fx-font-weight: 900; -fx-font-size: 18px;");

        Label inviterMsg = new Label(inviterName + " mời bạn vào bàn chơi poker!");
        inviterMsg.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 14px; -fx-font-weight: bold;");

        VBox detailsBox = new VBox(6);
        detailsBox.setAlignment(Pos.CENTER);
        detailsBox.setPadding(new Insets(10, 14, 10, 14));
        detailsBox.setStyle("-fx-background-color: rgba(20, 20, 25, 0.75); -fx-background-radius: 8; -fx-border-color: #5b4028; -fx-border-radius: 8;");

        Label roomLabel = new Label("Phòng: " + roomName + " (#" + roomId + ")");
        roomLabel.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 13px; -fx-font-weight: bold;");

        Label stakesLabel = new Label("Blind: $" + smallBlind + " / $" + bigBlind + "  •  Buy-in: $" + String.format("%,d", buyIn));
        stakesLabel.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 12px;");

        detailsBox.getChildren().addAll(roomLabel, stakesLabel);

        HBox buttons = new HBox(16);
        buttons.setAlignment(Pos.CENTER);

        Button btnAccept = new Button("CHẤP NHẬN");
        OrnateUi.button(btnAccept, "button-blue.png", 13);
        btnAccept.setOnAction(e -> {
            modal.close();
            if (onAccept != null) onAccept.run();
        });

        Button btnDecline = new Button("TỪ CHỐI");
        OrnateUi.button(btnDecline, "button-red.png", 13);
        btnDecline.setOnAction(e -> {
            modal.close();
            if (onDecline != null) onDecline.run();
        });

        buttons.getChildren().addAll(btnAccept, btnDecline);
        content.getChildren().addAll(title, inviterMsg, detailsBox, buttons);

        Scene scene = new Scene(content);
        scene.setFill(Color.TRANSPARENT);
        modal.setScene(scene);
        modal.show();
    }
}
