package nanoj.pumpControl.java.pumps;

import java.util.ArrayList;
import java.util.Iterator;

public class ConnectedSubPumpsList implements Iterable<ConnectedSubPump> {
    private final ArrayList<ConnectedSubPump> list = new ArrayList<>();
    private final ArrayList<Pump> connectedPumps = new ArrayList<>();

    public boolean notPresent(int index) {
        return index >= list.size() || index < 0;
    }

    public void addPump(Pump pump) {
        for (String subPump: pump.subPumps)
            list.add(new ConnectedSubPump(pump, subPump));
        connectedPumps.add(pump);
    }

    public void removePump(String pumpName,String port) {
        ArrayList<ConnectedSubPump> pumpsToRemove = new ArrayList<>();
        for (ConnectedSubPump pump: list)
            if (pump.name.equals(pumpName) && pump.port.equals(port))
                pumpsToRemove.add(pump);

        for (ConnectedSubPump pumpToRemove: pumpsToRemove)
            list.remove(pumpToRemove);

        for (ConnectedSubPump pump: list)
            if (pump.name.equals(pumpName) && pump.port.equals(port)) {
                connectedPumps.remove(pump.pump);
                break;
            }
    }

    @SuppressWarnings("unused")
    public void removePump(Pump pump) {
        ArrayList<ConnectedSubPump> found = new ArrayList<>();
        for (ConnectedSubPump subPump: list)
            if (subPump.pump.equals(pump))
                found.add(subPump);

        for (ConnectedSubPump foundPump : found)
            list.remove(foundPump);

        connectedPumps.remove(found.get(0).pump);
    }

    public ConnectedSubPump getConnectedSubPump(String name, String subPump, String port) throws PumpNotFoundException {
        ConnectedSubPump result = null;
        for (ConnectedSubPump connectedSubPump : list)
            if (connectedSubPump.name.equals(name) &&
                connectedSubPump.subPump.equals(subPump) &&
                connectedSubPump.port.equals(port))
            {
                result = connectedSubPump;
                break;
            }

        if (result == null)
            throw new PumpNotFoundException();
        else return result;
    }

    public Pump getPump(String pumpName, String port) throws PumpNotFoundException {
        Pump pump = null;
        for (ConnectedSubPump subPump: list)
            if (subPump.name.equals(pumpName) && subPump.port.equals(port)) {
                pump = subPump.pump;
                break;
            }

        if (pump == null)
            throw new PumpNotFoundException();
        else return pump;
    }

    public boolean anyPumpsConnected() {
        return !list.isEmpty();
    }

    public boolean noPumpsConnected() {
        return list.isEmpty();
    }

    public ConnectedSubPump getConnectedSubPump(int index) throws PumpIndexNotFound {
        if (notPresent(index))
            throw new PumpIndexNotFound(index);
        return list.get(index);
    }

    @SuppressWarnings("unused")
    public String getFullName(int index) throws PumpIndexNotFound {
        if (!notPresent(index))
            throw new PumpIndexNotFound(index);
        return list.get(index).getFullName();
    }

    protected ArrayList<Pump> getAllConnectedPumps() {
        return connectedPumps;
    }

    public String[] getAllFullNames() {
        String[] array = new String[list.size()];
        if (list.isEmpty())
            return new String[]{};
        else {
            for (int i = 0; i < list.size(); i++) {
                array[i] = list.get(i).getFullName();
            }
            return array;
        }
    }

    public String getPumpName(int index) throws PumpIndexNotFound {
        if (notPresent(index))
            throw new PumpIndexNotFound(index);
        return list.get(index).name;
    }

    public String getPumpSubName(int index) throws PumpIndexNotFound {
        if (notPresent(index))
            throw new PumpIndexNotFound(index);
        return list.get(index).subPump;
    }

    public String getPumpPort(int index) throws PumpIndexNotFound {
        if (notPresent(index))
            throw new PumpIndexNotFound(index);
        return list.get(index).port;
    }

    public int size() {
        return list.size();
    }

    public ArrayList<String> connectedPorts() {
        ArrayList<String> ports = new ArrayList<>();
        for (ConnectedSubPump pump: list) {
            if (!ports.contains(pump.port))
                ports.add(pump.port);
        }
        return ports;
    }

    @Override
    public Iterator<ConnectedSubPump> iterator() {
        return list.iterator();
    }

    public static class PumpNotFoundException extends Exception {
        public PumpNotFoundException() { super();}
    }
    
    public static class PumpIndexNotFound extends Exception {
        PumpIndexNotFound(int index) {
            super("SubPump index " + index + " doesn't exist in list.");
        }
    }
}
