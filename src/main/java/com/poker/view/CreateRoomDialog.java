package com.poker.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class CreateRoomDialog {

    public interface CreateRoomCallback {
        void onCreateRoom(String name, int maxPlayers, int sb, int bb, int buyIn, boolean isPrivate, String password);
    }

    public static void show(Stage owner, CreateRoomCallback callback) {
        show(owner, callback, false);
    }

    public static void show(Stage owner, CreateRoomCallback callback, boolean onlineMode) {
        Stage dialog = new Stage();
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);

        if (owner != null) {
            dialog.setX(owner.getX());
            dialog.setY(owner.getY());
        }
        double width = owner != null ? owner.getWidth() : 1280;
        double height = owner != null ? owner.getHeight() : 820;

        VBox root = new VBox(20);
        root.setPadding(new Insets(20, 35, 25, 35));

        Image roomBg = AssetLoader.getRoomListBackgroundImage();
        if (roomBg != null) {
            BackgroundImage myBI = new BackgroundImage(roomBg,
                    BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER,
                    new BackgroundSize(100, 100, true, true, true, true));
            root.setBackground(new Background(myBI));
        } else {
            root.setStyle("-fx-background-color: #20150f;");
        }

        // Top Header Title Bar
        HBox topHeaderBar = new HBox(15);
        topHeaderBar.setPadding(new Insets(12, 20, 12, 20));
        topHeaderBar.setAlignment(Pos.CENTER_LEFT);
        topHeaderBar.setStyle(OrnateUi.compactPanelStyle());

        Button btnBack = new Button("◄ Quay lại Sảnh");
        btnBack.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        btnBack.setStyle("-fx-background-color: #3d281a; -fx-text-fill: #f5e3bb; " +
                "-fx-border-color: #ad814c; -fx-border-radius: 7; -fx-background-radius: 7; -fx-cursor: hand; -fx-padding: 8 16;");
        btnBack.setOnMouseEntered(e -> btnBack.setStyle("-fx-background-color: #614126; -fx-text-fill: white; " +
                "-fx-border-color: #cfaa6c; -fx-border-radius: 7; -fx-background-radius: 7; -fx-cursor: hand; -fx-padding: 8 16;"));
        btnBack.setOnMouseExited(e -> btnBack.setStyle("-fx-background-color: #3d281a; -fx-text-fill: #f5e3bb; " +
                "-fx-border-color: #ad814c; -fx-border-radius: 7; -fx-background-radius: 7; -fx-cursor: hand; -fx-padding: 8 16;"));
        btnBack.setOnAction(e -> dialog.close());

        Label title = new Label("TẠO BÀN");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        title.setTextFill(Color.web("#facc15"));
        title.setStyle("-fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.9), 6, 0.8, 0, 2);");

        topHeaderBar.getChildren().addAll(btnBack, title);

        // Main Workspace (Left Live Preview + Right Form Panel)
        HBox mainWorkspace = new HBox(35);
        mainWorkspace.setAlignment(Pos.CENTER);
        VBox.setVgrow(mainWorkspace, Priority.ALWAYS);

        // --- LEFT PANEL: LIVE PREVIEW CARD ---
        VBox leftPreviewPanel = new VBox(15);
        leftPreviewPanel.setPrefWidth(420);
        leftPreviewPanel.setAlignment(Pos.CENTER);
        leftPreviewPanel.setPadding(new Insets(30, 25, 30, 25));
        leftPreviewPanel.setStyle(OrnateUi.framedStyle());

        Label lblPreviewHeader = new Label("🎴 BẢN XEM TRƯỚC BÀN CHƠI");
        lblPreviewHeader.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        lblPreviewHeader.setMaxWidth(Double.MAX_VALUE);
        lblPreviewHeader.setPrefHeight(58);
        lblPreviewHeader.setAlignment(Pos.CENTER);
        lblPreviewHeader.setStyle(OrnateUi.imageStyle("title-banner.png") + " -fx-text-fill: #ffe3a0;");

        Label lblPrevName = new Label("Texas Hold'em VIP Room");
        lblPrevName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        lblPrevName.setTextFill(Color.web("#facc15"));
        lblPrevName.setWrapText(true);
        lblPrevName.setStyle("-fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.9), 4, 0.8, 0, 1);");

        Separator sepPrev = new Separator();
        sepPrev.setMaxWidth(220);
        sepPrev.setStyle("-fx-opacity: 0.3;");

        // Dynamic seats bar in preview
        HBox prevSeatsBox = new HBox(6);
        prevSeatsBox.setAlignment(Pos.CENTER);

        Label lblPrevSeatsText = new Label("1/6 Ghế");
        lblPrevSeatsText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblPrevSeatsText.setTextFill(Color.web("#cbd5e1"));

        Label lblPrevBlinds = new Label("💵 Blind: $50 / $100");
        lblPrevBlinds.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblPrevBlinds.setTextFill(Color.web("#fbbf24"));

        Label lblPrevBuyIn = new Label("💰 Buy-in: $10,000");
        lblPrevBuyIn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblPrevBuyIn.setTextFill(Color.web("#4ade80"));

        Label lblModeBadge = new Label("CASH GAME • NO LIMIT");
        lblModeBadge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        lblModeBadge.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #94a3b8; -fx-padding: 4 12 4 12; -fx-background-radius: 12;");

        leftPreviewPanel.getChildren().addAll(lblPreviewHeader, lblPrevName, sepPrev, prevSeatsBox, lblPrevSeatsText, lblPrevBlinds, lblPrevBuyIn, lblModeBadge);

        // --- RIGHT PANEL: CREATION FORM ---
        VBox rightFormPanel = new VBox(22);
        rightFormPanel.setPrefWidth(550);
        rightFormPanel.setPadding(new Insets(30, 35, 30, 35));
        rightFormPanel.setStyle(OrnateUi.framedStyle());

        Label formTitle = new Label("CẤU HÌNH BÀN CHƠI");
        formTitle.setFont(Font.font("Georgia", FontWeight.BOLD, 18));
        formTitle.setMaxWidth(Double.MAX_VALUE);
        formTitle.setPrefHeight(60);
        formTitle.setAlignment(Pos.CENTER);
        formTitle.setStyle(OrnateUi.imageStyle("title-banner.png") + " -fx-text-fill: #ffe3a0;");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(20);
        grid.setAlignment(Pos.CENTER_LEFT);

        // 1. Tên Phòng
        Label lblName = new Label("Tên Phòng:");
        lblName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblName.setTextFill(Color.web("#facc15"));

        TextField txtName = new TextField("Texas Hold'em VIP Room");
        txtName.setPrefWidth(320);
        txtName.setStyle("-fx-background-color: #172f27; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10; -fx-border-color: #a77b47; -fx-border-radius: 6; -fx-background-radius: 6;");
        txtName.textProperty().addListener((obs, oldV, newV) -> {
            lblPrevName.setText(newV.trim().isEmpty() ? "Bàn Poker Chưa Đặt Tên" : newV);
        });

        grid.add(lblName, 0, 0);
        grid.add(txtName, 1, 0);

        // 2. Số người chơi (2 - 9 Max)
        Label lblPlayers = new Label("Số Ghế (Max):");
        lblPlayers.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblPlayers.setTextFill(Color.web("#e3c28d"));

        HBox seatSelectBox = new HBox(8);
        seatSelectBox.setAlignment(Pos.CENTER_LEFT);

        ComboBox<Integer> comboPlayers = new ComboBox<>();
        for (int i = onlineMode ? 6 : 2; i <= 9; i++) {
            comboPlayers.getItems().add(i);
        }
        comboPlayers.setValue(6);
        comboPlayers.setStyle("-fx-background-color: #172f27; -fx-text-fill: white; -fx-font-size: 14px;");

        // Quick buttons (2-Max, 6-Max, 9-Max)
        Button q2 = new Button("2-Max");
        Button q6 = new Button("6-Max");
        Button q9 = new Button("9-Max");
        String qBtnStyle = "-fx-background-color: #214236; -fx-text-fill: #e3c28d; -fx-font-weight: bold; -fx-font-size: 12px; -fx-cursor: hand; -fx-background-radius: 4;";
        q2.setStyle(qBtnStyle);
        q6.setStyle(qBtnStyle);
        q9.setStyle(qBtnStyle);

        q2.setOnAction(e -> comboPlayers.setValue(2));
        if (onlineMode) { q2.setVisible(false); q2.setManaged(false); }
        q6.setOnAction(e -> comboPlayers.setValue(6));
        q9.setOnAction(e -> comboPlayers.setValue(9));

        seatSelectBox.getChildren().addAll(comboPlayers, q2, q6, q9);

        grid.add(lblPlayers, 0, 1);
        grid.add(seatSelectBox, 1, 1);

        // Update Seats Preview helper
        Runnable updateSeatsPreview = () -> {
            int maxP = comboPlayers.getValue();
            prevSeatsBox.getChildren().clear();
            for (int i = 0; i < maxP; i++) {
                Region block = new Region();
                block.setPrefSize(10, 14);
                if (i == 0) {
                    block.setStyle("-fx-background-color: #22c55e; -fx-background-radius: 2;");
                } else {
                    block.setStyle("-fx-background-color: #334155; -fx-background-radius: 2;");
                }
                prevSeatsBox.getChildren().add(block);
            }
            lblPrevSeatsText.setText("1/" + maxP + " Ghế");
        };
        updateSeatsPreview.run();
        comboPlayers.valueProperty().addListener((obs, oldV, newV) -> updateSeatsPreview.run());

        // 3. Small Blind / Big Blind
        Label lblBlinds = new Label("Mức Cược (SB/BB):");
        lblBlinds.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblBlinds.setTextFill(Color.web("#fbbf24"));

        ComboBox<String> comboBlinds = new ComboBox<>();
        comboBlinds.getItems().addAll("$10 / $20", "$50 / $100", "$100 / $200", "$500 / $1,000", "$1,000 / $2,000");
        comboBlinds.setValue("$50 / $100");
        comboBlinds.setMaxWidth(Double.MAX_VALUE);
        comboBlinds.setStyle("-fx-background-color: #172f27; -fx-text-fill: white; -fx-font-size: 14px;");

        grid.add(lblBlinds, 0, 2);
        grid.add(comboBlinds, 1, 2);

        // 4. Buy-in
        Label lblBuyIn = new Label("Tiền Vào Bàn (Buy-in):");
        lblBuyIn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblBuyIn.setTextFill(Color.web("#4ade80"));

        TextField txtBuyIn = new TextField("10000");
        txtBuyIn.setStyle("-fx-background-color: #172f27; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10; -fx-border-color: #a77b47; -fx-border-radius: 6; -fx-background-radius: 6;");

        grid.add(lblBuyIn, 0, 3);
        grid.add(txtBuyIn, 1, 3);

        CheckBox privateRoom = new CheckBox("Phòng riêng (có mật khẩu)");
        privateRoom.setStyle("-fx-text-fill:#f5d99a; -fx-font-weight:bold;");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Mật khẩu phòng (8–72 ký tự)");
        passwordField.setStyle("-fx-background-color:#172f27; -fx-text-fill:white; -fx-border-color:#a77b47;");
        passwordField.visibleProperty().bind(privateRoom.selectedProperty());
        passwordField.managedProperty().bind(passwordField.visibleProperty());
        if (onlineMode) {
            grid.add(privateRoom, 1, 4);
            grid.add(passwordField, 1, 5);
        }

        comboBlinds.valueProperty().addListener((obs, oldV, newV) -> {
            lblPrevBlinds.setText("💵 Blind: " + newV);
            if (newV.contains("$50 / $100")) txtBuyIn.setText("10000");
            else if (newV.contains("$10 / $20")) txtBuyIn.setText("2000");
            else if (newV.contains("$100 / $200")) txtBuyIn.setText("20000");
            else if (newV.contains("$500")) txtBuyIn.setText("100000");
            else if (newV.contains("$1,000")) txtBuyIn.setText("200000");
        });

        txtBuyIn.textProperty().addListener((obs, oldV, newV) -> {
            try {
                int val = Integer.parseInt(newV.replaceAll("[^0-9]", ""));
                lblPrevBuyIn.setText("💰 Buy-in: $" + String.format("%,d", val));
            } catch (Exception ignored) {}
        });

        // Action Button: CREATE
        Button btnCreate = new Button("Tạo bàn");
        btnCreate.setMaxWidth(Double.MAX_VALUE);
        btnCreate.setPrefHeight(48);
        OrnateUi.button(btnCreate, "button-red.png", 17);

        btnCreate.setOnAction(e -> {
            String bVal = comboBlinds.getValue();
            int sb = 50;
            int bb = 100;
            if (bVal.contains("$10 / $20")) { sb = 10; bb = 20; }
            else if (bVal.contains("$100 / $200")) { sb = 100; bb = 200; }
            else if (bVal.contains("$500")) { sb = 500; bb = 1000; }
            else if (bVal.contains("$1,000")) { sb = 1000; bb = 2000; }

            int buyIn = 10000;
            try {
                buyIn = Integer.parseInt(txtBuyIn.getText().replaceAll("[^0-9]", ""));
            } catch (Exception ignored) {}

            if (onlineMode && privateRoom.isSelected() &&
                    (passwordField.getText().length() < 8 || passwordField.getText().length() > 72)) {
                new Alert(Alert.AlertType.WARNING, "Mật khẩu phòng phải có 8–72 ký tự.", ButtonType.OK).showAndWait();
                return;
            }
            if (onlineMode && (txtName.getText().isBlank() || buyIn < 1)) {
                new Alert(Alert.AlertType.WARNING, "Tên phòng và buy-in phải hợp lệ.", ButtonType.OK).showAndWait();
                return;
            }
            callback.onCreateRoom(txtName.getText(), comboPlayers.getValue(), sb, bb, buyIn,
                    onlineMode && privateRoom.isSelected(), onlineMode ? passwordField.getText() : "");
            dialog.close();
        });

        rightFormPanel.getChildren().addAll(formTitle, grid, btnCreate);

        mainWorkspace.getChildren().addAll(leftPreviewPanel, rightFormPanel);

        root.getChildren().addAll(topHeaderBar, mainWorkspace);

        Scene scene = new Scene(root, width, height);
        V2Theme.apply(scene);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
