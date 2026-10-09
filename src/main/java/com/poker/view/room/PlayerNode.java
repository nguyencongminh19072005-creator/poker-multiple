package com.poker.view.room;

import com.poker.view.AssetLoader;

import javafx.animation.ScaleTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import com.poker.model.game.card.Card;
import com.poker.view.room.SeatViewModel;

import java.util.List;

public class PlayerNode extends VBox {
    private final SeatViewModel player;
    private int visualSeatPosition; // 0 is the local player's bottom-center position.

    private final ImageView avatarView = new ImageView();
    private final Label nameLabel = new Label();
    private final Label chipsLabel = new Label();
    private final Label levelBadge = new Label("35");
    private final Label flagBadge = new Label();
    private final Label roleBadgeView = new Label();
    private final Label stateBadge = new Label();
    private final ProgressBar turnTimer = new ProgressBar(1);
    private final ImageView hostCrownView = new ImageView();
    private final HBox cardsBox = new HBox(-12); // Overlapping cards
    private javafx.animation.Animation showdownFlip;
    private final Label emojiBubble = new Label();
    private final StackPane winBanner = new StackPane();
    private final Label winTextLabel = new Label("WIN");
    private final Label winAmountLabel = new Label();

    private final StackPane avatarContainer = new StackPane();
    private final VBox infoBox = new VBox(1);
    private final StackPane seatVisual = new StackPane();
    private final ImageView seatFrameView = new ImageView();
    private final HBox seatContent = new HBox(6);
    private final HBox betBox = new HBox(4);
    private final ImageView betChipView = new ImageView();
    private final Label betAmountLabel = new Label();
    private final StackPane avatarWithBadges;

    public PlayerNode(SeatViewModel player, int visualSeatPosition, String levelStr, String flagStr) {
        this.player = player;
        this.visualSeatPosition = visualSeatPosition;
        this.setSpacing(4);
        this.setAlignment(Pos.CENTER);
        this.setPrefWidth(190);
        this.setMaxWidth(190);
        this.setPadding(new javafx.geometry.Insets(4, 5, 4, 5));

        if (levelStr != null) levelBadge.setText(levelStr);
        if (flagStr != null) flagBadge.setText(flagStr);

        // Avatar setup
        avatarView.setFitWidth(52);
        avatarView.setFitHeight(52);
        avatarView.setPreserveRatio(true);
        if (player.getAvatarFileName() != null) {
            avatarView.setImage(AssetLoader.getAvatarImage(player.getAvatarFileName()));
        }

        Circle clip = new Circle(26, 26, 24);
        avatarView.setClip(clip);

        Circle borderCircle = new Circle(26, 26, 25);
        borderCircle.setFill(Color.TRANSPARENT);
        borderCircle.setStroke(Color.web("#d4af37"));
        borderCircle.setStrokeWidth(2.5);

        // Level badge (top-left)
        levelBadge.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: #ecf0f1; -fx-font-weight: bold; " +
                "-fx-font-size: 9px; -fx-padding: 1 4; -fx-background-radius: 4; -fx-border-color: #7f8c8d; -fx-border-radius: 4;");

        // Flag badge (bottom-right)
        flagBadge.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 10px;");

        // Host crown badge (top-center)
        if (player.getSeatIndex() == 0) hostCrownView.setImage(AssetLoader.getHostCrownImage());
        hostCrownView.setFitWidth(25);
        hostCrownView.setFitHeight(25);
        hostCrownView.setPreserveRatio(true);
        hostCrownView.setTranslateY(-15);

        // Role badge (bottom-left): SB / BB / D
        roleBadgeView.setStyle("-fx-background-color: #fff1c0; -fx-background-radius: 20; " +
                "-fx-border-color: #b98535; -fx-border-radius: 20; -fx-padding: 2 4; " +
                "-fx-text-fill: #3a2410; -fx-font-size: 9px; -fx-font-weight: 900;");
        if (player.getRole() == SeatViewModel.PlayerRole.SMALL_BLIND) {
            roleBadgeView.setText("SB");
        } else if (player.getRole() == SeatViewModel.PlayerRole.BIG_BLIND) {
            roleBadgeView.setText("BB");
        } else if (player.getRole() == SeatViewModel.PlayerRole.DEALER) {
            roleBadgeView.setText("D");
        }

        avatarContainer.getChildren().addAll(borderCircle, avatarView);
        
        avatarWithBadges = new StackPane(avatarContainer, hostCrownView, levelBadge, flagBadge, roleBadgeView);
        StackPane.setAlignment(hostCrownView, Pos.TOP_CENTER);
        StackPane.setAlignment(levelBadge, Pos.TOP_LEFT);
        StackPane.setAlignment(flagBadge, Pos.BOTTOM_RIGHT);
        StackPane.setAlignment(roleBadgeView, Pos.BOTTOM_LEFT);

        // Name & Chips box
        nameLabel.setText(player.getName());
        nameLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 12px; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 3, 0, 0, 1);");

        chipsLabel.setStyle("-fx-text-fill: #ffdc79; -fx-font-weight: bold; -fx-font-size: 12px; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 3, 0, 0, 1);");

        infoBox.setAlignment(Pos.CENTER);
        infoBox.setMinWidth(98);
        infoBox.setMaxWidth(105);
        infoBox.setStyle("-fx-padding: 5 4; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 4, 0, 0, 1);");
        stateBadge.setStyle("-fx-text-fill:#ffd85a;-fx-font-size:9px;-fx-font-weight:900;");
        turnTimer.setMaxWidth(96);
        turnTimer.setPrefHeight(5);
        turnTimer.setVisible(false);
        turnTimer.setManaged(false);
        infoBox.getChildren().addAll(nameLabel, chipsLabel, stateBadge, turnTimer);

        Image seatFrame = AssetLoader.getPlayerSeatImage();
        if (seatFrame != null) {
            seatFrameView.setImage(seatFrame);
            seatFrameView.setFitWidth(180);
            seatFrameView.setFitHeight(60);
            seatFrameView.setPreserveRatio(false);
            seatVisual.getChildren().add(seatFrameView);
        }
        seatContent.getChildren().addAll(avatarWithBadges, infoBox);
        seatContent.setAlignment(Pos.CENTER_LEFT);
        seatContent.setPadding(new javafx.geometry.Insets(3, 5, 3, 4));
        seatVisual.getChildren().add(seatContent);
        seatVisual.setPrefSize(180, 60);
        seatVisual.setMaxSize(180, 60);
        seatVisual.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.7), 8, 0, 0, 3);");

        // Cards box setup
        cardsBox.setAlignment(Pos.CENTER);
        cardsBox.setMinHeight(72);

        // WIN Banner setup
        winTextLabel.setStyle("-fx-text-fill: #ecfdf5; " +
                "-fx-font-weight: 900; -fx-font-size: 20px; -fx-letter-spacing: 2px; " +
                "-fx-effect: dropshadow(three-pass-box, #065f46, 8, 0, 0, 1);");
        
        winAmountLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold; -fx-font-size: 13px;");

        VBox winContent = new VBox(0, winTextLabel, winAmountLabel);
        winContent.setAlignment(Pos.CENTER);

        winBanner.setStyle("-fx-background-color:linear-gradient(to right,#166534,#16a34a);"
                + "-fx-padding:4 18;-fx-background-radius:15;-fx-border-color:#86efac;"
                + "-fx-border-radius:15;-fx-border-width:1.5;");
        winBanner.setEffect(new DropShadow(15, Color.web("#22c55e")));
        winBanner.getChildren().add(winContent);
        winBanner.setVisible(false);

        // Emoji bubble
        emojiBubble.setStyle("-fx-font-size: 26px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 5, 0, 0, 1);");
        emojiBubble.setVisible(false);

        // Bet display box
        betChipView.setFitWidth(18);
        betChipView.setFitHeight(18);
        betChipView.setPreserveRatio(true);
        betAmountLabel.setStyle("-fx-text-fill: #f1c40f; -fx-font-weight: bold; -fx-font-size: 11px;");
        betBox.setAlignment(Pos.CENTER);
        betBox.setStyle("-fx-background-color: rgba(0,0,0,0.75); -fx-padding: 2 6; -fx-background-radius: 10;");
        betBox.getChildren().addAll(betChipView, betAmountLabel);
        betBox.setVisible(false);

        rebuildLayout();
        updateView(false, false, 0);
    }

    public void setVisualSeatPosition(int visualSeatPosition) {
        this.visualSeatPosition = visualSeatPosition;
        rebuildLayout();
    }

    /** Keep the avatar at the rail marker while the information panel faces the table. */
    public void setFacingLeft(boolean facingLeft) {
        seatFrameView.setScaleX(facingLeft ? -1 : 1);
        seatContent.getChildren().clear();
        if (facingLeft) seatContent.getChildren().addAll(infoBox, avatarWithBadges);
        else seatContent.getChildren().addAll(avatarWithBadges, infoBox);
        seatContent.setAlignment(facingLeft ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
    }

    /** Conceal already-received cards until the visual dealing animation reaches this seat. */
    public void setCardsTemporarilyHidden(boolean hidden) {
        cardsBox.setOpacity(hidden ? 0 : 1);
    }

    public void revealDealtCards() {
        cardsBox.setOpacity(1);
        cardsBox.setScaleX(0.15);
        ScaleTransition flip = new ScaleTransition(Duration.millis(150), cardsBox);
        flip.setToX(1);
        flip.play();
    }

    /** Flip two backs to the server-revealed faces after the hand is settled. */
    public void revealShowdownCards(List<Card> holeCards) {
        if (holeCards == null || holeCards.size() != 2) return;
        ScaleTransition close = new ScaleTransition(Duration.millis(180), cardsBox);
        close.setFromX(1);
        close.setToX(0.05);
        close.setOnFinished(event -> {
            cardsBox.getChildren().clear();
            for (Card card : holeCards) cardsBox.getChildren().add(onlineCard(AssetLoader.getCardImage(card)));
            ScaleTransition open = new ScaleTransition(Duration.millis(180), cardsBox);
            open.setFromX(0.05);
            open.setToX(1);
            showdownFlip = open;
            open.play();
        });
        showdownFlip = close;
        close.play();
    }

    public void showFinishedCardsClearly() {
        setOpacity(1);
    }

    private void rebuildLayout() {
        this.getChildren().clear();
        StackPane cardsWinStack = new StackPane(cardsBox, winBanner, emojiBubble);

        if (visualSeatPosition == 0) {
            // Visual Seat 0 (Hero Position at Bottom of screen): Cards above avatar
            StackPane.setAlignment(emojiBubble, Pos.TOP_LEFT);
            this.getChildren().addAll(betBox, cardsWinStack, seatVisual);
        } else {
            // Visual Seats around top/sides: Cards attached to avatar
            StackPane.setAlignment(emojiBubble, Pos.TOP_RIGHT);
            this.getChildren().addAll(seatVisual, cardsWinStack, betBox);
        }
    }

    public void updateView(boolean isTurn, boolean showAllCards, int heroSeatIndex) {
        nameLabel.setText(player.getName());
        chipsLabel.setText("$" + String.format("%,d", player.getChips()));

        // Turn highlight
        if (isTurn) {
            this.setEffect(new DropShadow(22, Color.web("#f1c40f")));
            avatarContainer.setStyle("-fx-border-color: #f39c12; -fx-border-width: 3; -fx-border-radius: 50;");
        } else {
            this.setEffect(new DropShadow(6, Color.rgb(0, 0, 0, 0.6)));
            avatarContainer.setStyle("");
        }

        // Folded / All in
        if (player.getStatus() == SeatViewModel.PlayerStatus.FOLDED) {
            this.setOpacity(0.45);
        } else {
            this.setOpacity(1.0);
        }

        // Bet display
        if (player.getCurrentBet() > 0) {
            betChipView.setImage(AssetLoader.getChipImageForValue(player.getCurrentBet()));
            betAmountLabel.setText("$" + String.format("%,d", player.getCurrentBet()));
            betBox.setVisible(true);
        } else {
            betBox.setVisible(false);
        }

        // Cards display: Reveal cards if this player is the Local Hero at heroSeatIndex or if Showdown
        boolean isHero = (player.getSeatIndex() == heroSeatIndex);
        cardsBox.getChildren().clear();
        List<Card> holeCards = player.getHoleCards();
        if (holeCards != null && !holeCards.isEmpty()) {
            for (Card card : holeCards) {
                ImageView cardView = new ImageView();
                cardView.setFitWidth(52);
                cardView.setFitHeight(76);
                cardView.setPreserveRatio(true);

                if (isHero || showAllCards || player.getStatus() == SeatViewModel.PlayerStatus.ALL_IN) {
                    cardView.setImage(AssetLoader.getCardImage(card));
                } else {
                    cardView.setImage(AssetLoader.getCardBackImage("red"));
                }

                DropShadow shadow = new DropShadow(6, Color.rgb(0, 0, 0, 0.7));
                cardView.setEffect(shadow);
                cardsBox.getChildren().add(cardView);
            }
        }
    }

    /** Paint the server's public seat and the current user's private cards. */
    public void updateWaitingSeat(long chips, boolean occupied, boolean host) {
        levelBadge.setVisible(false);
        flagBadge.setVisible(false);
        avatarContainer.setVisible(occupied);
        hostCrownView.setImage(occupied && host ? AssetLoader.getHostCrownImage() : null);
        roleBadgeView.setVisible(false);
        chipsLabel.setText(occupied ? "$" + String.format("%,d", chips) : "Ghế trống");
        cardsBox.getChildren().clear();
        betBox.setVisible(false);
        stateBadge.setText(occupied ? "NOT READY" : "");
        showTurnTimer(false, 0, 1);
    }

    public void setReadyStatus(boolean occupied, boolean ready) {
        stateBadge.setText(!occupied ? "" : ready ? "✓ READY" : "NOT READY");
        stateBadge.setStyle("-fx-text-fill:" + (ready ? "#52df82" : "#aeb6bc")
                + ";-fx-font-size:9px;-fx-font-weight:900;");
    }

    public void updateOnline(boolean occupied, long chips, long currentBet, String participation, boolean connected,
                             boolean isTurn, List<Card> ownCards, boolean showBacks,
                             boolean host, String role) {
        updateOnline(occupied, chips, currentBet, participation, connected, isTurn, ownCards,
                showBacks, host, role, "");
    }

    public void updateOnline(boolean occupied, long chips, long currentBet, String participation, boolean connected,
                             boolean isTurn, List<Card> ownCards, boolean showBacks,
                             boolean host, String role, String lastAction) {
        if (showdownFlip != null) {
            showdownFlip.stop();
            showdownFlip = null;
            cardsBox.setScaleX(1);
        }
        chipsLabel.setText(occupied ? "$" + String.format("%,d", chips) : "Ghế trống");
        levelBadge.setVisible(false);
        flagBadge.setVisible(false);
        avatarContainer.setVisible(occupied);
        roleBadgeView.setText(role);
        roleBadgeView.setVisible(role != null && !role.isBlank());
        hostCrownView.setImage(host ? AssetLoader.getHostCrownImage() : null);
        this.setOpacity(occupied && (!connected || "FOLDED".equals(participation)) ? 0.45 : 1.0);
        this.setEffect(new DropShadow(isTurn ? 22 : 6,
                isTurn ? Color.web("#f1c40f") : Color.rgb(0, 0, 0, 0.6)));
        avatarContainer.setStyle(isTurn
                ? "-fx-border-color: #f39c12; -fx-border-width: 3; -fx-border-radius: 50;" : "");
        betBox.setVisible(currentBet > 0);
        if (currentBet > 0) {
            betChipView.setImage(AssetLoader.getChipImageForValue((int) Math.min(currentBet, Integer.MAX_VALUE)));
            betAmountLabel.setText("$" + String.format("%,d", currentBet));
        }
        cardsBox.getChildren().clear();
        String action = lastAction == null ? "" : lastAction.toUpperCase(java.util.Locale.ROOT);
        stateBadge.setText("FOLDED".equals(participation) ? "FOLDED"
                : "ALL_IN".equals(participation) ? "ALL-IN"
                : isTurn ? "ĐANG HÀNH ĐỘNG"
                : java.util.Set.of("CHECK", "CALL", "BET", "RAISE").contains(action) ? action : "");
        stateBadge.setStyle("-fx-text-fill:" + ("FOLDED".equals(participation) ? "#9aa0a4" : "#ffd85a")
                + ";-fx-font-size:9px;-fx-font-weight:900;");
        showTurnTimer(isTurn, 1, 1);
        if (ownCards != null && !ownCards.isEmpty()) {
            for (Card card : ownCards) cardsBox.getChildren().add(onlineCard(AssetLoader.getCardImage(card)));
        } else if (showBacks) {
            cardsBox.getChildren().addAll(onlineCard(AssetLoader.getCardBackImage("red")),
                    onlineCard(AssetLoader.getCardBackImage("red")));
        }
    }

    public void showTurnTimer(boolean visible, long remainingSeconds, long totalSeconds) {
        turnTimer.setVisible(visible);
        turnTimer.setManaged(visible);
        if (visible) turnTimer.setProgress(Math.max(0, Math.min(1,
                remainingSeconds / (double) Math.max(1, totalSeconds))));
    }

    private ImageView onlineCard(javafx.scene.image.Image image) {
        ImageView view = new ImageView(image);
        view.setFitWidth(52);
        view.setFitHeight(76);
        view.setPreserveRatio(true);
        view.setEffect(new DropShadow(6, Color.rgb(0, 0, 0, 0.7)));
        return view;
    }

    public void showWin(long amount) {
        winTextLabel.setText("WIN");
        winTextLabel.setStyle("-fx-text-fill:#ecfdf5;"
                + "-fx-font-weight:900;-fx-font-size:20px;");
        winAmountLabel.setText("$" + String.format("%,d", amount));
        winAmountLabel.setStyle("-fx-text-fill:#2ecc71;-fx-font-weight:bold;-fx-font-size:13px;");
        winBanner.setStyle("-fx-background-color:linear-gradient(to right,#166534,#16a34a);"
                + "-fx-padding:4 18;-fx-background-radius:15;-fx-border-color:#86efac;"
                + "-fx-border-radius:15;-fx-border-width:1.5;");
        winBanner.setEffect(new DropShadow(15, Color.web("#22c55e")));
        winBanner.setVisible(true);

        ScaleTransition st = new ScaleTransition(Duration.millis(400), winBanner);
        st.setFromX(0.5);
        st.setFromY(0.5);
        st.setToX(1.1);
        st.setToY(1.1);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();
    }

    public void showLost() {
        winTextLabel.setText("LOST");
        winTextLabel.setStyle("-fx-text-fill:#e0e0e0;-fx-font-weight:900;-fx-font-size:20px;");
        winAmountLabel.setText("");
        winBanner.setStyle("-fx-background-color:linear-gradient(to right,#34383d,#666c72);"
                + "-fx-padding:4 18;-fx-background-radius:15;-fx-border-color:#9ca3aa;"
                + "-fx-border-radius:15;-fx-border-width:1.5;");
        winBanner.setEffect(new DropShadow(8, Color.rgb(0, 0, 0, 0.7)));
        winBanner.setScaleX(1);
        winBanner.setScaleY(1);
        winBanner.setVisible(true);
    }

    public void showDraw(long amount) {
        winTextLabel.setText("DRAW");
        winTextLabel.setStyle("-fx-text-fill:#fffdf0;-fx-font-weight:900;-fx-font-size:20px;");
        winAmountLabel.setText("$" + String.format("%,d", amount));
        winAmountLabel.setStyle("-fx-text-fill:#fff2a8;-fx-font-weight:bold;-fx-font-size:13px;");
        winBanner.setStyle("-fx-background-color:linear-gradient(to right,#a16207,#eab308);"
                + "-fx-padding:4 18;-fx-background-radius:15;-fx-border-color:#fde68a;"
                + "-fx-border-radius:15;-fx-border-width:1.5;");
        winBanner.setEffect(new DropShadow(12, Color.web("#facc15")));
        winBanner.setScaleX(1);
        winBanner.setScaleY(1);
        winBanner.setVisible(true);
    }

    public void hideWin() {
        winBanner.setVisible(false);
    }

    public void triggerEmoji(String emoji) {
        emojiBubble.setText(emoji);
        emojiBubble.setVisible(true);

        ScaleTransition st = new ScaleTransition(Duration.millis(350), emojiBubble);
        st.setFromX(0.2);
        st.setFromY(0.2);
        st.setToX(1.3);
        st.setToY(1.3);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.setOnFinished(e -> {
            javafx.animation.PauseTransition pt = new javafx.animation.PauseTransition(Duration.seconds(2));
            pt.setOnFinished(ev -> emojiBubble.setVisible(false));
            pt.play();
        });
        st.play();
    }

    public SeatViewModel getPlayer() {
        return player;
    }
}
