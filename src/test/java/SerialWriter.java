import jssc.SerialPort;
import jssc.SerialPortException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class SerialWriter {

    private String readLine(SerialPort serialPort) throws SerialPortException {
        int lineBufferSize = 1024;
        byte[] lineBuffer = new byte[lineBufferSize];
        int lineBufferIndex = 0;

        while (true) {
            byte[] buffer = serialPort.readBytes(1);
            final byte byteRead = buffer[0];

            lineBuffer[lineBufferIndex++] = byteRead;

            if (byteRead == '\r')
                return new String(Arrays.copyOfRange(lineBuffer, 0, lineBufferIndex));
        }
    }

    private char readInteractive(SerialPort serialPort) throws SerialPortException {
        return (char) serialPort.readBytes(1)[0];
    }

    private void writeString(SerialPort serialPort, String s) throws SerialPortException {
        serialPort.writeString(s);
        serialPort.writeBytes(new byte[]{'\r', '\n'});
    }

    private void writeStringInteractive(SerialPort serialPort, String s) throws SerialPortException {
        serialPort.writeString(s);
    }

    @Test
    void sendHelloWorld() throws SerialPortException {
        SerialPort port = new SerialPort("COM4");
        port.openPort();
        writeString(port, "Hello, World!");
        port.closePort();
    }

    @Test
    void sendLoginRequest() throws SerialPortException {
        SerialPort port = new SerialPort("COM4");
        port.openPort();

        writeStringInteractive(port, "Enter username: ");
        writeString(port, readLine(port));

        writeStringInteractive(port, "Enter password: ");
        writeString(port, readLine(port));

        port.closePort();
    }

    @Test
    void sendInteractiveMenu() throws SerialPortException {
        SerialPort port = new SerialPort("COM4");
        port.openPort();

        String[] options = {"Panda", "Pizza", "Cat", "Bunny"};
        for (int i = 0; i < options.length; i++)
            writeString(port, i + ". " + options[i]);

        writeStringInteractive(port, "Pick something: ");

        char c = readInteractive(port);
        writeString(port, Character.toString(c));
        writeString(port, "You chose the " + options[Character.getNumericValue(c)]);

        port.closePort();
    }
}
