package nanoj.pumpControl.java.pumps;

import mmcorej.CMMCore;
import java.util.*;
import nanoj.pumpControl.java.pumps.ConnectedSubPumpsList.PumpNotFoundException;

public class PumpManager extends Observable implements Observer {
    private CMMCore serialManager;
    private LinkedHashMap<String, Pump> availablePumps = new LinkedHashMap<String, Pump>();
    private ConnectedSubPumpsList connectedSubPumps = new ConnectedSubPumpsList();
    private boolean alive = true;
    private String status;

    private long wait = 100;  // Wait time for status checker in milliSeconds

    public static final String FAILED_TO_CONNECT = "Failed to connect to pump.";
    public static final String PORT_ALREADY_CONNECTED = "Port already in use.";
    public static final String NOT_AVAILABLE = "Pump not available to manager.";
    public static final String NEW_STATUS_AVAILABLE = "New status available.";
    public static final String NO_PUMP_CONNECTED = "No pump connected.";
    public static final String NEW_PUMP_CONNECTED = "New pump has been connected.";
    public static final String PUMP_DISCONNECTED = "A pump has been disconnected.";
    public static final String NO_PUMP_AVAILABLE = "No Pump available!";

    public static final String VOLUME_UNITS = "ul";
    public static final String TIME_UNITS = "sec";
    public static final String FLOW_RATE_UNITS = VOLUME_UNITS + "/" + TIME_UNITS;

    public final static PumpManager INSTANCE = new PumpManager();

    /*This no-argument constructor is required for the singleton design pattern.
      Remember to loadPlugins.
      loadPlugins is called after construction since the PumpManager class might be loaded in the class path and created
      before the application (in our case MicroManager) has loaded all plugin classes
      */
    private PumpManager() {  }

    public void loadPlugins() {
        //Looks in the class path for all classes declaring themselves as services of the type "nanoj.(...).Pump"

        ServiceLoader<Pump> serviceLoader = ServiceLoader.load(Pump.class);
        for (Pump pump : serviceLoader) {
            availablePumps.put(pump.getPumpName(), pump);
        }
    }

    public void setCore(CMMCore core) { serialManager = core; }

    public String connect(String pump, String port) throws Exception {
        if (connectedSubPumps.connectedPorts().contains(port)) return PORT_ALREADY_CONNECTED;
        if (!availablePumps.containsKey(pump)) return NOT_AVAILABLE;

        Pump newPump = availablePumps.get(pump).getNewInstance();
        newPump.setCore(serialManager);
        String answer = newPump.connectToPump(port);

        if (answer.equals(Pump.FAILED_TO_CONNECT)) {
            return Pump.FAILED_TO_CONNECT;
        }

        connectedSubPumps.addPump(newPump);

        newPump.addObserver(this);

        setChanged();
        notifyObservers(NEW_PUMP_CONNECTED);
        return answer;
    }

    public synchronized String startPumping(int pumpIndex, int syringe, double flowRate,
                               double targetVolume, Pump.Action action) throws Exception {
        String answer;

        ConnectedSubPump connectedSubPump = connectedSubPumps.getConnectedSubPump(pumpIndex);
        Pump pump = connectedSubPump.pump;

        pump.setUnitsOfVolume(VOLUME_UNITS);
        pump.setUnitsOfTime(TIME_UNITS);
        pump.setCurrentSubPump(connectedSubPump.subPump);
        pump.setSyringeDiameter(SyringeList.getDiameter(syringe));
        pump.setFlowRate(flowRate);
        pump.setTargetVolume(targetVolume);
        pump.startPumping(action);

        answer = action.toString() + " " + targetVolume+" "+pump.getUnitsOfVolume()+ " at " +flowRate+" "
                + pump.getUnitsOfFlowRate() + " with " + SyringeList.getVolumeWUnits(syringe) + " syringe.";

        setChanged();
        notifyObservers(NEW_STATUS_AVAILABLE);
        return answer;
    }

    public synchronized void stopAllPumps() throws Exception {
        for (Pump pump: connectedSubPumps.getAllConnectedPumps())
            pump.stopAllPumps();
    }

    public synchronized void stopPumping(int pumpIndex) throws Exception {
        String subPump = connectedSubPumps.getConnectedSubPump(pumpIndex).subPump;
        connectedSubPumps.getConnectedSubPump(pumpIndex).pump.stopPump(subPump);
        setChanged();
        notifyObservers(NEW_STATUS_AVAILABLE);
    }

    public synchronized void stopPumping(String pumpName, String subPump, String port) throws Exception {
        connectedSubPumps.getConnectedSubPump(pumpName,subPump,port).pump.stopPump();
        setChanged();
        notifyObservers(NEW_STATUS_AVAILABLE);
    }

    public synchronized boolean disconnect(int index) throws Exception {
        boolean success = connectedSubPumps.getConnectedSubPump(index).pump.disconnect();
        String name = connectedSubPumps.getConnectedSubPump(index).name;
        String port = connectedSubPumps.getConnectedSubPump(index).port;
        if (success)
            connectedSubPumps.removePump(name,port);

        setChanged();
        notifyObservers(PUMP_DISCONNECTED);
        return success;
    }

    public String[] getAvailablePumpsList() {
        if (availablePumps.size() > 0) {
            return availablePumps.keySet().toArray(new String[availablePumps.size()]);
        }
        else return new String[]{NO_PUMP_AVAILABLE};
    }

    public ConnectedSubPumpsList getConnectedPumpsList() {
        return connectedSubPumps;
    }

    public synchronized boolean isConnected(int pumpIndex) {
        return connectedSubPumps != null && connectedSubPumps.getConnectedSubPump(pumpIndex).pump.isConnected();
    }

    public synchronized boolean isConnected(String pumpName, String port, boolean fullName) {
        boolean isIt = false;
        if (fullName) {
            for (String pump: getAllFullNames())
                if (pump.equals(pumpName)) {
                    isIt = true;
                    break;
                }
        }
        else isIt = connectedSubPumps != null && connectedSubPumps.getPump(pumpName, port).isConnected();

        return isIt;
    }

    public double[] getMaxMin(int pumpIndex, double diameter) throws PumpNotFoundException {
        String name = connectedSubPumps.getPumpName(pumpIndex);
        String port = connectedSubPumps.getPumpPort(pumpIndex);
        String subPump = connectedSubPumps.getPumpSubName(pumpIndex);
        return connectedSubPumps.getPump(name,port).getMaxMin(subPump,diameter);
    }

    private synchronized void getStatusFromPump(int pumpIndex) throws Exception {
        if (isConnected(pumpIndex))
            status = connectedSubPumps.getConnectedSubPump(pumpIndex).pump.getStatus();
    }

    public String getStatus() {
        return status;
    }

    public boolean anyPumpsConnected() {
        return connectedSubPumps.anyPumpsConnected();
    }

    public boolean noPumpsConnected() {
        return connectedSubPumps.noPumpsConnected();
    }

    public String[] getAllFullNames() {
        return connectedSubPumps.getAllFullNames();
    }

    public void updateReferenceRate(int pumpIndex, double[] newRate) {
        connectedSubPumps.getConnectedSubPump(pumpIndex).pump.updateReferenceRate(
                connectedSubPumps.getConnectedSubPump(pumpIndex).subPump,
                newRate);
    }

    @Override
    public void update(Observable o, Object arg) {
        status = (String) arg;
    }
}
