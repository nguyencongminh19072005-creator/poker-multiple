package com.poker.view;

import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Applies the classic red v2 casino palette to the JavaFX views, including dynamic rows. */
public final class V2Theme {
    private static final Map<String, String> COLORS = new LinkedHashMap<>();

    static {
        COLORS.put("#0b0f19", "#1d120e");
        COLORS.put("#0b1329", "#20140f");
        COLORS.put("#0f172a", "#231811");
        COLORS.put("#111827", "#2c1d15");
        COLORS.put("#1a2332", "#352219");
        COLORS.put("#1e293b", "#3b271c");
        COLORS.put("#1f2937", "#3b281d");
        COLORS.put("#2a354c", "#513421");
        COLORS.put("#334155", "#61432d");
        COLORS.put("#374151", "#6b4931");
        COLORS.put("#3b82f6", "#bb8e50");
        COLORS.put("#2563eb", "#8c612e");
        COLORS.put("#60a5fa", "#dbb87b");
        COLORS.put("#38bdf8", "#efce95");
        COLORS.put("#10b981", "#167845");
        COLORS.put("#059669", "#0b5531");
        COLORS.put("#22c55e", "#26844c");
        COLORS.put("#34d399", "#52ac6b");
        COLORS.put("#94a3b8", "#cbb798");
        COLORS.put("#cbd5e1", "#e0d3bc");
        COLORS.put("#8b5cf6", "#9c6f8d");
        COLORS.put("#ec4899", "#b65e6c");
    }

    private V2Theme() { }

    public static void apply(Scene scene) {
        var resource = V2Theme.class.getResource("/poker-game.css");
        if (resource != null && !scene.getStylesheets().contains(resource.toExternalForm())) {
            scene.getStylesheets().add(resource.toExternalForm());
        }
        Set<Node> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        install(scene.getRoot(), visited);
    }

    private static void install(Node node, Set<Node> visited) {
        if (!visited.add(node)) return;
        restyle(node);
        node.styleProperty().addListener((obs, oldStyle, newStyle) -> restyle(node));
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) install(child, visited);
            parent.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> {
                while (change.next()) {
                    for (Node child : change.getAddedSubList()) install(child, visited);
                }
            });
        }
    }

    private static void restyle(Node node) {
        String source = node.getStyle();
        if (source == null || source.isEmpty()) return;
        String result = source;
        for (var entry : COLORS.entrySet()) result = result.replace(entry.getKey(), entry.getValue());
        result = result.replace("rgba(11, 19, 41,", "rgba(45, 26, 18,")
                .replace("rgba(15, 23, 42,", "rgba(49, 29, 20,")
                .replace("rgba(20, 28, 45,", "rgba(56, 34, 23,")
                .replace("rgba(20, 30, 50,", "rgba(56, 35, 24,")
                .replace("rgba(10, 16, 28,", "rgba(33, 23, 17,")
                .replace("rgba(26, 36, 56,", "rgba(69, 43, 27,")
                .replace("rgba(59, 130, 246,", "rgba(187, 142, 80,");
        if (!source.equals(result)) node.setStyle(result);
    }
}
