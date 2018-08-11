package nanoj.pumpControl.java.pumps;

public interface PumpInterface {

    Pump getNewInstance() throws Exception;
    String connectToPump(String comPort) throws Exception;
    boolean disconnect() throws Exception;
    void setSyringeDiameter(double diameter) throws Exception;
    void setFlowRate (double flowRate) throws Exception; //Flowrate is always equal to the defaults in Pump Abstract class
    void setTargetVolume(double target) throws Exception;
    void startPumping(Pump.Action direction) throws Exception; //direction = true, infuse; else, withdraw
    void stopPump() throws Exception; //Stops either a single pump device or (on hub devices) the current pump.
    void stopPump(String subPump) throws Exception; //Stops a specific pump on hub-devices.
    void stopAllPumps() throws Exception; // For pump hub type devices, this method stops all pumps.
    double[] getMaxMin(String subPump,double diameter); // Get Maximum and minimum flow rates for a given syringe diameter
    String[] getSubPumps();
    String sendCommand(String command) throws Exception;
    String getStatus() throws Exception;
    String getPumpName();
    boolean isConnected();

}
