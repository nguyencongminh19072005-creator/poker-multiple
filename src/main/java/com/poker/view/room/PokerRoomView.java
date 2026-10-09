package com.poker.view.room;

import com.poker.controller.room.RoomUiState;
import com.poker.view.OrnateUi;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** The room feature's layout; state and networking remain in RoomScreens. */
public final class PokerRoomView extends BorderPane {
    private final Label title = new Label();
    private final Label blinds = new Label();
    private final Label pot = new Label();
    private final Label status = new Label();
    private final MenuButton menu = new MenuButton("⋮");
    private final Button sit = new Button("NGỒI VÀO BÀN");
    private final Button ready = new Button("SẴN SÀNG");
    private final Button start = new Button("BẮT ĐẦU VÁN");
    private final Button leave = new Button("RỜI PHÒNG");
    private final HBox waitingBar = new HBox(12);
    private final BorderPane tableLayer = new BorderPane();
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
        menu.setStyle("-fx-background-color:#3d2418;-fx-text-fill:#ffe19a;-fx-font-size:17px;");
        MenuItem leaveMenu = new MenuItem("Rời phòng");
        leaveMenu.setOnAction(event -> onLeave.run());
        menu.getItems().add(leaveMenu);
        HBox topBar = new HBox(20, title, blinds, pot, new Region(), menu);
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
        Region chatSpacer = new Region();
        VBox.setVgrow(chatSpacer, Priority.ALWAYS);
        VBox chatDock = new VBox(chatSpacer, chat);
        chatDock.setAlignment(Pos.BOTTOM_RIGHT);
        chatDock.setPadding(new Insets(0, 12, 14, 0));
        chatDock.setPrefWidth(252);
        chatDock.setMaxWidth(252);
        chatDock.setPickOnBounds(false);
        tableLayer.setRight(chatDock);
        VBox.setVgrow(tableLayer, Priority.ALWAYS);
        center.getChildren().addAll(status, tableLayer);
        setCenter(center);

        for (Button button : new Button[]{sit, ready, start, leave})
            OrnateUi.button(button, button == leave ? "button-red.png" : "button-blue.png", 14);
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        waitingBar.getChildren().addAll(sit, ready, start, gap, leave);
        waitingBar.setAlignment(Pos.CENTER_LEFT);
        waitingBar.setPadding(new Insets(10, 18, 10, 18));
        waitingBar.setStyle("-fx-background-color:linear-gradient(to right,#25160f,#4d2a18,#25160f);"
                + "-fx-border-color:#bc8841;-fx-border-width:2 0 0 0;");
        VBox bottom = new VBox(waitingBar, actions);
        setBottom(bottom);
        leave.setOnAction(event -> onLeave.run());
        updateMode(RoomUiState.SPECTATING, false);
    }

    public void setTable(PokerTableContainer replacement) {
        table = replacement;
        // The table image is transparent outside the rail; let the room-wide felt show through.
        replacement.setStyle("-fx-background-color:transparent;");
        tableLayer.setCenter(replacement);
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
        boolean roomControls = !running;
        waitingBar.setVisible(roomControls);
        waitingBar.setManaged(roomControls);
        menu.setVisible(running);
        menu.setManaged(running);
        if (running && state == RoomUiState.SPECTATING) {
            actions.setVisible(false);
            actions.setManaged(false);
        }
    }

    public boolean confirmLeave(boolean gameRunning) {
        if (gameRunning) return true; // The server applies active-hand departure rules.
        ButtonType leaveChoice = new ButtonType("Rời phòng", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Bạn có chắc muốn rời phòng?", ButtonType.CANCEL, leaveChoice);
        alert.setTitle("Rời phòng");
        alert.setHeaderText("Rời phòng?");
        if (getScene() != null && getScene().getWindow() != null)
            alert.initOwner(getScene().getWindow());
        return alert.showAndWait().filter(leaveChoice::equals).isPresent();
    }

    public RoomChatPanel chat() { return chat; }
    public ControlPanelView actions() { return actions; }
    public PokerTableContainer table() { return table; }
    public Button sitButton() { return sit; }
    public Button readyButton() { return ready; }
    public Button startButton() { return start; }
}
