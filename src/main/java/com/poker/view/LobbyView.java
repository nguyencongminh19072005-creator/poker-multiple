package com.poker.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class LobbyView extends BorderPane {

    public interface LobbyActions {
        void onJoinRoom(String roomId, boolean isPrivate, boolean isSpectator);
        default void onJoinPrivateRoom(String roomId, String password, boolean isSpectator) {
            onJoinRoom(roomId, true, isSpectator);
        }
        void onCreateRoomRequested();
        void onOpenLeaderboard();
        void onOpenProfile();
        void onOpenFriends();
        void onOpenAdmin();
        void onLogout();
    }

    private final LobbyActions actions;
    private final String currentUsername;
    private final boolean onlineMode;
    private Label balanceLabel;
    private Label onlineValue;

    public LobbyView(String username, LobbyActions actions) {
        this(username, actions, false);
    }

    public LobbyView(String username, LobbyActions actions, boolean onlineMode) {
        this.currentUsername = username;
        this.actions = actions;
        this.onlineMode = onlineMode;

        // Set Background with dark overlay to soften background text & improve contrast
        Image bgImg = AssetLoader.getLobbyBackgroundImage();
        if (bgImg != null) {
            BackgroundImage myBI = new BackgroundImage(bgImg,
                    BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER,
                    new BackgroundSize(100, 100, true, true, true, true));
            BackgroundFill tint = new BackgroundFill(
                    Color.rgb(4, 11, 15, 0.34), CornerRadii.EMPTY, Insets.EMPTY);
            this.setBackground(new Background(
                    new BackgroundFill[]{tint}, new BackgroundImage[]{myBI}));
        } else {
            this.setStyle("-fx-background-color: #07090e;");
        }

        // Top Header
        HBox topBar = createTopBar();
        this.setTop(topBar);

        // Center Content: Room List & Controls (Full bottom dark overlay)
        VBox centerBox = createCenterContent();
        this.setCenter(centerBox);
    }

    private HBox createTopBar() {
        HBox topBar = new HBox(25);
        topBar.setPadding(new Insets(14, 32, 14, 32));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: linear-gradient(to bottom, #432719, #21140f); " +
                "-fx-border-color: #a77b47; -fx-border-width: 0 0 2 0; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.62), 12, 0, 0, 4);");

        // --- Top-Left: Player Profile Avatar with Gold Frame Asset ---
        int avatarIndex = (Math.abs(currentUsername.hashCode()) % 6) + 1;
        Image rawAvatarImg = AssetLoader.getAvatarImage("avatar_player_" + avatarIndex + ".png");

        StackPane avatarStack = new StackPane();
        avatarStack.setPrefSize(72, 72);

        if (rawAvatarImg != null) {
            javafx.scene.image.ImageView avatarView = new javafx.scene.image.ImageView(rawAvatarImg);
            avatarView.setFitWidth(50);
            avatarView.setFitHeight(50);
            avatarView.setPreserveRatio(true);

            // Circular clip for avatar
            javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(25, 25, 25);
            avatarView.setClip(clip);

            avatarStack.getChildren().add(avatarView);
        }

        VBox userDetailBox = new VBox(4);
        userDetailBox.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label(currentUsername);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        nameLabel.setTextFill(Color.WHITE);

        Label statusLabel = new Label("Online");
        statusLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        statusLabel.setTextFill(Color.LIGHTGREEN);
        Image onlineDot = AssetLoader.getOnlineStatusImage();
        if (onlineDot != null) {
            ImageView dotView = new ImageView(onlineDot);
            dotView.setFitWidth(10);
            dotView.setFitHeight(10);
            dotView.setPreserveRatio(true);
            statusLabel.setGraphic(dotView);
        } else {
            statusLabel.setText("🟢 Online");
        }

        userDetailBox.getChildren().addAll(nameLabel, statusLabel);

        HBox profileSection = new HBox(12, avatarStack, userDetailBox);
        profileSection.setAlignment(Pos.CENTER_LEFT);
        profileSection.setCursor(javafx.scene.Cursor.HAND);
        profileSection.setPadding(new Insets(7, 12, 7, 16));
        profileSection.setPrefSize(250, 90);
        profileSection.setMinSize(250, 90);
        profileSection.setStyle(OrnateUi.imageStyle("player-seat.png"));

        // Click on Avatar or Profile info opens Profile & Stats dialog
        profileSection.setOnMouseClicked(e -> actions.onOpenProfile());

        // Hover scale effect
        profileSection.setOnMouseEntered(e -> {
            profileSection.setScaleX(1.05);
            profileSection.setScaleY(1.05);
        });
        profileSection.setOnMouseExited(e -> {
            profileSection.setScaleX(1.0);
            profileSection.setScaleY(1.0);
        });

        // --- Middle: Balance Chips Display ---
        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        HBox balanceBox = new HBox(12);
        balanceBox.setAlignment(Pos.CENTER);
        balanceBox.setPadding(new Insets(10, 28, 10, 20));
        balanceBox.setStyle(OrnateUi.compactPanelStyle());

        Image chipStackImg = AssetLoader.getChipStackImage();

        if (chipStackImg != null) {
            javafx.scene.image.ImageView chipIconView = new javafx.scene.image.ImageView(chipStackImg);
            chipIconView.setFitWidth(40);
            chipIconView.setFitHeight(40);
            chipIconView.setPreserveRatio(true);
            balanceBox.getChildren().add(chipIconView);
        } else {
            Label chipIcon = new Label("💰");
            chipIcon.setFont(Font.font("Arial", 22));
            balanceBox.getChildren().add(chipIcon);
        }

        VBox balanceTextContainer = new VBox(2);
        balanceTextContainer.setAlignment(Pos.CENTER_LEFT);

        Label balanceTitle = new Label("CHIPS BALANCE");
        balanceTitle.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        balanceTitle.setTextFill(Color.web("#ca8a04"));

        balanceLabel = new Label("$100.000");
        balanceLabel.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        balanceLabel.setTextFill(Color.GOLD);

        balanceTextContainer.getChildren().addAll(balanceTitle, balanceLabel);
        balanceBox.getChildren().add(balanceTextContainer);

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        // --- Top-Right: Friends & Admin Buttons ---
        Button btnFriends = new Button("👥 Bạn bè");
        btnFriends.setPrefSize(128, 44);
        OrnateUi.button(btnFriends, "button-blue.png", 13);
        btnFriends.setOnAction(e -> actions.onOpenFriends());

        Button btnAdmin = new Button("🛠️ Admin");
        btnAdmin.setPrefSize(110, 44);
        OrnateUi.button(btnAdmin, "button-red.png", 13);
        btnAdmin.setOnAction(e -> actions.onOpenAdmin());
        if (!"admin".equalsIgnoreCase(currentUsername)) {
            btnAdmin.setVisible(false);
            btnAdmin.setManaged(false);
        }

        HBox rightSection = new HBox(12, btnFriends, btnAdmin);
        if (onlineMode) {
            Button logout = new Button("Đăng xuất");
            OrnateUi.button(logout, "button-red.png", 12);
            logout.setOnAction(e -> actions.onLogout());
            rightSection.getChildren().add(logout);
        }
        rightSection.setAlignment(Pos.CENTER_RIGHT);

        topBar.getChildren().addAll(profileSection, spacer1, balanceBox, spacer2, rightSection);
        return topBar;
    }

    private VBox createCenterContent() {
        VBox container = new VBox(15);
        container.setPadding(new Insets(0, 50, 40, 50));
        container.setAlignment(Pos.CENTER);

        // Center Logo
        Image logoImg = AssetLoader.getLogoImage();
        if (logoImg != null) {
            javafx.scene.image.ImageView centerLogo = new javafx.scene.image.ImageView(logoImg);
            centerLogo.setFitWidth(130);
            centerLogo.setPreserveRatio(true);
            VBox.setMargin(centerLogo, new Insets(-15, 0, 10, 0));
            container.getChildren().add(centerLogo);
        }

        // Populate Initial Active Rooms for data model
        allRooms = javafx.collections.FXCollections.observableArrayList();
        if (!onlineMode) {
            allRooms.add(new RoomModel("#101", "High Rollers VIP", "PokerKing", "4/6", "$500/$1.000", "$100.000", "Public", "Playing"));
            allRooms.add(new RoomModel("#102", "Beginners Table", "LuckyCat", "2/6", "$10/$20", "$2.000", "Public", "Waiting"));
            allRooms.add(new RoomModel("#103", "Private Club #7", "ShadowPlayer", "5/6", "$100/$200", "$20.000", "Private", "Waiting"));
            allRooms.add(new RoomModel("#104", "Pro Tournament", "AceSpade", "6/6", "$250/$500", "$50.000", "Public", "Full"));
        }
        filteredRooms = new javafx.collections.transformation.FilteredList<>(allRooms, p -> true);

        Label heading = new Label("POKER  •  WORLD CHAMPIONS");
        heading.setMinSize(600, 90);
        heading.setAlignment(Pos.CENTER);
        heading.setStyle(OrnateUi.imageStyle("title-banner.png") +
                " -fx-text-fill: #f4dc9a; -fx-font-family: Georgia; -fx-font-size: 30px; -fx-font-weight: 900;");

        VBox roomCards = new VBox(8);
        Runnable refreshRoomCards = () -> {
            roomCards.getChildren().clear();
            if (allRooms.isEmpty()) {
                Label empty = new Label(onlineMode ? "Chưa có phòng đang hoạt động" : "Chưa có phòng");
                empty.setMaxWidth(Double.MAX_VALUE);
                empty.setAlignment(Pos.CENTER);
                empty.setMinHeight(145);
                empty.setStyle("-fx-text-fill: #e8d0a4; -fx-font-size: 15px;");
                roomCards.getChildren().add(empty);
            } else {
                allRooms.stream().limit(4).forEach(room -> roomCards.getChildren().add(createLobbyRoomCard(room)));
            }
        };
        refreshRoomCards.run();
        allRooms.addListener((javafx.collections.ListChangeListener<RoomModel>) change -> refreshRoomCards.run());

        Label roomsHeading = new Label("PHÒNG ĐANG HOẠT ĐỘNG");
        roomsHeading.setMaxWidth(Double.MAX_VALUE);
        roomsHeading.setPrefHeight(32);
        roomsHeading.setAlignment(Pos.CENTER);
        roomsHeading.setStyle("-fx-text-fill: #f2d391; -fx-font-family: Georgia; -fx-font-size: 17px; -fx-font-weight: 900;");
        VBox roomPanel = new VBox(10, roomsHeading, roomCards);
        roomPanel.setPrefWidth(650);
        roomPanel.setPadding(new Insets(17, 30, 26, 30));
        OrnateUi.panel(roomPanel, "wood-panel.png");

        Label onlineCaption = new Label(onlineMode ? "NGƯỜI CHƠI ĐANG NGỒI" : "NGƯỜI CHƠI ONLINE");
        onlineCaption.setMaxWidth(Double.MAX_VALUE);
        onlineCaption.setAlignment(Pos.CENTER);
        onlineCaption.setStyle("-fx-text-fill: #e7d3aa; -fx-font-size: 13px; -fx-font-weight: bold;");
        onlineValue = new Label(onlineMode ? "0" : "68");
        onlineValue.setMaxWidth(Double.MAX_VALUE);
        onlineValue.setAlignment(Pos.CENTER);
        onlineValue.setStyle("-fx-text-fill: #ffe094; -fx-font-size: 36px; -fx-font-weight: 900;");
        VBox onlinePanel = new VBox(4, onlineCaption, onlineValue);
        onlinePanel.setAlignment(Pos.TOP_CENTER);
        onlinePanel.setPadding(new Insets(2, 12, 12, 12));
        onlinePanel.setPrefSize(280, 125);
        OrnateUi.panel(onlinePanel, "wood-panel.png");

        Button createRoomButton = new Button("TẠO PHÒNG");
        createRoomButton.setMaxWidth(Double.MAX_VALUE);
        createRoomButton.setPrefHeight(65);
        OrnateUi.button(createRoomButton, "button-red.png", 20);
        createRoomButton.setOnAction(e -> actions.onCreateRoomRequested());

        Button joinRoomButton = new Button("DANH SÁCH PHÒNG");
        joinRoomButton.setMaxWidth(Double.MAX_VALUE);
        joinRoomButton.setPrefHeight(65);
        OrnateUi.button(joinRoomButton, "button-blue.png", 17);
        joinRoomButton.setOnAction(e -> showRoomListDialog());

        Button leaderboardButton = new Button("BẢNG XẾP HẠNG");
        leaderboardButton.setMaxWidth(Double.MAX_VALUE);
        leaderboardButton.setPrefHeight(58);
        OrnateUi.button(leaderboardButton, "button-red.png", 16);
        leaderboardButton.setOnAction(e -> actions.onOpenLeaderboard());

        VBox sidebar = new VBox(10, onlinePanel, createRoomButton, joinRoomButton, leaderboardButton);
        sidebar.setPrefWidth(310);
        sidebar.setPadding(new Insets(15));
        sidebar.setStyle(OrnateUi.framedStyle());
        HBox dashboard = new HBox(20, roomPanel, sidebar);
        dashboard.setAlignment(Pos.CENTER);
        container.getChildren().addAll(heading, dashboard);
        return container;
    }

    private HBox createLobbyRoomCard(RoomModel room) {
        ImageView chip = new ImageView(AssetLoader.getChipImageForValue(25));
        chip.setFitWidth(46);
        chip.setFitHeight(46);
        chip.setPreserveRatio(true);
        Label name = new Label(room.getName());
        name.setStyle("-fx-text-fill: #fff0cc; -fx-font-size: 15px; -fx-font-weight: 900;");
        Label detail = new Label(room.blindsProperty().get() + "   •   Buy-in " + room.getBuyIn());
        detail.setStyle("-fx-text-fill: #d9b784; -fx-font-size: 11px;");
        VBox text = new VBox(3, name, detail);
        HBox.setHgrow(text, Priority.ALWAYS);
        Label players = new Label(room.playersProperty().get());
        players.setStyle("-fx-text-fill: #ffdf8e; -fx-font-size: 14px; -fx-font-weight: bold;");
        HBox row = new HBox(12, chip, text, players);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(11, 24, 11, 24));
        row.setMinHeight(74);
        row.setStyle(OrnateUi.imageStyle("room-card.png") + " -fx-cursor: hand;");
        row.setOnMouseClicked(e -> showRoomListDialog());
        return row;
    }

    public void showRoomListDialog() {
        Stage dialog = new Stage();
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setTitle("🎴 DANH SÁCH PHÒNG POKER");
        
        javafx.stage.Window owner = this.getScene() != null ? this.getScene().getWindow() : null;
        if (owner != null) {
            dialog.initOwner(owner);
            dialog.setX(owner.getX());
            dialog.setY(owner.getY());
        }

        double width = owner != null ? owner.getWidth() : 1280;
        double height = owner != null ? owner.getHeight() : 820;

        VBox container = new VBox(20);
        container.setPadding(new Insets(20, 35, 25, 35));

        Image roomBg = AssetLoader.getRoomListBackgroundImage();
        if (roomBg != null) {
            BackgroundImage myBI = new BackgroundImage(roomBg,
                    BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER,
                    new BackgroundSize(100, 100, true, true, true, true));
            container.setBackground(new Background(myBI));
        } else {
            container.setStyle("-fx-background-color: #0b1329;");
        }

        // Top Header Title Bar
        HBox filterBar = new HBox(15);
        filterBar.setPadding(new Insets(12, 20, 12, 20));
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setStyle(OrnateUi.compactPanelStyle());

        Button btnBack = new Button("◄ Quay lại Sảnh");
        btnBack.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        btnBack.setStyle("-fx-background-color: linear-gradient(to right, #1e293b, #334155); " +
                "-fx-text-fill: white; -fx-border-color: #3b82f6; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 16 8 16;");
        btnBack.setOnMouseEntered(e -> btnBack.setStyle("-fx-background-color: linear-gradient(to right, #2563eb, #3b82f6); " +
                "-fx-text-fill: white; -fx-border-color: #60a5fa; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 16 8 16;"));
        btnBack.setOnMouseExited(e -> btnBack.setStyle("-fx-background-color: linear-gradient(to right, #1e293b, #334155); " +
                "-fx-text-fill: white; -fx-border-color: #3b82f6; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 16 8 16;"));
        btnBack.setOnAction(e -> dialog.close());

        Label dialogTitle = new Label("🎴 DANH SÁCH BÀN CHƠI POKER");
        dialogTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        dialogTitle.setTextFill(Color.web("#facc15"));
        dialogTitle.setStyle("-fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.9), 6, 0.8, 0, 2);");

        filterBar.getChildren().addAll(btnBack, dialogTitle);

        // Main Content Area (Left Controls + Right Table View)
        HBox mainLayout = new HBox(25);
        VBox.setVgrow(mainLayout, Priority.ALWAYS);

        // --- LEFT SIDEBAR (Controls & Quick Play) ---
        VBox leftSidebar = new VBox(20);
        leftSidebar.setPrefWidth(250);
        leftSidebar.setAlignment(Pos.TOP_CENTER);
        leftSidebar.setPadding(new Insets(10, 0, 10, 0));

        // 1. CHƠI NGAY (Big Round Metallic Green Button)
        VBox quickPlayBox = new VBox(8);
        quickPlayBox.setAlignment(Pos.CENTER);

        Button btnQuickPlay = new Button("CHƠI\nNGAY");
        btnQuickPlay.setPrefSize(120, 120);
        btnQuickPlay.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        btnQuickPlay.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        btnQuickPlay.setStyle("-fx-background-color: radial-gradient(center 50% 50%, radius 60%, #34d399, #059669); " +
                "-fx-text-fill: white; -fx-background-radius: 100; " +
                "-fx-border-color: linear-gradient(to bottom right, #facc15, #ca8a04); -fx-border-radius: 100; -fx-border-width: 4; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(34, 197, 94, 0.6), 18, 0, 0, 4); -fx-cursor: hand;");
        btnQuickPlay.setOnMouseEntered(e -> btnQuickPlay.setStyle("-fx-background-color: radial-gradient(center 50% 50%, radius 60%, #4ade80, #10b981); " +
                "-fx-text-fill: white; -fx-background-radius: 100; " +
                "-fx-border-color: #fde047; -fx-border-radius: 100; -fx-border-width: 4; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(74, 222, 128, 0.95), 22, 0, 0, 4); -fx-cursor: hand;"));
        btnQuickPlay.setOnMouseExited(e -> btnQuickPlay.setStyle("-fx-background-color: radial-gradient(center 50% 50%, radius 60%, #34d399, #059669); " +
                "-fx-text-fill: white; -fx-background-radius: 100; " +
                "-fx-border-color: linear-gradient(to bottom right, #facc15, #ca8a04); -fx-border-radius: 100; -fx-border-width: 4; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(34, 197, 94, 0.6), 18, 0, 0, 4); -fx-cursor: hand;"));
        btnQuickPlay.setOnAction(e -> {
            RoomModel firstPublic = filteredRooms.stream()
                    .filter(room -> "Public".equalsIgnoreCase(room.getType()) &&
                            !"Full".equalsIgnoreCase(room.getStatus()))
                    .findFirst().orElse(null);
            if (firstPublic != null) {
                dialog.close();
                actions.onJoinRoom(firstPublic.getId(), false, false);
            } else {
                actions.onCreateRoomRequested();
            }
        });

        quickPlayBox.getChildren().add(btnQuickPlay);

        // 2. MỨC TIỀN Filter Box
        VBox moneyFilterBox = new VBox(8);
        moneyFilterBox.setPadding(new Insets(12));
        moneyFilterBox.setStyle(OrnateUi.compactPanelStyle());

        Label lblMoneyTitle = new Label("MỨC TIỀN");
        lblMoneyTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lblMoneyTitle.setTextFill(Color.web("#facc15"));

        ComboBox<String> comboMoneyFilter = new ComboBox<>();
        comboMoneyFilter.getItems().addAll("Tất cả", "$2k+", "$10k+", "$50k+");
        comboMoneyFilter.setValue("Tất cả");
        comboMoneyFilter.setMaxWidth(Double.MAX_VALUE);
        comboMoneyFilter.setStyle("-fx-background-color: #0f172a; -fx-text-fill: #facc15; -fx-font-weight: bold; -fx-font-size: 14px;");

        moneyFilterBox.getChildren().addAll(lblMoneyTitle, comboMoneyFilter);

        // Search Field
        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Tìm tên/ID phòng...");
        searchField.setStyle("-fx-font-size: 13px; -fx-background-color: #0f172a; -fx-text-fill: white; -fx-padding: 8; -fx-border-color: #334155; -fx-border-radius: 6; -fx-background-radius: 6;");

        Region sidebarSpacer = new Region();
        VBox.setVgrow(sidebarSpacer, Priority.ALWAYS);

        leftSidebar.getChildren().addAll(quickPlayBox, moneyFilterBox, searchField);

        // --- RIGHT TABLE AREA ---
        VBox rightTableArea = new VBox(0);
        HBox.setHgrow(rightTableArea, Priority.ALWAYS);

        // Top Tabs: TẤT CẢ, BÀN TRỐNG, CHƯA CHƠI
        HBox tabsBar = new HBox(8);
        tabsBar.setPadding(new Insets(0, 0, 5, 0));

        Button tabAll = new Button("TẤT CẢ");
        Button tabEmpty = new Button("BÀN TRỐNG");
        Button tabWaiting = new Button("CHƯA CHƠI");

        String activeTabStyle = "-fx-background-color: #22c55e; -fx-text-fill: black; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 6 6 0 0; -fx-padding: 8 20 8 20; -fx-cursor: hand;";
        String inactiveTabStyle = "-fx-background-color: #1e293b; -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 6 6 0 0; -fx-padding: 8 20 8 20; -fx-cursor: hand;";

        tabAll.setStyle(activeTabStyle);
        tabEmpty.setStyle(inactiveTabStyle);
        tabWaiting.setStyle(inactiveTabStyle);

        tabsBar.getChildren().addAll(tabAll, tabEmpty, tabWaiting);

        // Table Header
        HBox tableHeader = new HBox(10);
        tableHeader.setPadding(new Insets(12, 16, 12, 16));
        tableHeader.setStyle(OrnateUi.compactPanelStyle());

        Label colBlinds = new Label("Mức Tiền");
        colBlinds.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        colBlinds.setTextFill(Color.web("#facc15"));
        colBlinds.setPrefWidth(120);

        Label colRoomName = new Label("Tên Phòng");
        colRoomName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        colRoomName.setTextFill(Color.web("#facc15"));
        colRoomName.setPrefWidth(200);

        Label colHost = new Label("Chủ phòng");
        colHost.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        colHost.setTextFill(Color.web("#facc15"));
        colHost.setPrefWidth(140);

        Label colSeats = new Label("Ghế trống");
        colSeats.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        colSeats.setTextFill(Color.web("#facc15"));
        colSeats.setPrefWidth(150);

        Label colBuyIn = new Label("Giới Hạn (Buy-in)");
        colBuyIn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        colBuyIn.setTextFill(Color.web("#facc15"));
        colBuyIn.setPrefWidth(140);

        Label colAction = new Label("Thao tác");
        colAction.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        colAction.setTextFill(Color.web("#facc15"));

        tableHeader.getChildren().addAll(colBlinds, colRoomName, colHost, colSeats, colBuyIn, colAction);

        // Table Rows List Box
        VBox rowsBox = new VBox(0);
        rowsBox.setStyle("-fx-background-color: transparent;");

        Runnable updateTableRows = () -> {
            rowsBox.getChildren().clear();
            int idx = 0;
            for (RoomModel room : filteredRooms) {
                rowsBox.getChildren().add(createTableRow(room, dialog, idx % 2 == 0));
                idx++;
            }
        };

        updateTableRows.run();
        filteredRooms.addListener((javafx.collections.ListChangeListener<RoomModel>) c -> updateTableRows.run());

        // Tab selection logic
        tabAll.setOnAction(e -> {
            tabAll.setStyle(activeTabStyle);
            tabEmpty.setStyle(inactiveTabStyle);
            tabWaiting.setStyle(inactiveTabStyle);
            applyTableFilters(searchField, comboMoneyFilter, "ALL");
        });
        tabEmpty.setOnAction(e -> {
            tabAll.setStyle(inactiveTabStyle);
            tabEmpty.setStyle(activeTabStyle);
            tabWaiting.setStyle(inactiveTabStyle);
            applyTableFilters(searchField, comboMoneyFilter, "EMPTY");
        });
        tabWaiting.setOnAction(e -> {
            tabAll.setStyle(inactiveTabStyle);
            tabEmpty.setStyle(inactiveTabStyle);
            tabWaiting.setStyle(activeTabStyle);
            applyTableFilters(searchField, comboMoneyFilter, "WAITING");
        });

        searchField.textProperty().addListener((obs, oldV, newV) -> applyTableFilters(searchField, comboMoneyFilter, "ALL"));
        comboMoneyFilter.valueProperty().addListener((obs, oldV, newV) -> applyTableFilters(searchField, comboMoneyFilter, "ALL"));

        ScrollPane scrollTable = new ScrollPane(rowsBox);
        scrollTable.setFitToWidth(true);
        VBox.setVgrow(scrollTable, Priority.ALWAYS);
        scrollTable.setStyle("-fx-background: transparent; -fx-background-color: rgba(11, 19, 41, 0.7);");

        rightTableArea.getChildren().addAll(tabsBar, tableHeader, scrollTable);

        mainLayout.getChildren().addAll(leftSidebar, rightTableArea);

        container.getChildren().addAll(filterBar, mainLayout);
        Scene scene = new Scene(container, width, height);
        V2Theme.apply(scene);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private HBox createTableRow(RoomModel room, Stage dialog, boolean isEven) {
        HBox row = new HBox(10);
        row.setPadding(new Insets(12, 16, 12, 16));
        row.setAlignment(Pos.CENTER_LEFT);

        row.setStyle(OrnateUi.imageStyle("room-card.png") + " -fx-cursor: hand;");
        row.setOnMouseEntered(e -> row.setOpacity(0.85));
        row.setOnMouseExited(e -> row.setOpacity(1));

        // 1. Mức Tiền
        Label blindsLbl = new Label(room.blindsProperty().get());
        blindsLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        blindsLbl.setTextFill(Color.web("#facc15"));
        blindsLbl.setPrefWidth(120);

        // 2. Tên Phòng
        Label nameLbl = new Label(room.getName() + " (" + room.getId() + ")");
        nameLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        nameLbl.setTextFill(Color.WHITE);
        nameLbl.setPrefWidth(200);

        // 3. Chủ phòng
        Label hostLbl = new Label(room.getHost());
        hostLbl.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
        hostLbl.setTextFill(Color.web("#cbd5e1"));
        hostLbl.setPrefWidth(140);

        // 4. Ghế trống
        HBox seatsVisual = createSeatsVisual(room.playersProperty().get());
        seatsVisual.setPrefWidth(150);

        // 5. Giới hạn (Buy-in)
        Label buyInLbl = new Label(room.buyInProperty().get());
        buyInLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        buyInLbl.setTextFill(Color.web("#4ade80"));
        buyInLbl.setPrefWidth(140);

        // 6. Action Buttons
        boolean isPrivate = "Private".equalsIgnoreCase(room.getType());

        Button btnDetail = new Button("Chi tiết");
        btnDetail.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-font-size: 11px; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6 10;");
        btnDetail.setOnAction(e -> showRoomDetailDialog(dialog, room));

        Button btnJoinRow = new Button(isPrivate ? "MẬT KHẨU" : "VÀO BÀN");
        btnJoinRow.setStyle(isPrivate ?
                "-fx-background-color: linear-gradient(to right, #b45309, #d97706); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-background-radius: 6; -fx-cursor: hand;" :
                "-fx-background-color: linear-gradient(to right, #059669, #10b981); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-background-radius: 6; -fx-cursor: hand;");
        btnJoinRow.setOnAction(e -> {
            if (isPrivate) {
                showJoinPrivateRoomDialog(dialog, room);
            } else {
                dialog.close();
                actions.onJoinRoom(room.getId(), false, false);
            }
        });

        row.setOnMouseClicked(e -> {
            if (e.getTarget() == btnDetail) return;
            if (isPrivate) {
                showJoinPrivateRoomDialog(dialog, room);
            } else {
                dialog.close();
                actions.onJoinRoom(room.getId(), false, false);
            }
        });

        HBox actionsBox = new HBox(8, btnDetail, btnJoinRow);
        actionsBox.setAlignment(Pos.CENTER_LEFT);

        row.getChildren().addAll(blindsLbl, nameLbl, hostLbl, seatsVisual, buyInLbl, actionsBox);
        return row;
    }

    private void showJoinPrivateRoomDialog(Stage owner, RoomModel room) {
        Stage modal = new Stage();
        modal.initStyle(StageStyle.UNDECORATED);
        modal.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) modal.initOwner(owner);

        VBox content = new VBox(14);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(20, 24, 20, 24));
        content.setStyle(OrnateUi.framedStyle());

        Image modalImg = AssetLoader.getJoinPrivateRoomModalImage();
        if (modalImg != null) {
            ImageView headerView = new ImageView(modalImg);
            headerView.setFitWidth(320);
            headerView.setPreserveRatio(true);
            content.getChildren().add(headerView);
        } else {
            Label title = new Label("JOIN PRIVATE ROOM");
            title.setStyle("-fx-text-fill: #facc15; -fx-font-weight: bold; -fx-font-size: 18px;");
            content.getChildren().add(title);
        }

        Label roomInfo = new Label("Phòng: " + room.getName() + " (" + room.getId() + ")");
        roomInfo.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 13px;");

        PasswordField passField = new PasswordField();
        passField.setPromptText("Nhập mật khẩu phòng...");
        passField.setPrefWidth(280);
        passField.setPrefHeight(40);
        passField.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-text-fill: #ffffff; -fx-border-color: #ca8a04; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 0 12;");

        Label errorLbl = new Label();
        errorLbl.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px;");

        HBox btnsBox = new HBox(12);
        btnsBox.setAlignment(Pos.CENTER);

        Button btnJoin = new Button("VÀO PHÒNG");
        OrnateUi.button(btnJoin, "button-blue.png", 13);
        btnJoin.setOnAction(e -> {
            if (passField.getText().trim().isEmpty()) {
                errorLbl.setText("Vui lòng nhập mật khẩu phòng!");
                return;
            }
            modal.close();
            if (owner != null) owner.close();
            actions.onJoinPrivateRoom(room.getId(), passField.getText(), false);
        });

        Button btnCancel = new Button("HỦY");
        OrnateUi.button(btnCancel, "button-red.png", 13);
        btnCancel.setOnAction(e -> modal.close());

        btnsBox.getChildren().addAll(btnJoin, btnCancel);

        content.getChildren().addAll(roomInfo, passField, errorLbl, btnsBox);

        Scene scene = new Scene(content);
        V2Theme.apply(scene);
        scene.setFill(Color.TRANSPARENT);
        modal.setScene(scene);
        modal.showAndWait();
    }

    private void showRoomDetailDialog(Stage owner, RoomModel room) {
        Stage modal = new Stage();
        modal.initStyle(StageStyle.UNDECORATED);
        modal.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) modal.initOwner(owner);

        VBox content = new VBox(14);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(20, 24, 20, 24));
        content.setStyle(OrnateUi.framedStyle());

        Image detailImg = AssetLoader.getRoomDetailModalImage();
        if (detailImg != null) {
            ImageView iv = new ImageView(detailImg);
            iv.setFitWidth(240);
            iv.setPreserveRatio(true);
            content.getChildren().add(iv);
        } else {
            Label title = new Label("ROOM DETAIL / PREVIEW");
            title.setStyle("-fx-text-fill: #facc15; -fx-font-weight: bold; -fx-font-size: 18px;");
            content.getChildren().add(title);
        }

        VBox infoList = new VBox(6);
        infoList.setAlignment(Pos.CENTER_LEFT);
        infoList.setPadding(new Insets(8, 16, 8, 16));
        infoList.setStyle("-fx-background-color: rgba(0,0,0,0.4); -fx-background-radius: 8;");

        infoList.getChildren().addAll(
                createDetailRow("Phòng:", room.getName() + " (" + room.getId() + ")"),
                createDetailRow("Chủ phòng:", room.getHost()),
                createDetailRow("Số người:", room.playersProperty().get()),
                createDetailRow("Mức Blinds:", room.blindsProperty().get()),
                createDetailRow("Buy-in:", room.buyInProperty().get()),
                createDetailRow("Loại bàn:", room.getType())
        );

        HBox btns = new HBox(12);
        btns.setAlignment(Pos.CENTER);

        Button btnJoin = new Button("VÀO BÀN NGAY");
        OrnateUi.button(btnJoin, "button-blue.png", 13);
        btnJoin.setOnAction(e -> {
            modal.close();
            if (owner != null) owner.close();
            actions.onJoinRoom(room.getId(), "Private".equalsIgnoreCase(room.getType()), false);
        });

        Button btnClose = new Button("ĐÓNG");
        OrnateUi.button(btnClose, "button-red.png", 13);
        btnClose.setOnAction(e -> modal.close());

        btns.getChildren().addAll(btnJoin, btnClose);
        content.getChildren().addAll(infoList, btns);

        Scene scene = new Scene(content);
        V2Theme.apply(scene);
        scene.setFill(Color.TRANSPARENT);
        modal.setScene(scene);
        modal.showAndWait();
    }

    private HBox createDetailRow(String label, String value) {
        Label l = new Label(label);
        l.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-font-size: 12px;");
        l.setPrefWidth(90);
        Label v = new Label(value);
        v.setStyle("-fx-text-fill: #f8fafc; -fx-font-weight: bold; -fx-font-size: 12px;");
        HBox row = new HBox(8, l, v);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox createSeatsVisual(String playersStr) {
        HBox box = new HBox(4);
        box.setAlignment(Pos.CENTER_LEFT);

        int current = 3;
        int max = 6;
        if (playersStr != null && playersStr.contains("/")) {
            try {
                String[] parts = playersStr.split("/");
                current = Integer.parseInt(parts[0].trim());
                max = Integer.parseInt(parts[1].trim());
            } catch (Exception ignored) {}
        }

        for (int i = 0; i < max; i++) {
            Region block = new Region();
            block.setPrefSize(10, 14);
            if (i < current) {
                block.setStyle("-fx-background-color: #22c55e; -fx-background-radius: 2;");
            } else {
                block.setStyle("-fx-background-color: #334155; -fx-background-radius: 2;");
            }
            box.getChildren().add(block);
        }

        Label txt = new Label(" " + current + "/" + max);
        txt.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        txt.setTextFill(Color.web("#94a3b8"));
        box.getChildren().add(txt);
        return box;
    }

    private void applyTableFilters(TextField search, ComboBox<String> moneyFilter, String tabMode) {
        String searchText = search.getText().toLowerCase().trim();
        String moneyVal = moneyFilter.getValue();

        filteredRooms.setPredicate(room -> {
            boolean matchesSearch = searchText.isEmpty() ||
                    room.getName().toLowerCase().contains(searchText) ||
                    room.getId().toLowerCase().contains(searchText) ||
                    room.getHost().toLowerCase().contains(searchText);

            boolean matchesTab = true;
            if ("EMPTY".equalsIgnoreCase(tabMode)) {
                String playersStr = room.playersProperty().get();
                if (playersStr != null && playersStr.contains("/")) {
                    try {
                        String[] parts = playersStr.split("/");
                        int current = Integer.parseInt(parts[0].trim());
                        int max = Integer.parseInt(parts[1].trim());
                        matchesTab = current < max;
                    } catch (Exception ignored) {}
                }
            } else if ("WAITING".equalsIgnoreCase(tabMode)) {
                matchesTab = "Waiting".equalsIgnoreCase(room.getStatus());
            }

            boolean matchesMoney = true;
            if (moneyVal != null && !moneyVal.equalsIgnoreCase("Tất cả")) {
                matchesMoney = room.getBuyIn() != null && room.getBuyIn().contains(moneyVal.replace("+", ""));
            }

            return matchesSearch && matchesTab && matchesMoney;
        });
    }

    private javafx.collections.ObservableList<RoomModel> allRooms;
    private javafx.collections.transformation.FilteredList<RoomModel> filteredRooms;

    public void addNewRoom(RoomModel room) {
        if (allRooms != null) {
            allRooms.add(0, room);
        }
    }

    public void showOnlineRooms(java.util.List<RoomModel> rooms, long accountChips, long seatedPlayers) {
        if (!onlineMode) return;
        allRooms.setAll(rooms);
        balanceLabel.setText("$" + String.format("%,d", accountChips));
        onlineValue.setText(Long.toString(seatedPlayers));
    }

    // Helper model for TableView
    public static class RoomModel {
        private final javafx.beans.property.StringProperty id;
        private final javafx.beans.property.StringProperty name;
        private final javafx.beans.property.StringProperty host;
        private final javafx.beans.property.StringProperty players;
        private final javafx.beans.property.StringProperty blinds;
        private final javafx.beans.property.StringProperty buyIn;
        private final javafx.beans.property.StringProperty type;
        private final javafx.beans.property.StringProperty status;

        public RoomModel(String id, String name, String host, String players, String blinds, String buyIn, String type, String status) {
            this.id = new javafx.beans.property.SimpleStringProperty(id);
            this.name = new javafx.beans.property.SimpleStringProperty(name);
            this.host = new javafx.beans.property.SimpleStringProperty(host);
            this.players = new javafx.beans.property.SimpleStringProperty(players);
            this.blinds = new javafx.beans.property.SimpleStringProperty(blinds);
            this.buyIn = new javafx.beans.property.SimpleStringProperty(buyIn);
            this.type = new javafx.beans.property.SimpleStringProperty(type);
            this.status = new javafx.beans.property.SimpleStringProperty(status);
        }

        public javafx.beans.property.StringProperty idProperty() { return id; }
        public javafx.beans.property.StringProperty nameProperty() { return name; }
        public javafx.beans.property.StringProperty hostProperty() { return host; }
        public javafx.beans.property.StringProperty playersProperty() { return players; }
        public javafx.beans.property.StringProperty blindsProperty() { return blinds; }
        public javafx.beans.property.StringProperty buyInProperty() { return buyIn; }
        public javafx.beans.property.StringProperty typeProperty() { return type; }
        public javafx.beans.property.StringProperty statusProperty() { return status; }

        public String getId() { return id.get(); }
        public String getName() { return name.get(); }
        public String getHost() { return host.get(); }
        public String getBuyIn() { return buyIn.get(); }
        public String getType() { return type.get(); }
        public String getStatus() { return status.get(); }
    }
}
