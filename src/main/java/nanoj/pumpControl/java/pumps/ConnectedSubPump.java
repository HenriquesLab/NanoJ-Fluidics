package nanoj.pumpControl.java.pumps;

public class ConnectedSubPump {
    public final Pump pump;
    public final String name;
    public final String port;
    public final String subPump;

    public ConnectedSubPump(Pump pump, String subPump) {
        this.pump = pump;
        this.name = pump.name;
        this.port = pump.portName;
        this.subPump = subPump;
    }

    public String getFullName() {
        return subPump + ", " + port;
    }

    public String[] asConnectionArray() {
        return new String[]{name,subPump,port};
    }

    public String[] asCalibrationArray() {
        return new String[]{name,subPump,port,
                ""+pump.referenceRates.get(subPump)[0],
                ""+pump.referenceRates.get(subPump)[1],
                ""+pump.referenceRates.get(subPump)[2]};
    }
}
