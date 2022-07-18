package nanoj.pumpControl.java.pumps;

import gnu.io.NRSerialPort;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SerialConnection {

    private final NRSerialPort serialPort;
    private final DataInputStream input;
    private final DataOutputStream output;

    public SerialConnection(String comPort, BaudRate baudRate) {

        serialPort = new NRSerialPort(comPort, baudRate.bps);
        serialPort.connect();
        input = new DataInputStream(serialPort.getInputStream());
        output = new DataOutputStream(serialPort.getOutputStream());
    }

    public String readPortData() throws IOException {
        long start = System.currentTimeMillis();
        long timeLapsed;

        StringBuilder response = new StringBuilder();
        char previousByte = 0;

        while (true) {
            if (input.available() > 0) {
                char nextByte = (char) input.read();
                if (previousByte == '\r' && nextByte == '\n') {
                    response.deleteCharAt(response.length() - 1);
                    break;
                }
                response.append(nextByte);
                previousByte = nextByte;
            }
            else {
                timeLapsed = System.currentTimeMillis() - start;
                if (timeLapsed >= 10_000) {
                    throw new IOException("Connection timed out!");
                }
            }
        }

        return response.toString();
    }

    public String sendQuery(String command) throws Exception {
        output.writeBytes(command + ".");
        return readPortData();
    }

    public void disconnect() {
        serialPort.disconnect();
    }

    public boolean isConnected() {
        return serialPort.isConnected();
    }

    @SuppressWarnings("unused")
    public enum BaudRate {
        B_110(110), B_115200(115200), B_1200(1200), B_128000(128000),
        B_14400(14400), B_19200(19200), B_2400(2400), B_256000(256000),
        B_300(300), B_38400(38400), B_4800(4800), B_57600(57600),
        B_600(600), B_9600(9600);

        final int bps;

        BaudRate(int bps) {
            this.bps = bps;
        }
    }

}
