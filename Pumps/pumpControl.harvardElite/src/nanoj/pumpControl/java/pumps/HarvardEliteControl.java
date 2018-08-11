package nanoj.pumpControl.java.pumps;

import mmcorej.StrVector;

public final class HarvardEliteControl extends Pump {

    public HarvardEliteControl() {
        name = "Harvard Elite Pump 11";
        subPumps = new String[]{"Single"};
        defaultRate = new double[]{26.594,1473,0.0013855};
        referenceRates.put("Single",defaultRate);
    }

    @Override
    public String connectToPump(String comPort) throws Exception {
        //First, unload any potential leftovers of failed connections
        portName = comPort;
        StrVector devices = core.getLoadedDevices();
        for(int i = 0; i<devices.size(); i++) {
            if (devices.get(i).equals(portName)) {
                core.unloadDevice(portName);
            }
        }

        String answer;

        try {
            core.loadDevice(portName, "SerialManager", comPort);
            core.setProperty(portName, "BaudRate", "9600");
            core.setProperty(portName, "StopBits", "2");
            core.setProperty(portName, "Parity", "None");
            core.initializeDevice(portName);
            answer = sendCommand("cvolume");
        } catch (Exception e) {
            connected = false;
            e.printStackTrace();
            return FAILED_TO_CONNECT;
        }

        connected = true;
        return parseAnswer(answer);
    }

    @Override
    public synchronized void setSyringeDiameter(double diameter) throws Exception{
        sendCommand("diameter " + diameter);
    }

    @Override
    public synchronized void setFlowRate(double flowRate) throws Exception{
        sendCommand("irate " + flowRate + " " + unitsOfFlowRate);
        sendCommand("wrate " + flowRate + " " + unitsOfFlowRate);
    }

    @Override
    public synchronized void setTargetVolume(double target) throws Exception{
        sendCommand("tvolume " + target + " " + unitsOfVolume);
    }

    @Override
    public synchronized void startPumping(Action direction) throws Exception{
        String action;
        if(direction.equals(Action.Infuse)) action = "irun";
        else action = "wrun";
        sendCommand(action);
    }

    @Override
    public synchronized void stopPump() throws Exception {
        sendCommand("stop");
    }

    @Override
    public synchronized String getStatus() throws Exception{
        return parseAnswer(sendCommand(" "));
    }

    @Override
    public String sendCommand(String command) throws Exception {
        if (!connected) return "Failed command, pump not connected!";
        core.setSerialPortCommand(portName, command, "\r");
        return core.getSerialPortAnswer(portName, "\n");
    }

    private String parseAnswer(String answer) {
        if (answer == null) answer = "Null answer!";
        else if (answer.contains(":")) answer = "Pump is idle.";
        else if (answer.contains(">")) answer = "Pump is infusing.";
        else if (answer.contains("<")) answer = "Pump is withdrawing.";
        else if (answer.contains("*")) answer = "Pump is stalled.";
        else if (answer.contains("T*")) answer = "Pump reached target volume.";
        setStatus(answer);
        return answer;
    }
}

