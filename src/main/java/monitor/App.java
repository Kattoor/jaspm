package monitor;

import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage ignored) {
        new SettingsView()
                .onContinue(this::openMonitorView)
                .initUI()
                .show();
    }

    private void openMonitorView(Settings settings) {
        new MonitorView(settings)
                .showAndWait();
    }
}
