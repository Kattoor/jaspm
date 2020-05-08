package monitor;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import jssc.SerialPortException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class MonitorView extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    final ObservableList<Record> data = FXCollections.observableArrayList();
    final TableView<Record> serialOutput = new TableView<>(data);
    final TextField serialInteractiveOutput = new TextField();
    final TextField userInput = new TextField();
    final CheckBox interactiveMode = new CheckBox("Interactive");

    @Override
    public void start(Stage primaryStage) {
        initializeUI(primaryStage);
        Monitor monitor = initializeMonitor();
        coupleUIEvents(primaryStage, monitor);
    }

    void initializeUI(Stage primaryStage) {
        primaryStage.setTitle("spm");

        initializeTableView();

        HBox userInputPane = new HBox(userInput, interactiveMode);

        VBox root = new VBox(serialOutput, serialInteractiveOutput, userInputPane);
        VBox.setVgrow(serialOutput, Priority.ALWAYS);

        primaryStage.setScene(new Scene(root, 400, 400));
        primaryStage.show();
    }

    void initializeTableView() {
        TableColumn<Record, String> timeColumn = new TableColumn<>("Time");
        TableColumn<Record, String> contentColumn = new TableColumn<>("Content");

        DateTimeFormatter formatter =
                DateTimeFormatter
                        .ofPattern("dd.MM.yyyy:HH.mm.ss")
                        .withLocale(Locale.getDefault())
                        .withZone(ZoneId.systemDefault());
        timeColumn.setCellValueFactory(item -> new ReadOnlyStringWrapper(formatter.format(Instant.ofEpochMilli(item.getValue().getTime()))));
        contentColumn.setCellValueFactory(item -> new ReadOnlyStringWrapper(item.getValue().getContent()));

        timeColumn.prefWidthProperty().bind(serialOutput.widthProperty().divide(4));
        contentColumn.prefWidthProperty().bind(serialOutput.widthProperty().divide(4).multiply(3));

        serialOutput.getColumns().add(timeColumn);
        serialOutput.getColumns().add(contentColumn);
    }

    Monitor initializeMonitor() {
        Monitor monitor = new Monitor();

        monitor.setRecordCallback(this::handleIncoming);
        monitor.setInteractiveCallback(this::handleIncoming);

        new Thread(monitor.initListenTask()).start();

        return monitor;
    }

    void coupleUIEvents(Stage primaryStage, Monitor monitor) {
        primaryStage.setOnCloseRequest(event -> monitor.stopListening());

        userInput.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            try {
                if (interactiveMode.isSelected()) {
                    monitor.writeBytes(e.getText().getBytes());
                } else {
                    if (e.getCode().equals(KeyCode.ENTER)) {
                        monitor.writeBytes(userInput.getText().getBytes());
                        monitor.writeBytes(new byte[]{'\r'});
                        userInput.clear();
                    }
                }
            } catch (SerialPortException serialPortException) {
                serialPortException.printStackTrace();
            }
        });
    }

    <T> void handleIncoming(T incoming) {
        Platform.runLater(() -> {
            if (incoming.getClass() == Record.class) {
                data.add((Record) incoming);
                serialInteractiveOutput.clear();
            } else if (incoming.getClass() == String.class) {
                serialInteractiveOutput.appendText((String) incoming);
            }
        });
    }
}
