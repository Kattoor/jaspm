package monitor;

import javafx.concurrent.Task;
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;

import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class Monitor {

    private SerialPort serialPort;
    private Consumer<Record> recordCallback;
    private Consumer<String> interactiveCallback;

    private final AtomicBoolean listening = new AtomicBoolean(true);

    public Monitor(String portName, int baudRate, int dataBits, int stopBits) {
        this.serialPort = new SerialPort(portName);

        try {
            serialPort.openPort();
            serialPort.setParams(baudRate, dataBits, stopBits, SerialPort.PARITY_NONE);
            serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
        } catch (SerialPortException e) {
            e.printStackTrace();
        }
    }

    public void writeBytes(byte[] bytes) throws SerialPortException {
        serialPort.writeBytes(bytes);
    }

    public void setRecordCallback(Consumer<Record> recordCallback) {
        this.recordCallback = recordCallback;
    }

    public void setInteractiveCallback(Consumer<String> interactiveCallback) {
        this.interactiveCallback = interactiveCallback;
    }

    public void stopListening() {
        listening.set(false);
    }

    public Task<Void> initListenTask() {
        return new Task<Void>() {
            @Override
            protected Void call() {
                serialListenLoop();
                return null;
            }
        };
    }

    private void serialListenLoop() {
        try {
            int lineBufferSize = 1024;
            byte[] lineBuffer = new byte[lineBufferSize];
            int lineBufferIndex = 0;

            while (listening.get()) {
                byte[] buffer = serialPort.readBytes(1);
                final byte byteRead = buffer[0];

                lineBuffer[lineBufferIndex++] = byteRead;

                if (byteRead == '\n') {
                    Record record = new Record(Instant.now().toEpochMilli(), new String(Arrays.copyOfRange(lineBuffer, 0, lineBufferIndex)));
                    recordCallback.accept(record);

                    lineBuffer = new byte[lineBufferSize];
                    lineBufferIndex = 0;
                } else {
                    interactiveCallback.accept(new String(new byte[]{byteRead}));
                }
            }
        } catch (SerialPortException e) {
            e.printStackTrace();
        }
    }
}
