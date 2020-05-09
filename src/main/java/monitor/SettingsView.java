package monitor;

import com.sun.javafx.collections.ImmutableObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import jssc.SerialPort;
import jssc.SerialPortList;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class SettingsView extends Stage {

    private ComboBox<String> portsComboBox;
    private ComboBox<Integer> baudRatesComboBox;
    private ComboBox<Integer> dataBitsComboBox;
    private ComboBox<String> stopBitsComboBox;

    private final Map<String, Integer> stopBits = new HashMap<>();

    private Consumer<Settings> onContinueCallback;

    public SettingsView() {
        setTitle("jaspm Settings");
    }

    public SettingsView onContinue(Consumer<Settings> onContinueCallback) {
        this.onContinueCallback = onContinueCallback;
        return this;
    }

    public SettingsView initUI() {
        GridPane settingsGrid = createSettingsGrid(onContinueCallback);
        setScene(new Scene(settingsGrid, 400, 250));
        return this;
    }

    private GridPane createSettingsGrid(Consumer<Settings> onContinueCallback) {
        GridPane settingsGrid = new GridPane();

        addPortsRow(settingsGrid);
        addBautRatesRow(settingsGrid);
        addDataBitsRow(settingsGrid);
        addStopBitsRow(settingsGrid);
        addContinueButton(settingsGrid, onContinueCallback);

        settingsGrid.setPadding(new Insets(15));
        settingsGrid.setHgap(15);
        settingsGrid.setVgap(15);

        settingsGrid.prefWidthProperty().bind(this.widthProperty());

        return settingsGrid;
    }

    private void addPortsRow(GridPane settingsGrid) {
        String[] portNames = SerialPortList.getPortNames();
        Label portLabel = new Label("Port to monitor");
        portsComboBox = new ComboBox<>(new ImmutableObservableList<>(portNames));

        settingsGrid.addRow(0, portLabel, portsComboBox);

        portsComboBox.prefWidthProperty().bind(settingsGrid.widthProperty().divide(8).multiply(5));
        portsComboBox.getSelectionModel().select(0);
    }

    private void addBautRatesRow(GridPane settingsGrid) {
        Integer[] baudRates = {
                SerialPort.BAUDRATE_110,
                SerialPort.BAUDRATE_300,
                SerialPort.BAUDRATE_600,
                SerialPort.BAUDRATE_1200,
                SerialPort.BAUDRATE_4800,
                SerialPort.BAUDRATE_9600,
                SerialPort.BAUDRATE_14400,
                SerialPort.BAUDRATE_19200,
                SerialPort.BAUDRATE_38400,
                SerialPort.BAUDRATE_57600,
                SerialPort.BAUDRATE_115200,
                SerialPort.BAUDRATE_128000,
                SerialPort.BAUDRATE_256000
        };
        Label baudRatesLabel = new Label("BaudRate");
        baudRatesComboBox = new ComboBox<>(new ImmutableObservableList<>(baudRates));

        settingsGrid.addRow(1, baudRatesLabel, baudRatesComboBox);

        baudRatesComboBox.prefWidthProperty().bind(settingsGrid.widthProperty().divide(8).multiply(5));
        baudRatesComboBox.getSelectionModel().select(8);
    }

    private void addDataBitsRow(GridPane settingsGrid) {
        Integer[] dataBits = {
                SerialPort.DATABITS_5,
                SerialPort.DATABITS_6,
                SerialPort.DATABITS_7,
                SerialPort.DATABITS_8,
        };
        Label dataBitsLabel = new Label("DataBits");
        dataBitsComboBox = new ComboBox<>(new ImmutableObservableList<>(dataBits));

        settingsGrid.addRow(2, dataBitsLabel, dataBitsComboBox);

        dataBitsComboBox.prefWidthProperty().bind(settingsGrid.widthProperty().divide(8).multiply(5));
        dataBitsComboBox.getSelectionModel().select(3);
    }

    private void addStopBitsRow(GridPane settingsGrid) {
        stopBits.put("1", SerialPort.STOPBITS_1);
        stopBits.put("1.5", SerialPort.STOPBITS_1_5);
        stopBits.put("2", SerialPort.STOPBITS_2);

        Label stopBitsLabel = new Label("StopBits");

        stopBitsComboBox = new ComboBox<>(new ImmutableObservableList<>(stopBits.keySet().toArray(new String[0])));

        settingsGrid.addRow(3, stopBitsLabel, stopBitsComboBox);

        stopBitsComboBox.prefWidthProperty().bind(settingsGrid.widthProperty().divide(8).multiply(5));
        stopBitsComboBox.getSelectionModel().select(0);
    }

    private void addContinueButton(GridPane settingsGrid, Consumer<Settings> onContinueCallback) {
        Button continueButton = new Button("Continue");
        settingsGrid.addRow(4, continueButton);
        continueButton.setOnAction(event -> {
            this.close();
            onContinueCallback.accept(
                    new Settings(
                            portsComboBox.getSelectionModel().getSelectedItem(),
                            baudRatesComboBox.getSelectionModel().getSelectedItem(),
                            dataBitsComboBox.getSelectionModel().getSelectedItem(),
                            stopBits.get(stopBitsComboBox.getSelectionModel().getSelectedItem())));
        });
    }
}
