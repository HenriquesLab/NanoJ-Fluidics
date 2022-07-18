package nanoj.pumpControl.java.pumps;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;

public abstract class Pump extends java.util.Observable implements PumpInterface {
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
    protected String currentSubPump;
    protected String name;
    protected final LinkedHashMap<String, double[]> referenceRates = new LinkedHashMap<>();
    protected double[] defaultRate = new double[]{1,1,1};

    public enum Action {
        Infuse,
        Withdraw
    }

    public static final String FAILED_TO_CONNECT = "Failed to connect!";

    @Override
    public Pump getNewInstance() throws Exception {
        Class<?> child = Class.forName(this.getClass().getName());
        return (Pump) child.newInstance();
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
    public void stopPump(String pump) throws Exception {
        stopPump();
    }

    @Override
    public double[] getMaxMin(String subPump, double diameter) {
        double[] referenceRate = referenceRates.get(subPump);
        double rat = diameter / referenceRate[0];
        BigDecimal ratio = BigDecimal.valueOf(Math.pow(rat, 2));
        BigDecimal max = BigDecimal.valueOf(referenceRate[1]).multiply(ratio);
        BigDecimal min = BigDecimal.valueOf(referenceRate[2]).multiply(ratio);
        max = max.setScale(2, RoundingMode.HALF_UP);
        min = min.setScale(6, RoundingMode.HALF_UP);
        return new double[]{max.doubleValue(),min.doubleValue()};
    }

    public double[] getDefaultRate() {
        return defaultRate;
    }

    public void setCurrentSubPump(String subPump) {
        currentSubPump = subPump;
    }

    protected void setStatus(String text) {
        status = text;
        setChanged();
        notifyObservers(text);
    }

    @Override
    public String getStatus() throws Exception {
        return status;
    }

    public String getCurrentSubPump() { return currentSubPump; }

    public void setUnitsOfTime(String units) {
        unitsOfTime = units;
        unitsOfFlowRate = unitsOfVolume + "/" + unitsOfTime;
    }

    @SuppressWarnings("unused")
    public String getUnitsOfTime() { return unitsOfTime; }

    public void setUnitsOfVolume(String units) {
        unitsOfVolume = units;
        unitsOfFlowRate = unitsOfVolume + "/" + unitsOfTime;
    }

    public String getUnitsOfVolume() { return unitsOfVolume; }

    public String getUnitsOfFlowRate() {return unitsOfFlowRate; }

    @SuppressWarnings("unused")
    public long getTimeOut() { return timeOut; }

    @SuppressWarnings("unused")
    public void setTimeOut(String timeOut) { this.timeOut = Long.parseLong(timeOut); }

    @SuppressWarnings("unused")
    public synchronized double[] getReferenceRate(String subPump) {
        return referenceRates.get(subPump);
    }

    public synchronized void updateReferenceRate(String subPump, double[] newRate) {
        referenceRates.put(subPump,newRate);
    }

    public String getName() {
        return name;
    }
}
