package monitor;

import com.moandjiezana.toml.Toml;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import jssc.SerialPortException;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class MonitorView extends Stage {

    final ObservableList<Record> data = FXCollections.observableArrayList();
    final TableView<Record> serialOutput = new TableView<>(data);
    final TextField serialInteractiveOutput = new TextField();
    final TextField userInput = new TextField();
    final CheckBox interactiveMode = new CheckBox("Interactive");

    public MonitorView(Settings settings) {
        initializeUI();
        Monitor monitor = initializeMonitor(settings);
        coupleUIEvents(monitor);
    }

    private void initializeUI() {
        setTitle("spm");

        MenuBar menuBar = initializeMenuBar();
        initializeTableView();

        HBox userInputPane = new HBox(userInput, interactiveMode);

        VBox root = new VBox(menuBar, serialOutput, serialInteractiveOutput, userInputPane);
        VBox.setVgrow(serialOutput, Priority.ALWAYS);

        setScene(new Scene(root, 400, 400));
    }

    private MenuBar initializeMenuBar() {
        MenuItem menuItemBoot = new MenuItem("boot");
        MenuItem menuItemProgram = new MenuItem("program");

        String atProgram = "C:/Program Files (x86)/Atmel/Studio/7.0/atbackend/atprogram.exe";

        menuItemBoot.setOnAction(event -> {
            String[] chipErase = new String[]{atProgram, "-t", "atmelice", "-i", "PDI", "-d", "ATxmega256A3BU", "chiperase"};
            String[] program = new String[]{atProgram, "-t", "atmelice", "-i", "PDI", "-d", "ATxmega256A3BU", "program", "-c", "-fl", "-f", "C:/Users/jasper.catthoor/IdeaProjects/LoLoBranch/ewall_ze/LoLo/target/boot_TNM_NC/ewall_boot_TNM_NC.hex"};
            new Thread(() -> {
                try {
                    new ProcessBuilder(chipErase).inheritIO().start().waitFor();
                    new ProcessBuilder(program).inheritIO().start().waitFor();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        });

        menuItemProgram.setOnAction(event -> {
            String[] program = new String[]{atProgram, "-t", "atmelice", "-i", "PDI", "-d", "ATxmega256A3BU", "program", "-c", "-fl", "-f", "C:/Users/jasper.catthoor/IdeaProjects/LoLoBranch/ewall_ze/LoLo/target/TNM_NC/ewall_TNM_NC.hex"};
            try {
                new ProcessBuilder(program).inheritIO().start().waitFor();
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        });

        Menu menu = new Menu("Upload", null, menuItemBoot, menuItemProgram);

        return new MenuBar(menu);
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

    Monitor initializeMonitor(Settings settings) {
        Monitor monitor = new Monitor(
                settings.getPortName(),
                settings.getBaudRate(),
                settings.getDataBits(),
                settings.getStopBits());

        monitor.setRecordCallback(this::handleIncoming);
        monitor.setInteractiveCallback(this::handleIncoming);

        new Thread(monitor.initListenTask()).start();

        return monitor;
    }

    void coupleUIEvents(Monitor monitor) {
        setOnCloseRequest(event -> monitor.stopListening());

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
                handleIncomingLine((Record) incoming);
            } else if (incoming.getClass() == String.class) {
                serialInteractiveOutput.appendText((String) incoming);
            }
        });
    }

    private void handleIncomingLine(Record record) {
        data.add(record);
        serialInteractiveOutput.clear();

        String content = record.getContent();
        if (content.startsWith("log[")) {
            String serialNumber = content.substring(4).split("]")[0];
            String[] parameters = content.split("]:")[1].split(";");
            String firstPart = parameters[0];
            String locked = parameters[1];
            String pp = parameters[2];
            String cp = parameters[3];
            String cpn = parameters[4];
            String unknown = parameters[5];

            String[] meterValues = parameters[6].split(",");
            String energyWh = meterValues[0];
            String currentL1 = meterValues[1];
            String currentL2 = meterValues[2];
            String currentL3 = meterValues[3];
            String powerL1 = meterValues[4];
            String powerL2 = meterValues[5];
            String powerL3 = meterValues[6];
            String voltageL1 = meterValues[7];
            String voltageL2 = meterValues[8];
            String voltageL3 = meterValues[9];
            String freq = meterValues[10];
            String powerFactor = meterValues[11];
            String activePower = meterValues[12];
            String reActivePower = meterValues[13];
            String positiveActiveEnergyL1 = meterValues[14];
            String negativeActiveEnergyL1 = meterValues[15];
            String positiveReActiveEnergyL1 = meterValues[16];
            String negativeReActiveEnergyL1 = meterValues[17];
            String positiveActiveEnergyL2 = meterValues[18];
            String negativeActiveEnergyL2 = meterValues[19];
            String positiveReActiveEnergyL2 = meterValues[20];
            String negativeReActiveEnergyL2 = meterValues[21];
            String positiveActiveEnergyL3 = meterValues[22];
            String negativeActiveEnergyL3 = meterValues[23];
            String positiveReActiveEnergyL3 = meterValues[24];
            String negativeReActiveEnergyL3 = meterValues[25];
            String temperature = meterValues[26];

            String suffix = parameters[9];
            String[] suffixParts = suffix.split("\\|");
            String dFixValue = suffixParts[0];
            String doStatus = suffixParts[1];
            String ledStatus = suffixParts[2];
            String meterKWH = suffixParts[3];

            String[] evenMoreSuffixParts = suffixParts[4].split(",");
            String cp100 = evenMoreSuffixParts[0];
            String cpn100 = evenMoreSuffixParts[1];
            String lastIteraties = evenMoreSuffixParts[2];

            System.out.println("TEMPERATURE " + temperature);
            System.out.println("LED STATUS " + ledStatus);
            System.out.println("LOCKED: " + locked);

            sendData(
                    record.getTime(),
                    Double.parseDouble(temperature),
                    Integer.parseInt(ledStatus),
                    Integer.parseInt(locked) != 0,
                    Double.parseDouble(pp),
                    Double.parseDouble(cp),
                    Double.parseDouble(currentL1));
        }
    }

    private void insert(String url, String user, String password, String table, long time, Object value) {
        String query = String.format("INSERT INTO %s(time, value) VALUES(?, ?)", table);
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pst = con.prepareStatement(query)) {

            pst.setLong(1, time);

            pst.setObject(2, value);

            pst.executeUpdate();

        } catch (SQLException ignored) {
        }
    }

    private void sendData(long time, double temperature, int ledStatus, boolean locked, double pp, double cp, double currentL1) {
        Toml toml = Config.get();

        String user = Config.get().getString("postgres.user");
        String password = Config.getSecret().getString("postgres.password");

        String url = String.format("jdbc:postgresql://%s:%s/temperature",
                toml.getString("postgres.host"),
                toml.getString("postgres.port"));

        insert(url, user, password, "temperature", time, temperature);
        insert(url, user, password, "led", time, ledStatus);
        insert(url, user, password, "locked", time, locked);
        insert(url, user, password, "pp", time, pp);
        insert(url, user, password, "cp", time, cp);
        insert(url, user, password, "current_l1", time, currentL1);
    }
}
