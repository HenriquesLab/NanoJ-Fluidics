package nanoj.pumpControl.java.pumps;

import nanoj.pumpControl.java.sequentialProtocol.GUI;

import java.io.IOException;

import static nanoj.pumpControl.java.pumps.SerialConnection.BaudRate.B57600;

public class LegoControl extends Pump {
    private final GUI.Log log = GUI.Log.INSTANCE;
    private static final String SHIELD = "S";
    private static final String PUMP = "P";

    public final static SerialConnection.BaudRate BAUD_RATE = B57600;
    private SerialConnection connection;

    public LegoControl() {
        name = "NanoJ Lego Control Hub";
        timeOut = 2000;

        double max = 2.3;
        defaultRate = new double[]{4.699,max,max*0.25};
    }

    @Override
    public String connectToPump(String comPort) throws IOException {
        String answer;

        // Clean up any previous connection
        disconnect();

        portName = comPort;
        connection = new SerialConnection(comPort, BAUD_RATE);

        // Discard initial "Connected!" message
        connection.readPortData();

        try {
            answer = sendCommand("p");
        } catch (Exception e) {
            e.printStackTrace();
            return FAILED_TO_CONNECT;
        }

        connected = true;
        int[] sPumps = new int[] {
                Integer.parseInt(answer.substring(0, answer.indexOf("."))),
                Integer.parseInt(answer.substring(answer.indexOf(".") + 1))
        };

        subPumps = new String[sPumps[0]*sPumps[1]];
        int a = 0;
        for (int s = 0; s < sPumps[0]; s++)
            for (int p = 0; p < sPumps[1]; p++) {
                subPumps[a] = SHIELD + (s+1) + "," + PUMP + (p+1);
                a++;
            }

        for (String subPump : subPumps)
            referenceRates.put(subPump,defaultRate);

        return answer;
    }

    @Override
    public void setFlowRate(double givenFlowRate) throws Exception {
        flowRate = givenFlowRate;
        double[] maxMin = getMaxMin(currentSubPump,syringeDiameter);

        if (flowRate > maxMin[0]) flowRate = maxMin[0];
        else if(flowRate < maxMin[1]) flowRate = maxMin[1];

        // The pumps only accept values in the range of 0-255, so this calculates a ratio
        // and converts it to the proper range.

        int commandFlowInt = new Double((flowRate/maxMin[0])*255).intValue();

        String commandFlow = "";
        if (commandFlowInt < 10) {
            commandFlow = "00" + commandFlowInt;
        }
        else if (commandFlowInt < 100) {
            commandFlow = "0" + commandFlowInt;
        }
        else if (commandFlowInt < 256) {
            commandFlow = "" + commandFlowInt;
        }
        /* Pump serial command: sxynnn = for pump xy set speed nnn*/
        sendCommand("s" + parseSubPump(currentSubPump) + commandFlow);
    }

    @Override
    public void setTargetVolume(double target) { targetVolume = target; } //Target volume should be given in ul

    @Override
    public synchronized void startPumping(Action direction) throws Exception {
        // Target volume is in ul and flowrate in ul/sec but the arduino code
        // wants a duration in seconds. So we have to convert.

        int duration = (int) Math.round(targetVolume/flowRate);
        startPumping(duration,direction);
    }

    @Override
    public void startPumping(int duration, Action direction) throws Exception {
        int action;

        String target = "";

        if(duration < 10) {
            target = "0000" + duration;
        }
        else if(duration < 100) {
            target = "000" + duration;
        }
        else if(duration < 1000) {
            target = "00" + duration;
        }
        else if(duration < 10000) {
            target = "0" + duration;
        }
        if(direction.equals(Action.Infuse)) action = 1;
        else action = 2;

        /*
        rxydttttt - Start pump xy in direction d for ttttt seconds
        d = 1 is forward, d = 2 is backwards
        */
        sendCommand("r" + parseSubPump(currentSubPump) + action + target);
    }

    @Override
    public void disconnect() {
        if (connection != null && connection.isConnected()) {
            connection.disconnect();
        }
    }

    @Override
    public void stopAllPumps() throws Exception {
        sendCommand("a");
    }

    @Override
    public synchronized void stopPump() throws Exception {
        sendCommand("a" + parseSubPump(currentSubPump));
    }

    @Override
    public synchronized void stopPump(String subPump) throws Exception {
        sendCommand("a" + parseSubPump(subPump));
    }

    //TODO: Create a status getter that automatically parses the lego style reply.
    @Override
    public synchronized String getStatus() {
        return "Pump alive."/*sendQuery("g")*/;
    }

    @Override
    public String sendCommand(String command) throws Exception {
        log.message("Command sent to Lego pump: " + command);
        return connection.sendQuery(command);
    }

    private String parseSubPump(String subPumpString){
        int indexOfPump = subPumpString.indexOf(PUMP) + PUMP.length();
        String shield = Integer.parseInt(subPumpString.substring(SHIELD.length(),SHIELD.length()+1)) + "";
        String pump = Integer.parseInt(subPumpString.substring(indexOfPump,indexOfPump+1)) + "";
        return shield + pump;
    }

}
