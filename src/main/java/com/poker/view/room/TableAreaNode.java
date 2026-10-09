package com.poker.view.room;

import com.poker.view.AssetLoader;

import javafx.animation.ScaleTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.util.Duration;
import com.poker.model.game.card.Card;

import java.util.List;

public class TableAreaNode extends StackPane {
    private final HBox communityCardsBox = new HBox(8);
    private final boolean[] temporarilyHiddenCards = new boolean[5];
    private int revealedCardCount;
    private final Label potLabel = new Label("Total Pot : $0");
    private final Label phaseLabel = new Label("WAITING");
    private final VBox centerBox = new VBox(12);
    private final StackPane dealerButton = new StackPane();
    private final ImageView tableImageView = new ImageView();

    public TableAreaNode() {
        buildTableUI();
    }

    private void buildTableUI() {
        this.setPrefSize(920, 600);
        this.setMinSize(0, 0);
        this.setStyle("-fx-background-color: radial-gradient(center 50% 48%, radius 58%, rgba(20,78,61,0.20), transparent 72%);");

        // Vector layers keep the table polished even when optional image assets are missing.
        Ellipse tableShadow = createEllipse(438, 214, Color.rgb(0, 0, 0, 0.72));
        tableShadow.setTranslateY(12);
        tableShadow.setEffect(new DropShadow(34, Color.rgb(0, 0, 0, 0.88)));

        Ellipse outerRail = createEllipse(430, 207, Color.web("#8b642c"));
        outerRail.setStroke(Color.web("#d8b35b"));
        outerRail.setStrokeWidth(3);

        Ellipse innerRail = createEllipse(412, 190, Color.web("#17120e"));
        innerRail.setStroke(Color.web("#49351d"));
        innerRail.setStrokeWidth(3);

        Ellipse felt = createEllipse(398, 177, Color.web("#0b503b"));
        felt.setStroke(Color.web("#188060"));
        felt.setStrokeWidth(2);

        // 1. High-Res OpenDecks Golden Rim Poker Table PNG
        Image tableImg = AssetLoader.getTableImage();
        if (tableImg != null) {
            tableImageView.setImage(tableImg);
            tableImageView.setFitWidth(860);
            tableImageView.setFitHeight(575);
            tableImageView.setPreserveRatio(true);
            tableImageView.setEffect(new DropShadow(30, Color.rgb(0, 0, 0, 0.95)));
        }

        // 2. Dealer Button (Custom Asset or Golden D coin)
        Image dImg = AssetLoader.getDealerButtonImage();
        if (dImg != null) {
            ImageView dView = new ImageView(dImg);
            dView.setFitWidth(32);
            dView.setFitHeight(32);
            dView.setPreserveRatio(true);
            dView.setEffect(new DropShadow(8, Color.rgb(0, 0, 0, 0.85)));
            dealerButton.getChildren().add(dView);
        } else {
            Circle dCoin = new Circle(14);
            dCoin.setFill(LinearGradient.valueOf("linear-gradient(to bottom, #f39c12, #d4af37)"));
            dCoin.setStroke(Color.web("#ffffff"));
            dCoin.setStrokeWidth(1.5);
            dCoin.setEffect(new DropShadow(6, Color.rgb(0, 0, 0, 0.7)));

            Label dLabel = new Label("D");
            dLabel.setStyle("-fx-text-fill: #000000; -fx-font-weight: 900; -fx-font-size: 13px;");
            dealerButton.getChildren().addAll(dCoin, dLabel);
        }
        dealerButton.setTranslateX(-240);
        dealerButton.setTranslateY(80);

        // 3. Total Pot Label with Chip Graphic
        potLabel.setStyle("-fx-background-color: rgba(4, 18, 15, 0.88); -fx-text-fill: #ffe083; -fx-font-weight: 900; " +
                "-fx-font-size: 16px; -fx-padding: 7 18; -fx-background-radius: 18; " +
                "-fx-border-color: rgba(255,224,131,0.55); -fx-border-radius: 18; " +
                "-fx-graphic-text-gap: 8; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.85), 9, 0, 0, 2);");
        Image potChipImg = AssetLoader.getChipImageForValue(100);
        if (potChipImg != null) {
            ImageView chipIv = new ImageView(potChipImg);
            chipIv.setFitWidth(24);
            chipIv.setFitHeight(24);
            chipIv.setPreserveRatio(true);
            potLabel.setGraphic(chipIv);
        }

        // 4. Community Cards Box
        communityCardsBox.setAlignment(Pos.CENTER);
        communityCardsBox.setMinHeight(105);

        // 5. Turn Timer Indicator
        HBox timerBox = new HBox(6);
        timerBox.setAlignment(Pos.CENTER);
        Image timerImg = AssetLoader.getTimerImage();
        if (timerImg != null) {
            ImageView timerView = new ImageView(timerImg);
            timerView.setFitWidth(20);
            timerView.setFitHeight(20);
            timerView.setPreserveRatio(true);
            timerBox.getChildren().add(timerView);
        }
        Label timerTxt = new Label("TURN TIMER");
        timerTxt.setStyle("-fx-text-fill: #e5c07b; -fx-font-weight: bold; -fx-font-size: 10px; -fx-letter-spacing: 1px;");
        timerBox.getChildren().add(timerTxt);

        phaseLabel.setStyle("-fx-text-fill:#f5ddb0;-fx-font-size:12px;-fx-font-weight:900;"
                + "-fx-background-color:rgba(0,0,0,.45);-fx-padding:3 12;-fx-background-radius:12;");
        centerBox.setAlignment(Pos.CENTER);
        // Turn time belongs beside the acting seat and in the action panel, where
        // players can associate it with the correct person immediately.
        centerBox.getChildren().addAll(phaseLabel, communityCardsBox, potLabel);

        if (tableImg == null) {
            this.getChildren().addAll(tableShadow, outerRail, innerRail, felt);
        } else {
            this.getChildren().add(tableImageView);
        }
        this.getChildren().addAll(dealerButton, centerBox);
    }

    private Ellipse createEllipse(double radiusX, double radiusY, Color fill) {
        Ellipse ellipse = new Ellipse(radiusX, radiusY);
        ellipse.setFill(fill);
        ellipse.setMouseTransparent(true);
        return ellipse;
    }

    /** Paint server-provided board and commitments without consulting the local demo engine. */
    public void updateOnline(List<Card> board, long committedChips) {
        updateOnline(board, committedChips, "WAITING");
    }

    public void updateOnline(List<Card> board, long committedChips, String phase) {
        revealedCardCount = board.size();
        phaseLabel.setText(phase == null ? "" : phase.replace('_', ' '));
        potLabel.setText("POT  $" + String.format("%,d", committedChips));
        dealerButton.setVisible(false); // Online seats draw the dealer marker beside the owner.
        communityCardsBox.getChildren().clear();
        for (int i = 0; i < 5; i++) {
            ImageView cardView = new ImageView(i < board.size()
                    ? AssetLoader.getCardImage(board.get(i)) : AssetLoader.getCardBackImage("blue"));
            cardView.setFitWidth(65);
            cardView.setFitHeight(94);
            cardView.setPreserveRatio(true);
            cardView.setOpacity(temporarilyHiddenCards[i] ? 0 : i >= board.size() ? 0.18 : 1);
            communityCardsBox.getChildren().add(cardView);
        }
    }

    public void setCommunityCardTemporarilyHidden(int index, boolean hidden) {
        if (index < 0 || index >= temporarilyHiddenCards.length) return;
        temporarilyHiddenCards[index] = hidden;
        if (index < communityCardsBox.getChildren().size())
            communityCardsBox.getChildren().get(index).setOpacity(hidden ? 0 : index < revealedCardCount ? 1 : 0.18);
    }

    public void revealAllCommunityCards() {
        for (int i = 0; i < temporarilyHiddenCards.length; i++) {
            temporarilyHiddenCards[i] = false;
            if (i < communityCardsBox.getChildren().size())
                communityCardsBox.getChildren().get(i).setOpacity(i < revealedCardCount ? 1 : 0.18);
        }
    }

    public void revealCommunityCardWithFlip(int index) {
        setCommunityCardTemporarilyHidden(index, false);
        if (index >= 0 && index < communityCardsBox.getChildren().size()) {
            javafx.scene.Node card = communityCardsBox.getChildren().get(index);
            card.setScaleX(0.15);
            ScaleTransition flip = new ScaleTransition(Duration.millis(155), card);
            flip.setToX(1);
            flip.play();
        }
    }
}
