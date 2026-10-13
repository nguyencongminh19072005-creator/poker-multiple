package com.poker.view.room;

import com.poker.view.OrnateUi;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Compact room chat dock; collapsing never recreates messages or subscriptions. */
public final class RoomChatPanel extends StackPane {
    private static final double WIDTH = 240;
    private static final double HEIGHT = 220;
    private final VBox panel = new VBox(9);
    private final VBox messages = new VBox(7);
    private final ScrollPane history = new ScrollPane(messages);
    private final TextField input = new TextField();
    private final Button collapsedTab = new Button("<");
    private Runnable onSend = () -> { };
    private boolean open = true;
    private TranslateTransition transition;

    public RoomChatPanel() {
        setPickOnBounds(false);
        setPrefSize(WIDTH, HEIGHT);
        setMaxWidth(WIDTH);
        setMaxHeight(HEIGHT);
        panel.setPrefWidth(WIDTH);
        panel.setMaxWidth(WIDTH);
        panel.setPrefHeight(HEIGHT);
        panel.setMinHeight(HEIGHT);
        panel.setMaxHeight(HEIGHT);
        panel.setPadding(new Insets(9));
        panel.setStyle(OrnateUi.compactPanelStyle());

        Label title = new Label("CHAT PHÒNG");
        title.setStyle("-fx-text-fill:#f6dc9c;-fx-font-weight:900;-fx-font-size:13px;");
        Button collapse = new Button(">");
        OrnateUi.button(collapse, "button-blue.png", 13);
        collapse.setOnAction(event -> collapse());
        HBox header = new HBox(8, title, collapse);
        HBox.setHgrow(title, Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);

        messages.setPadding(new Insets(8));
        history.setFitToWidth(true);
        history.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        history.setMinHeight(100);
        history.setStyle("-fx-background:#241710;-fx-background-color:#241710;"
                + "-fx-border-color:#ad814c;");
        VBox.setVgrow(history, Priority.ALWAYS);
        input.setPromptText("Nhắn trong phòng...");
        input.setStyle("-fx-background-color:#241710;-fx-text-fill:#f4dfad;-fx-border-color:#ad814c;");
        input.setOnAction(event -> onSend.run());
        Button send = new Button("Gửi");
        OrnateUi.button(send, "button-blue.png", 12);
        send.setOnAction(event -> onSend.run());
        HBox composer = new HBox(5, input, send);
        HBox.setHgrow(input, Priority.ALWAYS);
        panel.getChildren().addAll(header, history, composer);

        OrnateUi.button(collapsedTab, "button-blue.png", 16);
        collapsedTab.setOnAction(event -> expand());
        collapsedTab.setVisible(false);
        collapsedTab.setManaged(false);
        StackPane.setAlignment(panel, Pos.BOTTOM_RIGHT);
        StackPane.setAlignment(collapsedTab, Pos.BOTTOM_RIGHT);
        getChildren().addAll(panel, collapsedTab);
    }

    public void setOnSend(Runnable onSend) { this.onSend = onSend; }
    public String messageText() { return input.getText().trim(); }
    public void clearInput() { input.clear(); }
    public void clearInputIfUnchanged(String sentText) {
        if (messageText().equals(sentText)) input.clear();
    }
    public void clearMessages() { messages.getChildren().clear(); }

    public void append(String prefix, String content, String kind) {
        Label line = new Label(prefix + content);
        line.setWrapText(true);
        line.setMaxWidth(WIDTH - 40);
        String color = "GAME".equals(kind) ? "#ffda72" : "SYSTEM".equals(kind) ? "#aeb8b6" : "#f5e4c6";
        line.setStyle("-fx-text-fill:" + color + ";-fx-font-size:12px;");
        messages.getChildren().add(line);
        history.layout();
        history.setVvalue(1);
    }

    public void collapse() { animate(false); }
    public void expand() { animate(true); }

    private void animate(boolean opening) {
        if (open == opening) return;
        open = opening;
        if (transition != null) transition.stop();
        if (opening) {
            collapsedTab.setVisible(false);
            collapsedTab.setManaged(false);
            panel.setVisible(true);
            panel.setManaged(true);
            panel.setTranslateX(WIDTH);
        }
        transition = new TranslateTransition(Duration.millis(240), panel);
        transition.setToX(opening ? 0 : WIDTH);
        transition.setOnFinished(event -> {
            if (!opening) {
                panel.setVisible(false);
                panel.setManaged(false);
                collapsedTab.setVisible(true);
                collapsedTab.setManaged(true);
            }
        });
        transition.play();
    }
}
