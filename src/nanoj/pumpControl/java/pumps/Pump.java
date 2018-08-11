package nanoj.pumpControl.java.pumps;

import javafx.beans.Observable;
import mmcorej.CMMCore;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

public abstract class Pump extends java.util.Observable implements PumpInterface {
    protected CMMCore core;
    protected String portName;
    protected String status = "Not connected.";
    protected double syringeDiameter;
    protected double targetVolume;
    protected double flowRate;
    protected long timeOut = 500;
    protected String unitsOfVolume = "ul";
    protected String unitsOfTime = "s";
    protected String unitsOfFlowRate = unitsOfVolume + "/" + unitsOfTime;
    protected boolean connected = false;
    protected String[] subPumps= null;
    protected int currentSubPump;
    protected String name;
    protected double[] referenceRates = new double[]{4,1,0.25};

    public enum Action {
        Infuse,
        Withdraw
    }

    public static final String[] SINGLE_PUMP = {"This is a single pump device."};
    public static final String FAILED_TO_CONNECT = "Failed to connect!";

    public void setCore(CMMCore givenCore) throws Exception { core = givenCore; }

    @Override
    public Pump getNewInstance() throws Exception {
        Class child = Class.forName(this.getClass().getName());
        return (Pump) child.newInstance();
    }

    @Override
    public boolean disconnect() throws Exception {
        /*
        if (!port.isOpened()) return true;
        if (port.closePort()) {
            connected = port.isOpened();
            return true;
        }
        else return false;
        */
        connected = false;
        core.unloadDevice(portName);
        return true;
    }

    @Override
    public String getPumpName() {
        return name;
    }

    @Override
    public void setSyringeDiameter(double diameter) throws Exception{
        syringeDiameter = diameter;
    }

    @Override
    public void stopAllPumps() throws Exception {
        stopPump();
    }

    @Override
    public void stopPump(int pumpIndex) throws Exception {
        stopPump();
    }

    @Override
    public double[] getMaxMin(double diameter) {
        double rat = diameter / referenceRates[0];
        BigDecimal ratio = new BigDecimal(Math.pow(rat,2));
        BigDecimal max = new BigDecimal(referenceRates[1]).multiply(ratio);
        BigDecimal min = new BigDecimal(referenceRates[2]).multiply(ratio);
        max = max.setScale(2, RoundingMode.HALF_UP);
        min = min.setScale(6, RoundingMode.HALF_UP);
        return new double[]{max.doubleValue(),min.doubleValue()};
    }

    @Override
    public boolean isConnected() { return connected; }

    @Override
    public String[] getSubPumps() {
        if(subPumps == null) return SINGLE_PUMP;
        else {
            return subPumps;
        }
    }

    public boolean setCurrentSubPump(String subPump) {
        int result = Arrays.binarySearch(subPumps, subPump);
        if (result > -1) {
            currentSubPump = result;
            return true;
        }
        else return false;
    }

    protected void setStatus(String text) {
        status = text;
        setChanged();
        notifyObservers(text);
    }

    public String getCurrentSubPump() { return subPumps[currentSubPump]; }

    public int getCurrentSubPumpIndex() { return currentSubPump; }

    public void setUnitsOfTime(String units) {
        unitsOfTime = units;
        unitsOfFlowRate = unitsOfVolume + "/" + unitsOfTime;
    };

    public String getUnitsOfTime() { return unitsOfTime; };

    public void setUnitsOfVolume(String units) {
        unitsOfVolume = units;
        unitsOfFlowRate = unitsOfVolume + "/" + unitsOfTime;
    };

    public String getUnitsOfVolume() { return unitsOfVolume; };

    public String getUnitsOfFlowRate() {return unitsOfFlowRate; }

    public long getTimeOut() { return timeOut; }

    public void setTimeOut(String timeOut) { this.timeOut = Long.parseLong(timeOut); }

    public double[] getReferenceRates() {
        return referenceRates;
    }

}
