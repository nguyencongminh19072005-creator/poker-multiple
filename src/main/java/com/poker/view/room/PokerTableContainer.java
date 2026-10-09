package com.poker.view.room;

import com.poker.view.AssetLoader;
import com.poker.view.OrnateUi;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;
import com.poker.view.room.SeatViewModel;

import java.util.ArrayList;
import java.util.List;

public class PokerTableContainer extends BorderPane {
    private static final double BOARD_WIDTH = 920;
    private static final double BOARD_HEIGHT = 600;
    // Clockwise from the local player's fixed bottom-center position.
    private static final double[][] SIX_SEATS = {
            {460, 535}, {220, 460}, {82, 270}, {264, 85}, {656, 85}, {828, 270}
    };
    private static final double[][] NINE_SEATS = {
            {460, 535}, {220, 460}, {74, 270}, {240, 85}, {355, 65},
            {565, 65}, {680, 85}, {828, 270}, {700, 460}
    };
    private static final double[][] SPECTATOR_SIX_SEATS = {
            {220, 460}, {82, 270}, {264, 85}, {656, 85}, {828, 270}, {700, 460}
    };
    private final TableAreaNode tableAreaNode = new TableAreaNode();
    private final List<PlayerNode> playerNodes = new ArrayList<>();
    private int heroSeatIndex = 0; // Local Client Player Seat Index (Default 0)
    private final Pane board = new Pane();
    private final Group scaledBoard = new Group(board);
    // Position the fixed-size board explicitly. StackPane's layout bounds can be
    // enlarged by seat/dealer decorations and visually push the table right.
    private final Pane boardViewport = new Pane(scaledBoard);
    private final Pane cardAnimationLayer = new Pane();
    private Animation cardAnimation;

    public PokerTableContainer(List<SeatViewModel> players) {
        this.setStyle("-fx-background-color: radial-gradient(center 50% 45%, radius 75%, #234537, #170f0d);");
        this.setPadding(new Insets(0));
        this.setMinSize(0, 0);
        this.setPrefSize(BOARD_WIDTH, BOARD_HEIGHT);
        boardViewport.setMinSize(0, 0);
        boardViewport.widthProperty().addListener((obs, oldValue, newValue) -> scaleBoard());
        boardViewport.heightProperty().addListener((obs, oldValue, newValue) -> scaleBoard());

        // Levels & Flags matching screenshot aesthetic
        String[] levels = {"41", "16", "33", "20", "31", "41"};
        String[] flags = {"VN", "BR", "GB", "US", "NL", "DE"};

        // Create Player Nodes with initial visual seat positions
        int nSeats = players.size();
        for (int i = 0; i < nSeats; i++) {
            SeatViewModel p = players.get(i);
            p.setSeatIndex(i); // Assign logical seat index
            String lvl = i < levels.length ? levels[i] : "10";
            String flg = i < flags.length ? flags[i] : "--";
            int initialVisualSeat = (i - heroSeatIndex + nSeats) % nSeats;
            PlayerNode pNode = new PlayerNode(p, initialVisualSeat, lvl, flg);
            playerNodes.add(pNode);
        }

        this.setCenter(boardViewport);
        buildLayout();
    }

    public void setHeroSeatIndex(int newHeroSeatIndex) {
        if (newHeroSeatIndex < -1 || newHeroSeatIndex >= playerNodes.size()) return;
        this.heroSeatIndex = newHeroSeatIndex;
        int nSeats = playerNodes.size();
        for (int i = 0; i < nSeats; i++) {
            int visualSeat = (i - Math.max(0, heroSeatIndex) + nSeats) % nSeats;
            playerNodes.get(i).setVisualSeatPosition(visualSeat);
        }
        buildLayout();
    }

    public int getHeroSeatIndex() {
        return heroSeatIndex;
    }

    private final StackPane winnerResultOverlay = new StackPane();

    private void scaleBoard() {
        double width = boardViewport.getWidth();
        double height = boardViewport.getHeight();
        if (width <= 0 || height <= 0) return;
        double availableWidth = Math.max(1, width - 16);
        double availableHeight = Math.max(1, height - 20);
        double scale = Math.min(1, Math.min(availableWidth / BOARD_WIDTH, availableHeight / BOARD_HEIGHT));
        scaledBoard.setScaleX(scale);
        scaledBoard.setScaleY(scale);
        scaledBoard.setLayoutX((width - BOARD_WIDTH) / 2);
        // Use a little of the available vertical slack for the dealer above the rail.
        double verticalSlack = Math.max(0, height - BOARD_HEIGHT);
        scaledBoard.setLayoutY((height - BOARD_HEIGHT) / 2 + Math.min(25, verticalSlack / 2));
    }

    private void buildLayout() {
        stopCardAnimations();
        board.getChildren().clear();
        board.setPrefSize(BOARD_WIDTH, BOARD_HEIGHT);
        board.setMinSize(BOARD_WIDTH, BOARD_HEIGHT);
        board.setMaxSize(BOARD_WIDTH, BOARD_HEIGHT);
        winnerResultOverlay.setMouseTransparent(true);
        winnerResultOverlay.setVisible(false);
        tableAreaNode.setPrefSize(BOARD_WIDTH, BOARD_HEIGHT);
        tableAreaNode.setMinSize(BOARD_WIDTH, BOARD_HEIGHT);
        tableAreaNode.setMaxSize(BOARD_WIDTH, BOARD_HEIGHT);
        board.getChildren().add(tableAreaNode);

        Image dealer = AssetLoader.getFemaleDealerImage();
        if (dealer != null) {
            ImageView dealerView = new ImageView(dealer);
            dealerView.setFitHeight(150);
            dealerView.setFitWidth(100);
            dealerView.setPreserveRatio(true);
            dealerView.setLayoutX((BOARD_WIDTH - 100) / 2);
            dealerView.setLayoutY(-82); // Feet meet the outer upper rail, not the felt.
            dealerView.setMouseTransparent(true);
            board.getChildren().add(dealerView);
        }

        double[][] anchors = playerNodes.size() > 6 ? NINE_SEATS
                : heroSeatIndex < 0 ? SPECTATOR_SIX_SEATS : SIX_SEATS;
        int nSeats = playerNodes.size();
        for (int i = 0; i < nSeats; i++) {
            int relative = (i - Math.max(0, heroSeatIndex) + nSeats) % nSeats;
            int slot = heroSeatIndex < 0 && nSeats <= 6
                    ? spectatorSlot(relative, nSeats) : visualSlot(relative, nSeats);
            PlayerNode node = playerNodes.get(i);
            node.setVisualSeatPosition(heroSeatIndex >= 0 ? relative : relative + 1);
            boolean hero = heroSeatIndex >= 0 && relative == 0;
            boolean facingLeft = !hero && anchors[slot][0] > BOARD_WIDTH / 2;
            node.setFacingLeft(facingLeft);
            node.setLayoutX(anchors[slot][0] - (hero ? 95 : facingLeft ? 153 : 38));
            node.setLayoutY(anchors[slot][1] - (hero ? 102 : 34));
            board.getChildren().add(node);
        }
        winnerResultOverlay.setPrefSize(BOARD_WIDTH, BOARD_HEIGHT);
        board.getChildren().add(winnerResultOverlay);
        cardAnimationLayer.setPrefSize(BOARD_WIDTH, BOARD_HEIGHT);
        cardAnimationLayer.setMouseTransparent(true);
        board.getChildren().add(cardAnimationLayer);
    }

    /** Only animates server-authorized public seats; it never selects or exposes card faces. */
    public void animateHandDeal(List<Integer> occupiedSeats) {
        stopCardAnimations();
        if (occupiedSeats == null || occupiedSeats.isEmpty()) return;
        Image back = AssetLoader.getCardBackImage("red");
        if (back == null) return;
        SequentialTransition sequence = new SequentialTransition();
        ParallelTransition shuffle = new ParallelTransition();
        List<ImageView> deckCards = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ImageView deckCard = flyingCard(back, 460 + i * 3, 112 - i * 2);
            deckCard.setOpacity(1);
            cardAnimationLayer.getChildren().add(deckCard);
            deckCards.add(deckCard);
            TranslateTransition fan = new TranslateTransition(Duration.millis(155), deckCard);
            fan.setByX((i - 1) * 24);
            fan.setByY(i == 1 ? -14 : 6);
            fan.setAutoReverse(true);
            fan.setCycleCount(2);
            shuffle.getChildren().add(fan);
        }
        shuffle.setOnFinished(event -> cardAnimationLayer.getChildren().removeAll(deckCards));
        sequence.getChildren().add(shuffle);
        for (int seat : occupiedSeats) {
            if (seat >= 1 && seat <= playerNodes.size())
                playerNodes.get(seat - 1).setCardsTemporarilyHidden(true);
        }
        for (int pass = 0; pass < 2; pass++) {
            for (int seat : occupiedSeats) {
                if (seat < 1 || seat > playerNodes.size()) continue;
                PlayerNode target = playerNodes.get(seat - 1);
                double targetX = target.getLayoutX() + 95 + (pass == 0 ? -12 : 12);
                double targetY = target.getLayoutY() + (seat - 1 == heroSeatIndex ? 42 : 93);
                sequence.getChildren().add(cardFlight(back, targetX, targetY,
                        pass == 1 ? target::revealDealtCards : null));
            }
        }
        cardAnimation = sequence;
        sequence.setOnFinished(event -> {
            cardAnimationLayer.getChildren().clear();
            for (PlayerNode player : playerNodes) player.setCardsTemporarilyHidden(false);
            cardAnimation = null;
        });
        sequence.play();
    }

    /** Flop, turn and river are animated only after their cards arrive in a server snapshot. */
    public void animateCommunityDeal(int fromCount, int toCount) {
        stopCardAnimations();
        Image back = AssetLoader.getCardBackImage("red");
        if (back == null) return;
        int first = Math.max(0, fromCount);
        int last = Math.min(5, toCount);
        if (first >= last) return;
        SequentialTransition sequence = new SequentialTransition();
        for (int index = first; index < last; index++) {
            final int slot = index;
            tableAreaNode.setCommunityCardTemporarilyHidden(slot, true);
            double centerX = 460 - 178.5 + slot * 73 + 32.5;
            sequence.getChildren().add(cardFlight(back, centerX, 303,
                    () -> tableAreaNode.revealCommunityCardWithFlip(slot)));
        }
        cardAnimation = sequence;
        sequence.setOnFinished(event -> {
            cardAnimationLayer.getChildren().clear();
            tableAreaNode.revealAllCommunityCards();
            cardAnimation = null;
        });
        sequence.play();
    }

    private SequentialTransition cardFlight(Image back, double targetX, double targetY, Runnable reveal) {
        ImageView flying = flyingCard(back, 460, 112);
        flying.setOpacity(0);
        cardAnimationLayer.getChildren().add(flying);
        FadeTransition appear = new FadeTransition(Duration.millis(1), flying);
        appear.setToValue(1);
        TranslateTransition travel = new TranslateTransition(Duration.millis(205), flying);
        travel.setToX(targetX - 460);
        travel.setToY(targetY - 112);
        travel.setInterpolator(javafx.animation.Interpolator.EASE_BOTH);
        SequentialTransition flight = new SequentialTransition(appear, travel);
        flight.setOnFinished(event -> {
            cardAnimationLayer.getChildren().remove(flying);
            if (reveal != null) reveal.run();
        });
        return flight;
    }

    private static ImageView flyingCard(Image image, double centerX, double centerY) {
        ImageView view = new ImageView(image);
        view.setFitWidth(36);
        view.setFitHeight(52);
        view.setPreserveRatio(true);
        view.setLayoutX(centerX - 18);
        view.setLayoutY(centerY - 26);
        view.setEffect(new javafx.scene.effect.DropShadow(9, javafx.scene.paint.Color.web("#130b08")));
        return view;
    }

    public void stopCardAnimations() {
        if (cardAnimation != null) {
            cardAnimation.stop();
            cardAnimation = null;
        }
        cardAnimationLayer.getChildren().clear();
        for (PlayerNode player : playerNodes) player.setCardsTemporarilyHidden(false);
        tableAreaNode.revealAllCommunityCards();
    }

    private static int visualSlot(int relative, int capacity) {
        if (capacity == 2) return new int[]{0, 4}[relative];
        if (capacity == 3) return new int[]{0, 3, 4}[relative];
        if (capacity == 4) return new int[]{0, 1, 3, 5}[relative];
        if (capacity == 5) return new int[]{0, 1, 3, 4, 5}[relative];
        if (capacity == 6) return relative;
        if (capacity == 7) return new int[]{0, 1, 2, 3, 5, 7, 8}[relative];
        if (capacity == 8) return new int[]{0, 1, 2, 3, 4, 5, 7, 8}[relative];
        return Math.min(8, relative);
    }

    private static int spectatorSlot(int relative, int capacity) {
        if (capacity == 2) return new int[]{2, 3}[relative];
        if (capacity == 3) return new int[]{0, 2, 4}[relative];
        if (capacity == 4) return new int[]{0, 2, 3, 5}[relative];
        if (capacity == 5) return new int[]{0, 1, 2, 3, 5}[relative];
        return relative;
    }

    public void triggerShowdownWin(List<SeatViewModel> winners, int potAmount) {
        for (PlayerNode pNode : playerNodes) {
            if (winners.contains(pNode.getPlayer())) {
                pNode.showWin(potAmount / winners.size());
            }
        }
        showWinnerOverlay(winners, potAmount);
    }

    /** Displays only the server's settled result; never guesses winners from visible cards. */
    public void showHandResult(String title, String details, long potAmount,
                               java.util.Map<Integer, Long> winnerSeats,
                               java.util.Set<Integer> loserSeats,
                               java.util.Set<Integer> drawSeats) {
        for (int index = 0; index < playerNodes.size(); index++) {
            Long payout = winnerSeats.get(index + 1);
            if (payout != null && payout > 0 && drawSeats.contains(index + 1))
                playerNodes.get(index).showDraw(payout);
            else if (payout != null && payout > 0) playerNodes.get(index).showWin(payout);
            else if (loserSeats.contains(index + 1)) playerNodes.get(index).showLost();
        }
        winnerResultOverlay.getChildren().clear();
        javafx.scene.control.Label titleLabel = new javafx.scene.control.Label(title);
        titleLabel.setStyle("-fx-text-fill: #ffdf73; -fx-font-size: 32px; -fx-font-weight: 900;");
        javafx.scene.control.Label detailsLabel = new javafx.scene.control.Label(details);
        detailsLabel.setWrapText(true);
        detailsLabel.setStyle("-fx-text-fill: white; -fx-font-size: 17px; -fx-font-weight: bold;");
        javafx.scene.control.Label potLabel = new javafx.scene.control.Label(
                "POT  $" + String.format("%,d", potAmount));
        potLabel.setStyle("-fx-text-fill: #ffe9a1; -fx-font-size: 17px; -fx-font-weight: 900;");
        VBox box = new VBox(7, titleLabel, detailsLabel, potLabel);
        box.setAlignment(Pos.CENTER);
        box.setMaxWidth(440);
        box.setStyle(OrnateUi.compactPanelStyle() + " -fx-padding: 20 28;");
        box.setEffect(new javafx.scene.effect.DropShadow(24, javafx.scene.paint.Color.web("#f59e0b")));
        winnerResultOverlay.getChildren().add(box);
        winnerResultOverlay.setVisible(true);
        javafx.animation.ScaleTransition appear = new javafx.animation.ScaleTransition(
                javafx.util.Duration.millis(350), box);
        appear.setFromX(0.4);
        appear.setFromY(0.4);
        appear.setToX(1);
        appear.setToY(1);
        appear.play();
        javafx.animation.PauseTransition dismiss = new javafx.animation.PauseTransition(
                javafx.util.Duration.seconds(5));
        dismiss.setOnFinished(event -> winnerResultOverlay.setVisible(false));
        dismiss.play();
    }

    private void showWinnerOverlay(List<SeatViewModel> winners, int potAmount) {
        winnerResultOverlay.getChildren().clear();
        if (winners == null || winners.isEmpty()) return;
        String names = winners.stream().map(SeatViewModel::getName).collect(java.util.stream.Collectors.joining(" & "));
        javafx.scene.control.Label title = new javafx.scene.control.Label(winners.size() == 1 ? "WINNER!" : "SPLIT POT!");
        title.setStyle("-fx-text-fill: #ffdf73; -fx-font-size: 32px; -fx-font-weight: 900; " +
                "-fx-effect: dropshadow(three-pass-box, #8b3f0a, 8, 0, 0, 2);");
        javafx.scene.control.Label playerNames = new javafx.scene.control.Label(names);
        playerNames.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        javafx.scene.control.Label amount = new javafx.scene.control.Label("POT  $" + String.format("%,d", potAmount));
        amount.setStyle("-fx-text-fill: #ffe9a1; -fx-font-size: 17px; -fx-font-weight: 900;");
        VBox box = new VBox(7, title, playerNames, amount);
        box.setAlignment(Pos.CENTER);
        box.setMaxWidth(360);
        box.setStyle(OrnateUi.compactPanelStyle() + " -fx-padding: 20 28;");
        box.setEffect(new javafx.scene.effect.DropShadow(24, javafx.scene.paint.Color.web("#f59e0b")));
        winnerResultOverlay.getChildren().add(box);
        winnerResultOverlay.setVisible(true);
        javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(350), box);
        st.setFromX(0.4);
        st.setFromY(0.4);
        st.setToX(1.0);
        st.setToY(1.0);
        st.play();
        javafx.animation.PauseTransition pt = new javafx.animation.PauseTransition(javafx.util.Duration.millis(3500));
        pt.setOnFinished(e -> winnerResultOverlay.setVisible(false));
        pt.play();
    }

    public void triggerEmojiOnHero(String emoji) {
        if (heroSeatIndex >= 0 && heroSeatIndex < playerNodes.size()) {
            playerNodes.get(heroSeatIndex).triggerEmoji(emoji);
        }
    }

    public TableAreaNode getTableAreaNode() {
        return tableAreaNode;
    }

    public List<PlayerNode> getPlayerNodes() {
        return playerNodes;
    }
}
