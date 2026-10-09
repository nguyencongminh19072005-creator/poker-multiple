package com.poker.view.room;

import com.poker.controller.room.RoomUiState;
import com.poker.view.OrnateUi;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Set;
import java.util.function.BiConsumer;

/** Fixed bottom action area. Only server supplied legal actions can be sent. */
public final class ControlPanelView extends VBox {
    private final Label turnLabel = new Label("Đang chờ bắt đầu");
    private final Button fold = new Button("FOLD");
    private final Button checkCall = new Button("CHECK");
    private final Button betRaise = new Button("BET");
    private final Button allInAction = new Button("ALL-IN");
    private final Button minus = new Button("−");
    private final Button plus = new Button("+");
    private final TextField amount = new TextField();
    private final Slider slider = new Slider();
    private final Label limits = new Label();
    private final Label raiseTitle = new Label("Raise amount");
    private final Button halfPot = new Button("1/2 POT");
    private final Button fullPot = new Button("POT");
    private final Button allIn = new Button("ALL-IN");
    private final Button confirm = new Button("CONFIRM");
    private final VBox raisePanel = new VBox(7);
    private BiConsumer<String, Long> send = (action, value) -> { };
    private String wagerAction = "BET";
    private boolean selectedAllIn;
    private long minimum;
    private long maximum;
    private long pot;
    private boolean myTurn;
    private boolean regularWagerAllowed;

    public ControlPanelView() {
        setAlignment(Pos.CENTER);
        setSpacing(5);
        setPadding(new Insets(6, 18, 8, 18));
        setStyle("-fx-background-color:linear-gradient(to bottom,#4d2d20,#160f0d);"
                + "-fx-border-color:#c09245;-fx-border-width:2 0 0 0;");
        turnLabel.setStyle("-fx-text-fill:#f5ddb0;-fx-font-size:15px;-fx-font-weight:900;");
        for (Button button : new Button[]{minus, plus, halfPot, fullPot, confirm}) {
            OrnateUi.button(button, "button-blue.png", 12);
        }
        OrnateUi.actionButton(fold, "red", 17);
        OrnateUi.actionButton(checkCall, "green", 17);
        OrnateUi.actionButton(betRaise, "blue", 17);
        OrnateUi.actionButton(allInAction, "orange", 17);
        OrnateUi.actionButton(allIn, "gold", 12);
        for (Button button : new Button[]{fold, checkCall, betRaise, allInAction}) {
            button.setPrefSize(158, 52);
            button.setMinSize(150, 48);
        }
        HBox actions = new HBox(14, fold, checkCall, betRaise, allInAction);
        actions.setAlignment(Pos.CENTER);
        fold.setOnAction(event -> send.accept("FOLD", 0L));
        checkCall.setOnAction(event -> send.accept(checkCall.getText().startsWith("CALL") ? "CALL" : "CHECK", 0L));
        allInAction.setOnAction(event -> send.accept("ALL_IN", 0L));
        betRaise.setOnAction(event -> {
            if (raisePanel.isManaged()) hideRaisePanel(); else showRaisePanel();
        });

        limits.setStyle("-fx-text-fill:#f5ddb0;-fx-font-size:12px;");
        raiseTitle.setStyle("-fx-text-fill:#ffe29a;-fx-font-size:14px;-fx-font-weight:900;");
        amount.setMaxWidth(150);
        amount.setAlignment(Pos.CENTER);
        amount.setStyle("-fx-background-color:#231610;-fx-text-fill:#ffe39c;-fx-border-color:#b9893f;");
        amount.textProperty().addListener((obs, old, value) -> {
            if (value.matches("[0-9]*") && !value.isEmpty()) {
                try { slider.setValue(Long.parseLong(value)); } catch (NumberFormatException ignored) { }
            }
        });
        slider.valueProperty().addListener((obs, old, value) -> {
            if (slider.isValueChanging()) {
                amount.setText(Long.toString(value.longValue()));
                selectedAllIn = false;
            }
        });
        minus.setOnAction(event -> select(Math.max(minimum, selectedAmount() - step()), false));
        plus.setOnAction(event -> select(Math.min(maximum, selectedAmount() + step()), false));
        halfPot.setOnAction(event -> select(clamp(Math.max(minimum, pot / 2)), false));
        fullPot.setOnAction(event -> select(clamp(Math.max(minimum, pot)), false));
        allIn.setOnAction(event -> select(maximum, true));
        confirm.setOnAction(event -> {
            long value = selectedAmount();
            if (value < minimum || value > maximum) {
                select(clamp(value), false);
                return;
            }
            send.accept(selectedAllIn ? "ALL_IN" : wagerAction, selectedAllIn ? 0L : value);
            hideRaisePanel();
        });
        HBox amountRow = new HBox(10, minus, amount, plus);
        amountRow.setAlignment(Pos.CENTER);
        HBox presets = new HBox(9, halfPot, fullPot, allIn);
        presets.setAlignment(Pos.CENTER);
        raisePanel.getChildren().addAll(raiseTitle, limits, amountRow, slider, presets, confirm);
        raisePanel.setAlignment(Pos.CENTER);
        raisePanel.setMaxWidth(430);
        raisePanel.setPadding(new Insets(9, 16, 10, 16));
        raisePanel.setStyle(OrnateUi.compactPanelStyle());
        VBox.setVgrow(slider, Priority.NEVER);
        getChildren().addAll(raisePanel, turnLabel, actions);
        hideRaisePanel();
        updateOnline(Set.of(), 0, 0, 0, 0, RoomUiState.WAITING, "Đang chờ bắt đầu", send);
    }

    public void updateOnline(Set<String> legal, long call, long minimum, long maximum,
                             long pot, RoomUiState state, String text, BiConsumer<String, Long> sender) {
        this.send = sender;
        this.maximum = Math.max(0, maximum);
        this.minimum = Math.min(this.maximum, Math.max(0, minimum));
        this.pot = Math.max(0, pot);
        myTurn = state == RoomUiState.PLAYING_MY_TURN;
        turnLabel.setText(text == null ? "" : text);
        turnLabel.setStyle("-fx-text-fill:" + (myTurn ? "#ffe065" : "#c7b394")
                + ";-fx-font-size:15px;-fx-font-weight:900;");
        fold.setDisable(!myTurn || !legal.contains("FOLD"));
        String middle = legal.contains("CHECK") ? "CHECK" : "CALL";
        checkCall.setText("CALL".equals(middle) ? "CALL $" + String.format("%,d", call) : middle);
        checkCall.setDisable(!myTurn || !legal.contains(middle));
        wagerAction = legal.contains("RAISE") ? "RAISE" : "BET";
        regularWagerAllowed = legal.contains(wagerAction);
        betRaise.setText(wagerAction);
        betRaise.setDisable(!myTurn || !regularWagerAllowed);
        allInAction.setDisable(!myTurn || !legal.contains("ALL_IN"));
        raiseTitle.setText("RAISE".equals(wagerAction) ? "Raise amount" : "Bet amount");
        limits.setText("Min: $" + String.format("%,d", this.minimum)
                + "                         Max: $" + String.format("%,d", this.maximum));
        slider.setMin(this.minimum);
        slider.setMax(this.maximum);
        slider.setDisable(this.maximum == this.minimum);
        select(clamp(selectedAmount()), false);
        allIn.setDisable(!legal.contains("ALL_IN"));
        if (!myTurn || !regularWagerAllowed) hideRaisePanel();
        setVisible(state == RoomUiState.PLAYING_MY_TURN
                || state == RoomUiState.PLAYING_NOT_MY_TURN || state == RoomUiState.GAME_FINISHED);
        setManaged(isVisible());
    }

    public void updateTurnTimer(long remainingSeconds, long totalSeconds) {
        if (myTurn) turnLabel.setText("YOUR TURN  ⏱ " + Math.max(0, remainingSeconds) + "s");
    }

    public void hideRaisePanel() {
        raisePanel.setVisible(false);
        raisePanel.setManaged(false);
        selectedAllIn = false;
    }

    private void showRaisePanel() {
        if (!myTurn) return;
        raisePanel.setVisible(true);
        raisePanel.setManaged(true);
        select(regularWagerAllowed ? clamp(selectedAmount()) : maximum, !regularWagerAllowed);
        confirm.setText("CONFIRM " + betRaise.getText());
    }

    private void select(long value, boolean allInSelected) {
        amount.setText(Long.toString(value));
        slider.setValue(value);
        selectedAllIn = allInSelected;
        confirm.setText(allInSelected ? "CONFIRM ALL-IN" : "CONFIRM " + wagerAction);
    }

    private long selectedAmount() {
        try { return Long.parseLong(amount.getText().trim()); }
        catch (RuntimeException ignored) { return minimum; }
    }

    private long clamp(long value) { return Math.max(minimum, Math.min(maximum, value)); }
    private long step() { return Math.max(1, (maximum - minimum) / 100); }
}
