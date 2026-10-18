package com.poker.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/** Hộp thoại mời bạn bè vào bàn chơi poker. */
public final class InviteFriendsDialog {

    public record FriendRow(long userId, String displayName, String presenceStatus) { }

    private InviteFriendsDialog() { }

    public static void show(Stage owner, String roomName, long roomId,
                            List<FriendRow> friends, Set<Long> existingMembers,
                            Consumer<Long> onInvite) {
        Stage modal = new Stage();
        modal.initStyle(StageStyle.UNDECORATED);
        modal.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) modal.initOwner(owner);

        VBox content = new VBox(16);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(24, 28, 24, 28));
        content.setStyle(OrnateUi.framedStyle() + "-fx-border-color: #d4af37; -fx-border-width: 2; -fx-border-radius: 12;");
        content.setPrefWidth(460);
        content.setMaxWidth(460);

        Label title = new Label("MỜI BẠN BÈ VÀO BÀN");
        title.setStyle("-fx-text-fill: #facc15; -fx-font-weight: 900; -fx-font-size: 20px;");

        Label subtitle = new Label("Bàn: " + roomName + "  (#" + roomId + ")");
        subtitle.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px; -fx-font-weight: bold;");

        VBox listContainer = new VBox(10);
        listContainer.setPadding(new Insets(4));

        if (friends.isEmpty()) {
            Label empty = new Label("Bạn chưa kết bạn với ai. Hãy thêm bạn từ sảnh chính!");
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px; -fx-font-style: italic;");
            listContainer.getChildren().add(empty);
        } else {
            java.util.List<FriendRow> sortedFriends = new java.util.ArrayList<>(friends);
            sortedFriends.sort((a, b) -> {
                boolean aOnline = "ONLINE".equalsIgnoreCase(a.presenceStatus());
                boolean bOnline = "ONLINE".equalsIgnoreCase(b.presenceStatus());
                if (aOnline != bOnline) {
                    return aOnline ? -1 : 1;
                }
                return a.displayName().compareToIgnoreCase(b.displayName());
            });

            for (FriendRow friend : sortedFriends) {
                boolean isOnline = "ONLINE".equalsIgnoreCase(friend.presenceStatus());

                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(8, 12, 8, 12));
                if (isOnline) {
                    row.setStyle("-fx-background-color: rgba(20, 20, 25, 0.7); -fx-background-radius: 8; -fx-border-color: #4a3424; -fx-border-radius: 8;");
                } else {
                    row.setStyle("-fx-background-color: rgba(16, 16, 20, 0.5); -fx-background-radius: 8; -fx-border-color: #33271d; -fx-border-radius: 8;");
                }

                VBox info = new VBox(2);
                HBox.setHgrow(info, Priority.ALWAYS);

                Label name = new Label(friend.displayName());
                name.setStyle("-fx-text-fill: " + (isOnline ? "#ffffff" : "#94a3b8") + "; -fx-font-size: 14px; -fx-font-weight: bold;");

                Label status = new Label(isOnline ? "● Trực tuyến" : "○ Ngoại tuyến");
                status.setStyle("-fx-text-fill: " + (isOnline ? "#4ade80" : "#64748b") + "; -fx-font-size: 11px;");
                info.getChildren().addAll(name, status);

                Button btnAction = new Button();
                if (existingMembers.contains(friend.userId())) {
                    btnAction.setText("Đã trong bàn");
                    btnAction.setDisable(true);
                    btnAction.setStyle("-fx-background-color: #334155; -fx-text-fill: #94a3b8; -fx-background-radius: 6; -fx-font-size: 12px; -fx-padding: 6 12;");
                } else if (!isOnline) {
                    btnAction.setText("Ngoại tuyến");
                    btnAction.setDisable(true);
                    btnAction.setStyle("-fx-background-color: #27272a; -fx-text-fill: #71717a; -fx-background-radius: 6; -fx-font-size: 12px; -fx-padding: 6 12;");
                } else {
                    btnAction.setText("Mời");
                    OrnateUi.button(btnAction, "button-blue.png", 12);
                    btnAction.setOnAction(e -> {
                        btnAction.setText("Đã gửi lời mời");
                        btnAction.setDisable(true);
                        btnAction.setStyle("-fx-background-color: #065f46; -fx-text-fill: #a7f3d0; -fx-background-radius: 6; -fx-font-size: 12px; -fx-padding: 6 12;");
                        onInvite.accept(friend.userId());
                    });
                }

                row.getChildren().addAll(info, btnAction);
                listContainer.getChildren().add(row);
            }
        }

        ScrollPane scroll = new ScrollPane(listContainer);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(260);
        scroll.setMaxHeight(260);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        Button btnClose = new Button("ĐÓNG");
        OrnateUi.button(btnClose, "button-red.png", 13);
        btnClose.setOnAction(e -> modal.close());

        content.getChildren().addAll(title, subtitle, scroll, btnClose);

        Scene scene = new Scene(content);
        scene.setFill(Color.TRANSPARENT);
        modal.setScene(scene);
        modal.show();
    }
}
