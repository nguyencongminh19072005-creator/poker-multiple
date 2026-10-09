package com.poker.view;

import javafx.scene.image.Image;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import com.poker.model.game.card.Card;

public class AssetLoader {
    private static final String DEFAULT_BASE_PATH = "D:/OpenDecks-Public-Domain-and-CC0-Playing-Cards";
    private static final File BASE_DIRECTORY = resolveBaseDirectory();
    private static final Map<String, Image> cache = new HashMap<>();

    public static Image loadImage(String relativePath) {
        return loadImageSized(relativePath, 0, 0);
    }

    public static Image loadImageSized(String relativePath, double width, double height) {
        String key = relativePath + "@" + (int) width + "x" + (int) height;
        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        String resourcePath = "/assets/" + relativePath.replace('\\', '/');
        URL bundledResource = AssetLoader.class.getResource(resourcePath);
        if (bundledResource != null) {
            Image image = new Image(bundledResource.toExternalForm(), width, height, true, true);
            cache.put(key, image);
            return image;
        }

        File file = new File(BASE_DIRECTORY, relativePath);
        if (!file.exists()) {
            cache.put(key, null);
            return null;
        }

        try {
            String url = file.toURI().toString();
            Image img = new Image(url, width, height, true, true);
            cache.put(key, img);
            return img;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static File resolveBaseDirectory() {
        String configuredPath = System.getProperty("poker.assets.path");
        if (configuredPath == null || configuredPath.isBlank()) {
            configuredPath = System.getenv("POKER_ASSETS_PATH");
        }
        return new File(configuredPath == null || configuredPath.isBlank()
                ? DEFAULT_BASE_PATH
                : configuredPath);
    }

    public static Image getTableImage() {
        return loadImageSized("reference-v2/table/poker-table-v2.png", 1000, 650);
    }

    public static Image getFemaleDealerImage() {
        return loadImageSized("reference-v2/dealer/female-dealer.png", 260, 390);
    }

    public static Image getBackgroundImage() {
        return loadImageSized("reference-v2/backgrounds/casino.png", 1600, 1000);
    }

    public static Image getAuthBackgroundImage() {
        return getBackgroundImage();
    }

    public static Image getLobbyBackgroundImage() {
        return getBackgroundImage();
    }

    public static Image getRoomListBackgroundImage() {
        return getBackgroundImage();
    }

    public static Image getLogoImage() {
        return null; // The red v2 title is rendered as scalable text.
    }

    public static Image getButtonFrame1Image() {
        return loadImage("button/btn_frame_1.png");
    }

    public static Image getLobbyRoomListButtonImage() {
        return null;
    }

    public static Image getLobbyCreateRoomButtonImage() {
        return null;
    }

    public static Image getLobbyLeaderboardButtonImage() {
        return null;
    }

    public static Image getLobbyProfileButtonImage() {
        return null;
    }

    public static Image getButtonFrame2Image() {
        return loadImage("button/btn_frame_2.png");
    }

    public static Image getFormFrameImage() {
        return loadImage("button/form_frame.png");
    }

    public static Image getAvatarImage(String avatarFileName) {
        Image img = loadImageSized("reference-v2/avatars/" + avatarFileName, 160, 160);
        return img != null ? img : loadImageSized("reference-v2/avatars/avatar_player_1.png", 160, 160);
    }

    public static Image getAvatarFrameImage() {
        return null; // The red v2 portraits already have a gold frame.
    }

    public static Image getDealerAvatarImage() {
        return getAvatarImage("avatar_player_1.png");
    }

    public static Image getChipFrameHudImage() {
        return loadImage("chips/chip_frame_hud_custom.png");
    }

    public static Image getCardImage(Card card) {
        if (card == null) return getCardBackImage("red");
        String path = card.getImageRelativePath().replace("png cards/", "reference-v2/cards/");
        return loadImageSized(path, 130, 190);
    }

    public static Image getCardBackImage(String color) {
        String back = "blue".equalsIgnoreCase(color) ? "blue" : "red";
        return loadImageSized("reference-v2/cards/card backs/card back " + back + ".png", 130, 190);
    }

    public static Image getChipImageForValue(int amount) {
        int denomination = amount >= 1000 ? 1000 : amount >= 500 ? 500 : amount >= 100 ? 100
                : amount >= 25 ? 25 : amount >= 5 ? 5 : 1;
        return loadImageSized("reference-v2/chips/chip-" + denomination + ".png", 128, 128);
    }

    public static Image getChipStackImage() {
        return getChipImageForValue(1000);
    }

    // --- Pages ---
    public static Image getLobbyPageImage() { return loadImage("pages/lobby.png"); }
    public static Image getCreateRoomPageImage() { return loadImage("pages/create-room.png"); }
    public static Image getWaitingRoomPageImage() { return loadImage("pages/waiting-room.png"); }
    public static Image getPokerGameRoomPageImage() { return loadImage("pages/poker-game-room.png"); }
    public static Image getSpectatorViewPageImage() { return loadImage("pages/spectator-view.png"); }
    public static Image getGameResultPageImage() { return null; }

    // --- Modals / Popup ---
    public static Image getJoinPrivateRoomModalImage() { return null; }
    public static Image getRoomDetailModalImage() { return null; }
    public static Image getConnectionLostModalImage() { return loadImage("modals/connection-lost.png"); }

    // --- Chat ---
    public static Image getRoomChatImage() { return loadImageSized("reference-v2/pack/ornate/chat-panel.png", 350, 450); }
    public static Image getInRoomChatImage() { return getRoomChatImage(); }

    // --- Poker components ---
    public static Image getPlayingCardBackImage() { return loadImage("poker-components/playing-card-back.png"); }
    public static Image getPlayingCardsImage() { return loadImage("poker-components/playing-cards.png"); }
    public static Image getPokerChipsImage() { return loadImage("poker-components/poker-chips.png"); }
    public static Image getDealerButtonImage() { return loadImageSized("reference-v2/pack/markers/dealer.png", 48, 48); }
    public static Image getSmallBlindImage() { return loadImageSized("reference-v2/pack/markers/small-blind.png", 48, 48); }
    public static Image getBigBlindImage() { return loadImageSized("reference-v2/pack/markers/big-blind.png", 48, 48); }

    // --- Player ---
    public static Image getPlayerSeatImage() { return loadImageSized("reference-v2/pack/ornate/player-seat.png", 320, 105); }
    public static Image getPlayerAvatarImage() { return getAvatarImage("avatar_player_1.png"); }
    public static Image getHostCrownImage() { return loadImageSized("reference-v2/pack/icons/crown.png", 64, 64); }
    public static Image getReadyStatusImage() { return loadImage("player/ready-status.png"); }
    public static Image getUnreadyStatusImage() { return loadImage("player/unready-status.png"); }

    // --- Actions ---
    public static Image getBtnFoldImage() { return null; }
    public static Image getBtnCheckImage() { return null; }
    public static Image getBtnCallImage() { return null; }
    public static Image getBtnRaiseImage() { return null; }
    public static Image getBtnAllInImage() { return null; }
    public static Image getBtnAllInFlameImage() { return loadImage("actions/btn-allin-flame.png"); }

    // --- Realtime ---
    public static Image getTimerImage() { return loadImageSized("reference-v2/pack/icons/timer.png", 48, 48); }
    public static Image getOnlineStatusImage() { return loadImageSized("reference-v2/pack/icons/online.png", 32, 32); }
    public static Image getReconnectingImage() { return loadImage("realtime/reconnecting.png"); }
    public static Image getReconnectingWifiImage() { return loadImageSized("reference-v2/pack/icons/wifi-off.png", 64, 64); }
    public static Image getReconnectingSpinnerImage() { return loadImageSized("reference-v2/pack/icons/timer.png", 64, 64); }
}
