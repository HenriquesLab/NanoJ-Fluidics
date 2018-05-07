package nanoj.pumpControl.java.pumps;

import nanoj.pumpControl.java.sequentialProtocol.GUI;

public final class DummyControl extends Pump implements PumpInterface {
    private GUI.Log log;
    private boolean demo = false;

    public DummyControl() {
        log = GUI.INSTANCE.log;

        if (demo) {
            name = "Lego pump";
            subPumps = new String[]{"LEGO S1P1","LEGO S1P2","LEGO S1P3","LEGO S1P4","LEGO S2P1","LEGO S2P2","LEGO S2P3","LEGO S2P4","LEGO S3P1",};
        } else {
            name = "Virtual pump";
            subPumps = new String[]{"Sub 1", "Sub 2", "Sub 3"};
        }
    }

    private String currentSubPump() {
        return portName + ", " + getCurrentSubPump() + ": ";
    }

    void message(String text) {
        log.message("Virtual pump says: " + text);
    }

    @Override
    public String connectToPump(String comPort) throws Exception {
        connected = true;
        portName = "Port " + comPort;
        core.loadDevice(portName, "SerialManager", comPort);
        status = "Connected to " + portName;
        return status;
    }

    @Override
    public void setSyringeDiameter(double diameter) throws Exception{
        status = currentSubPump() + "Set Syringe Diameter to " + diameter;
        message(status);
    }

    @Override
    public void setFlowRate(double flowRate) throws Exception {
        status = currentSubPump() + "Set Flow Rate to " + flowRate + " ul/s";;
        message(status);
    }

    @Override
    public void setTargetVolume(double target) throws Exception{
        status = currentSubPump() + "Set Syringe Volume to " + target + " ul";
        message(status);
    }

    @Override
    public void startPumping(boolean direction) throws Exception {
        String action;
        if(direction) action = "pushing.";
        else action = "withdrawing.";
        status = currentSubPump() + "Started " + action;
        message(status);
    }

    @Override
    public void stopAllPumps() throws Exception {
        status = "Stopped ALL the pumps.";
        message(status);
    }

    @Override
    public void stopPump() {
        status = "Stopped current pump: " + subPumps[currentSubPump];
        message(status);
    }

    @Override
    public void stopPump(int pumpIndex) throws Exception {
        status = "Stopped pump: " + subPumps[pumpIndex];
        message(status);
    }

    @Override
    public String sendCommand(String command) throws Exception { return null; }

    @Override
    public String getStatus() { return status; }

    @Override
    public double[] getMaxMin(double diameter) {
        double max = 1 * diameter;
        double min = 0.1 * diameter;
        return new double[]{max,min};
    }
}
