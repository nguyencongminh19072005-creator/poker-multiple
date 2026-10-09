package com.poker;

import com.poker.controller.app.PokerApplicationController;
import javafx.application.Application;
import javafx.stage.Stage;

/** Vòng đời JavaFX; được khởi chạy thông qua ClientMain. */
public final class PokerClientApplication extends Application {
    private PokerApplicationController application;

    @Override public void start(Stage stage) {
        stage.setTitle("Poker World Champions — Classic V2");
        stage.setMinWidth(1080);
        stage.setMinHeight(720);
        application = new PokerApplicationController(stage);
        application.start();
        stage.show();
    }

    @Override public void stop() {
        if (application != null) application.stop();
    }
}
