package com.poker.view;

import java.net.URL;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;

/** Decorative artwork only; game state and actions remain in the owning views. */
public final class OrnateUi {
    private static final String ROOT = "/assets/reference-v2/pack/ornate/";

    private OrnateUi() { }

    public static String imageStyle(String name) {
        return imageStyleAt(ROOT + name, "room-card.png".equals(name) ? "100% 145%" : "100% 100%");
    }

    private static String imageStyleAt(String path, String size) {
        URL resource = OrnateUi.class.getResource(path);
        if (resource == null) {
            return "-fx-background-color: #382316; -fx-border-color: #c69a55;";
        }
        return "-fx-background-color: transparent; -fx-border-color: transparent; "
                + "-fx-background-image: url('" + resource.toExternalForm() + "'); "
                + "-fx-background-repeat: no-repeat; -fx-background-position: center center; "
                + "-fx-background-size: " + size + ";";
    }

    public static String framedStyle() {
        return imageStyleAt("/assets/reference-v2/pack/frames/modal-dark.png", "100% 100%");
    }

    public static String compactPanelStyle() {
        return "-fx-background-color: linear-gradient(to bottom, #f0cf86, #956127), "
                + "linear-gradient(to bottom, #60341f, #1d110d), "
                + "radial-gradient(center 50% 28%, radius 84%, #4d2d20, #261711); "
                + "-fx-background-insets: 0, 2, 5; -fx-background-radius: 20, 18, 15; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 12, 0, 0, 4);";
    }

    public static void panel(Region region, String name) {
        region.setStyle(imageStyle(name));
    }

    public static void button(Button button, String name, int fontSize) {
        button.setStyle(imageStyle(name) + " -fx-text-fill: #fff1c2; "
                + "-fx-font-family: 'Segoe UI'; -fx-font-size: " + fontSize + "px; "
                + "-fx-font-weight: 900; -fx-cursor: hand; -fx-padding: 6 17;");
        button.setOnMouseEntered(e -> {
            button.setScaleX(1.035);
            button.setScaleY(1.035);
        });
        button.setOnMouseExited(e -> {
            button.setScaleX(1);
            button.setScaleY(1);
        });
    }

    /** Four clear action colors, backed by the bundled classic gold-framed PNGs. */
    public static void actionButton(Button button, String color, int fontSize) {
        button(button, "button-blue.png", fontSize);
        button.setStyle(imageStyleAt("/assets/reference-v2/pack/buttons/" + color + ".png", "100% 100%")
                + " -fx-text-fill: #fff8dd; -fx-font-family: 'Segoe UI';"
                + " -fx-font-size: " + fontSize + "px; -fx-font-weight: 900;"
                + " -fx-cursor: hand; -fx-padding: 9 18;"
                + " -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.62), 8, 0, 0, 3);");
    }
}
