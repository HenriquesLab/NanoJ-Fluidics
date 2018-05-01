package nanoj.pumpControl.java.pumps;

import mmcorej.CMMCore;
import java.util.*;

public class PumpManager extends Observable implements Runnable {
    private CMMCore serialManager;
    private LinkedHashMap<String, Pump> availablePumps = new LinkedHashMap<String, Pump>();
    private LinkedHashMap<String[], Pump> connectedPumps = new LinkedHashMap<String[], Pump>();
    private ArrayList<String[]> connectedPumpsArray = new ArrayList<String[]>();
    private ArrayList<String> connectedPorts = new ArrayList<String>();
    private String status = NO_PUMP_CONNECTED;
    private String[] currentPump = null;
    private boolean alive = true;

    private long wait = 100;  // Wait time for status checker in milliSeconds

    public static final String PORT_ALREADY_CONNECTED = "Port already in use.";
    public static final String NOT_AVAILABLE = "Pump not available to manager.";
    public static final String NEW_STATUS_AVAILABLE = "New status available.";
    public static final String NO_PUMP_CONNECTED = "No pump connected.";
    public static final String NEW_PUMP_CONNECTED = "New pump has been connected.";
    public static final String CURRENT_PUMP_CHANGED = "Changed the current pump.";
    public static final String NO_PUMP_AVAILABLE = "No Pump available!";
    public static final String VOLUME_UNITS = "ul";
    public static final String TIME_UNITS = "sec";
    public static final String FLOW_RATE_UNITS = VOLUME_UNITS + "/" + TIME_UNITS;
    public static final String INFUSE = "Infuse";
    public static final String WITHDRAW = "Withdraw";

    public final static PumpManager INSTANCE = new PumpManager();

    private PumpManager() {
    /*This no-argument constructor is required for the singleton design pattern.
      Remember to loadPlugins.
      loadPlugins is called after construction since the PumpManager class might be loaded in the class path and created
      before the application (in our case MicroManager) has loaded all plugin classes
      */
    }

    public void loadPlugins() {
        //Looks in the class path for all classes declaring themselves as services of the type "nanoj.(...).Pump"

        ServiceLoader<Pump> serviceLoader = ServiceLoader.load(Pump.class);
        for (Pump pump : serviceLoader) {
            availablePumps.put(pump.getPumpName(), pump);
        }
    }

    public void setCore(CMMCore core) throws Exception { serialManager = core; }

    public String connect(String pump, String port) throws Exception {
        if (connectedPorts.contains(port)) return PORT_ALREADY_CONNECTED;
        if (!availablePumps.containsKey(pump)) return NOT_AVAILABLE;

        Pump newPump = availablePumps.get(pump).getNewInstance();
        newPump.setCore(serialManager);
        String answer = newPump.connectToPump(port);
        String[] newSubPumps = newPump.getSubPumps();
        if (answer == Pump.FAILED_TO_CONNECT) {
            return Pump.FAILED_TO_CONNECT;
        }
        for (String subPump: newSubPumps) {
            String[] entry = new String[]{pump,subPump,port};
            connectedPumps.put(entry, newPump);
            connectedPumpsArray.add(entry);
        }
        connectedPorts.add(port);
        setChanged();
        notifyObservers(NEW_PUMP_CONNECTED);
        return answer;
    }

    public synchronized String startPumping(int syringe, double flowRate, double targetVolume, boolean infuse) throws Exception {
        String answer;
        String action;
        Pump pump = connectedPumps.get(currentPump);
        pump.setUnitsOfVolume(VOLUME_UNITS);
        pump.setUnitsOfTime(TIME_UNITS);
        pump.setCurrentSubPump(currentPump[1]);
        pump.setSyringeDiameter(SyringeList.getDiameter(syringe));
        pump.setFlowRate(flowRate);
        pump.setTargetVolume(targetVolume);
        pump.startPumping(infuse);
        if (infuse) action = INFUSE; else action = WITHDRAW;
        answer = action + " " + targetVolume+" "+pump.getUnitsOfVolume()+ " at " +flowRate+" "
                + pump.getUnitsOfFlowRate() + " with " + SyringeList.getVolumeWUnits(syringe) + " syringe.";
        return answer;
    }

    public synchronized String startPumping(int pumpIndex, int syringe, double flowRate,
                               double targetVolume, boolean infuse) throws Exception {
        String[] pumpKey = connectedPumpsArray.get(pumpIndex);
        String action;

        Pump pump = connectedPumps.get(pumpKey);
        pump.setUnitsOfVolume(VOLUME_UNITS);
        pump.setUnitsOfTime(TIME_UNITS);
        pump.setCurrentSubPump(pumpKey[1]);
        pump.setSyringeDiameter(SyringeList.getDiameter(syringe));
        pump.setFlowRate(flowRate);
        pump.setTargetVolume(targetVolume);

        pump.startPumping(infuse);

        if (infuse) action = INFUSE; else action = WITHDRAW;

        return action + " " + targetVolume+" "+pump.getUnitsOfVolume()+ " at " +flowRate+" "
                + pump.getUnitsOfFlowRate() + " with " + SyringeList.getVolumeWUnits(syringe) + " syringe.";
    }

    public synchronized boolean stopPumping() throws Exception {
        connectedPumps.get(currentPump).stopPump();
        return true;
    }

    public synchronized boolean stopPumping(String[] pump) throws Exception {
        connectedPumps.get(pump).stopPump();
        return true;
    }

    public synchronized  boolean disconnect(int zePump) throws Exception {
        boolean success;
        String[] pump = connectedPumpsArray.get(zePump);
        ArrayList<String[]> keysToRemove = new ArrayList<String[]>();
        success = connectedPumps.get(pump).disconnect();
        if (success) {
            for (String[] currentKey: connectedPumpsArray) {
                if (currentKey[0].equals(pump[0]) && currentKey[2].equals(pump[2])) {
                    keysToRemove.add(currentKey);
                }
            }
            //The actual removal is done on a later loop to prevent concurrency issues.
            for (String[] key: keysToRemove) {
                connectedPumps.remove(key);
                connectedPumpsArray.remove(key);
            }
            connectedPorts.remove(pump[2]);
        }
        return success;
    }

    public String[] getAvailablePumpsList() {
        if (availablePumps.size() > 0) {
            return availablePumps.keySet().toArray(new String[availablePumps.size()]);
        }
        else return new String[]{NO_PUMP_AVAILABLE};
    }

    public String[][] getConnectedPumpsList() {
        if (connectedPumpsArray.size() == 0) return new String[][]{{NO_PUMP_CONNECTED,"",""}};
        String[][] result = new String[connectedPumpsArray.size()][];
        for (int e=0; e<connectedPumpsArray.size(); e++) {
            result[e] = connectedPumpsArray.get(e);
        }
        return result;
    }

    public String getStatus() { return status; }

    public synchronized boolean isConnected() {
        return currentPump != null && connectedPumps != null && connectedPumps.get(currentPump).isConnected();
    }

    public synchronized boolean isConnected(String[] pump) {
        return connectedPumps != null && connectedPumps.get(pump).isConnected();
    }

    public void setCurrentPump(int index) {
        if (connectedPumpsArray.size() == 0) currentPump = null;
        else currentPump = connectedPumpsArray.get(index);
        setChanged();
        notifyObservers(CURRENT_PUMP_CHANGED);
    }

    public String[] getCurrentPump() { return currentPump; }

    public double[] getMaxMin(int pump, double diameter) {
        if (connectedPumps.size() > 0) {
            String[] pumpKey = connectedPumpsArray.get(pump);
            return connectedPumps.get(pumpKey).getMaxMin(diameter);
        }
        else return null;
    }

    public double[] getMaxMin(String pump, double diameter) {
        if (availablePumps.size() > 0) return availablePumps.get(pump).getMaxMin(diameter);
        else return new double[]{0,1};
    }

    private synchronized void getStatusFromPump() throws Exception {
        status = connectedPumps.get(currentPump).getStatus();
    }

    @Override
    public void run() {
        while (alive) {
            try {
                if (currentPump == null || connectedPumps.size() == 0)
                    status = NO_PUMP_CONNECTED;
                else {
                    wait = connectedPumps.get(currentPump).getTimeOut();
                    getStatusFromPump();
                    setChanged();
                    notifyObservers(NEW_STATUS_AVAILABLE);
                }
                Thread.sleep(wait);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
