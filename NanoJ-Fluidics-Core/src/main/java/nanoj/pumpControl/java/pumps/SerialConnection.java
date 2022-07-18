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
        B110(110), B115200(115200), B1200(1200), B128000(128000),
        B14400(14400), B19200(19200), B2400(2400), B256000(256000),
        B300(300), B38400(38400), B4800(4800), B57600(57600),
        B600(600), B9600(9600);

        final int bps;

        BaudRate(int bps) {
            this.bps = bps;
        }
    }

}
