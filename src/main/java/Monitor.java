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
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Monitor extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    private final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("spm");

        final ObservableList<Record> data = FXCollections.observableArrayList();
        final TableView<Record> serialOutput = new TableView<>(data);

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

        final TextField serialInteractiveOutput = new TextField("");

        final TextField userInput = new TextField("");
        final CheckBox interactiveMode = new CheckBox("Interactive");
        HBox userInputPane = new HBox(userInput, interactiveMode);

        VBox root = new VBox(serialOutput, serialInteractiveOutput, userInputPane);
        VBox.setVgrow(serialOutput, Priority.ALWAYS);

        primaryStage.setScene(new Scene(root, 400, 400));
        primaryStage.show();

        String[] portNames = SerialPortList.getPortNames();
        if (portNames.length == 0) return;
        System.out.println("Listening to port: " + portNames[0]);
        final SerialPort serialPort = new SerialPort(portNames[0]);
        try {
            serialPort.openPort();
            serialPort.setParams(
                    SerialPort.BAUDRATE_38400,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);
            serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
        } catch (SerialPortException e) {
            e.printStackTrace();
        }

        new Thread(() -> {
            try {
                int lineBufferSize = 1024;
                byte[] lineBuffer = new byte[lineBufferSize];
                int lineBufferIndex = 0;

                while (true) {
                    byte[] buffer = serialPort.readBytes(1);
                    final byte byteRead = buffer[0];

                    lineBuffer[lineBufferIndex++] = byteRead;

                    if (byteRead == '\n') {
                        queue.add(new String(Arrays.copyOfRange(lineBuffer, 0, lineBufferIndex)));
                        lineBuffer = new byte[lineBufferSize];
                        lineBufferIndex = 0;

                        Platform.runLater(() -> {
                            serialInteractiveOutput.clear();
                            data.add(new Record(Instant.now().toEpochMilli(), queue.poll()));
                        });
                    } else {
                        Platform.runLater(() -> serialInteractiveOutput.appendText(new String(new byte[]{byteRead})));
                    }
                }
            } catch (SerialPortException e) {
                e.printStackTrace();
            }
        }).start();

        userInput.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            try {
                if (interactiveMode.isSelected()) {
                    serialPort.writeBytes(e.getText().getBytes());
                } else {
                    if (e.getCode().equals(KeyCode.ENTER)) {
                        serialPort.writeBytes(userInput.getText().getBytes());
                        serialPort.writeBytes(new byte[]{'\r'});
                        userInput.clear();
                    }
                }
            } catch (SerialPortException serialPortException) {
                serialPortException.printStackTrace();
            }
        });
    }
}
