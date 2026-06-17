package client;

import javafx.stage.Stage;

public final class WindowUtil {

    private WindowUtil() {}

    public static void showMaximized(Stage stage) {
        stage.setResizable(true);
        stage.setMaximized(true);
        stage.show();
    }
}
