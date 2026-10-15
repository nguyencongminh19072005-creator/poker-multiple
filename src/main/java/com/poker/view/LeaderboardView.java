package com.poker.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.OverrunStyle;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.geometry.Rectangle2D;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

/** Bảng xếp hạng chung, hiển thị dữ liệu thật từ server. */
public class LeaderboardView extends BorderPane {
    public record RankingEntry(long rank, long userId, String name, long chipsEarned,
                               long gamesRated, String avatarUrl) { }
    public interface LeaderboardActions { void onBackToLobby(); }

    private final StackPane podium = new StackPane();
    private final VBox rows = new VBox(8);
    private final Label count = label("Đang tải...", 13, "#aeb8ca");

    public LeaderboardView(LeaderboardActions actions) { this(actions, false); }

    public LeaderboardView(LeaderboardActions actions, boolean onlineMode) {
        var background = AssetLoader.getLobbyBackgroundImage();
        if (background != null) setBackground(new Background(new BackgroundImage(background,
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER,
                new BackgroundSize(100, 100, true, true, true, true))));
        else setStyle("-fx-background-color: #090c14;");

        VBox panel = new VBox(16);
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPadding(new Insets(26, 32, 30, 32));
        panel.setMaxWidth(1080);
        panel.setStyle("-fx-background-color: #222630; -fx-background-radius: 28; "
                + "-fx-border-color: #626c80; -fx-border-radius: 28; -fx-border-width: 2; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 26, 0.25, 0, 10);");

        StackPane heading = new StackPane();
        heading.setMinHeight(48);
        Button back = new Button("← VỀ SẢNH");
        back.setOnAction(event -> actions.onBackToLobby());
        back.setStyle("-fx-background-color: #383e4e; -fx-text-fill: white; -fx-font-weight: bold; "
                + "-fx-background-radius: 20; -fx-padding: 10 18; -fx-cursor: hand;");
        Label title = label("XẾP HẠNG TOÀN HỆ THỐNG", 25, "#f5f6fb");
        heading.getChildren().addAll(title, back);
        StackPane.setAlignment(title, Pos.CENTER);
        StackPane.setAlignment(back, Pos.CENTER_LEFT);

        podium.setAlignment(Pos.BOTTOM_CENTER);
        podium.setMinHeight(350);
        podium.setMaxWidth(840);
        rows.setFillWidth(true);
        rows.setMaxWidth(840);
        VBox content = new VBox(18, podium, rows);
        content.setAlignment(Pos.TOP_CENTER);
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: #222630; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        count.setAlignment(Pos.CENTER);
        panel.getChildren().addAll(heading, count, scroll);
        StackPane outer = new StackPane(panel);
        outer.setPadding(new Insets(22));
        setCenter(outer);
        if (!onlineMode) showOnlineEntries(List.of());
    }

    public void showOnlineEntries(List<RankingEntry> entries) {
        podium.getChildren().clear();
        rows.getChildren().clear();
        count.setText(entries.size() + " người chơi");
        if (entries.isEmpty()) {
            rows.getChildren().add(label("Chưa có dữ liệu xếp hạng từ server.", 16, "#c7cfda"));
            return;
        }
        var podiumImage = AssetLoader.getRankingPodiumImage();
        if (podiumImage != null && !podiumImage.isError()) {
            ImageView art = new ImageView(podiumImage);
            // The PNG has transparent margins; crop only for display so the podium fills its lane.
            art.setViewport(new Rectangle2D(0, 185, podiumImage.getWidth(), 550));
            art.setFitWidth(840);
            art.setFitHeight(255);
            art.setPreserveRatio(false);
            StackPane.setAlignment(art, Pos.BOTTOM_CENTER);
            podium.getChildren().add(art);
        }
        HBox winners = new HBox(0);
        winners.setAlignment(Pos.BOTTOM_CENTER);
        winners.setMinWidth(840);
        winners.setMaxWidth(840);
        for (int index : new int[]{1, 0, 2}) {
            if (index < entries.size()) winners.getChildren().add(podiumCard(entries.get(index), index));
            else {
                Region emptyPlace = new Region();
                emptyPlace.setPrefWidth(280);
                winners.getChildren().add(emptyPlace);
            }
        }
        podium.getChildren().add(winners);
        for (int index = 3; index < entries.size(); index++) rows.getChildren().add(rankRow(entries.get(index)));
    }

    private VBox podiumCard(RankingEntry entry, int index) {
        boolean first = index == 0;
        String accent = first ? "#ffd54a" : index == 1 ? "#cbd5e1" : "#df9b63";
        VBox card = new VBox(5);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(4, 10, 58, 10));
        card.setMinWidth(280);
        card.setPrefWidth(280);
        card.setMaxWidth(280);
        card.setMinHeight(350);
        Label name = label(entry.name(), first ? 20 : 17, "#ffffff");
        name.setMinWidth(250);
        name.setMaxWidth(250);
        name.setAlignment(Pos.CENTER);
        name.setTextOverrun(OverrunStyle.ELLIPSIS);
        Region gap = new Region();
        VBox.setVgrow(gap, Priority.ALWAYS);
        Label position = label("#" + (index + 1), 22, accent);
        position.setStyle("-fx-effect: dropshadow(three-pass-box, black, 5, 0.8, 0, 2);");
        Label earnings = label(String.format("$%,d", entry.chipsEarned()), first ? 21 : 18, "#ffd54a");
        earnings.setMinWidth(250);
        earnings.setMaxWidth(250);
        earnings.setAlignment(Pos.CENTER);
        earnings.setTextOverrun(OverrunStyle.ELLIPSIS);
        card.getChildren().addAll(avatar(entry, first ? 74 : 66), name,
                earnings, gap, position);
        return card;
    }

    private HBox rankRow(RankingEntry entry) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 18, 10, 18));
        row.setMaxWidth(840);
        row.setStyle("-fx-background-color: #2d323e; -fx-background-radius: 13; "
                + "-fx-border-color: #525a69; -fx-border-radius: 13;");
        Label rank = label("#" + entry.rank(), 21, "#d8dde7");
        rank.setMinWidth(62);
        rank.setAlignment(Pos.CENTER);
        Label name = label(entry.name(), 17, "#ffffff");
        name.setMinWidth(200);
        name.setMaxWidth(320);
        name.setTextOverrun(OverrunStyle.ELLIPSIS);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label earnings = label(String.format("$%,d", entry.chipsEarned()), 20, "#ffd54a");
        earnings.setMinWidth(190);
        earnings.setAlignment(Pos.CENTER_RIGHT);
        row.getChildren().addAll(rank, avatar(entry, 52), name, spacer, earnings);
        return row;
    }

    private ImageView avatar(RankingEntry entry, double size) {
        String file = entry.avatarUrl() != null && entry.avatarUrl().matches("avatar_player_[1-6]\\.png")
                ? entry.avatarUrl() : "avatar_player_" + (Math.floorMod(entry.name().hashCode(), 6) + 1) + ".png";
        ImageView view = new ImageView(AssetLoader.getAvatarImage(file));
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setPreserveRatio(false);
        return view;
    }

    private static Label label(String text, int size, String color) {
        Label label = new Label(text);
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, size));
        label.setTextFill(Color.web(color));
        return label;
    }
}
