package com.poker.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class LeaderboardView extends BorderPane {
    private HBox podiumRow;
    private VBox ranksList;
    private final boolean onlineMode;

    public record RankingEntry(long rank, String name, long rating, long gamesRated) { }

    public interface LeaderboardActions {
        void onBackToLobby();
    }

    public LeaderboardView(LeaderboardActions actions) {
        this(actions, false);
    }

    public LeaderboardView(LeaderboardActions actions, boolean onlineMode) {
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
        topBar.setPadding(new Insets(14, 35, 14, 35));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: linear-gradient(to bottom, #5a3023, #281710); " +
                "-fx-border-color: #c69a55; -fx-border-width: 0 0 2 0;");

        Button btnBack = new Button("◄ Quay lại Sảnh");
        btnBack.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        btnBack.setPrefSize(165, 46);
        OrnateUi.button(btnBack, "button-red.png", 13);
        btnBack.setOnAction(e -> actions.onBackToLobby());

        Label title = new Label("🏆 BẢNG XẾP HẠNG POKER THẾ GIỚI 🏆");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        title.setTextFill(Color.web("#facc15"));
        title.setStyle("-fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.9), 6, 0.8, 0, 2);");

        topBar.getChildren().addAll(btnBack, title);
        this.setTop(topBar);

        // Center Content
        VBox container = new VBox(25);
        container.setPadding(new Insets(20, 40, 30, 40));
        container.setAlignment(Pos.TOP_CENTER);

        HBox filterBox = new HBox(15);
        filterBox.setAlignment(Pos.CENTER);

        Label lblSort = new Label("Lọc theo:");
        lblSort.setTextFill(Color.WHITE);
        lblSort.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        ComboBox<String> sortCombo = new ComboBox<>();
        sortCombo.getItems().addAll("Ranking Points", "Total Wins", "Total Chips", "Win Rate %");
        sortCombo.setValue("Ranking Points");
        if (onlineMode) sortCombo.setDisable(true);
        sortCombo.setStyle("-fx-background-color: #3b271c; -fx-text-fill: #f4dfb6; -fx-font-size: 13px; " +
                "-fx-border-color: #b58b50; -fx-border-radius: 7; -fx-background-radius: 7;");

        filterBox.getChildren().addAll(lblSort, sortCombo);

        // --- TOP 3 PODIUM CROWNS ROW ---
        podiumRow = new HBox(25);
        podiumRow.setAlignment(Pos.BOTTOM_CENTER);

        if (!onlineMode) {
            VBox cardRank2 = createPodiumCard("🥈 #2", "ShadowAce", "3,120 pts", "298 Wins", "$3,800,000", "64.2%", 2, "#94a3b8", "#e2e8f0");
            VBox cardRank1 = createPodiumCard("🥇 #1", "PokerKing_99", "3,450 pts", "342 Wins", "$4,250,000", "68.5%", 1, "#ca8a04", "#facc15");
            VBox cardRank3 = createPodiumCard("🥉 #3", "LuckyQueen", "2,890 pts", "260 Wins", "$2,950,000", "61.0%", 3, "#b45309", "#f97316");
            podiumRow.getChildren().addAll(cardRank2, cardRank1, cardRank3);
        }

        // --- REMAINING RANKS LIST (#4, #5, #6...) ---
        ranksList = new VBox(10);
        ranksList.setAlignment(Pos.TOP_CENTER);
        ranksList.setPadding(new Insets(10, 20, 10, 20));

        if (!onlineMode) ranksList.getChildren().addAll(
                createRankCard("#4", "BluffMaster", "2,540 pts", "210 Wins", "$2,100,000", "57.8%", 4),
                createRankCard("#5", "TexasHoldEmPro", "2,310 pts", "195 Wins", "$1,850,000", "55.4%", 5),
                createRankCard("#6", "admin", "1,980 pts", "150 Wins", "$1,200,000", "52.1%", 6)
        );

        VBox scrollContent = new VBox(25, filterBox, podiumRow, ranksList);
        scrollContent.setAlignment(Pos.TOP_CENTER);

        ScrollPane scrollPane = new ScrollPane(scrollContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        container.getChildren().add(scrollPane);
        this.setCenter(container);
    }

    public void showOnlineEntries(java.util.List<RankingEntry> entries) {
        podiumRow.getChildren().clear();
        ranksList.getChildren().clear();
        if (entries.isEmpty()) {
            Label empty = new Label("Chưa có bảng xếp hạng từ server.");
            empty.setTextFill(Color.WHITE);
            ranksList.getChildren().add(empty);
            return;
        }
        java.util.Map<Long, RankingEntry> top = new java.util.HashMap<>();
        for (RankingEntry entry : entries) if (entry.rank() <= 3) top.put(entry.rank(), entry);
        for (long rank : new long[]{2, 1, 3}) {
            RankingEntry entry = top.get(rank);
            if (entry != null) podiumRow.getChildren().add(createPodiumCard("#" + rank, entry.name(),
                    entry.rating() + " pts", entry.gamesRated() + " games", "—", "—", (int) rank,
                    "#ca8a04", "#facc15"));
        }
        for (RankingEntry entry : entries) if (entry.rank() > 3) {
            ranksList.getChildren().add(createRankCard("#" + entry.rank(), entry.name(),
                    entry.rating() + " pts", entry.gamesRated() + " games", "—", "—", (int) entry.rank()));
        }
    }

    private VBox createPodiumCard(String rankTitle, String username, String points, String wins, String chips, String winRate, int rank, String primaryColor, String accentColor) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(18, 15, 18, 15));

        int width = (rank == 1) ? 220 : 190;
        int height = (rank == 1) ? 290 : 250;
        card.setPrefSize(width, height);

        card.setStyle(OrnateUi.framedStyle() +
                " -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.58), 14, 0, 0, 5);");

        // Crown / Rank Crown Badge
        Label rankLbl = new Label(rankTitle);
        rankLbl.setFont(Font.font("Arial", FontWeight.BOLD, rank == 1 ? 18 : 15));
        rankLbl.setTextFill(Color.web(accentColor));

        // Avatar
        int avatarIndex = (Math.abs(username.hashCode()) % 6) + 1;
        Image avatarImg = AssetLoader.getAvatarImage("avatar_player_" + avatarIndex + ".png");
        Image frameImg = AssetLoader.getAvatarFrameImage();

        StackPane avatarStack = new StackPane();
        int avatarSize = (rank == 1) ? 75 : 60;
        avatarStack.setPrefSize(avatarSize, avatarSize);

        if (avatarImg != null) {
            javafx.scene.image.ImageView avatarView = new javafx.scene.image.ImageView(avatarImg);
            avatarView.setFitWidth(avatarSize - 15);
            avatarView.setFitHeight(avatarSize - 15);
            avatarView.setClip(new javafx.scene.shape.Circle((avatarSize - 15) / 2.0, (avatarSize - 15) / 2.0, (avatarSize - 15) / 2.0));
            avatarStack.getChildren().add(avatarView);
        }

        if (frameImg != null) {
            javafx.scene.image.ImageView frameView = new javafx.scene.image.ImageView(frameImg);
            frameView.setFitWidth(avatarSize);
            frameView.setFitHeight(avatarSize);
            frameView.setPreserveRatio(true);
            frameView.setClip(new javafx.scene.shape.Circle(avatarSize / 2.0, avatarSize / 2.0, avatarSize / 2.0));
            avatarStack.getChildren().add(frameView);
        }

        Label nameLbl = new Label(username);
        nameLbl.setFont(Font.font("Arial", FontWeight.BOLD, rank == 1 ? 16 : 14));
        nameLbl.setTextFill(Color.WHITE);

        Label ptsLbl = new Label("⭐ " + points);
        ptsLbl.setFont(Font.font("Arial", FontWeight.BOLD, rank == 1 ? 15 : 13));
        ptsLbl.setTextFill(Color.web(accentColor));

        Label chipsLbl = new Label(onlineMode ? wins : "💰 " + chips);
        chipsLbl.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        chipsLbl.setTextFill(Color.GOLD);

        Label winRateLbl = new Label("📈 Win Rate: " + winRate);
        winRateLbl.setFont(Font.font("Arial", FontWeight.NORMAL, 11));
        winRateLbl.setTextFill(Color.LIGHTGRAY);

        card.getChildren().addAll(rankLbl, avatarStack, nameLbl, ptsLbl, chipsLbl);
        if (!onlineMode) card.getChildren().add(winRateLbl);
        return card;
    }

    private HBox createRankCard(String rankStr, String username, String points, String wins, String chips, String winRate, int avatarIndex) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12, 25, 12, 25));
        card.setMaxWidth(800);
        card.setStyle(OrnateUi.compactPanelStyle() + " -fx-cursor: hand;");
        card.setOnMouseEntered(e -> card.setScaleX(1.01));
        card.setOnMouseExited(e -> card.setScaleX(1));

        // Rank Badge
        Label rankBadge = new Label(rankStr);
        rankBadge.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        rankBadge.setTextFill(Color.WHITE);
        rankBadge.setStyle("-fx-background-color: #6b3324; -fx-border-color: #bd9252; " +
                "-fx-border-radius: 13; -fx-background-radius: 13; -fx-padding: 6 14;");

        // Small Avatar
        Image avatarImg = AssetLoader.getAvatarImage("avatar_player_" + ((avatarIndex % 6) + 1) + ".png");
        javafx.scene.image.ImageView avatarView = new javafx.scene.image.ImageView(avatarImg);
        avatarView.setFitWidth(40);
        avatarView.setFitHeight(40);
        avatarView.setClip(new javafx.scene.shape.Circle(20, 20, 20));

        // Name & Wins
        VBox nameBox = new VBox(2);
        Label nameLbl = new Label(username);
        nameLbl.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        nameLbl.setTextFill(Color.WHITE);

        Label winsLbl = new Label(onlineMode ? wins : "🏆 " + wins + "  •  Win Rate: " + winRate);
        winsLbl.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        winsLbl.setTextFill(Color.LIGHTGRAY);
        nameBox.getChildren().addAll(nameLbl, winsLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Stats (Points & Chips)
        VBox statsBox = new VBox(2);
        statsBox.setAlignment(Pos.CENTER_RIGHT);

        Label ptsLbl = new Label("⭐ " + points);
        ptsLbl.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        ptsLbl.setTextFill(Color.web("#facc15"));

        Label chipsLbl = new Label("💰 " + chips);
        chipsLbl.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        chipsLbl.setTextFill(Color.web("#34d399"));

        statsBox.getChildren().add(ptsLbl);
        if (!onlineMode) statsBox.getChildren().add(chipsLbl);

        card.getChildren().addAll(rankBadge, avatarView, nameBox, spacer, statsBox);
        return card;
    }
}
