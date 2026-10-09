package com.poker.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ProfileHistoryView extends BorderPane {

    public interface ProfileActions {
        void onBackToLobby();
        void onLogout();
    }

    private String currentUsername;
    private final Long userId;
    private final ProfileActions actions;
    private final boolean onlineMode;
    private final java.util.function.Consumer<String> onlineNameChange;
    private Label balanceValue;
    private Label rankingValue;
    private final java.util.Map<String, Label> statisticValues = new java.util.HashMap<>();
    private VBox historyCards;

    public ProfileHistoryView(String username, Long userId, ProfileActions actions) {
        this(username, userId, actions, false, null);
    }

    public ProfileHistoryView(String username, Long userId, ProfileActions actions, boolean onlineMode,
                              java.util.function.Consumer<String> onlineNameChange) {
        this.currentUsername = username;
        this.userId = userId;
        this.actions = actions;
        this.onlineMode = onlineMode;
        this.onlineNameChange = onlineNameChange;

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
        HBox topBar = createTopBar();
        this.setTop(topBar);

        // Main Content Container: HBox split into Left Sidebar and Right Main Area
        HBox mainLayout = new HBox(30);
        mainLayout.setPadding(new Insets(30, 45, 30, 45));

        // --- LEFT SIDEBAR (Avatar, Username, ID, Edit Profile Button, Logout Button) ---
        VBox leftSidebar = createLeftSidebar();

        // --- RIGHT MAIN AREA (Assets, Stats Grid, Match History Table) ---
        VBox rightArea = createRightArea();
        HBox.setHgrow(rightArea, Priority.ALWAYS);

        mainLayout.getChildren().addAll(leftSidebar, rightArea);

        ScrollPane scrollPane = new ScrollPane(mainLayout);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        this.setCenter(scrollPane);
    }

    private HBox createTopBar() {
        HBox topBar = new HBox(20);
        topBar.setPadding(new Insets(14, 35, 14, 35));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: linear-gradient(to bottom, #5a3023, #281710); " +
                "-fx-border-color: #c69a55; -fx-border-width: 0 0 2 0;");

        Button btnBack = new Button("◄ Quay lại Sảnh");
        btnBack.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        btnBack.setPrefSize(165, 46);
        OrnateUi.button(btnBack, "button-red.png", 13);
        btnBack.setOnAction(e -> actions.onBackToLobby());

        Label title = new Label("👑 THÔNG TIN CÁ NHÂN & THỐNG KÊ NGƯỜI CHƠI");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        title.setTextFill(Color.web("#facc15"));
        title.setStyle("-fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.9), 6, 0.8, 0, 2);");

        topBar.getChildren().addAll(btnBack, title);
        return topBar;
    }

    private VBox createLeftSidebar() {
        VBox sidebar = new VBox(20);
        sidebar.setPadding(new Insets(10, 10, 10, 10));
        sidebar.setAlignment(Pos.TOP_CENTER);
        sidebar.setPrefWidth(310);
        sidebar.setStyle("-fx-background-color: transparent;");

        String classicCardStyle = OrnateUi.framedStyle();

        // --- TOP FRAME BOX: Avatar & Player Info ---
        VBox avatarFrameBox = new VBox(10);
        avatarFrameBox.setAlignment(Pos.CENTER);
        avatarFrameBox.setPrefWidth(285);
        avatarFrameBox.setPadding(new Insets(22, 20, 22, 20));
        avatarFrameBox.setStyle(classicCardStyle);

        // 1. Avatar with Gold Frame
        int avatarIndex = (Math.abs(currentUsername.hashCode()) % 6) + 1;
        Image rawAvatarImg = AssetLoader.getAvatarImage("avatar_player_" + avatarIndex + ".png");
        Image frameImg = AssetLoader.getAvatarFrameImage();

        StackPane avatarStack = new StackPane();
        avatarStack.setPrefSize(100, 100);

        if (rawAvatarImg != null) {
            ImageView avatarView = new ImageView(rawAvatarImg);
            avatarView.setFitWidth(68);
            avatarView.setFitHeight(68);
            avatarView.setPreserveRatio(true);
            avatarView.setClip(new Circle(34, 34, 34));
            avatarStack.getChildren().add(avatarView);
        }

        if (frameImg != null) {
            ImageView frameView = new ImageView(frameImg);
            frameView.setFitWidth(100);
            frameView.setFitHeight(100);
            frameView.setPreserveRatio(true);
            frameView.setClip(new Circle(50, 50, 50));
            avatarStack.getChildren().add(frameView);
        }

        // Username & Account ID
        Label nameLabel = new Label(currentUsername);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        nameLabel.setTextFill(Color.WHITE);

        Label idLabel = new Label(userId == null ? "User ID: chưa xác định" : "User ID: " + userId);
        idLabel.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        idLabel.setTextFill(Color.web("#f4d28c"));

        Button copyIdButton = new Button("Sao chép User ID để kết bạn");
        copyIdButton.setWrapText(true);
        copyIdButton.setMaxWidth(230);
        copyIdButton.setDisable(userId == null);
        copyIdButton.setOnAction(event -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(String.valueOf(userId));
            Clipboard.getSystemClipboard().setContent(content);
            copyIdButton.setText("Đã sao chép User ID");
        });

        HBox statusBox = new HBox(6);
        statusBox.setAlignment(Pos.CENTER);
        Label statusDot = new Label("🟢");
        Label statusText = new Label("Hoạt động (Normal)");
        statusText.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        statusText.setTextFill(Color.LIGHTGREEN);
        statusBox.getChildren().addAll(statusDot, statusText);

        VBox infoBox = new VBox(4, nameLabel, idLabel, copyIdButton, statusBox);
        infoBox.setAlignment(Pos.CENTER);

        avatarFrameBox.getChildren().addAll(avatarStack, infoBox);

        // --- BOTTOM FRAME BOX: 2 Action Buttons ---
        VBox buttonsFrameBox = new VBox(14);
        buttonsFrameBox.setAlignment(Pos.CENTER);
        buttonsFrameBox.setPrefWidth(285);
        buttonsFrameBox.setPadding(new Insets(22, 20, 22, 20));
        buttonsFrameBox.setStyle(classicCardStyle);

        // Button: Chỉnh sửa thông tin cá nhân
        Button btnEdit = new Button("✏️ Chỉnh sửa thông tin");
        btnEdit.setPrefWidth(210);
        btnEdit.setMaxWidth(210);
        btnEdit.setPrefHeight(38);
        OrnateUi.button(btnEdit, "button-blue.png", 13);
        btnEdit.setOnAction(e -> showEditProfileDialog());

        // Button: Đăng xuất
        Button btnLogout = new Button("🚪 Đăng xuất tài khoản");
        btnLogout.setPrefWidth(210);
        btnLogout.setMaxWidth(210);
        btnLogout.setPrefHeight(38);
        OrnateUi.button(btnLogout, "button-red.png", 13);
        btnLogout.setOnAction(e -> actions.onLogout());

        buttonsFrameBox.getChildren().addAll(btnEdit, btnLogout);

        sidebar.getChildren().addAll(avatarFrameBox, buttonsFrameBox);
        return sidebar;
    }

    private VBox createRightArea() {
        VBox rightArea = new VBox(25);

        // Assets & Rank Banner
        HBox assetBanner = new HBox(20);
        assetBanner.setAlignment(Pos.CENTER_LEFT);

        // Chips HUD Box
        HBox chipBox = new HBox(12);
        chipBox.setAlignment(Pos.CENTER);
        chipBox.setPadding(new Insets(12, 28, 12, 22));
        HBox.setHgrow(chipBox, Priority.ALWAYS);
        chipBox.setStyle(OrnateUi.imageStyle("room-card.png"));

        Image chipImg = AssetLoader.getChipStackImage();
        if (chipImg != null) {
            ImageView chipIcon = new ImageView(chipImg);
            chipIcon.setFitWidth(42);
            chipIcon.setFitHeight(42);
            chipBox.getChildren().add(chipIcon);
        } else {
            Label chipIcon = new Label("💰");
            chipIcon.setFont(Font.font("Arial", 24));
            chipBox.getChildren().add(chipIcon);
        }

        VBox balanceSub = new VBox(2);
        Label balHeader = new Label("TỔNG TÀI SẢN CHIPS");
        balHeader.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        balHeader.setTextFill(Color.web("#eab308"));

        Label balValue = new Label(onlineMode ? "Đang tải..." : "$100.000");
        balanceValue = balValue;
        balValue.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        balValue.setTextFill(Color.GOLD);
        balanceSub.getChildren().addAll(balHeader, balValue);

        chipBox.getChildren().add(balanceSub);

        // Ranking Position Badge
        HBox rankBox = new HBox(12);
        rankBox.setAlignment(Pos.CENTER);
        rankBox.setPadding(new Insets(12, 28, 12, 22));
        HBox.setHgrow(rankBox, Priority.ALWAYS);
        rankBox.setStyle(OrnateUi.imageStyle("room-card.png"));

        Label rankIcon = new Label("🏆");
        rankIcon.setFont(Font.font("Arial", 26));

        VBox rankSub = new VBox(2);
        Label rankHeader = new Label("XẾP HẠNG THÁCH ĐẤU");
        rankHeader.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        rankHeader.setTextFill(Color.web("#ec4899"));

        Label rankValue = new Label(onlineMode ? "Đang tải..." : "Hạng #4 (1,450 pts)");
        rankingValue = rankValue;
        rankValue.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        rankValue.setTextFill(Color.WHITE);
        rankSub.getChildren().addAll(rankHeader, rankValue);

        rankBox.getChildren().addAll(rankIcon, rankSub);

        assetBanner.getChildren().addAll(chipBox, rankBox);

        // Stats Grid Row
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(15);
        statsGrid.setVgap(15);

        VBox card1 = createStatCard("🎮 Tổng số trận", "128", "#3b82f6");
        VBox card2 = createStatCard("🥇 Trận thắng", "68", "#10b981");
        VBox card3 = createStatCard("❌ Trận thua", "60", "#ef4444");
        VBox card4 = createStatCard("📈 Tỷ lệ thắng", "53.1%", "#f59e0b");
        VBox card5 = createStatCard("⏱️ Thời gian chơi", "48h 30m", "#8b5cf6");
        VBox card6 = createStatCard("⭐ Ranking Points", "1,450 pts", "#ec4899");

        statsGrid.add(card1, 0, 0);
        statsGrid.add(card2, 1, 0);
        statsGrid.add(card3, 2, 0);
        statsGrid.add(card4, 0, 1);
        statsGrid.add(card5, 1, 1);
        statsGrid.add(card6, 2, 1);

        for (int i = 0; i < 3; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            cc.setPercentWidth(33.33);
            statsGrid.getColumnConstraints().add(cc);
        }

        // Match History Table Section
        VBox historySection = createMatchHistorySection();

        rightArea.getChildren().addAll(assetBanner, statsGrid, historySection);
        return rightArea;
    }

    private VBox createStatCard(String label, String value, String colorHex) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(18, 20, 18, 20));
        card.setMinWidth(150);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setStyle(OrnateUi.imageStyle("room-card.png"));

        Label valLbl = new Label(value);
        if (onlineMode) {
            valLbl.setText("—");
            statisticValues.put(label, valLbl);
        }
        valLbl.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        valLbl.setTextFill(Color.web(colorHex));

        Label titleLbl = new Label(label);
        titleLbl.setFont(Font.font("Arial", FontWeight.NORMAL, 13));
        titleLbl.setTextFill(Color.LIGHTGRAY);

        card.getChildren().addAll(valLbl, titleLbl);
        return card;
    }

    private VBox createMatchHistorySection() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(22, 25, 22, 25));
        box.setStyle(OrnateUi.framedStyle());

        HBox titleBox = new HBox(10);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Label header = new Label("📜 LỊCH SỬ CÁC TRẬN ĐẤU ĐÃ THAM GIA");
        header.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        header.setTextFill(Color.GOLD);

        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);

        Label filterTag = new Label("5 trận gần nhất");
        filterTag.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        filterTag.setTextFill(Color.LIGHTBLUE);
        filterTag.setStyle("-fx-background-color: rgba(59, 130, 246, 0.2); -fx-padding: 4 10 4 10; -fx-background-radius: 10;");

        titleBox.getChildren().addAll(header, titleSpacer, filterTag);

        VBox cardsList = new VBox(10);
        historyCards = cardsList;

        if (!onlineMode) cardsList.getChildren().addAll(
                createMatchCard("#M-904", "2026-08-27 21:15", "High Rollers VIP", "🥇 1st Place", "24 hands", "+$45,000", true),
                createMatchCard("#M-898", "2026-08-26 19:40", "Beginners Table", "🥈 2nd Place", "18 hands", "+$8,500", true),
                createMatchCard("#M-885", "2026-08-25 15:30", "Private Club #7", "4th Place", "12 hands", "-$12,000", false),
                createMatchCard("#M-870", "2026-08-24 22:05", "Pro Tournament", "🥇 1st Place", "35 hands", "+$95,000", true),
                createMatchCard("#M-856", "2026-08-23 18:20", "High Rollers VIP", "3rd Place", "15 hands", "-$5,000", false)
        );

        box.getChildren().addAll(titleBox, cardsList);
        return box;
    }

    public void showOnlineData(long accountChips, String rank, java.util.Map<String, String> statistics,
                               java.util.List<MatchModel> matches) {
        if (!onlineMode) return;
        balanceValue.setText("$" + String.format("%,d", accountChips));
        rankingValue.setText(rank);
        statistics.forEach((key, value) -> {
            Label label = statisticValues.get(key);
            if (label != null) label.setText(value);
        });
        historyCards.getChildren().clear();
        if (matches.isEmpty()) {
            Label empty = new Label("Chưa có lịch sử trận từ server.");
            empty.setTextFill(Color.WHITE);
            historyCards.getChildren().add(empty);
        } else {
            for (MatchModel match : matches) historyCards.getChildren().add(createMatchCard(
                    match.id.get(), match.date.get(), match.room.get(), match.pos.get(),
                    match.hands.get(), match.profit.get(), !match.profit.get().startsWith("-")));
        }
    }

    private HBox createMatchCard(String id, String date, String room, String pos, String hands, String profit, boolean isWin) {
        HBox card = new HBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12, 20, 12, 20));
        card.setStyle(OrnateUi.compactPanelStyle() + " -fx-cursor: hand;");
        card.setOnMouseEntered(e -> card.setScaleX(1.01));
        card.setOnMouseExited(e -> card.setScaleX(1));

        // Rank Badge Pill
        Label rankBadge = new Label(pos);
        rankBadge.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        rankBadge.setPadding(new Insets(6, 14, 6, 14));

        if (pos.contains("1st")) {
            rankBadge.setTextFill(Color.web("#1e1b4b"));
            rankBadge.setStyle("-fx-background-color: linear-gradient(to right, #facc15, #eab308); -fx-background-radius: 20; -fx-font-weight: bold;");
        } else if (pos.contains("2nd")) {
            rankBadge.setTextFill(Color.web("#0f172a"));
            rankBadge.setStyle("-fx-background-color: linear-gradient(to right, #e2e8f0, #94a3b8); -fx-background-radius: 20; -fx-font-weight: bold;");
        } else {
            rankBadge.setTextFill(Color.WHITE);
            rankBadge.setStyle("-fx-background-color: rgba(51, 65, 85, 0.8); -fx-background-radius: 20;");
        }

        // Room & Date Info
        VBox infoBox = new VBox(3);
        infoBox.setAlignment(Pos.CENTER_LEFT);

        HBox roomLine = new HBox(8);
        roomLine.setAlignment(Pos.CENTER_LEFT);
        Label roomLbl = new Label(room);
        roomLbl.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        roomLbl.setTextFill(Color.WHITE);

        Label idLbl = new Label(id);
        idLbl.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        idLbl.setTextFill(Color.web("#38bdf8"));
        roomLine.getChildren().addAll(roomLbl, idLbl);

        Label subLbl = new Label(date + "  •  " + hands);
        subLbl.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        subLbl.setTextFill(Color.LIGHTGRAY);

        infoBox.getChildren().addAll(roomLine, subLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Profit / Loss Pill
        Label profitPill = new Label(profit);
        profitPill.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        profitPill.setPadding(new Insets(8, 18, 8, 18));

        if (isWin) {
            profitPill.setTextFill(Color.web("#34d399"));
            profitPill.setStyle("-fx-background-color: rgba(16, 185, 129, 0.18); -fx-border-color: #10b981; -fx-border-radius: 20; -fx-background-radius: 20;");
        } else {
            profitPill.setTextFill(Color.web("#f87171"));
            profitPill.setStyle("-fx-background-color: rgba(239, 68, 68, 0.18); -fx-border-color: #ef4444; -fx-border-radius: 20; -fx-background-radius: 20;");
        }

        card.getChildren().addAll(rankBadge, infoBox, spacer, profitPill);
        return card;
    }

    private void showEditProfileDialog() {
        Stage dialog = new Stage();
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.initModality(Modality.APPLICATION_MODAL);

        javafx.stage.Window owner = this.getScene() != null ? this.getScene().getWindow() : null;
        if (owner != null) {
            dialog.initOwner(owner);
            dialog.setX(owner.getX());
            dialog.setY(owner.getY());
        }
        double width = owner != null ? owner.getWidth() : 1280;
        double height = owner != null ? owner.getHeight() : 820;

        VBox root = new VBox(0);
        root.setAlignment(Pos.TOP_CENTER);

        Image bgImg = AssetLoader.getBackgroundImage();
        if (bgImg != null) {
            BackgroundImage myBI = new BackgroundImage(bgImg,
                    BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER,
                    new BackgroundSize(100, 100, true, true, true, true));
            root.setBackground(new Background(myBI));
        } else {
            root.setStyle("-fx-background-color: #0b1329;");
        }

        // Top Header Bar
        HBox topHeaderBar = new HBox(15);
        topHeaderBar.setPadding(new Insets(14, 35, 14, 35));
        topHeaderBar.setAlignment(Pos.CENTER_LEFT);
        topHeaderBar.setStyle("-fx-background-color: linear-gradient(to bottom, #5a3023, #281710); " +
                "-fx-border-color: #c69a55; -fx-border-width: 0 0 2 0;");

        Button btnBack = new Button("◄ Quay lại Hồ sơ");
        btnBack.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        btnBack.setPrefSize(165, 46);
        OrnateUi.button(btnBack, "button-red.png", 13);
        btnBack.setOnAction(e -> dialog.close());

        Label dialogTitle = new Label("✏️ CHỈNH SỬA THÔNG TIN CÁ NHÂN");
        dialogTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        dialogTitle.setTextFill(Color.web("#facc15"));
        dialogTitle.setStyle("-fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.9), 6, 0.8, 0, 2);");

        topHeaderBar.getChildren().addAll(btnBack, dialogTitle);

        // Center Edit Card
        VBox editCard = new VBox(22);
        editCard.setAlignment(Pos.CENTER);
        editCard.setMaxWidth(480);
        editCard.setPadding(new Insets(35, 40, 35, 40));
        editCard.setStyle(OrnateUi.framedStyle());

        Label lblName = new Label("Tên hiển thị người chơi:");
        lblName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblName.setTextFill(Color.WHITE);

        TextField txtName = new TextField(currentUsername);
        txtName.setPromptText("Tên người chơi mới...");
        txtName.setStyle("-fx-background-color: #342319; -fx-text-fill: #f7e9cf; -fx-font-size: 14px; " +
                "-fx-padding: 10; -fx-border-color: #b38a4d; -fx-border-radius: 7; -fx-background-radius: 7;");

        Label avatarLabel = new Label("Chọn Avatar đại diện:");
        avatarLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        avatarLabel.setTextFill(Color.LIGHTGRAY);

        HBox avatarGrid = new HBox(12);
        avatarGrid.setAlignment(Pos.CENTER);

        for (int i = 1; i <= 6; i++) {
            Image img = AssetLoader.getAvatarImage("avatar_player_" + i + ".png");
            if (img != null) {
                ImageView view = new ImageView(img);
                view.setFitWidth(55);
                view.setFitHeight(55);
                view.setPreserveRatio(true);
                view.setCursor(javafx.scene.Cursor.HAND);
                view.setClip(new Circle(27.5, 27.5, 27.5));
                avatarGrid.getChildren().add(view);
            }
        }

        Button btnSave = new Button("💾 LƯU THAY ĐỔI");
        btnSave.setMaxWidth(Double.MAX_VALUE);
        btnSave.setPrefHeight(45);
        OrnateUi.button(btnSave, "button-blue.png", 15);
        btnSave.setOnAction(e -> {
            if (!txtName.getText().trim().isEmpty()) {
                this.currentUsername = txtName.getText().trim();
                if (onlineMode && onlineNameChange != null) onlineNameChange.accept(this.currentUsername);
            }
            dialog.close();
        });

        editCard.getChildren().addAll(lblName, txtName, avatarLabel, avatarGrid, btnSave);

        VBox centerBox = new VBox(editCard);
        centerBox.setAlignment(Pos.CENTER);
        VBox.setVgrow(centerBox, Priority.ALWAYS);

        root.getChildren().addAll(topHeaderBar, centerBox);

        Scene scene = new Scene(root, width, height);
        V2Theme.apply(scene);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    public static class MatchModel {
        private final javafx.beans.property.StringProperty id;
        private final javafx.beans.property.StringProperty date;
        private final javafx.beans.property.StringProperty room;
        private final javafx.beans.property.StringProperty pos;
        private final javafx.beans.property.StringProperty hands;
        private final javafx.beans.property.StringProperty profit;

        public MatchModel(String id, String date, String room, String pos, String hands, String profit) {
            this.id = new javafx.beans.property.SimpleStringProperty(id);
            this.date = new javafx.beans.property.SimpleStringProperty(date);
            this.room = new javafx.beans.property.SimpleStringProperty(room);
            this.pos = new javafx.beans.property.SimpleStringProperty(pos);
            this.hands = new javafx.beans.property.SimpleStringProperty(hands);
            this.profit = new javafx.beans.property.SimpleStringProperty(profit);
        }

        public javafx.beans.property.StringProperty idProperty() { return id; }
        public javafx.beans.property.StringProperty dateProperty() { return date; }
        public javafx.beans.property.StringProperty roomProperty() { return room; }
        public javafx.beans.property.StringProperty posProperty() { return pos; }
        public javafx.beans.property.StringProperty handsProperty() { return hands; }
        public javafx.beans.property.StringProperty profitProperty() { return profit; }
    }
}
