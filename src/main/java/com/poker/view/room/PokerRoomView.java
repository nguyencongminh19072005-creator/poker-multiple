package com.poker.view.room;

import com.poker.controller.room.RoomUiState;
import com.poker.view.AssetLoader;
import com.poker.view.OrnateUi;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/** The room feature's layout; state and networking remain in RoomScreens. */
public final class PokerRoomView extends BorderPane {
    private final Label title = new Label();
    private final Label blinds = new Label();
    private final Label pot = new Label();
    private final Label status = new Label();
    private final Button sit = new Button("NGỒI VÀO BÀN");
    private final Button ready = new Button("SẴN SÀNG");
    private final Button start = new Button("BẮT ĐẦU VÁN");
    private final Button leave = new Button("RỜI PHÒNG");
    private final HBox waitingBar = new HBox(12);
    private final StackPane tableLayer = new StackPane();
    private final StackPane notice = new StackPane();
    private PauseTransition noticeTimer;
    private final VBox center = new VBox(4);
    private final RoomChatPanel chat = new RoomChatPanel();
    private final ControlPanelView actions = new ControlPanelView();
    private PokerTableContainer table;

    public PokerRoomView(Runnable onLeave) {
        // One continuous background spans the table, the chat dock, and all room margins.
        setStyle("-fx-background-color:#17241e;");

        String headerText = "-fx-text-fill:#ffe19a;-fx-font-size:16px;-fx-font-weight:900;";
        title.setStyle(headerText);
        blinds.setStyle(headerText);
        pot.setStyle(headerText);
        OrnateUi.button(leave, "button-red.png", 14);
        leave.setOnAction(event -> onLeave.run());
        HBox topBar = new HBox(20, title, blinds, pot, new Region(), leave);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(11, 18, 11, 18));
        topBar.setStyle("-fx-background-color:linear-gradient(to right,#25160f,#58311c,#25160f);"
                + "-fx-border-color:#bc8841;-fx-border-width:0 0 2 0;");
        HBox.setHgrow(topBar.getChildren().get(3), Priority.ALWAYS);
        setTop(topBar);

        status.setStyle("-fx-text-fill:#ffe3aa;-fx-font-size:13px;-fx-font-weight:bold;");
        center.setPadding(new Insets(6, 10, 4, 10));
        center.setStyle("-fx-background-color:radial-gradient(center 45% 45%, radius 80%, #234537, #131c18);");
        tableLayer.setMinSize(0, 0);
        // Center the table across the whole room; the compact chat floats in
        // the bottom-left margin instead of reserving a right-hand column.
        StackPane.setAlignment(chat, Pos.BOTTOM_LEFT);
        StackPane.setMargin(chat, new Insets(0, 0, 14, 12));
        notice.setStyle(OrnateUi.noticeStyle()
                + "-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,.75),18,0,0,6);");
        notice.setPrefSize(480, 112);
        notice.setMaxSize(480, 112);
        notice.setMouseTransparent(true);
        notice.setVisible(false);
        StackPane.setAlignment(notice, Pos.TOP_CENTER);
        StackPane.setMargin(notice, new Insets(16, 0, 0, 0));
        tableLayer.getChildren().addAll(chat, notice);
        VBox.setVgrow(tableLayer, Priority.ALWAYS);
        center.getChildren().addAll(status, tableLayer);
        setCenter(center);

        for (Button button : new Button[]{sit, ready, start})
            OrnateUi.button(button, "button-blue.png", 14);
        waitingBar.getChildren().addAll(sit, ready, start);
        waitingBar.setAlignment(Pos.CENTER_LEFT);
        waitingBar.setPadding(new Insets(10, 18, 10, 18));
        waitingBar.setStyle("-fx-background-color:linear-gradient(to right,#25160f,#4d2a18,#25160f);"
                + "-fx-border-color:#bc8841;-fx-border-width:2 0 0 0;");
        VBox bottom = new VBox(waitingBar, actions);
        setBottom(bottom);
        updateMode(RoomUiState.SPECTATING, false);
    }

    public void setTable(PokerTableContainer replacement) {
        if (table != null) tableLayer.getChildren().remove(table);
        table = replacement;
        // The table image is transparent outside the rail; let the room-wide felt show through.
        replacement.setStyle("-fx-background-color:transparent;");
        tableLayer.getChildren().add(0, replacement);
        replacement.setOnMousePressed(event -> actions.hideRaisePanel());
    }

    public void updateHeader(String roomName, long roomId, long smallBlind, long bigBlind,
                             long handNumber, long potAmount, String message) {
        title.setText("ROOM: " + roomName + "  #" + roomId);
        blinds.setText("SB/BB  $" + smallBlind + "/$" + bigBlind + "  •  HAND #" + handNumber);
        pot.setText("POT  $" + String.format("%,d", potAmount));
        status.setText(message);
    }

    public void updateMode(RoomUiState state, boolean running) {
        boolean roomControls = !running || state == RoomUiState.SPECTATING && sit.isVisible();
        waitingBar.setVisible(roomControls);
        waitingBar.setManaged(roomControls);
        actions.setVisible(!running || state != RoomUiState.SPECTATING);
        actions.setManaged(!running || state != RoomUiState.SPECTATING);
    }

    public boolean confirmLeave(boolean gameRunning) {
        if (gameRunning) {
            showNotice("VÁN ĐANG DIỄN RA", "Bạn có thể rời phòng sau khi ván kết thúc.", null);
            return false;
        }
        ButtonType leaveChoice = new ButtonType("Rời phòng", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Bạn có chắc muốn rời phòng?", ButtonType.CANCEL, leaveChoice);
        alert.setTitle("Rời phòng");
        alert.setHeaderText("Rời phòng?");
        if (getScene() != null && getScene().getWindow() != null)
            alert.initOwner(getScene().getWindow());
        return alert.showAndWait().filter(leaveChoice::equals).isPresent();
    }

    public void showNotice(String heading, String message, String avatarFile) {
        if (noticeTimer != null) noticeTimer.stop();
        Label title = new Label(heading);
        title.setStyle("-fx-text-fill:#ffe19a;-fx-font-size:16px;-fx-font-weight:900;");
        Label detail = new Label(message);
        detail.setWrapText(true);
        detail.setMaxWidth(avatarFile == null ? 420 : 360);
        detail.setStyle("-fx-text-fill:#fff5df;-fx-font-size:13px;-fx-font-weight:bold;");
        VBox words = new VBox(5, title, detail);
        words.setAlignment(Pos.CENTER_LEFT);
        HBox content = new HBox(14);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(16, 24, 16, 24));
        if (avatarFile != null) {
            ImageView portrait = new ImageView(AssetLoader.getAvatarImage(avatarFile));
            portrait.setFitWidth(56);
            portrait.setFitHeight(56);
            portrait.setPreserveRatio(true);
            portrait.setClip(new Circle(28, 28, 27));
            content.getChildren().add(portrait);
        }
        content.getChildren().add(words);
        notice.getChildren().setAll(content);
        notice.setVisible(true);
        noticeTimer = new PauseTransition(Duration.seconds(5));
        noticeTimer.setOnFinished(event -> notice.setVisible(false));
        noticeTimer.play();
    }

    public RoomChatPanel chat() { return chat; }
    public ControlPanelView actions() { return actions; }
    public PokerTableContainer table() { return table; }
    public Button sitButton() { return sit; }
    public Button readyButton() { return ready; }
    public Button startButton() { return start; }
}
