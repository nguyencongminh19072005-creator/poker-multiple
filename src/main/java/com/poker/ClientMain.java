package com.poker;

import javafx.application.Application;

/** Launcher Java thuần để VS Code chạy JavaFX bằng Maven classpath. */
public final class ClientMain {
    private ClientMain() { }

    public static void main(String[] args) {
        Application.launch(PokerClientApplication.class, args);
    }
}
