package com.poker.view.preview;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.poker.model.game.card.Card;
import com.poker.view.room.SeatViewModel;
import com.poker.model.game.card.Rank;
import com.poker.model.game.card.Suit;
import com.poker.view.room.PokerTableContainer;

import java.util.ArrayList;
import java.util.List;

/** Standalone visual preview of the exact JavaFX card animations, without a server. */
public final class AnimationPreview extends Application {
    private static final List<Card> BOARD = List.of(
            new Card(Suit.HEARTS, Rank.ACE), new Card(Suit.SPADES, Rank.KING),
            new Card(Suit.DIAMONDS, Rank.TEN), new Card(Suit.CLUBS, Rank.SEVEN),
            new Card(Suit.HEARTS, Rank.TWO));
    private static final List<Card> HERO_CARDS = List.of(
            new Card(Suit.SPADES, Rank.QUEEN), new Card(Suit.SPADES, Rank.JACK));
    private static final List<Integer> SEATS = List.of(1, 2, 3, 4, 5, 6);

    private PokerTableContainer table;
    private Label status;
    private Timeline autoplay;
    private int revealedCards;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        List<SeatViewModel> players = new ArrayList<>();
        String[] names = {"Bạn", "AnyaP", "Ace", "Lucky", "King", "Queen"};
        for (int i = 0; i < 6; i++)
            players.add(new SeatViewModel(i + 1, names[i], "avatar_player_" + (i + 1) + ".png", 10000, i == 0));
        table = new PokerTableContainer(players);
        table.setHeroSeatIndex(0);

        Label title = new Label("PREVIEW · HOẠT ẢNH BÀI POKER");
        title.setStyle("-fx-text-fill:#ffe39b; -fx-font-size:21px; -fx-font-weight:bold;");
        Label note = new Label("Dùng đúng bàn, ghế và hiệu ứng của game. Dữ liệu bài dưới đây chỉ để xem thử.");
        note.setWrapText(true);
        note.setStyle("-fx-text-fill:#e2cda7; -fx-font-size:12px;");
        status = new Label();
        status.setWrapText(true);
        status.setStyle("-fx-text-fill:#fff1cc; -fx-font-size:14px; -fx-font-weight:bold;");

        Button playAll = button("▶ Xem toàn bộ", this::playAll);
        Button deal = button("1 · Xáo & chia bài", () -> { stopAutoplay(); deal(); });
        Button flop = button("2 · Flop (3 lá)", () -> { stopAutoplay(); showBoard(3); });
        Button turn = button("3 · Turn (1 lá)", () -> { stopAutoplay(); showBoard(4); });
        Button river = button("4 · River (1 lá)", () -> { stopAutoplay(); showBoard(5); });
        Button reset = button("↺ Bắt đầu lại", () -> { stopAutoplay(); resetTable(); });
        VBox sidebar = new VBox(12, title, note, status, playAll, deal, flop, turn, river, reset);
        sidebar.setAlignment(Pos.TOP_LEFT);
        sidebar.setPadding(new Insets(28));
        sidebar.setPrefWidth(300);
        sidebar.setStyle("-fx-background-color:#2d1912; -fx-border-color:#bd8d45; -fx-border-width:0 0 0 2;");

        BorderPane root = new BorderPane();
        root.setCenter(table);
        root.setRight(sidebar);
        root.setStyle("-fx-background-color:#150d0b;");
        BorderPane.setMargin(table, new Insets(16));
        Scene scene = new Scene(root, 1280, 720);
        stage.setTitle("Poker · Preview hoạt ảnh chia bài");
        stage.setScene(scene);
        stage.setMinWidth(1040);
        stage.setMinHeight(640);
        stage.show();
        resetTable();
        VBox.setVgrow(status, Priority.NEVER);
    }

    private static Button button(String text, Runnable action) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefHeight(40);
        button.setStyle("-fx-background-color:linear-gradient(#874311,#4d1d0c); "
                + "-fx-text-fill:#ffe7aa; -fx-font-weight:bold; -fx-border-color:#e2ad57; "
                + "-fx-border-radius:12; -fx-background-radius:12; -fx-cursor:hand;");
        button.setOnAction(event -> action.run());
        return button;
    }

    private void resetTable() {
        table.stopCardAnimations();
        revealedCards = 0;
        table.getTableAreaNode().updateOnline(List.of(), 150);
        for (int i = 0; i < table.getPlayerNodes().size(); i++) {
            table.getPlayerNodes().get(i).updateOnline(true, 10000 - i * 500, 0,
                    "ACTIVE", true, false, i == 0 ? HERO_CARDS : List.of(), i != 0,
                    i == 0, i == 0 ? "D" : i == 1 ? "SB" : i == 2 ? "BB" : "");
        }
        status.setText("Sẵn sàng · bấm xem toàn bộ hoặc từng bước.");
    }

    private void deal() {
        resetTable();
        status.setText("Đang xáo và chia 2 lá tới mỗi ghế…");
        table.animateHandDeal(SEATS);
    }

    private void showBoard(int count) {
        if (count <= revealedCards) return;
        int before = revealedCards;
        revealedCards = Math.min(count, BOARD.size());
        table.getTableAreaNode().updateOnline(BOARD.subList(0, revealedCards), 150);
        table.animateCommunityDeal(before, revealedCards);
        status.setText(revealedCards == 3 ? "Flop · 3 lá bài chung" :
                revealedCards == 4 ? "Turn · lá bài chung thứ 4" : "River · lá bài chung thứ 5");
    }

    private void playAll() {
        stopAutoplay();
        deal();
        autoplay = new Timeline(
                new KeyFrame(Duration.seconds(3.4), event -> showBoard(3)),
                new KeyFrame(Duration.seconds(4.4), event -> showBoard(4)),
                new KeyFrame(Duration.seconds(5.4), event -> showBoard(5)));
        autoplay.play();
    }

    private void stopAutoplay() {
        if (autoplay != null) {
            autoplay.stop();
            autoplay = null;
        }
    }

    @Override
    public void stop() {
        stopAutoplay();
        if (table != null) table.stopCardAnimations();
    }
}
