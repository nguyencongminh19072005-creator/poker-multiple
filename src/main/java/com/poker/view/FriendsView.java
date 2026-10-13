package com.poker.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.List;

/** Chỉ hiển thị giao diện bạn bè; không gọi API và không xử lý JSON. */
public final class FriendsView extends BorderPane {
    public record FriendModel(long userId, String displayName, String presence) { }
    public record RequestModel(long requestId, String displayName) { }

    public interface Actions {
        void onBack();
        void onSendRequest(String userId);
        void onRemove(long userId);
        void onAccept(long requestId);
        void onReject(long requestId);
    }

    private final Actions actions;
    private final TextField friendId = new TextField();
    private final VBox friendsList = new VBox(8);
    private final VBox incomingList = new VBox(8);
    private final Label friendCount = heading("Bạn bè đã kết nối");
    private final Label requestCount = heading("Lời mời đang chờ");

    public FriendsView(Actions actions) {
        this.actions = actions;
        Image background = AssetLoader.getLobbyBackgroundImage();
        if (background != null) setBackground(new Background(new BackgroundImage(background,
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER,
                new BackgroundSize(100, 100, true, true, true, true))));

        Button back = new Button("← Về sảnh");
        OrnateUi.button(back, "button-red.png", 13);
        back.setMinSize(140, 42);
        back.setOnAction(event -> actions.onBack());
        Label title = heading("BẠN BÈ");
        title.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:25px;-fx-font-weight:900;"
                + "-fx-text-fill:#ffdc77;");
        HBox top = new HBox(16, back, title);
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(12, 22, 12, 22));
        top.setStyle("-fx-background-color:linear-gradient(to right,#25160f,#58311c,#25160f);"
                + "-fx-border-color:#bc8841;-fx-border-width:0 0 2 0;");
        setTop(top);

        friendId.setPromptText("Nhập ID người chơi (ví dụ: #123)");
        friendId.setPrefHeight(42);
        friendId.setStyle("-fx-background-color:#211710;-fx-text-fill:#fff0d0;"
                + "-fx-prompt-text-fill:#b9a487;-fx-border-color:#a47a44;"
                + "-fx-border-radius:8;-fx-background-radius:8;-fx-padding:8 12;");
        Button add = new Button("Gửi lời mời");
        OrnateUi.button(add, "button-blue.png", 14);
        add.setMinSize(152, 42);
        add.setOnAction(event -> actions.onSendRequest(friendId.getText()));
        friendId.setOnAction(event -> actions.onSendRequest(friendId.getText()));
        HBox inviteForm = new HBox(12, friendId, add);
        inviteForm.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(friendId, Priority.ALWAYS);

        VBox panel = new VBox(18,
                section(heading("THÊM BẠN"), inviteForm),
                section(friendCount, friendsList),
                section(requestCount, incomingList));
        panel.setFillWidth(true);
        panel.setMaxWidth(850);
        panel.setPadding(new Insets(22));
        panel.setStyle(OrnateUi.compactPanelStyle());

        StackPane canvas = new StackPane(panel);
        canvas.setAlignment(Pos.TOP_CENTER);
        canvas.setPadding(new Insets(25, 20, 30, 20));
        ScrollPane scroll = new ScrollPane(canvas);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background:transparent;-fx-background-color:transparent;"
                + "-fx-border-color:transparent;");
        setCenter(scroll);
    }

    private static VBox section(Label label, Region content) {
        VBox section = new VBox(12);
        section.setFillWidth(true);
        section.setPadding(new Insets(16));
        section.setStyle("-fx-background-color:rgba(18,12,10,0.72);"
                + "-fx-background-radius:12;-fx-border-color:#9e7545;"
                + "-fx-border-radius:12;-fx-border-width:1;");
        section.getChildren().addAll(label, content);
        return section;
    }

    public void clearFriendId() { friendId.clear(); }

    public void showData(List<FriendModel> friends, List<RequestModel> requests) {
        friendsList.getChildren().clear();
        incomingList.getChildren().clear();
        friendCount.setText("BẠN BÈ  •  " + friends.size());
        requestCount.setText("LỜI MỜI ĐANG CHỜ  •  " + requests.size());
        if (friends.isEmpty()) friendsList.getChildren().add(emptyState("Chưa có bạn bè"));
        for (FriendModel friend : friends) {
            Label name = message(friend.displayName());
            name.setStyle("-fx-text-fill:#fff0cb;-fx-font-size:15px;-fx-font-weight:900;");
            Label detail = message("ID #" + friend.userId());
            detail.setStyle("-fx-text-fill:#c6ad85;-fx-font-size:12px;");
            VBox identity = new VBox(3, name, detail);
            HBox.setHgrow(identity, Priority.ALWAYS);
            boolean online = "ONLINE".equalsIgnoreCase(friend.presence());
            Label presence = message(online ? "● Online" : "● Offline");
            presence.setStyle("-fx-text-fill:" + (online ? "#56dc8a" : "#aeb3b9")
                    + ";-fx-font-size:12px;-fx-font-weight:bold;");
            Button remove = new Button("Xóa bạn");
            OrnateUi.button(remove, "button-red.png", 12);
            remove.setMinSize(105, 36);
            remove.setOnAction(event -> actions.onRemove(friend.userId()));
            friendsList.getChildren().add(row(identity, presence, remove));
        }
        if (requests.isEmpty()) incomingList.getChildren().add(emptyState("Không có lời mời mới"));
        for (RequestModel request : requests) {
            Label name = message(request.displayName());
            name.setStyle("-fx-text-fill:#fff0cb;-fx-font-size:15px;-fx-font-weight:900;");
            HBox.setHgrow(name, Priority.ALWAYS);
            Button accept = new Button("Chấp nhận");
            Button reject = new Button("Từ chối");
            OrnateUi.button(accept, "button-blue.png", 12);
            OrnateUi.button(reject, "button-red.png", 12);
            accept.setMinSize(112, 36);
            reject.setMinSize(95, 36);
            accept.setOnAction(event -> actions.onAccept(request.requestId()));
            reject.setOnAction(event -> actions.onReject(request.requestId()));
            incomingList.getChildren().add(row(name, accept, reject));
        }
    }

    private static HBox row(javafx.scene.Node... items) {
        HBox row = new HBox(12, items);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMinHeight(58);
        row.setPadding(new Insets(8, 12, 8, 12));
        row.setStyle("-fx-background-color:#342219;-fx-background-radius:9;"
                + "-fx-border-color:#6f5135;-fx-border-radius:9;");
        return row;
    }

    private static Label emptyState(String text) {
        Label label = message(text);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER);
        label.setMinHeight(58);
        label.setStyle("-fx-text-fill:#c9b69a;-fx-font-size:13px;"
                + "-fx-background-color:#2d1e17;-fx-background-radius:9;");
        return label;
    }

    private static Label heading(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:20px;"
                + "-fx-font-weight:900;-fx-text-fill:#f8d77d;");
        return label;
    }

    private static Label message(String text) {
        Label label = new Label(text);
        label.setTextFill(Color.web("#f5e3bb"));
        return label;
    }
}
