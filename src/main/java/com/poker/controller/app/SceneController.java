package com.poker.controller.app;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.event.ActionEvent;
import javafx.scene.control.ButtonBase;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Modality;
import com.poker.view.ErrorDialogView;
import com.poker.view.UserFacingErrors;
import com.poker.view.SoundEffects;
import com.poker.view.V2Theme;

/** Chỉ quản lý cửa sổ và chuyển màn hình JavaFX. */
public final class SceneController {
    private final Stage stage;
    private Stage activeError;
    private ErrorDialogView activeErrorView;

    public SceneController(Stage stage) { this.stage = stage; }

    public void show(Parent root) {
        Scene scene = new Scene(root, 1200, 780);
        V2Theme.apply(scene);
        scene.addEventFilter(ActionEvent.ACTION, event -> {
            if (event.getTarget() instanceof ButtonBase)
                SoundEffects.play(SoundEffects.Sound.BUTTON_CLICK);
        });
        stage.setScene(scene);
    }

    public void showError(String message) {
        if (!stage.isShowing()) return;
        String detail = UserFacingErrors.fromMessage(message);
        if (activeError != null && activeError.isShowing()) {
            activeErrorView.setMessage(detail);
            activeError.toFront();
            return;
        }
        Stage dialog = new Stage(StageStyle.TRANSPARENT);
        dialog.initOwner(stage);
        dialog.initModality(Modality.WINDOW_MODAL);
        ErrorDialogView view = new ErrorDialogView(detail, dialog::close);
        Scene scene = new Scene(view, 620, 320);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        activeError = dialog;
        activeErrorView = view;
        dialog.setOnHidden(event -> {
            if (activeError == dialog) {
                activeError = null;
                activeErrorView = null;
            }
        });
        dialog.show();
        dialog.setX(stage.getX() + (stage.getWidth() - 620) / 2);
        dialog.setY(stage.getY() + (stage.getHeight() - 320) / 2);
    }

    public Stage stage() { return stage; }
}
