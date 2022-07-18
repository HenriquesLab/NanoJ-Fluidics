package nanoj.pumpControl.java.pumps;

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

    public final SerialConnection.BaudRate baudRate;
    protected SerialConnection connection;
    protected String portName;

    protected SerialPump(SerialConnection.BaudRate baudRate) {
        this.baudRate = baudRate;
    }

    @Override
    public String getConnectionIdentifier() {
        return portName;
    }

    @Override
    public String connectToPump(String connectionIdentifier) throws Exception {
        // Clean up any previous connection
        disconnect();

        portName = connectionIdentifier;
        connection = new SerialConnection(connectionIdentifier, baudRate);

        return connection.readPortData();
    }

    @Override
    public void disconnect() {
        if (isConnected()) {
            connection.disconnect();
        }
    }

    @Override
    public boolean isConnected() {
        return connection != null && connection.isConnected();
    }

    @Override
    public String sendCommand(String command) throws Exception {
        setStatus("Sending command to pump: " + command);
        String response = connection.sendQuery(command);
        setStatus("Pump response: " + response);
        return response;
    }


}
