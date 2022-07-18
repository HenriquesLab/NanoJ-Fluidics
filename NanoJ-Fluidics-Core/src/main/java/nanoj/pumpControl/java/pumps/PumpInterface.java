package nanoj.pumpControl.java.pumps;

public interface PumpInterface {

    Pump getNewInstance() throws Exception;

    String connectToPump(String connectionIdentifier) throws Exception;

    String getConnectionIdentifier();

    void disconnect() throws Exception;

    void setSyringeDiameter(double diameter) throws Exception;

    void setFlowRate (double flowRate) throws Exception;

    void setTargetVolume(double target) throws Exception;

    /**
     * Starts pumping in the given direction.
     *
     * @param direction direction = true, infuse; else, withdraw
     */
    void startPumping(Pump.Action direction) throws Exception;

    void startPumping(int seconds, Pump.Action direction) throws Exception;

    /**
     * Stops either a single pump device or (on hub devices) the current pump.
     */
    void stopPump() throws Exception;

    /**
     * Stops a specific pump on hub-devices.
     */
    void stopPump(String subPump) throws Exception;

    /**
     * For pump hub type devices, this method stops all pumps.
     * Otherwise, it just stops the one pump.
     */
    void stopAllPumps() throws Exception;

    /**
     * Get Maximum and minimum flow rates for a given syringe diameter
     *
     * @return An array containing the maximum and minimum flow-rates, respectively
     */
    double[] getMaxMin(String subPump,double diameter);

    String sendCommand(String command) throws Exception;

    String getStatus() throws Exception;

    String getPumpName();

    boolean isConnected();

}
