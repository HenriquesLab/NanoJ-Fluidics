package nanoj.pumpControl.java.pumps;

import nanoj.pumpControl.java.sequentialProtocol.GUI;

/**
 * A Pump class that connects through a serial port.
 * <p>
 *     It provides default implementations of {@link #connectToPump(String)}, {@link #disconnect()} and
 *     {@link #sendCommand}. Serial connections are managed through a {@link SerialConnection} convenience wrapper
 *     and child classes can make use of it for direct control of the serial connection through the
 *     {@link #connection} field.
 * </p>
 */
public abstract class SerialPump extends Pump {
    protected final GUI.Log log = GUI.Log.INSTANCE;

    public final SerialConnection.BaudRate baudRate;
    protected SerialConnection connection;

    protected SerialPump(SerialConnection.BaudRate baudRate) {
        this.baudRate = baudRate;
    }

    @Override
    public String connectToPump(String comPort) throws Exception {
        // Clean up any previous connection
        disconnect();

        portName = comPort;
        connection = new SerialConnection(comPort, baudRate);

        return connection.readPortData();
    }

    @Override
    public void disconnect() {
        if (connection != null && connection.isConnected()) {
            connection.disconnect();
        }
    }

    @Override
    public String sendCommand(String command) throws Exception {
        log.message("Command sent to Lego pump: " + command);
        return connection.sendQuery(command);
    }


}
