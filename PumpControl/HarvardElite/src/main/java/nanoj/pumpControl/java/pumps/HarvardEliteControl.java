package nanoj.pumpControl.java.pumps;

public final class HarvardEliteControl extends SerialPump {

    public HarvardEliteControl() {
        super(SerialConnection.BaudRate.B_9600);
        name = "Harvard Elite Pump 11";
        subPumps = new String[]{"Single"};
        defaultRate = new double[]{26.594,1473,0.0013855};
        referenceRates.put("Single",defaultRate);
    }

    @Override
    public String connectToPump(String connectionIdentifier) throws Exception {
        super.connectToPump(connectionIdentifier);
        String status = parseAnswer(sendCommand("cvolume"));
        setStatus(status);
        return status;
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
    public void startPumping(int seconds, Action direction) throws Exception {
        sendCommand("tvolume " + seconds + " " + unitsOfVolume);

        String action;
        if(direction.equals(Action.Infuse)) {
            sendCommand("irate " + 1 + " " + unitsOfFlowRate);
            action = "irun";
        }
        else {
            sendCommand("wrate " + 1 + " " + unitsOfFlowRate);
            action = "wrun";
        }
        sendCommand(action);
    }

    @Override
    public synchronized void stopPump() throws Exception {
        sendCommand("stop");
    }

    @Override
    public synchronized String getStatus() throws Exception {
        return parseAnswer(sendCommand(" "));
    }

    private String parseAnswer(String answer) {
        if (answer == null) answer = "Null answer!";
        else if (answer.contains(":")) answer = "Pump is idle.";
        else if (answer.contains(">")) answer = "Pump is infusing.";
        else if (answer.contains("<")) answer = "Pump is withdrawing.";
        else if (answer.contains("*")) answer = "Pump is stalled.";
        else if (answer.contains("T*")) answer = "Pump reached target volume.";
        return answer;
    }
}

