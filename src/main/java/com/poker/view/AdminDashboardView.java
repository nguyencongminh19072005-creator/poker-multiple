package com.poker.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class AdminDashboardView extends BorderPane {
    private final java.util.Map<String, Label> overviewValues = new java.util.HashMap<>();
    private TableView<UserModel> onlineUsers;
    private VBox onlineRooms;
    private final boolean onlineMode;
    private final AdminActions actions;

    public interface AdminActions {
        void onBackToLobby();
        default void onToggleAccount(UserModel user) { }
    }

    public AdminDashboardView(AdminActions actions) {
        this(actions, false);
    }

    public AdminDashboardView(AdminActions actions, boolean onlineMode) {
        this.actions = actions;
        this.onlineMode = onlineMode;
        // Background
        Image bgImg = AssetLoader.getLobbyBackgroundImage();
        if (bgImg != null) {
            BackgroundImage myBI = new BackgroundImage(bgImg,
                    BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER,
                    new BackgroundSize(100, 100, true, true, true, true));
            this.setBackground(new Background(myBI));
        } else {
            this.setStyle("-fx-background-color: #07090e;");
        }

        // Top Header
        HBox topBar = new HBox(20);
        topBar.setPadding(new Insets(15, 30, 15, 30));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: linear-gradient(to bottom, #7c101b, #3a090d); " +
                "-fx-border-color: #bd8d4a; -fx-border-width: 0 0 2 0;");

        Button btnBack = new Button("⬅️ Back to Lobby");
        if (onlineMode) btnBack.setText("Đăng xuất");
        btnBack.setStyle("-fx-background-color: #58371f; -fx-text-fill: #fff0c2; " +
                "-fx-border-color: #c69c5e; -fx-border-radius: 7; -fx-background-radius: 7; " +
                "-fx-font-weight: bold; -fx-cursor: hand;");
        btnBack.setOnAction(e -> actions.onBackToLobby());

        Label title = new Label("🛠️ SYSTEM ADMIN DASHBOARD");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        title.setTextFill(Color.web("#f4d88e"));

        topBar.getChildren().addAll(btnBack, title);
        this.setTop(topBar);

        // Center Content
        VBox container = new VBox(20);
        container.setPadding(new Insets(25, 40, 25, 40));

        // System Overview Cards
        HBox statsCards = new HBox(20);
        statsCards.setAlignment(Pos.CENTER);
        statsCards.getChildren().addAll(
                createCard("Total Users", onlineMode ? "—" : "1,248", "#b88b4d"),
                createCard(onlineMode ? "Active Users" : "Online Users", onlineMode ? "—" : "84", "#4a9a60"),
                createCard("Active Rooms", onlineMode ? "—" : "12", "#d3a04d"),
                createCard("System Chips", onlineMode ? "—" : "$85.4M", "#a96c72")
        );

        TabPane tabPane = new TabPane();
        tabPane.setStyle("-fx-background-color: rgba(20, 28, 40, 0.9);");

        // Tab 1: User Management
        Tab userTab = new Tab("👥 User Management");
        userTab.setClosable(false);
        VBox userBox = createUserManagementTab();
        userTab.setContent(userBox);

        // Tab 2: Room & Game Monitoring
        Tab roomTab = new Tab("🃏 Active Games & Rooms");
        roomTab.setClosable(false);
        VBox roomBox = createRoomMonitoringTab();
        roomTab.setContent(roomBox);

        tabPane.getTabs().addAll(userTab, roomTab);

        container.getChildren().addAll(statsCards, tabPane);
        this.setCenter(container);
    }

    @SuppressWarnings("deprecation")
    private VBox createUserManagementTab() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(20));

        TableView<UserModel> userTable = new TableView<>();
        onlineUsers = userTable;
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<UserModel, String> colId = new TableColumn<>("User ID");
        colId.setCellValueFactory(data -> data.getValue().idProperty());

        TableColumn<UserModel, String> colName = new TableColumn<>("Username");
        colName.setCellValueFactory(data -> data.getValue().nameProperty());

        TableColumn<UserModel, String> colChips = new TableColumn<>("Total Chips");
        colChips.setCellValueFactory(data -> data.getValue().chipsProperty());

        TableColumn<UserModel, String> colStatus = new TableColumn<>("Account Status");
        colStatus.setCellValueFactory(data -> data.getValue().statusProperty());

        TableColumn<UserModel, Void> colAction = new TableColumn<>("Admin Action");
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnToggle = new Button("Toggle Lock");
            {
                btnToggle.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
                btnToggle.setOnAction(e -> {
                    UserModel user = getTableView().getItems().get(getIndex());
                    if (onlineMode) { actions.onToggleAccount(user); return; }
                    if ("Active".equals(user.statusProperty().get())) {
                        user.statusProperty().set("Banned");
                    } else {
                        user.statusProperty().set("Active");
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
                    UserModel user = getTableView().getItems().get(getIndex());
                    btnToggle.setText("LOCKED".equals(user.statusProperty().get()) ? "Mở khóa" : "Khóa");
                    setGraphic(btnToggle);
                }
            }
        });

        userTable.getColumns().add(colId);
        userTable.getColumns().add(colName);
        userTable.getColumns().add(colChips);
        userTable.getColumns().add(colStatus);
        userTable.getColumns().add(colAction);

        if (!onlineMode) {
            userTable.getItems().add(new UserModel("#U-1001", "PokerKing_99", "$4,250,000", "Active"));
            userTable.getItems().add(new UserModel("#U-1002", "Cheater_X", "$0", "Banned"));
            userTable.getItems().add(new UserModel("#U-1003", "LuckyQueen", "$2,950,000", "Active"));
            userTable.getItems().add(new UserModel("#U-1004", "SpamBot_01", "$10,000", "Banned"));
        }

        box.getChildren().add(userTable);
        return box;
    }

    private VBox createRoomMonitoringTab() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(20));

        Label label = new Label("Live Rooms Monitored Real-time");
        label.setTextFill(Color.WHITE);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        onlineRooms = new VBox(8);
        box.getChildren().addAll(label, onlineRooms);
        return box;
    }

    private VBox createCard(String title, String val, String colorHex) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(15, 25, 15, 25));
        card.setMinWidth(160);
        card.setStyle("-fx-background-color: rgba(20, 28, 45, 0.95); -fx-border-color: " + colorHex + "; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label valLbl = new Label(val);
        overviewValues.put(title, valLbl);
        valLbl.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        valLbl.setTextFill(Color.web(colorHex));

        Label titleLbl = new Label(title);
        titleLbl.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        titleLbl.setTextFill(Color.LIGHTGRAY);

        card.getChildren().addAll(valLbl, titleLbl);
        return card;
    }

    public void showOnlineData(java.util.Map<String, String> overview, java.util.List<UserModel> users,
                               java.util.List<String> rooms) {
        if (!onlineMode) return;
        overview.forEach((key, value) -> {
            Label target = overviewValues.get(key);
            if (target != null) target.setText(value);
        });
        onlineUsers.getItems().setAll(users);
        onlineRooms.getChildren().clear();
        for (String room : rooms) {
            Label row = new Label(room);
            row.setTextFill(Color.WHITE);
            row.setStyle("-fx-padding: 8 14; -fx-background-color: #3b281d; -fx-border-color: #9d7844;");
            onlineRooms.getChildren().add(row);
        }
    }

    public static class UserModel {
        private final javafx.beans.property.StringProperty id;
        private final javafx.beans.property.StringProperty name;
        private final javafx.beans.property.StringProperty chips;
        private final javafx.beans.property.StringProperty status;

        public UserModel(String id, String name, String chips, String status) {
            this.id = new javafx.beans.property.SimpleStringProperty(id);
            this.name = new javafx.beans.property.SimpleStringProperty(name);
            this.chips = new javafx.beans.property.SimpleStringProperty(chips);
            this.status = new javafx.beans.property.SimpleStringProperty(status);
        }

        public javafx.beans.property.StringProperty idProperty() { return id; }
        public javafx.beans.property.StringProperty nameProperty() { return name; }
        public javafx.beans.property.StringProperty chipsProperty() { return chips; }
        public javafx.beans.property.StringProperty statusProperty() { return status; }
    }
}
