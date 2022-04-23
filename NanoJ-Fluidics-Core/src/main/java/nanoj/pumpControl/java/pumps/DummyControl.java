package nanoj.pumpControl.java.pumps;

import nanoj.pumpControl.java.sequentialProtocol.GUI;

public final class DummyControl extends Pump implements PumpInterface {
    private final GUI.Log log;

    public DummyControl() {
        log = GUI.INSTANCE.log;

        boolean demo = false;
        //noinspection ConstantConditions
        if (demo) {
            name = "NanoJ Lego Control Hub ";
            subPumps = new String[]{
                    "S1,P1",
                    "S1,P2",
                    "S1,P3",
                    "S1,P4",
                    "S2,P1",
                    "S2,P2",
                    "S2,P3",
                    "S2,P4",
                    "S3,P1"};
        } else {
            name = "Virtual pump";
            subPumps = new String[]{"Sub 1", "Sub 2", "Sub 3"};
        }

        for (String subPump: subPumps)
            referenceRates.put(subPump,new double[]{4,1,0.25});
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
        portName = comPort;
        core.loadDevice(portName, "SerialManager", comPort);
        setStatus("Connected to " + portName);
        return status;
    }

    @Override
    public void setSyringeDiameter(double diameter) {
        setStatus(currentSubPump() + "Set Syringe Diameter to " + diameter);
    }

    @Override
    public void setFlowRate(double flowRate) {
        setStatus(currentSubPump() + "Set Flow Rate to " + flowRate + " ul/s");
        message(status);
    }

    @Override
    public void setTargetVolume(double target) {
        setStatus(currentSubPump() + "Set Syringe Volume to " + target + " ul");
        message(status);
    }

    @Override
    public void startPumping(Action direction) {
        message("Reference diameter for sub pump is: " + referenceRates.get(currentSubPump)[0]);
        message("Reference max rate for sub pump is: " + referenceRates.get(currentSubPump)[1]);
        message("Reference min rate for sub pump is: " + referenceRates.get(currentSubPump)[2]);
        setStatus(currentSubPump() + " told to " + direction);
        message(status);
    }

    @Override
    public void startPumping(int seconds, Action direction) {
        String action;
        if(direction.equals(Action.Infuse)) action = "push";
        else action = "withdraw";
        message("Reference diameter for sub pump is: " + referenceRates.get(currentSubPump)[0]);
        message("Reference max rate for sub pump is: " + referenceRates.get(currentSubPump)[1]);
        message("Reference min rate for sub pump is: " + referenceRates.get(currentSubPump)[2]);
        setStatus(currentSubPump() + " told to " + action + " for " + seconds + " seconds.");
        message(status);
    }

    @Override
    public void stopAllPumps() {
        setStatus("Stopped ALL the pumps.");
        message(status);
    }

    @Override
    public void stopPump() {
        setStatus("Stopped current pump: " + currentSubPump);
        message(status);
    }

    @Override
    public void stopPump(String subPump) {
        setStatus("Stopped pump: " + subPump);
        message(status);
    }

    @Override
    public String sendCommand(String command) { return null; }

    @Override
    public String getStatus() { return status; }

}
