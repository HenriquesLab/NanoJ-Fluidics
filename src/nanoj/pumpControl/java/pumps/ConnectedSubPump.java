package nanoj.pumpControl.java.pumps;

public class ConnectedSubPump {
    public final String name;
    public final String port;
    public final String subPump;
    public final Pump pump;

    public ConnectedSubPump(Pump pump, String subPump) {
        this.name = pump.name;
        this.subPump = subPump;
        this.port = pump.portName;
        this.pump = pump;
    }

    public String getFullName() {
        return subPump + ", " + port;
    }

    public String[] asArray() {
        return new String[]{name,subPump,port};
    }
}
