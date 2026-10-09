package com.poker.controller.app;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import com.poker.view.V2Theme;

/** Chỉ quản lý cửa sổ và chuyển màn hình JavaFX. */
public final class SceneController {
    private final Stage stage;
    private Alert activeError;

    public SceneController(Stage stage) { this.stage = stage; }

    public void show(Parent root) {
        Scene scene = new Scene(root, 1200, 780);
        V2Theme.apply(scene);
        stage.setScene(scene);
    }

    public void showError(String message) {
        if (!stage.isShowing()) return;
        String detail = message == null || message.isBlank()
                ? "Có lỗi không xác định. Vui lòng kiểm tra Terminal và thử lại."
                : message;
        if (activeError != null && activeError.isShowing()) {
            if (!detail.equals(activeError.getContentText())) activeError.setContentText(detail);
            return;
        }
        Alert alert = new Alert(Alert.AlertType.ERROR, detail, ButtonType.OK);
        alert.initOwner(stage);
        activeError = alert;
        alert.setOnHidden(event -> { if (activeError == alert) activeError = null; });
        alert.show();
    }

    public Stage stage() { return stage; }
}
