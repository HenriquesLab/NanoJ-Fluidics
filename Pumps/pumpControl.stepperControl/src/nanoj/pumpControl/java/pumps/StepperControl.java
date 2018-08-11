package nanoj.pumpControl.java.pumps;

import mmcorej.StrVector;
import nanoj.pumpControl.java.sequentialProtocol.GUI;


public class StepperControl extends Pump{
    private GUI.Log log = GUI.Log.INSTANCE;
    private String comPort;

    public StepperControl() {
        subPumps = new String[]{"Single"};
        name = "NanoJ 3D printed pump";
        timeOut = 2000;
    }

    @Override
    public String connectToPump(String comPort) throws Exception {
        this.comPort = comPort;
        //First, unload any potential leftovers of failed connections
        portName = comPort;
        StrVector devices = core.getLoadedDevices();
        for (int i = 0; i < devices.size(); i++) {
            if (devices.get(i).equals(portName)) {
                core.unloadDevice(portName);
            }
        }

        String answer;

        try {
            answer = sendCommand("Hello");
            connected = true;
        } catch (Exception e) {
            e.printStackTrace();
            return FAILED_TO_CONNECT;
        }

        return answer;
    }

    @Override
    public void setFlowRate(double flowRate) throws Exception {
        int rate = (int) flowRate;
        sendCommand("s" + rate);
    }

    @Override
    public void setTargetVolume(double target) throws Exception {
        targetVolume = target;  //Target volume should be given in ul
    }

    @Override
    public void startPumping(Action forward) throws Exception {

        int direction = 2;
        if(forward.equals(Action.Infuse)) direction = 1;

        int volume = (int) targetVolume;

        sendCommand("r" + direction + "" + volume);
    }

    @Override
    public void stopPump() throws Exception {
        sendCommand("a");
    }

    @Override
    public double[] getMaxMin(String subPump, double diameter) {
        double max = 1 * diameter * 15;
        double min = 0.1 * diameter;
        return new double[]{max,min};
    }

    @Override
    public String sendCommand(String command) throws Exception {
        /* code assuming the core object doesn't need to reconnect every time
        core.setSerialPortCommand(portName, command + ".", "\r");
        String answer = core.getSerialPortAnswer(portName, "\n");
        //CharVector answer =  core.readFromSerialPort(portName);
        out("Command sent to Lego pump: " + command + ", heard: " + answer);
        return "" + answer;
        */

        //First, unload any potential leftovers of failed connections
        portName = comPort;
        StrVector devices = core.getLoadedDevices();
        for(int i = 0; i<devices.size(); i++) {
            if (devices.get(i).equals(portName)) {
                core.unloadDevice(portName);
            }
        }
        String result;
        try {

            core.loadDevice(portName, "SerialManager", comPort);
            core.setProperty(portName, "AnswerTimeout", "" + timeOut);
            core.setProperty(portName, "BaudRate", "57600");
            core.setProperty(portName, "StopBits", "2");
            core.setProperty(portName, "Parity", "None");
            core.initializeDevice(portName);

            core.setSerialPortCommand(portName, "", "\r");
            core.getSerialPortAnswer(portName, "\n");

            log.message("Command sent to 3D printed pump: " + command);
            core.setSerialPortCommand(portName, command + ".", "\r");
            result = core.getSerialPortAnswer(portName, "\n");
        } catch (Exception e) {
            throw e;
        }
        result = result.substring(0, result.length()-1);
        String prefix = "Response from pump: ";
        log.message(prefix + result);
        setStatus(prefix + result);
        return result;

    }

    @Override
    public String getStatus() throws Exception {
        return "All is well, friend.";
    }

}
